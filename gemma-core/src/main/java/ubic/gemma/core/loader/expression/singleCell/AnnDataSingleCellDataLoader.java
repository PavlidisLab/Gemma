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
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
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

    private BioAssayToSampleNameMatcher sampleNameComparator;
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
    public void setBioAssayToSampleNameMatcher( BioAssayToSampleNameMatcher bioAssayToSampleNameMatcher ) {
        this.sampleNameComparator = bioAssayToSampleNameMatcher;
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
    public Set<String> getSampleNames() throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
        CategoricalArray sampleNames;
        try ( H5File h5File = openFile(); H5Group v = h5File.getGroup( "var" ) ) {
            sampleNames = loadCategoricalColumnFromDataframe( v, sampleFactorName );
        }
        return new HashSet<>( Arrays.asList( sampleNames.categories ) );
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        Assert.isTrue( !bioAssays.isEmpty(), "At least one bioassay must be provided" );
        Assert.notNull( sampleNameComparator, "A sample name comparator is necessary to match samples to BioMaterials." );
        Assert.notNull( sampleFactorName, "The sample factor name must be set." );
        SingleCellDimension singleCellDimension = new SingleCellDimension();
        try ( H5File h5File = openFile() ) {
            String[] cellIds;
            try ( H5Group v = h5File.getGroup( "var" ) ) {
                cellIds = loadStringIndexFromDataframe( v );
            }
            CategoricalArray sampleNames;
            try ( H5Group v = h5File.getGroup( "var" ) ) {
                sampleNames = loadCategoricalColumnFromDataframe( v, sampleFactorName );
            }
            // ensure that samples are properly grouped, they do not have to be in any particular order
            Set<String> previouslySeenGroups = new HashSet<>();
            String currentGroup = null;
            for ( int i = 0; i < sampleNames.size(); i++ ) {
                String sampleName = requireNonNull( sampleNames.get( i ), "Sample name cannot be missing." );
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
            List<String> cellIdsL = new ArrayList<>();
            List<BioAssay> bas = new ArrayList<>();
            int[] basO = new int[0];
            int j = 0;
            for ( int i = 0; i < sampleNames.size(); i++ ) {
                String sampleName = requireNonNull( sampleNames.get( i ), "Sample name cannot be missing." );
                if ( !sampleName.equals( currentSampleName ) ) {
                    Optional<BioAssay> result = getBioAssayBySampleName( bioAssays, sampleName );
                    if ( result.isPresent() ) {
                        currentSampleName = sampleName;
                        currentSample = result.get();
                        bas.add( currentSample );
                        basO = ArrayUtils.add( basO, j );
                    } else {
                        unmatchedSamples.add( sampleName );
                        // skip until next sample
                        // the i++ of the for-loop will position the index at the next sample
                        for ( ; i < sampleNames.size() - 1; i++ ) {
                            if ( !sampleName.equals( sampleNames.get( i + 1 ) ) ) {
                                break;
                            }
                        }
                        continue;
                    }
                }
                cellIdsL.add( cellIds[i] );
                j++;
            }
            if ( bioAssays.isEmpty() ) {
                throw new IllegalArgumentException( "No samples were matched." );
            }
            if ( !unmatchedSamples.isEmpty() ) {
                String msg = "No BioAssays match the following samples: " + String.join( ", ", unmatchedSamples );
                if ( ignoreUnmatchedSamples ) {
                    log.warn( msg );
                } else {
                    throw new IllegalStateException( msg );
                }
            }
            singleCellDimension.setBioAssays( bas );
            singleCellDimension.setBioAssaysOffset( basO );
            singleCellDimension.setCellIds( cellIdsL );
            singleCellDimension.setNumberOfCells( cellIdsL.size() );
        }
        return singleCellDimension;
    }

    @Override
    public Set<QuantitationType> getQuantitationTypes() throws IOException {
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
        } else if ( et.equals( "csr_matrix" ) || et.equals( "csc_matrix" ) ) {
            // CSC is wrong for loading vectors, but the data type is suitable for detection
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
                qt.setScale( detectScale( h5File, path ) );
            } else {
                throw new IllegalArgumentException( "Unsupported datatype " + datasetType );
            }
        }
        qt.setRepresentation( PrimitiveType.DOUBLE );
        log.info( "Detected quantitation type for '" + path + "': " + qt );
        return qt;
    }

    private ScaleType detectScale( H5File h5File, String path ) {
        if ( "X".equals( path ) && "dict".equals( h5File.getStringAttribute( "uns", "encoding-type" ) ) ) {
            try ( H5Group uns = h5File.getGroup( "uns" ) ) {
                if ( uns.getChildren().contains( "log1p" ) ) {
                    log.info( h5File.getPath().getFileName() + " contains unstructured data describing the scale type: " + ScaleType.LOG1P + ", using it for X." );
                    return ScaleType.LOG1P;
                }
            }
        }
        // FIXME: infer scale from data using the logic from ExpressionDataDoubleMatrixUtil.inferQuantitationType()
        log.warn( "Scale type cannot be detected for non-counting data in " + path + "." );
        return ScaleType.OTHER;
    }

    @Override
    public Optional<CellTypeAssignment> getCellTypeAssignment( SingleCellDimension dimension ) throws IOException {
        Assert.notNull( cellTypeFactorName, "A cell type factor name must be set to determine cell type assignments." );
        try ( H5File h5File = openFile(); H5Group group = h5File.getGroup( "var/" + cellTypeFactorName ) ) {
            CategoricalArray cellTypes = new CategoricalArray( group );
            CellTypeAssignment assignment = new CellTypeAssignment();
            int unknownCellTypeCode = -1;
            for ( int i = 0; i < cellTypes.categories.length; i++ ) {
                String ct = cellTypes.categories[i];
                if ( ct.equals( unknownCellTypeIndicator ) ) {
                    if ( unknownCellTypeCode != -1 ) {
                        throw new IllegalStateException( "There is not than one unknown cell type indicator." );
                    }
                    log.info( h5File.getPath().getFileName() + " uses a special indicator for unknown cell types: " + ct + " with code: " + i + ", its occurrences will be replaced with -1." );
                    unknownCellTypeCode = i;
                    continue;
                }
                assignment.getCellTypes().add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, ct, null ) );
            }
            if ( unknownCellTypeIndicator != null && unknownCellTypeCode == -1 ) {
                throw new IllegalStateException( "The unknown cell type indicator %s was not found." );
            }
            assignment.setNumberOfCellTypes( assignment.getCellTypes().size() );
            if ( unknownCellTypeCode != -1 ) {
                // rewrite unknown codes
                for ( int i = 0; i < cellTypes.codes.length; i++ ) {
                    if ( cellTypes.codes[i] == unknownCellTypeCode ) {
                        cellTypes.codes[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                    }
                }
            }
            assignment.setCellTypeIndices( cellTypes.codes );
            return Optional.of( assignment );
        }
    }

    @Override
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
        Set<ExperimentalFactor> factors = new HashSet<>();
        try ( H5File h5File = openFile() ) {
            try ( H5Group vars = h5File.getGroup( "var" ) ) {
                String indexColumn = vars.getStringAttribute( "_index" );
                CategoricalArray sampleNames = loadCategoricalColumnFromDataframe( vars, sampleFactorName );
                for ( String factorName : vars.getChildren() ) {
                    if ( factorName.equals( indexColumn ) || factorName.equals( sampleFactorName ) || factorName.equals( cellTypeFactorName ) ) {
                        // cell IDs, sample names, etc. are not useful as sample characteristics
                        continue;
                    }
                    String encodingType = vars.getStringAttribute( factorName, "encoding-type" );
                    String[] values;
                    PrimitiveType representation;
                    if ( "categorical".equals( encodingType ) ) {
                        values = loadCategoricalColumnFromDataframe( vars, factorName ).toStringVector();
                        representation = PrimitiveType.STRING;
                    } else if ( "string-array".equals( encodingType ) ) {
                        values = loadStringArrayColumnFromDataframe( vars, factorName );
                        representation = PrimitiveType.STRING;
                    } else if ( "array".equals( encodingType ) ) {
                        try ( H5Dataset vector = vars.getDataset( factorName ) ) {
                            switch ( vector.getType().getFundamentalType() ) {
                                case INTEGER:
                                    values = Arrays.stream( vector.toIntegerVector() ).mapToObj( Integer::toString ).toArray( String[]::new );
                                    representation = PrimitiveType.INT;
                                    break;
                                case FLOAT:
                                    values = Arrays.stream( vector.toDoubleVector() ).mapToObj( Double::toString ).toArray( String[]::new );
                                    representation = PrimitiveType.DOUBLE;
                                    break;
                                default:
                                    log.warn( "Unsupported datatype for array encoding: " + vector.getType() );
                                    continue;
                            }
                        }
                    } else {
                        log.warn( "Unsupported encoding type: " + encodingType );
                        continue;
                    }
                    extractSingleValueBySampleName( sampleNames, values ).ifPresent( sampleValues -> {
                        boolean isCategorical = "categorical".equals( encodingType ) || "string-array".equals( encodingType );
                        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( factorName, isCategorical ? FactorType.CATEGORICAL : FactorType.CONTINUOUS );
                        Characteristic c = Characteristic.Factory.newInstance( factorName, null );
                        factor.setCategory( c );
                        if ( "categorical".equals( encodingType ) ) {
                            Arrays.stream( values ).distinct().forEach( fv -> {
                                FactorValue fvO = FactorValue.Factory.newInstance( factor );
                                fvO.getCharacteristics().add( Statement.Factory.newInstance( c.getCategory(), c.getCategoryUri(), fv, null ) );
                                fvO.setValue( fv );
                                factor.getFactorValues().add( fvO );
                            } );
                            if ( factorValueAssignments != null ) {
                                Map<String, FactorValue> fvByValue = factor.getFactorValues().stream()
                                        .collect( Collectors.toMap( FactorValue::getValue, fv -> fv ) );
                                sampleValues.forEach( ( sn, v ) -> {
                                    for ( BioAssay sample : samples ) {
                                        if ( sampleNameComparator.matches( sample, sn ) ) {
                                            factorValueAssignments
                                                    .computeIfAbsent( sample.getSampleUsed(), k -> new HashSet<>() )
                                                    .add( fvByValue.get( v ) );
                                        }
                                    }
                                } );
                            }
                        } else {
                            // measurement, no need to create any FVs since those are unique to each sample
                            if ( factorValueAssignments != null ) {
                                sampleValues.forEach( ( sn, v ) -> {
                                    for ( BioAssay sample : samples ) {
                                        if ( sampleNameComparator.matches( sample, sn ) ) {
                                            FactorValue fvO = FactorValue.Factory.newInstance( factor );
                                            Measurement measurement = new Measurement();
                                            measurement.setRepresentation( representation );
                                            measurement.setValue( v );
                                            fvO.setMeasurement( measurement );
                                            fvO.setValue( v );
                                            factor.getFactorValues().add( fvO );
                                            factorValueAssignments
                                                    .computeIfAbsent( sample.getSampleUsed(), k -> new HashSet<>() )
                                                    .add( fvO );
                                        }
                                    }
                                } );
                            }
                        }
                        factors.add( factor );
                    } );
                }
            }
        }
        return factors;
    }

    @Override
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
        Map<BioMaterial, Set<Characteristic>> result = new HashMap<>();
        try ( H5File h5File = openFile() ) {
            try ( H5Group vars = h5File.getGroup( "var" ) ) {
                String indexColumn = vars.getStringAttribute( "_index" );
                CategoricalArray sampleNames = loadCategoricalColumnFromDataframe( vars, sampleFactorName );
                for ( String factorName : vars.getChildren() ) {
                    if ( factorName.equals( indexColumn ) || factorName.equals( sampleFactorName ) || factorName.equals( cellTypeFactorName ) ) {
                        // cell IDs, sample names, etc. are not useful as sample characteristics
                        continue;
                    }
                    String encodingType = vars.getStringAttribute( factorName, "encoding-type" );
                    String[] values;
                    if ( "categorical".equals( encodingType ) ) {
                        // to be valid for sample-level, a characteristic has to be the same for all cells
                        values = loadCategoricalColumnFromDataframe( vars, factorName ).toStringVector();
                    } else if ( "string-array".equals( encodingType ) ) {
                        values = loadStringArrayColumnFromDataframe( vars, factorName );
                    } else if ( "array".equals( encodingType ) ) {
                        try ( H5Dataset vector = vars.getDataset( factorName ) ) {
                            Assert.isTrue( Objects.equals( vars.getStringAttribute( "encoding-type" ), "dataframe" ),
                                    "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
                            Assert.isTrue( Objects.equals( vars.getStringAttribute( factorName, "encoding-type" ), "array" ),
                                    "The column " + factorName + " is not an array." );
                            switch ( vector.getType().getFundamentalType() ) {
                                case INTEGER:
                                    values = Arrays.stream( vector.toIntegerVector() ).mapToObj( Integer::toString ).toArray( String[]::new );
                                    break;
                                case FLOAT:
                                    values = Arrays.stream( vector.toDoubleVector() ).mapToObj( Double::toString ).toArray( String[]::new );
                                    break;
                                default:
                                    log.warn( "Unsupported datatype for array encoding: " + vector.getType() );
                                    continue;
                            }
                        }
                    } else {
                        log.warn( "Unsupported encoding type: " + encodingType );
                        continue;
                    }
                    extractSingleValueBySampleName( sampleNames, values )
                            .ifPresent( fvs -> fvs.forEach( ( sampleName, value ) -> samples.stream()
                                    .filter( b -> sampleNameComparator.matches( b, sampleName ) )
                                    .forEach( ba -> result.computeIfAbsent( ba.getSampleUsed(), k -> new HashSet<>() )
                                            .add( Characteristic.Factory.newInstance( factorName, null, value, null ) ) ) ) );
                }
            }
        }
        return result;
    }

    /**
     * Extract a single value by sample name if possible.
     */
    private <T> Optional<Map<String, T>> extractSingleValueBySampleName( CategoricalArray sampleNames, T[] values ) {
        Map<String, T> cs = new HashMap<>();
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            if ( cs.containsKey( sampleName ) && !cs.get( sampleName ).equals( values[i] ) ) {
                return Optional.empty();
            }
            cs.put( sampleName, values[i] );
        }
        return Optional.of( cs );
    }

    @Override
    public Set<String> getGenes() throws IOException {
        try ( H5File h5File = openFile(); H5Group obs = h5File.getGroup( "obs" ) ) {
            return new HashSet<>( Arrays.asList( loadStringIndexFromDataframe( obs ) ) );
        }
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors(
            Map<String, CompositeSequence> elementsMapping,
            SingleCellDimension dimension,
            QuantitationType quantitationType ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
        Assert.isTrue( quantitationType.getName().equals( "X" ) || quantitationType.getName().startsWith( "layers/" ),
                "The name of the quantitation must refer to a valid path in the HDF5 file." );
        // we don't want to close it since it will be closed by the stream
        H5File h5File = openFile();
        try {
            // load genes
            String[] genes;
            try ( H5Group obs = h5File.getGroup( "obs" ) ) {
                genes = loadStringIndexFromDataframe( obs );
            }
            Set<String> genesSet = new HashSet<>( Arrays.asList( genes ) );
            if ( !CollectionUtils.containsAny( genesSet, elementsMapping.keySet() ) ) {
                throw new IllegalArgumentException( "None of the genes are present in the elements mapping." );
            }
            Set<String> unknownGenes = SetUtils.difference( genesSet, elementsMapping.keySet() );
            if ( !unknownGenes.isEmpty() ) {
                String msg;
                if ( unknownGenes.size() > 10 ) {
                    msg = String.format( "%d/%d genes are not present in the elements mapping.", unknownGenes.size(), genes.length );
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
            CategoricalArray samples;
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
                return loadVectorsFromSparseMatrix( new CsrMatrix( h5File.getGroup( path ) ), samples, genes, quantitationType, dimension, elementsMapping )
                        .onClose( h5File::close );
            } else if ( matrixEncodingType.equals( "csc_matrix" ) ) {
                throw new IllegalArgumentException( "The matrix at '" + path + "' is stored as CSC; it must be converted to CSR for being loaded." );
            } else {
                throw new UnsupportedOperationException( "Loading single-cell data from " + matrixEncodingType + " is not supported." );
            }
        } catch ( Throwable e ) {
            h5File.close();
            throw e;
        }
    }


    private Stream<SingleCellExpressionDataVector> loadVectorsFromSparseMatrix( CsrMatrix matrix, CategoricalArray samples, String[]
            genes, QuantitationType qt, SingleCellDimension scd, Map<String, CompositeSequence> elementsMapping ) {
        Assert.isTrue( genes.length == matrix.shape[0],
                "The number of supplied genes does not match the number of rows in the sparse matrix." );
        Assert.isTrue( samples.size() == matrix.shape[1],
                "The number of supplied samples does not match the number of columns in the sparse matrix." );
        Assert.isTrue( scd.getCellIds().size() <= matrix.shape[1],
                "The number of cells in the dimension cannot exceed the number of columns in the sparse matrix." );

        // build a sample offset index for efficiently selecting samples
        // if a sample does not have a corresponding BioAssay (i.e. an unwanted sample), it is set to null
        int numberOfSamples = samples.categories.length;
        List<BioAssay> samplesBioAssay = new ArrayList<>( numberOfSamples );
        int[] samplesBioAssayOffset = new int[numberOfSamples];
        int W = 0;
        String currentSample = null;
        for ( int i = 0; i < samples.size(); i++ ) {
            String sample = requireNonNull( samples.get( i ), "Sample name cannot be missing." );
            if ( !sample.equals( currentSample ) ) {
                currentSample = sample;
                samplesBioAssayOffset[W++] = i;
                samplesBioAssay.add( getBioAssayBySampleName( scd.getBioAssays(), sample ).orElse( null ) );
            }
        }

        if ( samplesBioAssay.size() != numberOfSamples ) {
            throw new IllegalStateException( "The number of distinct contiguous sample does not match the number of distinct samples, this is likely due to the samples being unsorted." );
        }

        // in the simple scenario, the SCD sample layout exactly match what is present in he data, so we don't need to
        // do anything in particular
        boolean isSimpleCase = scd.getBioAssays().equals( samplesBioAssay );

        return IntStream.range( 0, matrix.shape[0] )
                // ignore entries that are missing a gene mapping
                .filter( i -> elementsMapping.containsKey( genes[i] ) )
                .mapToObj( i -> {
                    assert matrix.intptr[i] < matrix.intptr[i + 1];
                    SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
                    vector.setDesignElement( elementsMapping.get( genes[i] ) );
                    vector.setSingleCellDimension( scd );
                    vector.setQuantitationType( qt );
                    int[] IX = matrix.indices.slice( matrix.intptr[i], matrix.intptr[i + 1] ).toIntegerVector();
                    if ( !ArrayUtils.isSorted( IX ) ) {
                        // this is annoying, AnnData does not guarantee that indices are sorted
                        // https://github.com/scverse/anndata/issues/1388
                        throw new IllegalStateException( String.format( "Indices for %s are not sorted.", genes[i] ) );
                    }
                    // simple case: the number of BAs match the number of samples and the sample order matches
                    if ( isSimpleCase ) {
                        // this is using the same storage strategy from ByteArrayConverter
                        vector.setData( matrix.data.slice( matrix.intptr[i], matrix.intptr[i + 1] ).toByteVector( H5Type.IEEE_F64BE ) );
                        vector.setDataIndices( IX );
                    } else {
                        // for each sample we want, we compute the starting, ending and position in the vector
                        int[] sampleStarts = new int[scd.getBioAssaysOffset().length];
                        int[] sampleEnds = new int[scd.getBioAssaysOffset().length];
                        int[] sampleIndices = new int[scd.getBioAssaysOffset().length];
                        // this will hold the size of the vector necessary to hold all the desired sample
                        int nnz = 0;
                        List<BioAssay> bioAssays = scd.getBioAssays();
                        for ( int k = 0; k < bioAssays.size(); k++ ) {
                            BioAssay ba = bioAssays.get( k );
                            int j = samplesBioAssay.indexOf( ba );
                            assert j != -1;
                            int start = Arrays.binarySearch( IX, samplesBioAssayOffset[j] );
                            if ( start < 0 ) {
                                // note here from poirigui, in ListUtils you might notice that getSparseRangeArrayElement() subtract 2
                                // instead of 1,
                                //
                                start = -start - 1;
                            }
                            int end;
                            if ( j < samplesBioAssayOffset.length - 1 ) {
                                end = Arrays.binarySearch( IX, samplesBioAssayOffset[j + 1] );
                                if ( end < 0 ) {
                                    end = -end - 1;
                                }
                            } else {
                                end = IX.length;
                            }
                            nnz += end - start;
                            sampleStarts[k] = start;
                            sampleEnds[k] = end;
                            sampleIndices[k] = j;
                        }
                        byte[] vectorData = new byte[8 * nnz];
                        int[] vectorIndices = new int[nnz];
                        // select indices relevant to BAs
                        int sampleOffsetInVector = 0;
                        for ( int i1 = 0; i1 < bioAssays.size(); i1++ ) {
                            int j = sampleIndices[i1];
                            int start = sampleStarts[i1];
                            int end = sampleEnds[i1];
                            int sampleNnz = end - start;
                            byte[] w = matrix.data.slice( start, end ).toByteVector( H5Type.IEEE_F64BE );
                            System.arraycopy( w, 0, vectorData, 8 * sampleOffsetInVector, 8 * sampleNnz );
                            System.arraycopy( IX, start, vectorIndices, sampleOffsetInVector, sampleNnz );
                            // adjust indices to be relative to the BA offset
                            for ( int k = sampleOffsetInVector; k < sampleOffsetInVector + sampleNnz; k++ ) {
                                vectorIndices[k] = vectorIndices[k] - samplesBioAssayOffset[j] + scd.getBioAssaysOffset()[i1];
                            }
                            sampleOffsetInVector += sampleNnz;
                        }
                        vector.setData( vectorData );
                        vector.setDataIndices( vectorIndices );
                    }
                    return vector;
                } )
                .onClose( matrix::close );
    }

    private H5File openFile() throws IOException {
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

    /**
     * Load the string index of a dataframe.
     */
    private String[] loadStringIndexFromDataframe( H5Group df ) {
        Assert.isTrue( Objects.equals( df.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        String indexColumn = df.getStringAttribute( "_index" );
        Assert.notNull( indexColumn );
        Assert.isTrue( df.exists( indexColumn ) );
        return df.getDataset( indexColumn ).toStringVector();
    }

    /**
     * Load the values of a categorical column from a dataframe.
     */
    private CategoricalArray loadCategoricalColumnFromDataframe( H5Group df, String columnName ) {
        Assert.isTrue( Objects.equals( df.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        Assert.isTrue( Objects.equals( df.getStringAttribute( columnName, "encoding-type" ), "categorical" ),
                "The column " + columnName + " is not categorical." );
        try ( H5Group dataset = df.getGroup( columnName ) ) {
            return new CategoricalArray( dataset );
        }
    }

    /**
     * Load a string array column from a dataframe.
     */
    private String[] loadStringArrayColumnFromDataframe( H5Group df, String columnName ) {
        Assert.isTrue( Objects.equals( df.getStringAttribute( "encoding-type" ), "dataframe" ),
                "The H5 group must have an 'encoding-type' attribute set to 'dataframe'." );
        Assert.isTrue( Objects.equals( df.getStringAttribute( columnName, "encoding-type" ), "string-array" ),
                "The column " + columnName + " is not a string array." );
        return df.getDataset( columnName ).toStringVector();
    }

    private static class CategoricalArray {

        private final String[] categories;
        private final int[] codes;

        public CategoricalArray( H5Group group ) {
            Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "categorical" ),
                    "The H5 group does not have an 'encoding-type' attribute set to 'categorical'." );
            this.categories = group.getDataset( "categories" ).toStringVector();
            this.codes = group.getDataset( "codes" ).toIntegerVector();
        }

        @Nullable
        public String get( int i ) {
            return codes[i] != -1 ? categories[codes[i]] : null;
        }

        public String[] toStringVector() {
            String[] vec = new String[codes.length];
            for ( int i = 0; i < vec.length; i++ ) {
                vec[i] = codes[i] != -1 ? categories[codes[i]] : null;
            }
            return vec;
        }

        public int size() {
            return codes.length;
        }
    }

    private static class CsrMatrix implements AutoCloseable {

        private final H5Group group;
        private final int[] shape;
        private final int[] intptr;
        private final H5Dataset data;
        private final H5Dataset indices;

        public CsrMatrix( H5Group group ) {
            Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "csr_matrix" ),
                    "The H5 group does not have an 'encoding-type' attribute set to 'csr_matrix'." );
            this.shape = group.getAttribute( "shape" )
                    .map( H5Attribute::toIntegerVector )
                    .orElseThrow( () -> new IllegalStateException( "The sparse matrix does not have a shape attribute." ) );
            this.group = group;
            try ( H5Dataset indptr = group.getDataset( "indptr" ) ) {
                this.intptr = indptr.toIntegerVector();
            }
            assert intptr.length == shape[0] + 1;
            this.data = group.getDataset( "data" );
            this.indices = group.getDataset( "indices" );
        }

        @Override
        public void close() {
            data.close();
            indices.close();
            group.close();
        }
    }

    private Optional<BioAssay> getBioAssayBySampleName( Collection<BioAssay> bioAssays, String sampleName ) {
        // lookup the sample
        List<BioAssay> match = bioAssays.stream()
                .filter( ba -> sampleNameComparator.matches( ba, sampleName ) )
                .collect( Collectors.toList() );
        if ( match.size() > 1 ) {
            throw new IllegalStateException( String.format( "There is more than one BioAssay matching %s.", sampleName ) );
        } else if ( match.size() == 1 ) {
            return Optional.of( match.iterator().next() );
        } else {
            return Optional.empty();
        }
    }
}
