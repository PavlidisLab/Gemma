package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.*;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Reads single-cell vectors from the <a href="https://anndata.readthedocs.io/en/latest/fileformat-prose.html">AnnData on-disk HDF5 format</a>.
 *
 * @author poirigui
 */
@CommonsLog
@Setter
public class AnnDataSingleCellDataLoader implements SingleCellDataLoader {

    private static final Comparator<String> sampleNameComparator = String.CASE_INSENSITIVE_ORDER;

    /**
     * Path to the HDF5 file.
     */
    private final Path file;

    private boolean ignoreUnmatchedSamples = true;

    private boolean ignoreUnmatchedDesignElements = true;

    /**
     * The name of the sample factor under {@code /var}.
     */
    private String sampleFactorName;

    /**
     * The name of the cell type factor under {@code /var}.
     */
    @Nullable
    private String cellTypeFactorName;

    /**
     * An indicator for unknown cell type if the dataset uses something else than the {@code -1} code.
     */
    @Nullable
    private String unknownCellTypeIndicator;

    public AnnDataSingleCellDataLoader( Path file ) {
        this.file = file;
    }

    @Override
    public void setIgnoreUnmatchedSamples( boolean ignoreUnmatchedSamples ) {
        this.ignoreUnmatchedSamples = ignoreUnmatchedSamples;
    }

    @Override
    public void setIgnoreUnmatchedDesignElements( boolean ignoreUnmatchedDesignElements ) {
        this.ignoreUnmatchedDesignElements = ignoreUnmatchedDesignElements;
    }

