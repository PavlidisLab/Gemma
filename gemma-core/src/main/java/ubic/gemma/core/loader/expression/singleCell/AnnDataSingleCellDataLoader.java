package ubic.gemma.core.loader.expression.singleCell;

import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.anndata.*;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5FundamentalType;
import ubic.gemma.core.loader.util.hdf5.H5Type;
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

    /**
     * Transpose obs/var dataframes.
     */
    private boolean transpose = false;

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
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> v = getCellsDataframe( h5File ) ) {
            return v.getColumn( sampleFactorName, String.class ).uniqueValues();
        }
    }

    @Override
    public SingleCellDimension getSingleCellDimension( Collection<BioAssay> bioAssays ) throws IOException {
        Assert.isTrue( !bioAssays.isEmpty(), "At least one bioassay must be provided" );
        Assert.notNull( sampleNameComparator, "A sample name comparator is necessary to match samples to BioAssays." );
        Assert.notNull( sampleFactorName, "The sample factor name must be set." );
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
        try ( AnnData h5File = AnnData.open( file ) ) {
            if ( h5File.getX() != null ) {
                qts.add( createQt( h5File, h5File.getX() ) );
            }
            for ( String layer : h5File.getLayers() ) {
                qts.add( createQt( h5File, h5File.getLayer( layer ) ) );
            }
        }
        return qts;
    }

    private QuantitationType createQt( AnnData h5File, Layer layer ) {
        QuantitationType qt = new QuantitationType();
        qt.setName( layer.getPath() );
        qt.setDescription( "AnnData data from layer " + layer.getPath() + " in " + this.file.getFileName().toString() + "." );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        H5FundamentalType fundamentalType;
        try ( Matrix matrix = layer.getMatrix(); H5Type datasetType = matrix.getDataType() ) {
            fundamentalType = datasetType.getFundamentalType();
        }
        if ( fundamentalType.equals( H5FundamentalType.INTEGER ) ) {
            qt.setType( StandardQuantitationType.COUNT );
            qt.setScale( ScaleType.COUNT );
            // TODO: support integer QTs
            qt.setRepresentation( PrimitiveType.DOUBLE );
        } else if ( fundamentalType.equals( H5FundamentalType.FLOAT ) ) {
            qt.setType( StandardQuantitationType.AMOUNT );
            qt.setScale( detectScale( h5File, layer ) );
            qt.setRepresentation( PrimitiveType.DOUBLE );
        } else {
            throw new IllegalArgumentException( "Unsupported H5 fundamental type " + fundamentalType + " for a quantitation type." );
        }
        qt.setDescription( String.format( "Data from a layer located at '%s' originally encoded as an %s of %ss.",
                layer.getPath(), layer.getEncodingType(), fundamentalType.toString().toLowerCase() ) );
        log.info( "Detected quantitation type for '" + layer.getPath() + "': " + qt );
        return qt;
    }

    private ScaleType detectScale( AnnData h5File, Layer layer ) {
        if ( "X".equals( layer.getPath() ) ) {
            try ( Mapping uns = h5File.getUns() ) {
                if ( uns != null && uns.getKeys().contains( "log1p" ) ) {
                    log.info( h5File + " contains unstructured data describing the scale type: " + ScaleType.LOG1P + ", using it for X." );
                    return ScaleType.LOG1P;
                }
            }
        }
        // FIXME: infer scale from data using the logic from ExpressionDataDoubleMatrixUtil.inferQuantitationType()
        log.warn( "Scale type cannot be detected for non-counting data in " + layer.getPath() + "." );
        return ScaleType.OTHER;
    }

    @Override
    public Set<CellTypeAssignment> getCellTypeAssignments( SingleCellDimension dimension ) throws IOException {
        Assert.notNull( cellTypeFactorName, "A cell type factor name must be set to determine cell type assignments." );
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> var = getCellsDataframe( h5File ) ) {
            // TODO: support cell types encoded as string-array
            CategoricalArray<String> cellTypes = var.getCategoricalColumn( cellTypeFactorName, String.class );
            CellTypeAssignment assignment = new CellTypeAssignment();
            int unknownCellTypeCode = -1;
            for ( int i = 0; i < cellTypes.getCategories().length; i++ ) {
                String ct = cellTypes.getCategories()[i];
                if ( ct.equals( unknownCellTypeIndicator ) ) {
                    if ( unknownCellTypeCode != -1 ) {
                        throw new IllegalStateException( "There is not than one unknown cell type indicator." );
                    }
                    log.info( h5File + " uses a special indicator for unknown cell types: " + ct + " with code: " + i + ", its occurrences will be replaced with -1." );
                    unknownCellTypeCode = i;
                    continue;
                }
                assignment.getCellTypes().add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, ct, null ) );
            }
            if ( unknownCellTypeIndicator != null && unknownCellTypeCode == -1 ) {
                throw new IllegalStateException( String.format( "The unknown cell type indicator %s was not found. Possible values are: %s. If none of these indicate a missing cell type, set the indicator to null.",
                        unknownCellTypeIndicator, String.join( ", ", cellTypes.getCategories() ) ) );
            }
            assignment.setNumberOfCellTypes( assignment.getCellTypes().size() );
            if ( unknownCellTypeCode != -1 ) {
                // rewrite unknown codes
                for ( int i = 0; i < cellTypes.getCodes().length; i++ ) {
                    if ( cellTypes.getCodes()[i] == unknownCellTypeCode ) {
                        cellTypes.getCodes()[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
                    }
                }
            }
            assignment.setCellTypeIndices( cellTypes.getCodes() );
            return Collections.singleton( assignment );
        }
    }

    @Override
    public Set<CellLevelCharacteristics> getOtherCellLevelCharacteristics( SingleCellDimension dimension ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
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
                // conclusion, this is a cell-type factor
                int[] indices = new int[values.length];
                List<Characteristic> characteristics = new ArrayList<>();
                for ( int i = 0; i < values.length; i++ ) {
                    String val = values[i];
                    if ( val != null ) {
                        Characteristic c = Characteristic.Factory.newInstance( Categories.UNCATEGORIZED, val, null );
                        c.setDescription( "Imported from column " + factorName + " in AnnData file " + h5File + "." );
                        int j = characteristics.indexOf( c );
                        if ( j == -1 ) {
                            characteristics.add( c );
                            j = characteristics.size() - 1;
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
    public Set<ExperimentalFactor> getFactors( Collection<BioAssay> samples, @Nullable Map<BioMaterial, Set<FactorValue>> factorValueAssignments ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
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
                                    for ( BioAssay sample : sampleNameComparator.match( samples, sn ) ) {
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
                                    for ( BioAssay sample : sampleNameComparator.match( samples, sn ) ) {
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
    public Map<BioMaterial, Set<Characteristic>> getSamplesCharacteristics( Collection<BioAssay> samples ) throws IOException {
        Assert.notNull( sampleFactorName, "A sample factor name must be set." );
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
                            .ifPresent( fvs -> fvs.forEach( ( sampleName, value ) -> sampleNameComparator.match( samples, sampleName )
                                    .forEach( ba -> result.computeIfAbsent( ba.getSampleUsed(), k -> new HashSet<>() )
                                            .add( Characteristic.Factory.newInstance( factorName, null, value, null ) ) ) ) );
                }
            }
        }
        return result;
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
    private <T> Optional<Map<String, T>> extractSingleValueBySampleName( Dataframe.Column<?, String> sampleNames, T[] values ) {
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
        try ( AnnData h5File = AnnData.open( file ); Dataframe<?> obs = getGenesDataframe( h5File ) ) {
            return obs.getColumn( obs.getIndexColumn(), String.class ).uniqueValues();
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
        AnnData h5File = AnnData.open( file );
        try {
            // load genes
            Dataframe.Column<?, String> genes;
            try ( Dataframe<?> obs = getGenesDataframe( h5File ) ) {
                genes = obs.getColumn( obs.getIndexColumn(), String.class );
            }
            Set<String> genesSet = genes.uniqueValues();
            if ( !CollectionUtils.containsAny( genesSet, elementsMapping.keySet() ) ) {
                throw new IllegalArgumentException( "None of the genes are present in the elements mapping." );
            }
            Set<String> unknownGenes = SetUtils.difference( genesSet, elementsMapping.keySet() );
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
            if ( quantitationType.getName().startsWith( "layers/" ) ) {
                String layerName = quantitationType.getName().substring( "layers/".length() );
                layer = h5File.getLayer( layerName );
            } else {
                layer = requireNonNull( h5File.getX(), h5File + " does not have a layer for path 'X'." );
            }
            String matrixEncodingType = layer.getType();
            if ( ( matrixEncodingType.equals( "csr_matrix" ) && !transpose ) || ( matrixEncodingType.equals( "csc_matrix" ) && transpose ) ) {
                return loadVectorsFromSparseMatrix( layer.getSparseMatrix(), samples, genes, quantitationType, dimension, elementsMapping )
                        .onClose( h5File::close );
            } else if ( matrixEncodingType.equals( "csr_matrix" ) ) {
                throw new UnsupportedOperationException( "The matrix at '" + quantitationType.getName() + "' is stored as CSR and transposition is enabled; it must be converted to CSC for being loaded." );
            } else if ( matrixEncodingType.equals( "csc_matrix" ) ) {
                throw new UnsupportedOperationException( "The matrix at '" + quantitationType.getName() + "' is stored as CSC; it must be converted to CSR for being loaded." );
            } else {
                throw new UnsupportedOperationException( "Loading single-cell data from " + matrixEncodingType + " is not supported." );
            }
        } catch ( Throwable e ) {
            h5File.close();
            throw e;
        }
    }

    private Stream<SingleCellExpressionDataVector> loadVectorsFromSparseMatrix( SparseMatrix matrix, Dataframe.Column<?, String> samples, Dataframe.Column<?, String> genes, QuantitationType qt, SingleCellDimension scd, Map<String, CompositeSequence> elementsMapping ) {
        Assert.isTrue( genes.size() == matrix.getShape()[0],
                "The number of supplied genes does not match the number of rows in the sparse matrix." );
        Assert.isTrue( samples.size() == matrix.getShape()[1],
                "The number of supplied samples does not match the number of columns in the sparse matrix." );
        Assert.isTrue( scd.getCellIds().size() <= matrix.getShape()[1],
                "The number of cells in the dimension cannot exceed the number of columns in the sparse matrix." );

        // build a sample offset index for efficiently selecting samples
        // if a sample does not have a corresponding BioAssay (i.e. an unwanted sample), it is set to null
        int numberOfSamples = samples.uniqueValues().size();
        List<BioAssay> samplesBioAssay = new ArrayList<>( numberOfSamples );
        List<String> samplesName = new ArrayList<>( numberOfSamples );
        int[] samplesBioAssayOffset = new int[numberOfSamples];
        int W = 0;
        String currentSample = null;
        for ( int i = 0; i < samples.size(); i++ ) {
            String sample = requireNonNull( samples.get( i ), "Sample name cannot be missing." );
            if ( !sample.equals( currentSample ) ) {
                currentSample = sample;
                samplesBioAssayOffset[W++] = i;
                samplesBioAssay.add( getBioAssayBySampleName( scd.getBioAssays(), sample ).orElse( null ) );
                samplesName.add( sample );
            }
        }

        if ( samplesBioAssay.size() != numberOfSamples ) {
            throw new IllegalStateException( "The number of distinct contiguous sample does not match the number of distinct samples, this is likely due to the samples being unsorted." );
        }

        // in the simple scenario, the SCD sample layout exactly match what is present in he data, so we don't need to
        // do anything in particular
        boolean isSimpleCase = scd.getBioAssays().equals( samplesBioAssay );

        if ( !isSimpleCase ) {
            // map BAs to the correponding sample name in data
            String scdNames = scd.getBioAssays().stream()
                    .map( ba -> samplesName.get( samplesBioAssay.indexOf( ba ) ) )
                    .collect( Collectors.joining( ", " ) );
            log.info( "Samples in the data do not follow the same layout, will have to slice and adjust indices.\n"
                    + "\tIn data: " + String.join( ", ", samplesName ) + "\n"
                    + "\tIn single-cell dimension: " + scdNames );
        }

        return IntStream.range( 0, matrix.getShape()[0] )
                // ignore entries that are missing a gene mapping
                .filter( i -> elementsMapping.containsKey( genes.get( i ) ) )
                .mapToObj( i -> {
                    assert matrix.getIndptr()[i] < matrix.getIndptr()[i + 1];
                    SingleCellExpressionDataVector vector = new SingleCellExpressionDataVector();
                    vector.setDesignElement( elementsMapping.get( genes.get( i ) ) );
                    vector.setSingleCellDimension( scd );
                    vector.setQuantitationType( qt );
                    int[] IX;
                    try ( H5Dataset indices = matrix.getIndices() ) {
                        IX = indices.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toIntegerVector();
                    }
                    if ( !ArrayUtils.isSorted( IX ) ) {
                        // this is annoying, AnnData does not guarantee that indices are sorted
                        // https://github.com/scverse/anndata/issues/1388
                        throw new IllegalStateException( String.format( "Indices for %s are not sorted.", genes.get( i ) ) );
                    }
                    // simple case: the number of BAs match the number of samples and the sample order match
                    if ( isSimpleCase ) {
                        // this is using the same storage strategy from ByteArrayConverter
                        try ( H5Dataset data = matrix.getData() ) {
                            vector.setData( data.slice( matrix.getIndptr()[i], matrix.getIndptr()[i + 1] ).toByteVector( H5Type.IEEE_F64BE ) );
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
                            int j = samplesBioAssay.indexOf( ba );
                            assert j != -1;
                            int start = Arrays.binarySearch( IX, samplesBioAssayOffset[j] );
                            if ( start < 0 ) {
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
                            try ( H5Dataset data = matrix.getData() ) {
                                data.slice( start, end ).toByteVector( vectorData, sampleOffsetInVector, sampleOffsetInVector + sampleNnz, H5Type.IEEE_F64BE );
                            }
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

    private Dataframe<?> getCellsDataframe( AnnData h5File ) {
        return transpose ? h5File.getObs() : h5File.getVar();
    }

    private Dataframe<?> getGenesDataframe( AnnData h5File ) {
        return transpose ? h5File.getVar() : h5File.getObs();
    }

    private Optional<BioAssay> getBioAssayBySampleName( Collection<BioAssay> bioAssays, String sampleName ) {
        // lookup the sample
        Set<BioAssay> match = sampleNameComparator.match( bioAssays, sampleName );
        if ( match.size() > 1 ) {
            throw new IllegalStateException( String.format( "There is more than one BioAssay matching %s.", sampleName ) );
        } else if ( match.size() == 1 ) {
            return Optional.of( match.iterator().next() );
        } else {
            return Optional.empty();
        }
    }
}
