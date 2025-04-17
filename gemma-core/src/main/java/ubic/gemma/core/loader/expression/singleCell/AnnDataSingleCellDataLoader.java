package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.SingleCellDescriptive;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.util.anndata.*;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5FundamentalType;
import ubic.gemma.core.loader.util.hdf5.H5Type;
import ubic.gemma.core.loader.util.mapper.BioAssayMapper;
import ubic.gemma.core.loader.util.mapper.DesignElementMapper;
import ubic.gemma.core.loader.util.mapper.EntityMapper;
import ubic.gemma.model.common.description.Categories;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
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
     * Prefix used for naming the QT.
     */
    private static final String QT_NAME_PREFIX = "AnnData";

    /**
     * Prefix used for naming the QT derived from a specific layer.
     */
    private static final String LAYERED_QT_NAME_PREFIX = QT_NAME_PREFIX + " from layer ";

    /**
     * Path to the HDF5 file.
     */
    private final Path file;

    private BioAssayMapper bioAssayToSampleNameMapper;
    private boolean ignoreUnmatchedSamples = true;

    private DesignElementMapper designElementToGeneMapper;
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

    /**
     * Use or not the {@code raw.X} layer.
     * <p>
     * The default is to use {@code X} if {@code raw.X} is not present. If {@code raw.X} is present and no value is
     * specified, an exception will be raised.
     */
    @Nullable
    private Boolean useRawX = null;

    /**
     * Transpose obs/var dataframes.
     */
    private boolean transpose = false;

    /**
     * Maximum number of characteristics to consider when loading a cell-level characteristic.
     */
    private int maxCharacteristics = 100;

    public AnnDataSingleCellDataLoader( Path file ) {
        this.file = file;
    }

    @Override
    public Set<String> getSampleNames() throws IOException {
        checkSampleFactorName();
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> v = getCellsDataframe( h5File ) ) {
            return v.getColumn( sampleFactorName, String.class ).uniqueValues();
        }
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        Assert.isTrue( !bioAssays.isEmpty(), "At least one bioassay must be provided" );
        Assert.notNull( bioAssayToSampleNameMapper, "A sample name comparator is necessary to match samples to BioAssays." );
        checkSampleFactorName();
        SingleCellDimension singleCellDimension = new SingleCellDimension();
        try ( AnnData h5File = AnnData.open( file ) ) {
            Dataframe.Column<?, String> cellIds;
            Dataframe.Column<?, String> sampleNames;
            try ( Dataframe<?> v = getCellsDataframe( h5File ) ) {
                cellIds = v.getColumn( v.getIndexColumn(), String.class );
                sampleNames = v.getColumn( sampleFactorName, String.class );
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
                cellIdsL.add( cellIds.get( i ) );
                j++;
            }
            if ( bas.isEmpty() ) {
                throw new IllegalArgumentException( "No samples were matched. Possible sample names: " + String.join( ", ", unmatchedSamples ) );
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
        try ( AnnData h5File = AnnData.open( file ) ) {
            if ( getX( h5File ) != null ) {
                qts.add( createQt( h5File, getX( h5File ), null ) );
            }
            for ( String layer : h5File.getLayers() ) {
                qts.add( createQt( h5File, h5File.getLayer( layer ), layer ) );
            }
        }
        return qts;
    }

    private QuantitationType createQt( AnnData h5File, Layer layer, @Nullable String layerName ) {
        QuantitationType qt = new QuantitationType();
        // FIXME: rename this layer to include 'AnnData', but we need to layer name later on for loading vectors
        if ( layerName != null ) {
            qt.setName( LAYERED_QT_NAME_PREFIX + layerName );
        } else {
            qt.setName( QT_NAME_PREFIX );
        }
        if ( layerName != null ) {
            qt.setDescription( "AnnData data loaded from layer " + layerName + "." );
        } else {
            qt.setDescription( "AnnData" );
        }
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        H5FundamentalType fundamentalType;
        try ( Matrix matrix = layer.getMatrix(); H5Type datasetType = matrix.getDataType() ) {
            fundamentalType = datasetType.getFundamentalType();
        }
        detectQuantitationType( qt, h5File, layer, fundamentalType );
        qt.setDescription( String.format( "Data from a layer located at '%s' originally encoded as an %s of %ss.",
                layer.getPath(), layer.getEncodingType(), fundamentalType.toString().toLowerCase() ) );
        log.info( "Detected quantitation type for '" + layer.getPath() + "': " + qt );
        return qt;
    }

    /**
     * TODO: use {@link ubic.gemma.core.analysis.preprocess.detect.QuantitationTypeDetectionUtils}
     */
    private void detectQuantitationType( QuantitationType qt, AnnData h5File, Layer layer, H5FundamentalType fundamentalType ) {
        if ( fundamentalType.equals( H5FundamentalType.INTEGER ) ) {
            qt.setType( StandardQuantitationType.COUNT );
            qt.setScale( ScaleType.COUNT );
            // TODO: support integer QTs
            qt.setRepresentation( PrimitiveType.INT );
        } else if ( fundamentalType.equals( H5FundamentalType.FLOAT ) ) {
            // detect various count encodings
            for ( ScaleType st : new ScaleType[] { ScaleType.COUNT, ScaleType.LOG2, ScaleType.LN, ScaleType.LOG10, ScaleType.LOG1P } ) {
                if ( isCountEncodedInDouble( layer, st ) ) {
                    qt.setType( StandardQuantitationType.COUNT );
                    qt.setScale( st );
                    qt.setRepresentation( PrimitiveType.DOUBLE );
                    return;
                }
            }
            qt.setType( StandardQuantitationType.AMOUNT );
            if ( isLog1p( h5File, layer ) ) {
                qt.setScale( ScaleType.LOG1P );
            } else {
                // FIXME: infer scale from data using the logic from ExpressionDataDoubleMatrixUtil.inferQuantitationType()
                log.warn( "Scale type cannot be detected for non-counting data in " + layer.getPath() + "." );
                qt.setScale( ScaleType.OTHER );
            }
            qt.setRepresentation( PrimitiveType.DOUBLE );
        } else {
            throw new IllegalArgumentException( "Unsupported H5 fundamental type " + fundamentalType + " for a quantitation type." );
        }
    }

    private boolean isCountEncodedInDouble( Layer layer, ScaleType scaleType ) {
        H5Dataset data = layer.getMatrix().getData();
        if ( data.getShape().length == 2 ) {
            log.warn( "Detecting counts from dense matrices are not supported, ignoring " + layer + "." );
            return false;
        }
        for ( int i = 0; i < data.size(); i += 1000 ) {
            double[] vec = data.slice( i, Math.min( i + 1000, data.size() ) ).toDoubleVector();
            for ( double d : vec ) {
                double uv;
                switch ( scaleType ) {
                    case COUNT:
                        uv = d;
                        break;
                    case LOG2:
                        uv = Math.pow( 2, d );
                        break;
                    case LN:
                        uv = Math.exp( d );
                        break;
                    case LOG10:
                        uv = Math.pow( 10, d );
                        break;
                    case LOG1P:
                        uv = Math.expm1( d );
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }
                if ( uv != Math.rint( uv ) ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check if the given layer is scaled with log1p.
     */
    private boolean isLog1p( AnnData h5File, Layer layer ) {
        if ( "X".equals( layer.getPath() ) ) {
            try ( Mapping uns = h5File.getUns() ) {
                if ( uns != null && uns.getKeys().contains( "log1p" ) ) {
                    log.info( h5File + " contains unstructured data describing the scale type: " + ScaleType.LOG1P + ", using it for X." );
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        checkCellTypeFactorName();
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> var = getCellsDataframe( h5File ) ) {
            // TODO: support cell types encoded as string-array
            CategoricalArray<String> cellTypes = var.getCategoricalColumn( cellTypeFactorName, String.class );
            CellTypeAssignment assignment = new CellTypeAssignment();
            int unknownCellTypeCode = CellTypeAssignment.UNKNOWN_CELL_TYPE;
            for ( int i = 0; i < cellTypes.getCategories().length; i++ ) {
                String ct = cellTypes.getCategories()[i];
                if ( ct.equals( unknownCellTypeIndicator ) ) {
                    if ( unknownCellTypeCode != CellTypeAssignment.UNKNOWN_CELL_TYPE ) {
                        throw new IllegalStateException( "There is not than one unknown cell type indicator." );
                    }
                    log.info( h5File + " uses a special indicator for unknown cell types: " + ct + " with code: " + i + ", its occurrences will be replaced with -1." );
                    unknownCellTypeCode = i;
                    continue;
                }
                assignment.getCellTypes().add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, ct, null ) );
            }
            if ( unknownCellTypeIndicator != null && unknownCellTypeCode == CellTypeAssignment.UNKNOWN_CELL_TYPE ) {
                throw new IllegalStateException( String.format( "The unknown cell type indicator %s was not found. Possible values are: %s. If none of these indicate a missing cell type, set the indicator to null.",
                        unknownCellTypeIndicator, String.join( ", ", cellTypes.getCategories() ) ) );
            }
            assignment.setNumberOfCellTypes( assignment.getCellTypes().size() );
            int[] codes = cellTypes.getCodes();
            if ( unknownCellTypeCode != -1 ) {
                // rewrite unknown codes, make a copy to ensure we don't modify the AnnData object
                codes = Arrays.copyOf( codes, codes.length );
                for ( int i = 0; i < codes.length; i++ ) {
                    if ( codes[i] == unknownCellTypeCode ) {
                        codes[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                    }
                }
            }
            assignment.setCellTypeIndices( codes );
            assignment.setNumberOfAssignedCells( ( int ) Arrays.stream( codes ).filter( i -> i != CellTypeAssignment.UNKNOWN_CELL_TYPE ).count() );
            return Collections.singleton( assignment );
        }
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws
            IOException {
        checkSampleFactorName();
        if ( cellTypeFactorName == null ) {
            log.warn( "No cell type factor name is set, cell types might be treated as \"other cell-level characteristics\"." );
        }
        Set<CellLevelCharacteristics> results = new HashSet<>();
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> vars = getCellsDataframe( h5File ) ) {
            Dataframe.Column<?, String> sampleNames = vars.getColumn( sampleFactorName, String.class );
            String indexColumn = vars.getIndexColumn();
            for ( String factorName : vars.getColumns() ) {
                if ( factorName.equals( indexColumn ) || factorName.equals( sampleFactorName ) || factorName.equals( cellTypeFactorName ) ) {
                    // cell IDs, sample names, etc. are not useful as cell-level characteristics
                    continue;
                }
                String encodingType = vars.getColumnEncodingType( factorName );
                String[] values;
                if ( "categorical".equals( encodingType ) ) {
                    // to be valid for sample-level, a characteristic has to be the same for all cells
                    try ( CategoricalArray<?> ca = vars.getCategoricalColumn( factorName ) ) {
                        values = categoricalArrayToStringVector( ca );
                    }
                } else if ( "string-array".equals( encodingType ) ) {
                    values = vars.getStringArrayColumn( factorName );
                } else if ( "array".equals( encodingType ) ) {
                    try ( H5Dataset vector = vars.getArrayColumn( factorName ) ) {
                        switch ( vector.getType().getFundamentalType() ) {
                            case INTEGER:
                                values = Arrays.stream( vars.getArrayColumn( factorName ).toIntegerVector() ).mapToObj( Integer::toString ).toArray( String[]::new );
                                break;
                            case FLOAT:
                                values = Arrays.stream( vars.getArrayColumn( factorName ).toDoubleVector() ).mapToObj( Double::toString ).toArray( String[]::new );
                                break;
                            default:
                                log.warn( "Unsupported datatype for array encoding: " + vector.getType() );
                                continue;
                        }
                    }
                } else if ( "nullable-integer".equals( encodingType ) ) {
                    values = Arrays.stream( vars.getNullableIntegerArrayColumn( factorName ) )
                            .map( v -> v != null ? String.valueOf( v ) : null )
                            .toArray( String[]::new );
                } else if ( "nullable-boolean".equals( encodingType ) ) {
                    values = Arrays.stream( vars.getNullableBooleanArrayColumn( factorName ) )
                            .map( v -> v != null ? String.valueOf( v ) : null )
                            .toArray( String[]::new );
                } else {
                    log.warn( "Unsupported encoding type: " + encodingType );
                    continue;
                }
                if ( extractSingleValueBySampleName( sampleNames, values ).isPresent() ) {
                    continue;
                }
                // conclusion, this is a cell-level characteristic
                if ( values.length > maxCharacteristics ) {
                    log.warn( "The " + factorName + " column has too too many values (" + values.length + ") for importing into a cell-level characteristic, ignoring." );
                    continue;
                }
                int[] indices = new int[values.length];
                Map<String, Integer> valToIndex = new HashMap<>();
                List<Characteristic> characteristics = new ArrayList<>();
                for ( int i = 0; i < values.length; i++ ) {
                    String val = values[i];
                    if ( val != null ) {
                        int j;
                        if ( valToIndex.containsKey( val ) ) {
                            j = valToIndex.get( val );
                        } else {
                            Characteristic c = createCharacteristic( h5File, factorName, val );
                            j = characteristics.size();
                            characteristics.add( c );
                            valToIndex.put( val, j );
                        }
                        indices[i] = j;
                    } else {
                        indices[i] = CellLevelCharacteristics.UNKNOWN_CHARACTERISTIC;
                    }
                }
                results.add( CellLevelCharacteristics.Factory.newInstance( characteristics, indices ) );
            }
        }
        return results;
    }

    @Override
    public Set<ExperimentalFactor> getFactors
            ( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws
            IOException {
        checkSampleFactorName();
        Set<ExperimentalFactor> factors = new HashSet<>();
        try ( AnnData h5File = AnnData.open( file ) ) {
            try ( Dataframe<?> vars = getCellsDataframe( h5File ) ) {
                String indexColumn = vars.getIndexColumn();
                Dataframe.Column<?, String> sampleNames = vars.getColumn( sampleFactorName, String.class );
                for ( String factorName : vars.getColumns() ) {
                    if ( factorName.equals( indexColumn ) || factorName.equals( sampleFactorName ) || factorName.equals( cellTypeFactorName ) ) {
                        // cell IDs, sample names, etc. are not useful as sample characteristics
                        continue;
                    }
                    String encodingType = vars.getColumnEncodingType( factorName );
                    String[] values;
                    PrimitiveType representation;
                    if ( "categorical".equals( encodingType ) ) {
                        try ( CategoricalArray<?> ca = vars.getCategoricalColumn( factorName ) ) {
                            values = categoricalArrayToStringVector( ca );
                        }
                        representation = PrimitiveType.STRING;
                    } else if ( "string-array".equals( encodingType ) ) {
                        values = vars.getStringArrayColumn( factorName );
                        representation = PrimitiveType.STRING;
                    } else if ( "array".equals( encodingType ) ) {
                        try ( H5Dataset vector = vars.getArrayColumn( factorName ) ) {
                            switch ( vector.getType().getFundamentalType() ) {
                                case INTEGER:
                                    values = Arrays.stream( vars.getArrayColumn( factorName ).toIntegerVector() ).mapToObj( Integer::toString ).toArray( String[]::new );
                                    representation = PrimitiveType.INT;
                                    break;
                                case FLOAT:
                                    values = Arrays.stream( vars.getArrayColumn( factorName ).toDoubleVector() ).mapToObj( Double::toString ).toArray( String[]::new );
                                    representation = PrimitiveType.DOUBLE;
                                    break;
                                default:
                                    log.warn( "Unsupported datatype for array encoding: " + vector.getType() );
                                    continue;
                            }
                        }
                    } else if ( "nullable-integer".equals( encodingType ) ) {
                        values = Arrays.stream( vars.getNullableIntegerArrayColumn( factorName ) )
                                .map( v -> v != null ? String.valueOf( v ) : null )
                                .toArray( String[]::new );
                        representation = PrimitiveType.INT;
                    } else if ( "nullable-boolean".equals( encodingType ) ) {
                        values = Arrays.stream( vars.getNullableBooleanArrayColumn( factorName ) )
                                .map( v -> v != null ? String.valueOf( v ) : null )
                                .toArray( String[]::new );
                        representation = PrimitiveType.BOOLEAN;
                    } else {
                        log.warn( "Unsupported encoding type: " + encodingType );
                        continue;
                    }
                    extractSingleValueBySampleName( sampleNames, values ).ifPresent( sampleValues -> {
                        boolean isCategorical = "categorical".equals( encodingType ) || "string-array".equals( encodingType );
                        ExperimentalFactor factor = ExperimentalFactor.Factory.newInstance( factorName, isCategorical ? FactorType.CATEGORICAL : FactorType.CONTINUOUS );
                        Characteristic c = Characteristic.Factory.newInstance( factorName, null );
                        factor.setCategory( c );
                        if ( isCategorical ) {
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
                                    for ( BioAssay sample : bioAssayToSampleNameMapper.matchAll( samples, sn ) ) {
                                        factorValueAssignments
                                                .computeIfAbsent( sample.getSampleUsed(), k -> new HashSet<>() )
                                                .add( fvByValue.get( v ) );
                                    }
                                } );
                            }
                        } else {
                            // measurement, no need to create any FVs since those are unique to each sample
                            if ( factorValueAssignments != null ) {
                                sampleValues.forEach( ( sn, v ) -> {
                                    for ( BioAssay sample : bioAssayToSampleNameMapper.matchAll( samples, sn ) ) {
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
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws
            IOException {
        checkSampleFactorName();
        Map<BioMaterial, Set<Characteristic>> result = new HashMap<>();
        try ( AnnData h5File = AnnData.open( file ) ) {
            try ( Dataframe<?> vars = getCellsDataframe( h5File ) ) {
                String indexColumn = vars.getIndexColumn();
                Dataframe.Column<?, String> sampleNames = vars.getColumn( sampleFactorName, String.class );
                for ( String factorName : vars.getColumns() ) {
                    if ( factorName.equals( indexColumn ) || factorName.equals( sampleFactorName ) || factorName.equals( cellTypeFactorName ) ) {
                        // cell IDs, sample names, etc. are not useful as sample characteristics
                        continue;
                    }
                    String encodingType = vars.getColumnEncodingType( factorName );
                    String[] values;
                    if ( "categorical".equals( encodingType ) ) {
                        // to be valid for sample-level, a characteristic has to be the same for all cells
                        try ( CategoricalArray<?> ca = vars.getCategoricalColumn( factorName ) ) {
                            values = categoricalArrayToStringVector( ca );
                        }
                    } else if ( "string-array".equals( encodingType ) ) {
                        values = vars.getStringArrayColumn( factorName );
                    } else if ( "array".equals( encodingType ) ) {
                        try ( H5Dataset vector = vars.getArrayColumn( factorName ) ) {
                            switch ( vector.getType().getFundamentalType() ) {
                                case INTEGER:
                                    values = Arrays.stream( vars.getArrayColumn( factorName ).toIntegerVector() ).mapToObj( Integer::toString ).toArray( String[]::new );
                                    break;
                                case FLOAT:
                                    values = Arrays.stream( vars.getArrayColumn( factorName ).toDoubleVector() ).mapToObj( Double::toString ).toArray( String[]::new );
                                    break;
                                default:
                                    log.warn( "Unsupported datatype for array encoding: " + vector.getType() );
                                    continue;
                            }
                        }
                    } else if ( "nullable-integer".equals( encodingType ) ) {
                        values = Arrays.stream( vars.getNullableIntegerArrayColumn( factorName ) )
                                .map( v -> v != null ? String.valueOf( v ) : null )
                                .toArray( String[]::new );
                    } else if ( "nullable-boolean".equals( encodingType ) ) {
                        values = Arrays.stream( vars.getNullableBooleanArrayColumn( factorName ) )
                                .map( v -> v != null ? String.valueOf( v ) : null )
                                .toArray( String[]::new );
                    } else {
                        log.warn( "Unsupported encoding type: " + encodingType );
                        continue;
                    }
                    extractSingleValueBySampleName( sampleNames, values )
                            .ifPresent( fvs -> fvs.forEach( ( sampleName, value ) -> bioAssayToSampleNameMapper.matchAll( samples, sampleName )
                                    .forEach( ba -> result.computeIfAbsent( ba.getSampleUsed(), k -> new HashSet<>() )
                                            .add( createCharacteristic( h5File, factorName, value ) ) ) ) );
                }
            }
        }
        return result;
    }

    private Characteristic createCharacteristic( AnnData h5File, String factorName, String value ) {
        Characteristic c = Characteristic.Factory.newInstance( factorName, null, value, null );
        c.setDescription( "Imported from column " + factorName + " in AnnData file " + h5File + "." );
        return c;
    }

    private String[] categoricalArrayToStringVector( CategoricalArray<?> c ) {
        String[] vec = new String[c.getCodes().length];
        for ( int i = 0; i < vec.length; i++ ) {
            vec[i] = c.getCodes()[i] != -1 ? String.valueOf( c.getCategories()[c.getCodes()[i]] ) : null;
        }
        return vec;
    }

    /**
     * Extract a single value by sample name if possible.
     */
    private <
            T> Optional<Map<String, T>> extractSingleValueBySampleName( Dataframe.Column<?, String> sampleNames, T[]
            values ) {
        Map<String, T> cs = new HashMap<>();
        for ( int i = 0; i < sampleNames.size(); i++ ) {
            String sampleName = sampleNames.get( i );
            if ( cs.containsKey( sampleName ) && !Objects.equals( cs.get( sampleName ), values[i] ) ) {
                return Optional.empty();
            }
            cs.put( sampleName, values[i] );
        }
        return Optional.of( cs );
    }

    @Override
    public Set<String> getGenes() throws IOException {
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> obs = getGenesDataframe( h5File, null ) ) {
            return obs.getColumn( obs.getIndexColumn(), String.class ).uniqueValues();
        }
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( Collection<BioAssay> samples ) throws
            IOException {
        return getSequencingMetadata( getSingleCellDimension( samples ) );
    }

    @Override
    public Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension dimension ) throws IOException {
        checkSampleFactorName();
        try ( AnnData h5File = AnnData.open( file ) ) {
            String unsupportedMatrixWarningMessage = "Sequencing metadata can only be extracted from sparse matrices encoded in the CSR format. Layer with counting data %s will be ignored.";

            Dataframe.Column<?, String> samples = getCellsDataframe( h5File ).getColumn( sampleFactorName, String.class );

            Layer X = getX( h5File );
            QuantitationType Xqt = createQt( h5File, X, null );
            if ( Xqt.getType() == StandardQuantitationType.COUNT ) {
                String matrixEncodingType = X.getEncodingType();
                if ( ( matrixEncodingType.equals( "csr_matrix" ) && !transpose ) || ( matrixEncodingType.equals( "csc_matrix" ) && transpose ) ) {
                    return getSequencingMetadata( dimension, Xqt, samples, X.getSparseMatrix() );
                } else {
                    log.warn( String.format( unsupportedMatrixWarningMessage, "X" ) );
                }
            }

            // check each layer to see if we can found one with counts
            for ( String layerName : h5File.getLayers() ) {
                Layer layer = h5File.getLayer( layerName );
                QuantitationType qt = createQt( h5File, layer, layerName );
                if ( qt.getType() != StandardQuantitationType.COUNT ) {
                    continue;
                }
                String matrixEncodingType = layer.getEncodingType();
                if ( ( matrixEncodingType.equals( "csr_matrix" ) && !transpose ) || ( matrixEncodingType.equals( "csc_matrix" ) && transpose ) ) {
                    return getSequencingMetadata( dimension, qt, samples, layer.getSparseMatrix() );
                } else {
                    log.warn( String.format( unsupportedMatrixWarningMessage, layerName ) );
                }
            }
        }
        return Collections.emptyMap();
    }

    /**
     * Extract sequencing metadata from a given layer containing count data.
     * <p>
     * Since we're not loading data, just summing it, we can process it more efficiently.
     */
    private Map<BioAssay, SequencingMetadata> getSequencingMetadata( SingleCellDimension scd, QuantitationType
            qt, Dataframe.Column<?, String> samples, SparseMatrix matrix ) {
        SampleMetadata m = createSampleMetadata( scd, samples );

        // we just need enough information for SingleCellDescriptive.sum() to work
        SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
        vector.setSingleCellDimension( scd );
        vector.setQuantitationType( qt );

        double[] librarySize = new double[scd.getBioAssays().size()];
        for ( int i = 0; i < matrix.getShape()[0]; i++ ) {
            populateVectorData( vector, i, scd, matrix, m );
            double[] S = SingleCellDescriptive.sumUnscaled( vector );
            for ( int j = 0; j < S.length; j++ ) {
                librarySize[j] += S[j];
            }
        }

        Map<BioAssay, SequencingMetadata> result = new HashMap<>( scd.getBioAssays().size() );
        for ( int i = 0; i < scd.getBioAssays().size(); i++ ) {
            result.put( scd.getBioAssays().get( i ), SequencingMetadata.builder().readCount( Math.round( librarySize[i] ) ).build() );
        }
        return result;
    }

    /**
     * Obtain the genes for a specific QT.
     */
    public Set<String> getGenes( QuantitationType qt ) throws IOException {
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> obs = getGenesDataframe( h5File, getLayerName( qt ) ) ) {
            return obs.getColumn( obs.getIndexColumn(), String.class ).uniqueValues();
        }
    }

    @Override
    public Stream<SingleCellExpressionDataVector> loadVectors
            ( Collection<CompositeSequence> designElements, SingleCellDimension dimension, QuantitationType
                    quantitationType ) throws IOException, IllegalArgumentException {
        return loadVectors( designElements, dimension, quantitationType, getLayerName( quantitationType ) );
    }

    @Override
    public void close() throws IOException {

    }

    @Nullable
    private String getLayerName( QuantitationType quantitationType ) {
        if ( quantitationType.getName().startsWith( LAYERED_QT_NAME_PREFIX ) ) {
            return quantitationType.getName().substring( LAYERED_QT_NAME_PREFIX.length() );
        } else {
            return null;
        }
    }

    /**
     * Load single-cell vectors from a particular layer in the AnnData file.
     * @param layerName the name of the layer to load under {@code layers/}, or null to load the {@code X}
     */
    private Stream<SingleCellExpressionDataVector> loadVectors(
            Collection<CompositeSequence> designElements,
            SingleCellDimension dimension,
            QuantitationType quantitationType,
            @Nullable String layerName ) throws IOException {
        Assert.notNull( designElementToGeneMapper, "A design element mapper must be set to load vectors." );
        checkSampleFactorName();
        // we don't want to close it since it will be closed by the stream
        EntityMapper.StatefulEntityMapper<CompositeSequence> statefulDesignElementMapper = designElementToGeneMapper.forCandidates( designElements );
        // indicate if closing the H5 file been delegated to the Stream
        boolean closeDelegatedToStream = false;
        AnnData h5File = AnnData.open( file );
        try {
            // load genes
            Dataframe.Column<?, String> genes;
            try ( Dataframe<?> obs = getGenesDataframe( h5File, layerName ) ) {
                genes = obs.getColumn( obs.getIndexColumn(), String.class );
            }
            Set<String> genesSet = genes.uniqueValues();
            if ( !statefulDesignElementMapper.containsAny( genesSet ) ) {
                throw new IllegalArgumentException( String.format( "None of the genes are present in the elements mapping. Examples of gene identifiers: %s. You might have to transpose the dataset with the 'transpose' transformation first.",
                        genesSet.stream().limit( 10 ).collect( Collectors.joining( ", " ) ) ) );
            }
            Set<String> unknownGenes = genesSet.stream().filter( g -> !statefulDesignElementMapper.contains( g ) ).collect( Collectors.toSet() );
            if ( !unknownGenes.isEmpty() ) {
                String msg;
                if ( unknownGenes.size() > 10 ) {
                    msg = String.format( "%d/%d genes are not present in the elements mapping.", unknownGenes.size(), genes.size() );
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
            Dataframe.Column<?, String> samples;
            try ( Dataframe<?> v = getCellsDataframe( h5File ) ) {
                samples = v.getColumn( sampleFactorName, String.class );
            }
            Layer layer;
            if ( layerName != null ) {
                layer = h5File.getLayer( layerName );
            } else {
                layer = requireNonNull( getX( h5File ), h5File + " does not have a layer for path 'X'." );
            }
            String matrixEncodingType = layer.getType();
            log.info( "Loading data from " + layer + "..." );
            if ( ( matrixEncodingType.equals( "csr_matrix" ) && !transpose ) || ( matrixEncodingType.equals( "csc_matrix" ) && transpose ) ) {
                closeDelegatedToStream = true;
                return loadVectorsFromSparseMatrix( layer.getSparseMatrix(), samples, genes, quantitationType, dimension, designElements )
                        .onClose( () -> {
                            try {
                                h5File.close();
                            } catch ( IOException e ) {
                                throw new RuntimeException( e );
                            }
                        } );
            } else if ( matrixEncodingType.equals( "csr_matrix" ) ) {
                throw new UnsupportedOperationException( "The matrix at '" + layer.getPath() + "' is stored as CSR and transposition is enabled; it must be converted to CSC for being loaded." );
            } else if ( matrixEncodingType.equals( "csc_matrix" ) ) {
                throw new UnsupportedOperationException( "The matrix at '" + layer.getPath() + "' is stored as CSC; it must be converted to CSR for being loaded." );
            } else {
                throw new UnsupportedOperationException( "Loading single-cell data from " + matrixEncodingType + " is not supported." );
            }
        } finally {
            if ( !closeDelegatedToStream ) {
                h5File.close();
            }
        }
    }

    private Stream<SingleCellExpressionDataVector> loadVectorsFromSparseMatrix( SparseMatrix
            matrix, Dataframe.Column<?, String> samples, Dataframe.Column<?, String> genes, QuantitationType
            qt, SingleCellDimension scd, Collection<CompositeSequence> designElements ) {
        Assert.isTrue( genes.size() == matrix.getShape()[0],
                "The number of supplied genes does not match the number of rows in the sparse matrix." );
        Assert.isTrue( samples.size() == matrix.getShape()[1],
                "The number of supplied samples does not match the number of columns in the sparse matrix." );
        Assert.isTrue( scd.getNumberOfCells() <= matrix.getShape()[1],
                "The number of cells in the dimension cannot exceed the number of columns in the sparse matrix." );

        SampleMetadata m = createSampleMetadata( scd, samples );

        EntityMapper.StatefulEntityMapper<CompositeSequence> statefulDesignElementMapper = designElementToGeneMapper
                .forCandidates( designElements );

        return IntStream.range( 0, matrix.getShape()[0] )
                // ignore entries that are missing a gene mapping
                .filter( i -> statefulDesignElementMapper.contains( genes.get( i ) ) )
                .mapToObj( i -> {
                    assert matrix.getIndptr()[i] < matrix.getIndptr()[i + 1];
                    SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
                    vector.setOriginalDesignElement( genes.get( i ) );
                    vector.setDesignElement( statefulDesignElementMapper.matchOne( genes.get( i ) ).get() );
                    vector.setSingleCellDimension( scd );
                    vector.setQuantitationType( qt );
                    populateVectorData( vector, i, scd, matrix, m );
                    return vector;
                } )
                .onClose( matrix::close );
    }

    @Value
    private static class SampleMetadata {
        /**
         * Samples, as they appear in the data.
         * <p>
         * An unmapped sample is encoded by {@code null}.
         */
        BioAssay[] samplesBioAssay;
        /**
         * Index of the samples in {@link #samplesBioAssay}.
         * <p>
         * This is meant to speed-up lookups.
         */
        Map<BioAssay, Integer> samplesBioAssayIndex;
        /**
         * Offset (i.e. the column where it starts) of the sample in the data matrix.
         */
        int[] samplesBioAssayOffset;
        /**
         * Indicate if the sample layout exactly match that of the {@link SingleCellDimension}.
         */
        boolean isSimpleCase;
    }

    /**
     * Create a data structure that facilitate locating samples in the data matrix.
     */
    private SampleMetadata createSampleMetadata( SingleCellDimension scd, Dataframe.Column<?, String> samples ) {
        int numberOfSamples = samples.uniqueValues().size();
        // build a sample offset index for efficiently selecting samples
        // if a sample does not have a corresponding BioAssay (i.e. an unwanted sample), it is set to null
        List<BioAssay> samplesBioAssay = new ArrayList<>( numberOfSamples );
        Map<BioAssay, Integer> samplesBioAssayIndex = new HashMap<>( numberOfSamples );
        List<String> samplesName = new ArrayList<>( numberOfSamples );
        int[] samplesBioAssayOffset = new int[numberOfSamples];
        int W = 0;
        String currentSample = null;
        for ( int i = 0; i < samples.size(); i++ ) {
            String sample = requireNonNull( samples.get( i ), "Sample name cannot be missing." );
            if ( !sample.equals( currentSample ) ) {
                currentSample = sample;
                samplesBioAssayOffset[W] = i;
                BioAssay ba = getBioAssayBySampleName( scd.getBioAssays(), sample ).orElse( null );
                samplesBioAssay.add( ba );
                if ( ba != null ) {
                    samplesBioAssayIndex.put( ba, W );
                }
                samplesName.add( sample );
                W++;
            }
        }
        if ( samplesBioAssay.size() != numberOfSamples ) {
            throw new IllegalStateException( "The number of distinct contiguous sample does not match the number of distinct samples, this is likely due to the samples being unsorted." );
        }
        // in the simple scenario, the SCD sample layout exactly match what is present in he data, so we don't need to
        // do anything in particular
        boolean isSimpleCase = scd.getBioAssays().equals( samplesBioAssay );
        if ( !isSimpleCase ) {
            // map BAs to the corresponding sample name in data
            String scdNames = scd.getBioAssays().stream()
                    .map( ba -> samplesName.get( samplesBioAssayIndex.get( ba ) ) )
                    .collect( Collectors.joining( ", " ) );
            log.info( "Samples in the data do not follow the same layout, will have to slice and adjust indices.\n"
                    + "\tIn data: " + String.join( ", ", samplesName ) + "\n"
                    + "\tIn single-cell dimension: " + scdNames );
        }
        return new SampleMetadata( samplesBioAssay.toArray( new BioAssay[0] ), samplesBioAssayIndex, samplesBioAssayOffset, isSimpleCase );
    }

    private void populateVectorData( SingleCellExpressionDataVector vector, int i, SingleCellDimension
            scd, SparseMatrix matrix, SampleMetadata m ) {
        int[] IX;
        try ( H5Dataset indices = matrix.getIndices() ) {
            IX = indices.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toIntegerVector();
        }
        if ( !ArrayUtils.isSorted( IX ) ) {
            // this is annoying, AnnData does not guarantee that indices are sorted
            // https://github.com/scverse/anndata/issues/1388
            throw new IllegalStateException( String.format( "Indices for %s are not sorted.", vector.getDesignElement() ) );
        }
        // simple case: the number of BAs match the number of samples and the sample order match
        if ( m.isSimpleCase ) {
            // this is using the same storage strategy from ByteArrayConverter
            try ( H5Dataset data = matrix.getData() ) {
                switch ( vector.getQuantitationType().getRepresentation() ) {
                    case FLOAT:
                        vector.setData( data.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toByteVector( H5Type.IEEE_F32BE ) );
                        break;
                    case DOUBLE:
                        vector.setData( data.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toByteVector( H5Type.IEEE_F64BE ) );
                        break;
                    case INT:
                        vector.setData( data.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toByteVector( H5Type.STD_I32BE ) );
                        break;
                    case LONG:
                        vector.setData( data.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toByteVector( H5Type.STD_I64BE ) );
                        break;
                }
            }
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
                assert m.samplesBioAssayIndex.containsKey( ba );
                int j = m.samplesBioAssayIndex.get( ba );
                int start = Arrays.binarySearch( IX, m.samplesBioAssayOffset[j] );
                if ( start < 0 ) {
                    start = -start - 1;
                }
                int end;
                if ( j < m.samplesBioAssayOffset.length - 1 ) {
                    end = Arrays.binarySearch( IX, m.samplesBioAssayOffset[j + 1] );
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
                if ( sampleNnz == 0 ) {
                    continue;
                }
                try ( H5Dataset data = matrix.getData() ) {
                    H5Dataset.H5Dataspace s = data.slice( start, end );
                    switch ( vector.getQuantitationType().getRepresentation() ) {
                        case FLOAT:
                            s.toByteVector( vectorData, sampleOffsetInVector, sampleOffsetInVector + sampleNnz, H5Type.IEEE_F32BE );
                            break;
                        case DOUBLE:
                            s.toByteVector( vectorData, sampleOffsetInVector, sampleOffsetInVector + sampleNnz, H5Type.IEEE_F64BE );
                            break;
                        case INT:
                            s.toByteVector( vectorData, sampleOffsetInVector, sampleOffsetInVector + sampleNnz, H5Type.STD_I32BE );
                            break;
                        case LONG:
                            s.toByteVector( vectorData, sampleOffsetInVector, sampleOffsetInVector + sampleNnz, H5Type.STD_I64BE );
                            break;
                    }
                }
                System.arraycopy( IX, start, vectorIndices, sampleOffsetInVector, sampleNnz );
                // adjust indices to be relative to the BA offset
                for ( int k = sampleOffsetInVector; k < sampleOffsetInVector + sampleNnz; k++ ) {
                    vectorIndices[k] = vectorIndices[k] - m.samplesBioAssayOffset[j] + scd.getBioAssaysOffset()[i1];
                }
                sampleOffsetInVector += sampleNnz;
            }
            vector.setData( vectorData );
            vector.setDataIndices( vectorIndices );
        }
    }

    private Dataframe<?> getCellsDataframe( AnnData h5File ) {
        return transpose ? h5File.getObs() : getVar( h5File, false );
    }

    private Dataframe<?> getGenesDataframe( AnnData h5File, @Nullable String layerName ) {
        return transpose ? getVar( h5File, layerName != null ) : h5File.getObs();
    }

    private Layer getX( AnnData h5File ) {
        checkRawX( h5File );
        if ( useRawX != null && useRawX ) {
            return h5File.getRawX();
        } else {
            return h5File.getX();
        }
    }

    /**
     * @param ignoreRawVar do not consider {@link #useRawX} when retrieving the {@code var} dataframe. This is only
     *                     useful for retrieving the variables corresponding to {@code X} or {@code layers/*}.
     */
    private Dataframe<?> getVar( AnnData h5File, boolean ignoreRawVar ) {
        checkRawX( h5File );
        // check if the AnnData file has been filtered
        if ( ( useRawX != null && useRawX ) && !ignoreRawVar ) {
            return h5File.getRawVar();
        } else {
            return h5File.getVar();
        }
    }

    private void checkRawX( AnnData h5File ) {
        if ( h5File.getRawX() != null ) {
            Assert.notNull( useRawX, "The AnnData object at " + file + " has as 'raw.X' group. Explicitly set useRawX or converted first with the 'unraw' transformation." );
        } else {
            Assert.isTrue( useRawX == null || !useRawX, "THe AnnData object at " + file + " does not have a 'raw.X' group. Leave useRawX unset or false." );
        }
    }

    private Optional<BioAssay> getBioAssayBySampleName( Collection<BioAssay> bioAssays, String sampleName ) {
        // lookup the sample
        Set<BioAssay> match = bioAssayToSampleNameMapper.matchAll( bioAssays, sampleName );
        if ( match.size() > 1 ) {
            throw new IllegalStateException( String.format( "There is more than one BioAssay matching %s.", sampleName ) );
        } else if ( match.size() == 1 ) {
            return Optional.of( match.iterator().next() );
        } else {
            return Optional.empty();
        }
    }

    private void checkSampleFactorName() throws IOException {
        if ( sampleFactorName == null ) {
            throw new IllegalStateException( "The sample factor name must be set. Possible values are:\n\t"
                    + getPossibleSampleNameOrCellTypeColumns().entrySet().stream()
                    .map( e -> e.getKey() + ":\t" + e.getValue().stream().limit( 10 ).collect( Collectors.joining( ", " ) ) + ", ..." )
                    .collect( Collectors.joining( "\n\t" ) ) );
        }
    }

    private void checkCellTypeFactorName() throws IOException {
        if ( cellTypeFactorName == null ) {
            throw new IllegalStateException( "A cell type factor name must be set to determine cell type assignments. Possible values are:\n\t"
                    + getPossibleSampleNameOrCellTypeColumns().entrySet().stream()
                    .map( e -> e.getKey() + ":\t" + e.getValue().stream().limit( 10 ).collect( Collectors.joining( ", " ) ) + ", ..." )
                    .collect( Collectors.joining( "\n\t" ) ) );
        }
    }

    /**
     * Obtain a mapping of possible sample name columns with their unique values.
     * <p>
     * TODO: do some more filtering for the suggested columns.
     */
    private Map<String, Set<String>> getPossibleSampleNameOrCellTypeColumns() throws IOException {
        Map<String, Set<String>> candidates = new HashMap<>();
        try ( AnnData f = AnnData.open( file ) ) {
            try ( Dataframe<?> obs = getCellsDataframe( f ) ) {
                for ( String column : obs.getColumns() ) {
                    if ( String.class.isAssignableFrom( obs.getColumnType( column ) ) ) {
                        candidates.put( column, obs.getColumn( column, String.class ).uniqueValues() );
                    }
                }
            }
        }
        return candidates;
    }
}
