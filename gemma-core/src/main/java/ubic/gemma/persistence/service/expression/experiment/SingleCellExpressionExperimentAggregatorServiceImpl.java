package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
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
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleEnd;
import static ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVectorUtils.getSampleStart;
import static ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentSplitServiceImpl.mapCellTypeAssignmentToCellTypeFactor;

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

    @Override
    @Transactional
    public QuantitationType aggregateVectors( ExpressionExperiment ee, QuantitationType quantitationType, List<BioAssay> cellBAs, boolean makePreferred ) {
        // FIXME: this is needed because if EE is not in the session, getPreferredSingleCellDataVectors() will retrieve
        //        a distinct QT than that of ee.getQuantitationTypes()
        ee = expressionExperimentService.loadOrFail( ee.getId() );
        Collection<SingleCellExpressionDataVector> vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, quantitationType );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have single-cell vectors for " + quantitationType + "." );
        }
        ExpressionExperiment finalEe = ee;
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee, quantitationType )
                .orElseThrow( () -> new IllegalStateException( finalEe + " does not have a preferred cell type assignment." ) );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( finalEe + " does not have a cell type factor." ) );
        SingleCellDimension scd = vectors.iterator().next().getSingleCellDimension();
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        // check the QT and determine how to aggregate its data
        // TODO: support other types and representations for aggregation
        Assert.isTrue( qt.getGeneralType().equals( GeneralType.QUANTITATIVE ), "Only quantitative data can be aggregated." );
        Assert.isTrue( qt.getType().equals( StandardQuantitationType.COUNT ) || qt.getType().equals( StandardQuantitationType.AMOUNT ),
                "Only counts or amounts can be aggregated." );
        Assert.isTrue( qt.getRepresentation().equals( PrimitiveType.DOUBLE ),
                "Only vectors of doubles can be aggregated." );
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

        Map<Characteristic, FactorValue> cellType2Factor = mapCellTypeAssignmentToCellTypeFactor( cta, cellTypeFactor );

        // assigne sample to cell types
        Map<BioAssay, Characteristic> cellTypes = assignSampleToCellType( cellBAs, cta, cellType2Factor );

        String cellTypeFactorName;
        if ( cellTypeFactor.getName() != null ) {
            cellTypeFactorName = cellTypeFactor.getName();
        } else if ( cellTypeFactor.getCategory() != null ) {
            cellTypeFactorName = cellTypeFactor.getCategory().getCategory();
        } else {
            log.warn( "Could not find a suitable name for " + cellTypeFactor + ", will default to 'cell type'." );
            cellTypeFactorName = "cell type";
        }

        // TODO: reuse an existing BAD with the same layout
        BioAssayDimension newBad = new BioAssayDimension();
        newBad.setName( scd.getName() + " aggregated by " + cellTypeFactorName );
        newBad.setDescription( scd.getDescription() );
        newBad.setBioAssays( cellBAs );
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
        newQt.setIsPreferred( makePreferred );

        Map<BioAssay, Integer> sourceSampleToIndex = ListUtils.indexOfElements( vectors.iterator().next().getSingleCellDimension().getBioAssays() );

        double[] normalizationFactor;
        double[] librarySize;
        if ( canLog2cpm ) {
            log.info( "Original data uses the COUNT type, but a log2cpm transformation will be performed, the resulting type for the aggregate will be AMOUNT." );
            newQt.setType( StandardQuantitationType.AMOUNT );
            newQt.setScale( ScaleType.LOG2 );
            // TODO: compute normalization factors from data
            normalizationFactor = new double[cellBAs.size()];
            Arrays.fill( normalizationFactor, 1.0 );
            librarySize = computeLibrarySize( vectors, newBad, cta, sourceBioAssayMap, sourceSampleToIndex, cellTypes, method );
            for ( int i = 0; i < librarySize.length; i++ ) {
                if ( librarySize[i] == 0 ) {
                    log.warn( "Library size for " + cellBAs.get( i ) + " is zero, this will cause NaN values in the log2cpm transformation." );
                }
            }
        } else {
            normalizationFactor = null;
            librarySize = null;
        }

        // sparsity metrics, only needed for preferred QTs
        Map<BioAssay, boolean[]> cellsByBioAssay;
        Map<BioAssay, Integer> designElementsByBioAssay;
        Map<BioAssay, Integer> cellByDesignElementByBioAssay;
        if ( makePreferred ) {
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
            rawVector.setDataAsDoubles( aggregateData( v, newBad, cta, sourceBioAssayMap, sourceSampleToIndex, cellTypes, method, cellsByBioAssay, designElementsByBioAssay, cellByDesignElementByBioAssay, canLog2cpm, normalizationFactor, librarySize ) );
            rawVectors.add( rawVector );
        }

        if ( makePreferred ) {
            log.info( "Applying single-cell sparsity metrics to the aggregated assays..." );
            for ( BioAssay ba : cellBAs ) {
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
            if ( makePreferred ) {
                details.append( " Number of cells=" ).append( cellBa.getNumberOfCells() )
                        .append( " Number of design elements=" ).append( cellBa.getNumberOfDesignElements() )
                        .append( " Number of cells x design elements=" ).append( cellBa.getNumberOfCellsByDesignElements() );
            }
            if ( librarySize != null ) {
                if ( librarySize[i] == 0 ) {
                    details.append( " Library Size is zero, the aggregate is filled with NAs" );
                } else {
                    details.append( " Library Size=" ).append( librarySize[i] );
                }
            }
        }
        log.info( note + "\n" + details );
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class, note, details.toString() );

        return newQt;
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

    private Map<BioAssay, Characteristic> assignSampleToCellType( Collection<BioAssay> cellBAs, CellTypeAssignment cta, Map<Characteristic, FactorValue> cellType2Factor ) {
        Map<BioAssay, Characteristic> cellTypes = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            boolean found = false;
            for ( Characteristic ct : cta.getCellTypes() ) {
                FactorValue fv = cellType2Factor.get( ct );
                if ( fv != null && ba.getSampleUsed().getAllFactorValues().contains( fv ) ) {
                    cellTypes.put( ba, ct );
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
    private double[] computeLibrarySize( Collection<SingleCellExpressionDataVector> vectors, BioAssayDimension bad, CellTypeAssignment cta, Map<BioAssay, BioAssay> sourceBioAssayMap, Map<BioAssay, Integer> sourceSampleToIndex, Map<BioAssay, Characteristic> cellTypes, SingleCellExpressionAggregationMethod method ) {
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        double[] librarySize = new double[numSamples];
        for ( SingleCellExpressionDataVector scv : vectors ) {
            DoubleBuffer scrv = ByteBuffer.wrap( scv.getData() ).asDoubleBuffer();
            for ( int i = 0; i < numSamples; i++ ) {
                BioAssay sample = samples.get( i );
                BioAssay sourceSample = sourceBioAssayMap.get( sample );
                Characteristic cellType = cellTypes.get( sample );
                int sourceSampleIndex = requireNonNull( sourceSampleToIndex.get( sourceSample ),
                        () -> "Could not locate the source sample of " + sample + " (" + sourceSample + ") in " + scv.getSingleCellDimension() + "." );
                int start = getSampleStart( scv, sourceSampleIndex, 0 );
                int end = getSampleEnd( scv, sourceSampleIndex, start );
                for ( int k = start; k < end; k++ ) {
                    if ( cellType.equals( cta.getCellType( k ) ) ) {
                        if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                            librarySize[i] += scrv.get( k );
                        } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                            librarySize[i] += Math.exp( scrv.get( k ) );
                        } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                            librarySize[i] += Math.expm1( scrv.get( k ) );
                        } else {
                            throw new UnsupportedOperationException( "Unsupported aggregation method: " + method );
                        }
                    }
                }
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
            CellTypeAssignment cta,
            Map<BioAssay, BioAssay> sourceBioAssayMap,
            Map<BioAssay, Integer> sourceSampleToIndex,
            Map<BioAssay, Characteristic> cellTypes,
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
        DoubleBuffer scrv = ByteBuffer.wrap( scv.getData() ).asDoubleBuffer();
        for ( int i = 0; i < numSamples; i++ ) {
            BioAssay sample = samples.get( i );
            BioAssay sourceSample = sourceBioAssayMap.get( sample );
            int sourceSampleIndex = requireNonNull( sourceSampleToIndex.get( sourceSample ),
                    () -> "Could not locate the source sample of " + sample + " (" + sourceSample + ") in " + scv.getSingleCellDimension() + "." );
            Characteristic cellType = cellTypes.get( sample );
            // samples are not necessarily ordered, so we cannot use the start=end trick
            int start = getSampleStart( scv, sourceSampleIndex, 0 );
            int end = getSampleEnd( scv, sourceSampleIndex, start );
            rv[i] = 0;
            for ( int k = start; k < end; k++ ) {
                if ( cellType.equals( cta.getCellType( k ) ) ) {
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        rv[i] += scrv.get( k );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        rv[i] += Math.exp( scrv.get( k ) );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        rv[i] += Math.expm1( scrv.get( k ) );
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
                    metrics.addExpressedCells( scv, sourceSampleIndex, cellType, v );
                    return v;
                } );
            }
            if ( designElementsByBioAssay != null ) {
                designElementsByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + metrics.getNumberOfDesignElements( Collections.singleton( scv ), sourceSampleIndex, cellType ) );
            }
            if ( cellByDesignElementByBioAssay != null ) {
                cellByDesignElementByBioAssay.compute( sample, ( k, v ) -> ( v != null ? v : 0 ) + metrics.getNumberOfCellsByDesignElements( Collections.singleton( scv ), sourceSampleIndex, cellType ) );
            }
        }

        return rv;
    }

    @Override
    @Transactional
    public int removeAggregatedVectors( ExpressionExperiment ee, QuantitationType qt ) {
        // this is needed because the raw vectors must be loaded
        ee = expressionExperimentService.reload( ee );
        Collection<BioAssayDimension> dimensions = expressionExperimentService.getBioAssayDimensions( ee, qt, RawExpressionDataVector.class );
        for ( BioAssayDimension dimension : dimensions ) {
            Collection<QuantitationType> otherUsers = new HashSet<>( expressionExperimentService.getQuantitationTypes( ee, dimension ) );
            otherUsers.remove( qt );
            // check if the dimension is still used by other vectors
            if ( qt.getIsPreferred() && !otherUsers.isEmpty() ) {
                for ( BioAssay ba : dimension.getBioAssays() ) {
                    ba.setNumberOfCells( null );
                    ba.setNumberOfDesignElements( null );
                    ba.setNumberOfCellsByDesignElements( null );
                }
                // dimension is immutable, but not the BAs so this is OK
                bioAssayService.update( dimension.getBioAssays() );
            } else if ( otherUsers.isEmpty() ) {
                log.info( "Removing unused dimension " + dimension + "..." );
                bioAssayDimensionService.remove( dimension );
            }
        }
        return expressionExperimentService.removeRawDataVectors( ee, qt );
    }
}
