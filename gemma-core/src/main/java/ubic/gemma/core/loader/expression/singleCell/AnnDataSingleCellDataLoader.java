package ubic.gemma.core.loader.expression.singleCell;

import hdf.hdf5lib.HDF5Constants;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.loader.util.hdf5.H5Group;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.Statement;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * Reads single-cell vectors from the <a href="https://anndata.readthedocs.io/en/latest/fileformat-prose.html">AnnData on-disk HDF5 format</a>.
 *
 * @author poirigui
 */
@CommonsLog
@Setter
public class AnnDataSingleCellDataLoader implements SingleCellDataLoader {

    /**
     * Path to the HDF5 file.
     */
    private final Path file;

    /**
     * An indicator for unknown cell type if the dataset uses something else than the {@code -1} code.
     */
    @Nullable
    private String unknownCellTypeIndicator;

    public AnnDataSingleCellDataLoader( Path file ) {
        this.file = file;
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) {
        return new SingleCellDimension();
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() {
        return Collections.singleton( new QuantitationType() );
    }

    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment() {
        try ( H5File h5File = openFile() ) {
            return h5File.getChildren( "obs" ).stream()
                    // find a cell type indicator
                    .filter( s -> s.toLowerCase().startsWith( "celltype" )
                            && Objects.equals( h5File.getStringAttribute( "obs/" + s, "encoding-type" ), "categorical" ) )
                    .map( s -> h5File.getGroup( "obs/" + s ) )
                    .findFirst()
                    .map( group -> {
                        CellTypeAssignment assignment = new CellTypeAssignment();
                        int unknownCellTypeCode = -1;
                        try ( H5Dataset categories = group.getDataset( "categories" ) ) {
                            String[] cellTypes = categories.toStringVector();
                            for ( int i = 0; i < cellTypes.length; i++ ) {
                                String ct = cellTypes[i];
                                if ( ct.equals( unknownCellTypeIndicator ) ) {
                                    if ( unknownCellTypeCode != -1 ) {
                                        throw new IllegalStateException( "There is not than one unknown cell type indicator." );
                                    }
                                    log.info( "Dataset uses a special indicator for unknown cell types: " + ct + " with code: " + i + ", its occurrences will be replaced with -1." );
                                    unknownCellTypeCode = i;
                                    continue;
                                }
                                assignment.getCellTypes().add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, ct, null ) );
                            }
                            if ( unknownCellTypeIndicator != null && unknownCellTypeCode == -1 ) {
                                throw new IllegalStateException( "The unknown cell type indicator %s was not found." );
                            }
                            assignment.setNumberOfCellTypes( assignment.getCellTypes().size() );
                        }
                        try ( H5Dataset codes = group.getDataset( "codes" ) ) {
                            int[] vec = codes.toIntegerVector();
                            if ( unknownCellTypeCode != -1 ) {
                                log.info( "Rewriting unknown cell type codes..." );
                                // rewrite unknown codes
                                for ( int i = 0; i < vec.length; i++ ) {
                                    if ( vec[i] == unknownCellTypeCode ) {
                                        vec[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                                    }
                                }
                            }
                            assignment.setCellTypeIndices( vec );
                        }
                        return assignment;
                    } );
        }
    }

    @Override
    public Set<ExperimentalFactor> getFactors() throws IOException {
        Set<ExperimentalFactor> factors = new HashSet<>();
        try ( H5File h5File = openFile() ) {
            for ( String g : h5File.getChildren( "obs" ) ) {
                boolean isCategorical = Objects.equals( h5File.getStringAttribute( "obs/" + g, "encoding-type" ), "categorical" );
                ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( g, isCategorical ? FactorType.CATEGORICAL : FactorType.CONTINUOUS );
                Characteristic c = Characteristic.Factory.newInstance( g, null );
                factor.setCategory( c );
                if ( isCategorical ) {
                    try ( H5Dataset categories = h5File.getDataset( "obs/" + g + "/categories" ) ) {
                        // popuate FVs
                        for ( String fv : categories.toStringVector() ) {
                            FactorValue fvO = FactorValue.Factory.newInstance( factor );
                            fvO.getCharacteristics().add( Statement.Factory.newInstance( c.getCategory(), c.getCategoryUri(), fv, null ) );
                            fvO.setValue( fv );
                            factor.getFactorValues().add( fvO );
                        }
                    }
                }
                factors.add( factor );
            }
        }
        return factors;
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) {
        H5File h5File = openFile();
        Stream<SingleCellExpressionDataVector> stream;
        try {
            String matrixEncodingType = h5File.getStringAttribute( "X", "encoding-type" );
            if ( matrixEncodingType == null ) {
                throw new IllegalArgumentException( "The 'X' location does not have an encoding-type attribute." );
            }
            if ( matrixEncodingType.equals( "csr_matrix" ) ) {
                stream = loadVectorsFromSparseMatrix( h5File.getGroup( "X" ) );
            } else if ( matrixEncodingType.equals( "array" ) ) {
                stream = loadVectorsFromDenseMatrix( h5File.getDataset( "X" ) );
            } else {
                throw new UnsupportedOperationException( "Loading single-cell data from " + matrixEncodingType + " is not supported." );
            }
        } catch ( Throwable e ) {
            h5File.close();
            throw e;
        }
        return stream.onClose( h5File::close );
    }

    private H5File openFile() {
        H5File h5File = H5File.open( file );
        String encodingType = h5File.getStringAttribute( "encoding-type" );
        if ( !Objects.equals( encodingType, "anndata" ) ) {
            h5File.close();
            throw new IllegalArgumentException( "The HDF5 file does not have its 'encoding-type' set to 'anndata'." );
        }
        String encodingVersion = h5File.getStringAttribute( "encoding-version" );
        if ( encodingVersion == null ) {
            h5File.close();
            throw new IllegalArgumentException( "The HDF5 file does not have an 'encoding-version' attribute set." );
        }
        return h5File;
    }

    private Stream<SingleCellExpressionDataVector> loadVectorsFromSparseMatrix( H5Group X ) {
        int[] shape = requireNonNull( X.getAttribute( "shape" ) ).toIntegerVector();
        int[] intptr;
        try ( H5Dataset indptr = X.getDataset( "indptr" ) ) {
            intptr = indptr.toIntegerVector();
        }
        assert intptr.length == shape[0] + 1;
        H5Dataset data = X.getDataset( "data" );
        H5Dataset indices = X.getDataset( "indices" );
        return IntStream.range( 0, shape[0] )
                .mapToObj( i -> {
                    assert intptr[i] < intptr[i + 1];
                    SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
                    // this is using the same storage strategy from ByteArrayConverter
                    System.out.printf( "slicing data from %d to %d...%n", intptr[i], intptr[i + 1] );
                    vector.setData( data.slice( intptr[i], intptr[i + 1] ).toByteVector( HDF5Constants.H5T_IEEE_F64BE ) );
                    System.out.printf( "slicing indices from %d to %d...%n", intptr[i], intptr[i + 1] );
                    vector.setDataIndices( indices.slice( intptr[i], intptr[i + 1] ).toIntegerVector() );
                    return vector;
                } )
                .onClose( indices::close )
                .onClose( data::close )
                .onClose( X::close );
    }

    private Stream<SingleCellExpressionDataVector> loadVectorsFromDenseMatrix( H5Dataset X ) {
        X.close();
        throw new UnsupportedOperationException( "Loading single-cell data from dense matrices is not supported." );
    }
}
