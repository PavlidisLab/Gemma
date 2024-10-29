package ubic.gemma.persistence.service.expression.experiment;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;

import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.service.expression.experiment.SingleCellUtils.mapCellTypeAssignmentToCellTypeFactor;
import static ubic.gemma.persistence.util.ByteArrayUtils.byteArrayToDoubles;
import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

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
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee )
                .orElseThrow( () -> new IllegalStateException( finalEe + " does not have a preferred cell type assignment." ) );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee )
                .orElseThrow( () -> new IllegalStateException( finalEe + " does not have a cell type factor." ) );
        return aggregateVectorsInternal( ee, cellBAs, makePreferred, vectors.iterator().next().getSingleCellDimension(), vectors.iterator().next().getQuantitationType(), vectors, cta, cellTypeFactor );
    }

    private QuantitationType aggregateVectorsInternal( ExpressionExperiment ee, List<BioAssay> cellBAs, boolean makePreferred, SingleCellDimension scd, QuantitationType qt, Collection<SingleCellExpressionDataVector> vectors, CellTypeAssignment cellTypeAssignment, ExperimentalFactor cellTypeFactor ) {
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

        Map<Characteristic, FactorValue> cellType2Factor = mapCellTypeAssignmentToCellTypeFactor( cellTypeAssignment, cellTypeFactor );

        // assigne sample to cell types
        Map<BioAssay, Characteristic> cellTypes = new HashMap<>();
        for ( BioAssay ba : cellBAs ) {
            boolean found = false;
            for ( Characteristic ct : cellTypeAssignment.getCellTypes() ) {
                FactorValue fv = cellType2Factor.get( ct );
                if ( ba.getSampleUsed().getAllFactorValues().contains( fv ) ) {
                    cellTypes.put( ba, ct );
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                throw new IllegalStateException( ba + " does not have an assigned cell type, make sure that its counterpart " + ba.getSampleUsed() + " has a cell type factor assigned." );
            }
        }

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

        // create vectors now
        QuantitationType newQt = QuantitationType.Factory.newInstance( qt );
        newQt.setName( qt.getName() + " aggregated by " + cellTypeFactorName );
        newQt.setDescription( ( StringUtils.isNotBlank( qt.getDescription() ) ? qt.getDescription() + "\n" : "" )
                + "Expression data has been aggregated by " + cellTypeFactorName + " using " + method + "." );
        newQt.setIsPreferred( makePreferred );

        Map<BioAssay, Integer> cellsByBioAssay = new HashMap<>();
        Map<BioAssay, Integer> designElementsByBioAssay = new HashMap<>();
        Map<BioAssay, Integer> cellByDesignElementByBioAssay = new HashMap<>();
        Collection<RawExpressionDataVector> rawVectors = new ArrayList<>( vectors.size() );
        for ( SingleCellExpressionDataVector v : vectors ) {
            RawExpressionDataVector rawVector = new RawExpressionDataVector();
            rawVector.setExpressionExperiment( ee );
            rawVector.setQuantitationType( newQt );
            rawVector.setBioAssayDimension( newBad );
            rawVector.setDesignElement( v.getDesignElement() );
            rawVector.setData( aggregateData( v, newBad, cellTypeAssignment, sourceBioAssayMap, cellTypes, method, cellsByBioAssay, designElementsByBioAssay, cellByDesignElementByBioAssay ) );
            rawVectors.add( rawVector );
        }

        if ( makePreferred ) {
            log.info( "Applying single-cell sparsity metrics to the aggregated assays..." );
            for ( BioAssay ba : cellBAs ) {
                ba.setNumberOfCells( cellsByBioAssay.getOrDefault( ba, 0 ) );
                ba.setNumberOfDesignElements( designElementsByBioAssay.getOrDefault( ba, 0 ) );
                ba.setNumberOfCellsByDesignElements( cellByDesignElementByBioAssay.getOrDefault( ba, 0 ) );
            }
            bioAssayService.update( cellBAs );
        }

        int newVecs = expressionExperimentService.addRawDataVectors( ee, newQt, rawVectors );
        String note = String.format( "Created %d aggregated raw vectors for %s.", newVecs, newQt );
        log.info( note );
        auditTrailService.addUpdateEvent( ee, DataAddedEvent.class, note );

        return newQt;
    }

    /**
     * Aggregate the single-cell data to match the target BAD.
     */
    private byte[] aggregateData( SingleCellExpressionDataVector scv, BioAssayDimension bad, CellTypeAssignment cta, Map<BioAssay, BioAssay> sourceBioAssayMap, Map<BioAssay, Characteristic> cellTypes, SingleCellExpressionAggregationMethod method, Map<BioAssay, Integer> cellsByBioAssay, Map<BioAssay, Integer> designElementsByBioAssay, Map<BioAssay, Integer> cellByDesignElementByBioAssay ) {
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        double[] rv = new double[numSamples];
        double[] scrv = byteArrayToDoubles( scv.getData() );
        int[] IX = scv.getDataIndices();
        int[] bioAssaysOffset = scv.getSingleCellDimension().getBioAssaysOffset();
        for ( int i = 0; i < numSamples; i++ ) {
            BioAssay sample = samples.get( i );
            BioAssay sourceSample = sourceBioAssayMap.get( sample );
            Characteristic cellType = cellTypes.get( sample );
            int j = scv.getSingleCellDimension().getBioAssays().indexOf( sourceSample );
            int start = Arrays.binarySearch( IX, bioAssaysOffset[j] );
            if ( start < 0 ) {
                start = -start - 1;
            }
            int end = Arrays.binarySearch( IX, j < bioAssaysOffset.length - 1 ? bioAssaysOffset[j + 1] : bioAssaysOffset.length );
            if ( end < 0 ) {
                end = -end - 1;
            }
            rv[i] = 0;
            for ( int k = start; k < end; k++ ) {
                if ( cellType.equals( cta.getCellType( k ) ) ) {
                    if ( method == SingleCellExpressionAggregationMethod.SUM ) {
                        rv[i] += scrv[k];
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                        rv[i] += Math.exp( scrv[k] );
                    } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                        rv[i] += Math.expm1( scrv[k] );
                    }
                }
            }
            if ( method == SingleCellExpressionAggregationMethod.LOG_SUM ) {
                // note: if rv[i] is zero (i.e. no cell in sample), this will become -infinity
                rv[i] = Math.log( rv[i] );
            } else if ( method == SingleCellExpressionAggregationMethod.LOG1P_SUM ) {
                rv[i] = Math.log1p( rv[i] );
            }
            // TODO: update the metrics
        }
        return doubleArrayToBytes( rv );
    }
}
