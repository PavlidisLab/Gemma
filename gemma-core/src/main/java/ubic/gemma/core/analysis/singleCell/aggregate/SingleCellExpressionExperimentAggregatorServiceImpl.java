package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.nio.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.analysis.singleCell.aggregate.CellLevelCharacteristicsMappingUtils.createMappingByFactorValueCharacteristics;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleEnd;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleStart;

/**
 * Aggregates single-cell expression data.
 */
@Service
@CommonsLog
public class SingleCellExpressionExperimentAggregatorServiceImpl implements SingleCellExpressionExperimentAggregatorService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private SingleCellSparsityMetrics metrics;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Override
    @Transactional
    public QuantitationType aggregateVectorsByCellType( ExpressionExperiment ee, List<BioAssay> cellBAs, AggregateConfig config ) {
        QuantitationType qt = singleCellExpressionExperimentService.getPreferredSingleCellQuantitationType( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred set of single-cell vectors." ) );
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, qt )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a preferred cell type assignment." ) );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( ee + " does not have a cell type factor." ) );
        Map<Characteristic, FactorValue> cellType2Factor = createMappingByFactorValueCharacteristics( cta, cellTypeFactor );
        return aggregateVectors( ee, qt, cellBAs, cta, cellTypeFactor, cellType2Factor, config );
    }

    @Override
    @Transactional
    public QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType qt, List<BioAssay> cellBAs, CellLevelCharacteristics cellLevelCharacteristics, ExperimentalFactor factor, Map<Characteristic, FactorValue> cellType2Factor, AggregateConfig config ) throws UnsupportedScaleTypeForAggregationException {
        // FIXME: this is needed because if EE is not in the session, getSingleCellDataVectors() will retrieve
        //        a distinct QT than that of ee.getQuantitationTypes()
        ee = expressionExperimentService.reload( ee );
        qt = quantitationTypeService.reload( qt );
        SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig vectorInitConfig = SingleCellExpressionExperimentService.SingleCellVectorInitializationConfig.builder()
                .includeCellIds( false )
                .includeData( true )
                .includeDataIndices( true )
                .build();
        Collection<SingleCellExpressionDataVector> vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, qt, vectorInitConfig );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have single-cell vectors for " + qt + "." );
        }

        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        // check the QT and determine how to aggregate its data
        // TODO: support other types and representations for aggregation
        Assert.isTrue( qt.getGeneralType().equals( GeneralType.QUANTITATIVE ), "Only quantitative data can be aggregated." );
        Assert.isTrue( qt.getType().equals( StandardQuantitationType.COUNT ) || qt.getType().equals( StandardQuantitationType.AMOUNT ),
                "Only counts or amounts can be aggregated." );
        SingleCellExpressionAggregationMethod method;
        switch ( qt.getScale() ) {
            case LINEAR:
            case COUNT:
                method = SingleCellExpressionAggregationMethod.SUM;
                break;
            case LOG1P:
                method = SingleCellExpressionAggregationMethod.LOG1P_SUM;
                break;
            case LN:
            case LOG2:
            case LOG10:
            case LOGBASEUNKNOWN:
                method = SingleCellExpressionAggregationMethod.LOG_SUM;
                break;
            default:
                throw new UnsupportedScaleTypeForAggregationException( qt.getScale() );
        }

        log.info( "Aggregating single-cell data with scale " + qt.getScale() + " using " + method + "." );

        // map subpopulation bioassay to their sample
        Map<BioAssay, BioAssay> sourceBioAssayMap = createSourceBioAssayMap( ee, cellBAs );

        // assigne sample to cell types
        Map<BioAssay, Integer> cellTypeIndices = assignSampleToCellTypeIndex( cellBAs, cellLevelCharacteristics, cellType2Factor );

        String cellTypeFactorName;
        if ( factor.getName() != null ) {
            cellTypeFactorName = factor.getName();
        } else if ( factor.getCategory() != null ) {
            cellTypeFactorName = factor.getCategory().getCategory();
        } else {
            log.warn( "Could not find a suitable name for " + factor + ", will default to 'cell type'." );
            cellTypeFactorName = "cell type";
        }

        // this is aligned with how other dimensions are named
        BioAssayDimension newBad = BioAssayDimension.Factory.newInstance( new ArrayList<>( cellBAs ) );
        newBad = bioAssayDimensionService.findOrCreate( newBad );

        boolean canLog2cpm = qt.getType() == StandardQuantitationType.COUNT
                && ( qt.getScale() == ScaleType.LOG2
                || qt.getScale() == ScaleType.LN
                || qt.getScale() == ScaleType.LOG10
                || qt.getScale() == ScaleType.LOG1P
                || qt.getScale() == ScaleType.LINEAR
                || qt.getScale() == ScaleType.COUNT );

        // create vectors now
        QuantitationType newQt = QuantitationType.Factory.newInstance( qt );
        newQt.setName( qt.getName() + " aggregated by " + cellTypeFactorName + ( canLog2cpm ? " (log2cpm)" : "" ) );
        newQt.setDescription( ( StringUtils.isNotBlank( qt.getDescription() ) ? qt.getDescription() + "\n" : "" )
                + "Expression data has been aggregated by " + cellTypeFactorName + " using " + method + "."
                + ( canLog2cpm ? " The data was subsequently converted to log2cpm." : "" ) );
        newQt.setIsPreferred( config.isMakePreferred() );
        // we're always aggregating into doubles, regardless of the input representation
        newQt.setRepresentation( PrimitiveType.DOUBLE );

        Map<BioAssay, Integer> sourceSampleToIndex = ListUtils.indexOfElements( vectors.iterator().next().getSingleCellDimension().getBioAssays() );

        double[] normalizationFactor;
        double[] librarySize;
        Map<BioAssay, Double> sourceSampleLibrarySizeAdjustments = new HashMap<>();
        if ( canLog2cpm ) {
            log.info( "Original data uses the COUNT type, but a log2cpm transformation will be performed, the resulting type for the aggregate will be AMOUNT." );
            newQt.setType( StandardQuantitationType.AMOUNT );
            newQt.setScale( ScaleType.LOG2 );
            // TODO: compute normalization factors from data
            normalizationFactor = new double[cellBAs.size()];
            Arrays.fill( normalizationFactor, 1.0 );
            librarySize = computeLibrarySize( vectors, newBad, cellLevelCharacteristics, sourceBioAssayMap, sourceSampleToIndex, sourceSampleLibrarySizeAdjustments, cellTypeIndices, method, config.isAdjustLibrarySizes() );
            for ( int i = 0; i < librarySize.length; i++ ) {
                if ( librarySize[i] == 0 ) {
                    log.warn( "Library size for " + cellBAs.get( i ) + " is zero, this will cause NaN values in the log2cpm transformation." );
                }
            }
        } else {
            normalizationFactor = null;
            librarySize = null;
        }

        // update sequencing metadata
        if ( librarySize != null ) {
            updateSequencingMetadata( scd, librarySize );
        }

        // sparsity metrics, only needed for preferred QTs
        Map<BioAssay, boolean[]> cellsByBioAssay;
        Map<BioAssay, Integer> designElementsByBioAssay;
        Map<BioAssay, Integer> cellByDesignElementByBioAssay;
        if ( config.isMakePreferred() ) {
            cellsByBioAssay = new HashMap<>();
            designElementsByBioAssay = new HashMap<>();
            cellByDesignElementByBioAssay = new HashMap<>();
        } else {
            cellsByBioAssay = null;
            designElementsByBioAssay = null;
            cellByDesignElementByBioAssay = null;
        }

        Collection<RawExpressionDataVector> rawVectors = new ArrayList<>( vectors.size() );
        for ( SingleCellExpressionDataVector v : vectors ) {
            RawExpressionDataVector rawVector = new RawExpressionDataVector();
            rawVector.setExpressionExperiment( ee );
            rawVector.setQuantitationType( newQt );
            rawVector.setBioAssayDimension( newBad );
            rawVector.setDesignElement( v.getDesignElement() );
            rawVector.setDataAsDoubles( aggregateData( v, newBad, cellLevelCharacteristics, sourceBioAssayMap, sourceSampleToIndex, cellTypeIndices, method, cellsByBioAssay, designElementsByBioAssay, cellByDesignElementByBioAssay, canLog2cpm, normalizationFactor, librarySize ) );
            rawVectors.add( rawVector );
            if ( rawVectors.size() % 1000 == 0 ) {
                log.info( String.format( "Aggregated %d/%d single-cell vectors.", rawVectors.size(), vectors.size() ) );
            }
        }

        if ( config.isMakePreferred() ) {
            log.info( "Applying single-cell sparsity metrics to the aggregated assays..." );
            for ( BioAssay ba : cellBAs ) {
                assert cellsByBioAssay != null;
                boolean[] expressedCells = cellsByBioAssay.get( ba );
                if ( expressedCells != null ) {
                    int count = 0;
                    for ( boolean b : expressedCells ) {
                        if ( b ) {
                            count++;
                        }
                    }
                    ba.setNumberOfCells( count );
                } else {
                    ba.setNumberOfCells( 0 );
                }
                ba.setNumberOfDesignElements( designElementsByBioAssay.getOrDefault( ba, 0 ) );
                ba.setNumberOfCellsByDesignElements( cellByDesignElementByBioAssay.getOrDefault( ba, 0 ) );
            }
            bioAssayService.update( cellBAs );
        }

        int newVecs = expressionExperimentService.addRawDataVectors( ee, newQt, rawVectors );
        String note = String.format( "Created %d aggregated raw vectors for %s.", newVecs, newQt );
        StringBuilder details = new StringBuilder();
        details.append( "Single-cell quantitation type: " ).append( qt ).append( "\n" );
        details.append( "Single-cell dimension: " ).append( scd ).append( "\n" );
        details.append( "Aggregated assays:" );
        for ( int i = 0; i < cellBAs.size(); i++ ) {
            BioAssay cellBa = cellBAs.get( i );
            details.append( "\n" ).append( "\t" ).append( cellBa );
            if ( config.isMakePreferred() ) {
                details.append( " Number of cells=" ).append( cellBa.getNumberOfCells() )
                        .append( " Number of design elements=" ).append( cellBa.getNumberOfDesignElements() )
                        .append( " Number of cells x design elements=" ).append( cellBa.getNumberOfCellsByDesignElements() );
            }
            if ( librarySize != null ) {
                if ( librarySize[i] == 0 ) {
                    details.append( " Library Size is zero, the aggregate is filled with NAs" );
                } else {
                    details.append( " Library Size=" ).append( String.format( "%.2f", librarySize[i] ) );
                    Double lsa = sourceSampleLibrarySizeAdjustments.get( sourceBioAssayMap.get( cellBa ) );
                    if ( lsa != null && lsa != 1.0 ) {
                        details.append( " (adjusted from " ).append( String.format( "%.2f", librarySize[i] / lsa ) ).append( " due to unmapped genes)" );
                    }
                }
            }
        }
        log.info( note + "\n" + details );
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class, note, details.toString() );

        return newQt;
    }

    private void updateSequencingMetadata( SingleCellDimension scd, double[] librarySize ) {
        for ( int i = 0; i < scd.getBioAssays().size(); i++ ) {
            BioAssay ba = scd.getBioAssays().get( i );
            ba.setSequenceReadCount( Math.round( librarySize[i] ) );
        }
        bioAssayService.update( scd.getBioAssays() );
    }

    private Map<BioAssay, BioAssay> createSourceBioAssayMap( ExpressionExperiment ee, Collection<BioAssay> cellBAs ) {
        Map<BioAssay, BioAssay> sourceBioAssayMap = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            Assert.notNull( ba.getSampleUsed().getSourceBioMaterial(),
                    ba + "'s sample does not have a source biomaterial." );
            Set<BioAssay> sourceBAs = ee.getBioAssays().stream()
                    .filter( ba.getSampleUsed().getSourceBioMaterial().getBioAssaysUsedIn()::contains )
                    .collect( Collectors.toSet() );
            if ( sourceBAs.isEmpty() ) {
                throw new IllegalStateException( ba + " does not have a source BioAssay in " + ee );
            } else if ( sourceBAs.size() > 1 ) {
                throw new IllegalStateException( ba + " has more than one source BioAssay in " + ee );
            }
            sourceBioAssayMap.put( ba, sourceBAs.iterator().next() );
        }
        return sourceBioAssayMap;
    }

    private Map<BioAssay, Integer> assignSampleToCellTypeIndex( Collection<BioAssay> cellBAs, CellLevelCharacteristics cta, Map<Characteristic, FactorValue> cellType2Factor ) {
        Map<BioAssay, Integer> cellTypes = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            boolean found = false;
            List<Characteristic> types = cta.getCharacteristics();
            for ( int i = 0; i < types.size(); i++ ) {
                FactorValue fv = cellType2Factor.get( types.get( i ) );
                if ( fv != null && ba.getSampleUsed().getAllFactorValues().contains( fv ) ) {
                    cellTypes.put( ba, i );
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                throw new IllegalStateException( ba + " does not have an assigned cell type, make sure that its counterpart " + ba.getSampleUsed() + " has a cell type factor assigned." );
            }
        }
        return cellTypes;
    }

    /**
     * Compute the library size for each sample.
     */
    private double[] computeLibrarySize( Collection<SingleCellExpressionDataVector> vectors, BioAssayDimension bad, CellLevelCharacteristics cta, Map<BioAssay, BioAssay> sourceBioAssayMap, Map<BioAssay, Integer> sourceSampleToIndex, Map<BioAssay, Double> sourceSampleLibrarySizeAdjustments, Map<BioAssay, Integer> cellTypeIndices, SingleCellExpressionAggregationMethod method, boolean adjustLibrarySizes ) throws IllegalStateException {
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        int numSourceSamples = sourceSampleToIndex.size();
        // library sizes of the sub-assays
        double[] librarySize = new double[numSamples];
        // library sizes of the source assays
        double[] sourceLibrarySize = new double[numSourceSamples];
        for ( SingleCellExpressionDataVector scv : vectors ) {
            PrimitiveType representation = scv.getQuantitationType().getRepresentation();
            Buffer scrv = scv.getDataAsBuffer();
            for ( int i = 0; i < numSamples; i++ ) {
                BioAssay sample = samples.get( i );
                BioAssay sourceSample = sourceBioAssayMap.get( sample );
                // comparing index is *much* faster than checking for equality
                int cellTypeIndex = cellTypeIndices.get( sample );
                int sourceSampleIndex = requireNonNull( sourceSampleToIndex.get( sourceSample ),
                        () -> "Could not locate the source sample of " + sample + " (" + sourceSample + ") in " + scv.getSingleCellDimension() + "." );
                // samples are not necessarily ordered, so we cannot use the start=end trick
                int start = getSampleStart( scv, sourceSampleIndex, 0 );
                int end = getSampleEnd( scv, sourceSampleIndex, start );
                for ( int k = start; k < end; k++ ) {
                    double unscaledValue;
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        unscaledValue = getDouble( scrv, k, representation );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        unscaledValue = Math.exp( getDouble( scrv, k, representation ) );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        unscaledValue = Math.expm1( getDouble( scrv, k, representation ) );
                    } else {
                        throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
                    }
                    if ( cellTypeIndex == cta.getIndices()[scv.getDataIndices()[k]] ) {
                        librarySize[i] += unscaledValue;
                    }
                    sourceLibrarySize[sourceSampleIndex] += unscaledValue;
                }
            }
        }
        if ( adjustLibrarySizes ) {
            log.info( "Adjusting library sizes..." );
            for ( Map.Entry<BioAssay, Integer> e : sourceSampleToIndex.entrySet() ) {
                BioAssay sourceSample = e.getKey();
                int sourceSampleIndex = e.getValue();
                if ( sourceSample.getSequenceReadCount() == null )
                    continue;
                if ( sourceSample.getSequenceReadCount() < sourceLibrarySize[sourceSampleIndex] ) {
                    throw new IllegalStateException(
                            String.format( "The library size for %s (%.2f) exceeds the number of reads (%d).",
                                    sourceSample, sourceLibrarySize[sourceSampleIndex], sourceSample.getSequenceReadCount() ) );

                }
                sourceSampleLibrarySizeAdjustments.put( sourceSample, sourceSample.getSequenceReadCount() / sourceLibrarySize[e.getValue()] );
            }
            // adjust library sizes
            for ( int i = 0; i < librarySize.length; i++ ) {
                BioAssay sample = bad.getBioAssays().get( i );
                BioAssay sourceSample = sourceBioAssayMap.get( sample );
                Double adjustment = sourceSampleLibrarySizeAdjustments.get( sourceSample );
                if ( adjustment == null ) {
                    continue;
                }
                // this will scale the library size to the number of reads in the source sample instead of the number of
                // reads that we recorded in the vectors
                librarySize[i] *= adjustment;
            }
        }
        return librarySize;
    }

    /**
     * Aggregate the single-cell data to match the target BAD.
     *
     * @param performLog2cpm whether to perform log2cpm transformation or not, if provided librarySize must be set
     * @param librarySize    library size for each sample, used for log2cpm transformation
     */
    private double[] aggregateData(
            SingleCellExpressionDataVector scv,
            BioAssayDimension bad,
            CellLevelCharacteristics cta,
            Map<BioAssay, BioAssay> sourceBioAssayMap,
            Map<BioAssay, Integer> sourceSampleToIndex,
            Map<BioAssay, Integer> cellTypeIndices,
            SingleCellExpressionAggregationMethod method,
            @Nullable Map<BioAssay, boolean[]> cellsByBioAssay,
            @Nullable Map<BioAssay, Integer> designElementsByBioAssay,
            @Nullable Map<BioAssay, Integer> cellByDesignElementByBioAssay,
            boolean performLog2cpm,
            @Nullable double[] normalizationFactor,
            @Nullable double[] librarySize ) {
        Assert.isTrue( !performLog2cpm || ( normalizationFactor != null && librarySize != null ),
                "Normalization factors and library size must be provided for log2cpm transformation." );
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        double[] rv = new double[numSamples];
        Buffer scrv = scv.getDataAsBuffer();
        PrimitiveType representation = scv.getQuantitationType().getRepresentation();
        for ( int i = 0; i < numSamples; i++ ) {
            BioAssay sample = samples.get( i );
            BioAssay sourceSample = sourceBioAssayMap.get( sample );
            int sourceSampleIndex = requireNonNull( sourceSampleToIndex.get( sourceSample ),
                    () -> "Could not locate the source sample of " + sample + " (" + sourceSample + ") in " + scv.getSingleCellDimension() + "." );
            Integer cellTypeIndex = cellTypeIndices.get( sample );
            // samples are not necessarily ordered, so we cannot use the start=end trick
            int start = getSampleStart( scv, sourceSampleIndex, 0 );
            int end = getSampleEnd( scv, sourceSampleIndex, start );
            rv[i] = 0;
            for ( int k = start; k < end; k++ ) {
                if ( cellTypeIndex == cta.getIndices()[scv.getDataIndices()[k]] ) {
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        rv[i] += getDouble( scrv, k, representation );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        rv[i] += Math.exp( getDouble( scrv, k, representation ) );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        rv[i] += Math.expm1( getDouble( scrv, k, representation ) );
                    } else {
                        throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
                    }
                }
            }

            if ( performLog2cpm ) {
                if ( librarySize[i] == 0 ) {
                    // this is technically a 0/0 situation
                    rv[i] = Double.NaN;
                } else {
                    rv[i] = Math.log( 1e6 * normalizationFactor[i] * ( rv[i] + 0.5 ) / ( librarySize[i] + 1.0 ) ) / Math.log( 2 );
                }
            } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                rv[i] = Math.log( rv[i] );
            } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                rv[i] = Math.log1p( rv[i] );
            } else {
                throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
            }

            if ( cellsByBioAssay != null ) {
                cellsByBioAssay.compute( sample, ( k, v ) -> {
                    if ( v == null ) {
                        v = new boolean[scv.getSingleCellDimension().getNumberOfCells()];
                    }
                    metrics.addExpressedCells( scv, sourceSampleIndex, cta, cellTypeIndex, v );
                    return v;
                } );
            }
            if ( designElementsByBioAssay != null ) {
                designElementsByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + metrics.getNumberOfDesignElements( Collections.singleton( scv ), sourceSampleIndex, cta, cellTypeIndex ) );
            }
            if ( cellByDesignElementByBioAssay != null ) {
                cellByDesignElementByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + metrics.getNumberOfCellsByDesignElements( Collections.singleton( scv ), sourceSampleIndex, cta, cellTypeIndex ) );
            }

        }

        return rv;
    }

    private double getDouble( Buffer buffer, int k, PrimitiveType representation ) {
        switch ( representation ) {
            case FLOAT:
                return ( ( FloatBuffer ) buffer ).get( k );
            case DOUBLE:
                return ( ( DoubleBuffer ) buffer ).get( k );
            case INT:
                return ( ( IntBuffer ) buffer ).get( k );
            case LONG:
                return ( ( LongBuffer ) buffer ).get( k );
            default:
                throw new UnsupportedOperationException( "Unsupported representation " + representation );
        }
    }

    @Override
    @Transactional
    public int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt ) {
        return removeAggregatedVectors( ee, qt, false );
    }

    @Override
    @Transactional
    public int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt, boolean keepDimension ) {
        // this is needed because the raw vectors must be loaded
        ee = expressionExperimentService.reload( ee );
        qt = quantitationTypeService.reload( qt );
        // gather all the assays that the aggregated vectors were using
        Collection<BioAssay> bioAssays;
        BioAssayDimension dimension = expressionExperimentService.getBioAssayDimension( ee, qt, RawExpressionDataVector.class );
        if ( dimension != null ) {
            bioAssays = dimension.getBioAssays();
        } else {
            log.warn( "No BioAssayDimension found for " + qt + " in " + ee + "." );
            bioAssays = Collections.emptyList();
        }
        int removedVectors = expressionExperimentService.removeRawDataVectors( ee, qt, keepDimension );
        // clear sparsity metrics if we are removing the preferred QT
        if ( qt.getIsPreferred() ) {
            log.info( "Clearing sparsity metrics on " + bioAssays.size() + " assays since we're removing preferred aggregated vectors..." );
            for ( BioAssay ba : bioAssays ) {
                ba.setNumberOfCells( null );
                ba.setNumberOfDesignElements( null );
                ba.setNumberOfCellsByDesignElements( null );
                bioAssayService.update( ba );
            }
        }
        return removedVectors;
    }

    /**
     * Methods for aggregating single-cell expression data.
     */
    public enum SingleCellExpressionAggregationMethod {
        /**
         * Aggregate data by summing it.
         */
        SUM,
        /**
         * Equivalent to {@link #SUM} for log-transformed data.
         */
        LOG_SUM,
        /**
         * Equivalent to {@link #SUM} for data transformed by {@code log 1 + X}
         */
        LOG1P_SUM;
    }
}