    @Override
    public Set<String> getSampleNames() {
        String[] sampleNames;
        try ( H5File h5File = openFile(); H5Group v = h5File.getGroup( "var" ) ) {
            sampleNames = loadCategoricalColumnFromDataframe( v, sampleFactorName );
        }
        return new HashSet<>( Arrays.asList( sampleNames ) );
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) {
        Assert.notNull( sampleFactorName, "The sample factor name must be set." );
        SingleCellDimension singleCellDimension = new SingleCellDimension();
        try ( H5File h5File = openFile() ) {
            String[] sampleNames;
            try ( H5Group v = h5File.getGroup( "var" ) ) {
                sampleNames = loadCategoricalColumnFromDataframe( v, sampleFactorName );
            }
            // ensure that samples are properly grouped, they do not have to be in any particular order
            Set<String> previouslySeenGroups = new HashSet<>();
            String currentGroup = null;
            for ( int i = 0; i < sampleNames.length; i++ ) {
                String sampleName = sampleNames[i];
                if ( previouslySeenGroups.contains( sampleName ) ) {
                    throw new IllegalArgumentException( String.format( "The cell at position %d is not grouped with cells from %s. You have to sort the AnnData matrix by sample.", i, sampleName ) );
                }
                if ( !sampleName.equals( currentGroup ) ) {
                    if ( currentGroup != null ) {
                        previouslySeenGroups.add( currentGroup );
                    }
                    currentGroup = sampleName;
                }
            }
            String currentSampleName = null;
            BioAssay currentSample;
            Set<String> unmatchedSamples = new TreeSet<>();
            for ( int i = 0; i < sampleNames.length; i++ ) {
                String sampleName = sampleNames[i];
                if ( !sampleName.equals( currentSampleName ) ) {
                    // lookup the sample
                    List<BioAssay> match = bioAssays.stream()
                            .filter( ba -> sampleNameComparator.compare( ba.getSampleUsed().getName(), sampleName ) == 0 )
                            .collect( Collectors.toList() );
                    if ( match.size() == 1 ) {
                        currentSampleName = sampleName;
                        currentSample = match.get( 0 );
                        singleCellDimension.getBioAssays().add( currentSample );
                        // FIXME: this is utterly inefficient
                        singleCellDimension.setBioAssaysOffset( ArrayUtils.add( singleCellDimension.getBioAssaysOffset(), i ) );
                    } else if ( match.size() > 1 ) {
                        throw new IllegalStateException( String.format( "There is more than one BioAssay matching %s.", sampleName ) );
                    } else {
                        unmatchedSamples.add( sampleName );
                    }
                }
            }
            if ( !unmatchedSamples.isEmpty() ) {
                String msg = "No BioAssays match the following samples: " + String.join( ", ", unmatchedSamples );
                if ( ignoreUnmatchedSamples ) {
                    log.warn( msg );
                } else {
                    throw new IllegalStateException( msg );
                }
            }
            try ( H5Group v = h5File.getGroup( "var" ) ) {
                String[] cellIds = loadStringIndexFromDataframe( v );
                singleCellDimension.setCellIds( Arrays.asList( cellIds ) );
                singleCellDimension.setNumberOfCells( cellIds.length );
            }
        }
        return singleCellDimension;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() {
        Set<QuantitationType> qts = new HashSet<>();
        try ( H5File h5File = openFile() ) {
            if ( h5File.exists( "X" ) ) {
                qts.add( createQt( h5File, "X" ) );
            }
            if ( h5File.exists( "layers" ) ) {
                for ( String layer : h5File.getChildren( "layers" ) ) {
                    qts.add( createQt( h5File, "layers/" + layer ) );
                }
            }
        }
        return qts;
    }

    private QuantitationType createQt( H5File h5File, String path ) {
        String et = h5File.getStringAttribute( path, "encoding-type" );
        H5Dataset dataset;
        if ( et == null ) {
            throw new IllegalArgumentException();
        } else if ( et.equals( "csr_matrix" ) ) {
            dataset = h5File.getDataset( path + "/data" );
        } else if ( et.equals( "dense" ) ) {
            dataset = h5File.getDataset( path );
        } else {
            throw new UnsupportedOperationException( "Loading single-cell data from " + et + " is not supported." );
        }
        QuantitationType qt = new QuantitationType();
        qt.setName( path );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        try ( H5Type datasetType = dataset.getType() ) {
            if ( datasetType.getFundamentalType().equals( H5FundamentalType.INTEGER ) ) {
                qt.setType( StandardQuantitationType.COUNT );
                qt.setScale( ScaleType.COUNT );
            } else if ( datasetType.getFundamentalType().equals( H5FundamentalType.FLOAT ) ) {
                qt.setType( StandardQuantitationType.AMOUNT );
                // FIXME: infer scale from data using the logic from ExpressionDataDoubleMatrixUtil.inferQuantitationType()
                log.warn( "Scale type cannot be detected for non-counting data." );
                qt.setScale( ScaleType.OTHER );
            } else {
                throw new IllegalArgumentException( "Unsupported datatype " + datasetType );
            }
        }
        qt.setRepresentation( PrimitiveType.DOUBLE );
        log.info( "Detected quantitation type for '" + path + "': " + qt );
        return qt;
    }

    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment() {
        if ( cellTypeFactorName == null ) {
            log.warn( "No cell type factor name was set. Cell type assignment will not be loaded. Use getFactors() to identify a suitable candidate." );
            return Optional.empty();
        }
        try ( H5File h5File = openFile() ) {
            H5Group group = h5File.getGroup( "var/" + cellTypeFactorName );
            String et = group.getStringAttribute( "encoding-type" );
            Assert.notNull( et, "The cell type factor must have an 'encoding-type' attribute set." );
            Assert.isTrue( et.equals( "categorical" ), "The cell type factor must be categorical; factor encoding is " + et );
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
                    // rewrite unknown codes
                    for ( int i = 0; i < vec.length; i++ ) {
                        if ( vec[i] == unknownCellTypeCode ) {
                            vec[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                        }
                    }
                }
                assignment.setCellTypeIndices( vec );
            }
            return Optional.of( assignment );
        }
    }

