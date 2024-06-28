package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.service.expression.experiment.SingleCellUtils.mapCellTypeAssignmentToCellTypeFactor;

/**
 * Aggregates single-cell expression data.
 */
@Service
public class SingleCellExpressionExperimentAggregatorServiceImpl implements SingleCellExpressionExperimentAggregatorService {

    private static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    /**
     * Aggregate preferred single-cell data vectors.
     * @return the quantitation type of the newly created vectors
     */
    @Override
    @Transactional
    public QuantitationType aggregateVectors( ExpressionExperiment ee, List<BioAssay> cellBAs ) {
        Collection<SingleCellExpressionDataVector> vectors;
        vectors = singleCellExpressionExperimentService.getPreferredSingleCellDataVectors( ee );
        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( ee + " does not have a set of preferred single-cell vectors." );
        }
        CellTypeAssignment cta = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( ee );
        if ( cta == null ) {
            throw new IllegalStateException( ee + " does not have a preferred cell type assignment." );
        }
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee );
        if ( cellTypeFactor == null ) {
            throw new IllegalStateException( ee + " does not have a cell type factor." );
        }
        return aggregateVectorsInternal( ee, cellBAs, vectors.iterator().next().getQuantitationType(), vectors, cta, cellTypeFactor );
    }

    @Override
    @Transactional
    public QuantitationType aggregateVectors( ExpressionExperiment ee, List<BioAssay> cellBAs, QuantitationType quantitationType, CellTypeAssignment cellTypeAssignment ) {
        Assert.isTrue( ee.getBioAssays().containsAll( cellBAs ) );
        Collection<SingleCellExpressionDataVector> vectors;
        vectors = singleCellExpressionExperimentService.getSingleCellDataVectors( ee, quantitationType );
        ExperimentalFactor cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( ee );
        if ( cellTypeFactor == null ) {
            throw new IllegalStateException( ee + " does not have a cell type factor." );
        }
        return aggregateVectorsInternal( ee, cellBAs, quantitationType, vectors, cellTypeAssignment, cellTypeFactor );
    }

    private QuantitationType aggregateVectorsInternal( ExpressionExperiment ee, List<BioAssay> cellBAs, QuantitationType qt, Collection<SingleCellExpressionDataVector> vectors, CellTypeAssignment cellTypeAssignment, ExperimentalFactor cellTypeFactor ) {
        // TODO: support other types and representations for aggregation
        Assert.isTrue( qt.getScale().equals( ScaleType.COUNT ),
                "Only count data can be aggregated." );
        Assert.isTrue( qt.getRepresentation().equals( PrimitiveType.DOUBLE ),
                "Only vectors of doubles can be aggregated." );
        SingleCellDimension bad = vectors.iterator().next().getSingleCellDimension();
        Assert.isTrue( bad.getCellTypeAssignments().contains( cellTypeAssignment ) );

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
        for ( Characteristic ct : cellTypeAssignment.getCellTypes() ) {
            FactorValue fv = cellType2Factor.get( ct );
            for ( BioAssay ba : cellBAs ) {
                if ( ba.getSampleUsed().getAllFactorValues().contains( fv ) ) {
                    cellTypes.put( ba, ct );
                }
            }
        }

        BioAssayDimension newBad = new BioAssayDimension();
        newBad.setName( bad.getName() + " aggregated by " + cellTypeFactor );
        newBad.setDescription( bad.getDescription() + "\n" + "Expression data has been aggregated by " + cellTypeFactor + " using SUM()." );
        newBad.setBioAssays( cellBAs );

        // create vectors now
        QuantitationType newQt = new QuantitationType();
        newQt.setType( qt.getType() );
        newQt.setRepresentation( qt.getRepresentation() );
        newQt.setIsPreferred( qt.getIsPreferred() );
        newQt.setScale( qt.getScale() );

        Collection<RawExpressionDataVector> rawVectors = new ArrayList<>( vectors.size() );
        for ( SingleCellExpressionDataVector v : vectors ) {
            RawExpressionDataVector rawVector = new RawExpressionDataVector();
            rawVector.setExpressionExperiment( ee );
            rawVector.setQuantitationType( newQt );
            rawVector.setBioAssayDimension( newBad );
            rawVector.setDesignElement( v.getDesignElement() );
            rawVector.setData( aggregateData( v, newBad, cellTypeAssignment, sourceBioAssayMap, cellTypes ) );
            rawVectors.add( rawVector );
        }

        expressionExperimentService.addRawDataVectors( ee, newQt, rawVectors );

        return newQt;
    }

    /**
     * Aggregate the single-cell data to match the target BAD.
     */
    private byte[] aggregateData( SingleCellExpressionDataVector scv, BioAssayDimension bad, CellTypeAssignment cta, Map<BioAssay, BioAssay> sourceBioAssayMap, Map<BioAssay, Characteristic> cellTypes ) {
        List<BioAssay> samples = bad.getBioAssays();
        int numSamples = samples.size();
        double[] rv = new double[numSamples];
        double[] scrv = byteArrayConverter.byteArrayToDoubles( scv.getData() );
        int[] bioAssaysOffset = scv.getSingleCellDimension().getBioAssaysOffset();
        for ( int i = 0; i < numSamples; i++ ) {
            BioAssay sample = sourceBioAssayMap.get( samples.get( i ) );
            Characteristic cellType = cellTypes.get( sample );
            int j = scv.getSingleCellDimension().getBioAssays().indexOf( sample );
            int start = bioAssaysOffset[j];
            int end = j < bioAssaysOffset.length - 1 ? bioAssaysOffset[j + 1] : bioAssaysOffset.length;
            rv[i] = 0;
            for ( int k = start; k < end; k++ ) {
                if ( cellType.equals( cta.getCellType( k ) ) ) {
                    rv[i] += scrv[k];
                }
            }
        }
        return byteArrayConverter.doubleArrayToBytes( rv );
    }
}