    @Override
    public Set<ExperimentalFactor> getFactors() throws IOException {
        Set<ExperimentalFactor> factors = new HashSet<>();
        try ( H5File h5File = openFile() ) {
            for ( String g : h5File.getChildren( "var" ) ) {
                boolean isCategorical = Objects.equals( h5File.getStringAttribute( "var/" + g, "encoding-type" ), "categorical" );
                ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( g, isCategorical ? FactorType.CATEGORICAL : FactorType.CONTINUOUS );
                Characteristic c = Characteristic.Factory.newInstance( g, null );
                factor.setCategory( c );
                if ( isCategorical ) {
                    try ( H5Dataset categories = h5File.getDataset( "var/" + g + "/categories" ) ) {
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
    public Set<String> getGenes() throws IOException {
        try ( H5File h5File = openFile(); H5Group obs = h5File.getGroup( "obs" ) ) {
            return new HashSet<>( Arrays.asList( loadStringIndexFromDataframe( obs ) ) );
        }
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors( Map<String, CompositeSequence> elementsMapping, SingleCellDimension dimension, QuantitationType quantitationType ) {
        Assert.isTrue( quantitationType.getName().equals( "X" ) || quantitationType.getName().startsWith( "layers/" ),
                "The name of the quantitation must refer to a valid path in the HDF5 file." );
        H5File h5File = openFile();
        try {
            // load genes
            String[] genes;
            try ( H5Group obs = h5File.getGroup( "obs" ) ) {
                genes = loadStringIndexFromDataframe( obs );
            }
            Set<String> genesSet = new HashSet<>( Arrays.asList( genes ) );
            if ( !CollectionUtils.containsAny( genesSet, elementsMapping.keySet() ) ) {
                throw new IllegalArgumentException( "None of the genes in the HDF5 file are present in the elements mapping." );
            }
            Set<String> unknownGenes = SetUtils.difference( genesSet, elementsMapping.keySet() );
            if ( !unknownGenes.isEmpty() ) {
                String msg;
                if ( unknownGenes.size() > 10 ) {
                    msg = String.format( "%d/%d are not present in the elements mapping.", unknownGenes.size(), genes.length );
                } else {
                    msg = "The following genes are not present in the elements mapping: " + unknownGenes.stream().sorted().collect( Collectors.joining( ", " ) );
                }
                if ( ignoreUnmatchedDesignElements ) {
                    log.warn( msg );
                } else {
                    throw new IllegalArgumentException( msg );
                }
            }
            // load the sample layout (necessary for slicing the necessary samples)
            String[] samples;
            try ( H5Group v = h5File.getGroup( "var" ) ) {
                samples = loadCategoricalColumnFromDataframe( v, sampleFactorName );
            }
            String path = quantitationType.getName();
            if ( !h5File.exists( path ) ) {
                throw new IllegalArgumentException( "No such path in the HDF5 file: " + path );
            }
            String matrixEncodingType = h5File.getStringAttribute( path, "encoding-type" );
            if ( matrixEncodingType == null ) {
                throw new IllegalArgumentException( "The '" + path + "' location does not have an encoding-type attribute." );
            }
            if ( matrixEncodingType.equals( "csr_matrix" ) ) {
                return loadVectorsFromSparseMatrix( h5File.getGroup( path ), samples, genes, quantitationType, dimension, elementsMapping )
                        .onClose( h5File::close );
            } else {
                throw new UnsupportedOperationException( "Loading single-cell data from " + matrixEncodingType + " is not supported." );
            }
        } catch ( Throwable e ) {
            h5File.close();
            throw e;
        }
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

    private String[] loadStringIndexFromDataframe( H5Group df ) {
        Assert.isTrue( Objects.equals( df.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        String indexColumn = df.getStringAttribute( "_index" );
        Assert.notNull( indexColumn );
        Assert.isTrue( df.exists( indexColumn ) );
        return df.getDataset( indexColumn ).toStringVector();
    }

    private String[] loadCategoricalColumnFromDataframe( H5Group df, String columnName ) {
        Assert.isTrue( Objects.equals( df.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        try ( H5Group dataset = df.getGroup( columnName ) ) {
            String[] categories = dataset.getDataset( "categories" ).toStringVector();
            int[] codes = dataset.getDataset( "codes" ).toIntegerVector();
            return Arrays.stream( codes ).mapToObj( i -> i != -1 ? categories[i] : null ).toArray( String[]::new );
        }
    }

    private Stream<SingleCellExpressionDataVector> loadVectorsFromSparseMatrix( H5Group X, String[] samples, String[] genes, QuantitationType qt, SingleCellDimension scd, Map<String, CompositeSequence> elementsMapping ) {
        int[] shape = X.getAttribute( "shape" )
                .map( H5Attribute::toIntegerVector )
                .orElseThrow( () -> new IllegalStateException( "The sparse matrix does not have a shape attribute." ) );
        Assert.isTrue( genes.length == shape[0],
                "The number of supplied genes does not match the number of rows in the sparse matrix." );
        Assert.isTrue( scd.getCellIds().size() == shape[1],
                "The number of cells in the dimension does not match the number of columns in the sparse matrix." );
        // build a sample offset index for efficiently selecting samples
        String[] sampleName = new String[0];
        int[] sampleOffset = new int[0];
        String currentSample = null;
        for ( int i = 0; i < samples.length; i++ ) {
            String sample = samples[i];
            if ( !sample.equals( currentSample ) ) {
                currentSample = sample;
                sampleName = ArrayUtils.add( sampleName, sample );
                sampleOffset = ArrayUtils.add( sampleOffset, i );
            }
        }
        int[] finalSampleOffset = sampleOffset;
        int[] intptr;
        try ( H5Dataset indptr = X.getDataset( "indptr" ) ) {
            intptr = indptr.toIntegerVector();
        }
        assert intptr.length == shape[0] + 1;
        H5Dataset data = X.getDataset( "data" );
        H5Dataset indices = X.getDataset( "indices" );
        String[] finalSampleName = sampleName;
        return IntStream.range( 0, shape[0] )
                // ignore entries that are missing a gene mapping
                .filter( i -> elementsMapping.containsKey( genes[i] ) )
                .mapToObj( i -> {
                    assert intptr[i] < intptr[i + 1];
                    SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
                    vector.setDesignElement( elementsMapping.get( genes[i] ) );
                    vector.setSingleCellDimension( scd );
                    vector.setQuantitationType( qt );
                    // simple case: the number of BAs match the number of samples
                    if ( scd.getBioAssays().size() == finalSampleOffset.length ) {
                        // this is using the same storage strategy from ByteArrayConverter
                        vector.setData( data.slice( intptr[i], intptr[i + 1] ).toByteVector( H5Type.IEEE_F64BE ) );
                        vector.setDataIndices( indices.slice( intptr[i], intptr[i + 1] ).toIntegerVector() );
                    } else {
                        log.info( "Slicing relevant samples..." );
                        int[] IX = indices.slice( intptr[i], intptr[i + 1] ).toIntegerVector();
                        // compute the vector length
                        int nnz = 0;
                        for ( BioAssay ba : scd.getBioAssays() ) {
                            int z = ArrayUtils.indexOf( finalSampleName, ba );
                            nnz += finalSampleOffset[z + 1] - finalSampleOffset[z];
                        }
                        byte[] vectorData = new byte[8 * nnz];
                        int[] vectorIndices = new int[nnz];
                        // select indices relevant to BAs
                        nnz = 0;
                        for ( int j = 0; j < finalSampleOffset.length; j++ ) {
                            // find the first position where the sample occurs (in term of insertion point)
                            int start = Arrays.binarySearch( IX, j ) - 2;
                            if ( start < 0 ) {
                                start = -start - 2;
                            }
                            int end;
                            // find the ending poisition where the sample occurs
                            if ( j < finalSampleOffset.length - 1 ) {
                                end = Arrays.binarySearch( IX, j + 1 ) - 2;
                                if ( end < 0 ) {
                                    end = -end - 2;
                                }
                            } else {
                                end = IX.length;
                            }
                            int sampleNnz = end - start;
                            byte[] w = data.slice( start, end ).toByteVector( H5Type.IEEE_F64BE );
                            System.arraycopy( w, start, vectorData, 8 * nnz, sampleNnz );
                            System.arraycopy( IX, start, vectorIndices, nnz, sampleNnz );
                            // adjust indices to be relative to the BA offset
                            for ( int k = nnz; k < nnz + sampleNnz; k++ ) {
                                vectorIndices[k] = vectorData[k] - finalSampleOffset[j] + scd.getBioAssaysOffset()[j];
                            }
                            nnz += sampleNnz;
                        }
                        vector.setData( vectorData );
                        vector.setDataIndices( vectorIndices );
                    }
                    return vector;
                } )
                .onClose( indices::close )
                .onClose( data::close )
                .onClose( X::close );
    }
}
