package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import org.springframework.lang.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SingleCellExpressionExperimentCreateSubSetsAndAggregateServiceImpl implements SingleCellExpressionExperimentCreateSubSetsAndAggregateService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentSubSetService singleCellExpressionExperimentSubSetService;

    @Autowired
    private SingleCellExpressionExperimentAggregateService singleCellExpressionExperimentAggregateService;

    @Autowired
    private EntityUrlBuilder entityUrlBuilder;
    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;
    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    /**
     * Not transactional: {@link #createSubSets} commits the subset/BioAssay/BioMaterial rows it
     * creates — and with them the ACL rows {@code AclEventListener} inserts alongside each —
     * BEFORE {@link #aggregateVectors} opens its own, separate, potentially hour-long transaction
     * to stream and aggregate the single-cell data.
     * <p>
     * Wrapping both in one transaction (as this method did until 2026-09-30) holds those ACL
     * inserts' locks for the aggregation's entire duration. Every ACL insert anywhere in Gemma
     * references one of a handful of shared {@code acl_sid} rows and a shared {@code acl_class} row
     * per entity type, so a long-held lock here blocks unrelated ACL-creating writers elsewhere in
     * the system — measured on frink 2026-09-30: {@code diffExAnalyze} failed 14+ times over roughly
     * an hour with {@code Lock wait timeout exceeded} on its own {@code acl_entry} insert while this
     * method's transaction, opened by a concurrent {@code aggregateSingleCellData} run, was still
     * open and had not touched an ACL table in well over an hour.
     * <p>
     * {@code Propagation.NEVER} rather than simply dropping {@code @Transactional}: it fails loudly
     * if this is ever called from within an existing transaction instead of silently joining one and
     * reintroducing the problem. {@link #createSubSets}, {@link #aggregateVectors} and
     * {@link #aggregateVectorsByCellType} remain independently {@code @Transactional} — each opens
     * its own transaction exactly as it already does when called directly from outside this class.
     */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public QuantitationType createSubSetsAndAggregateByCellType( ExpressionExperiment expressionExperiment, SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig, SingleCellAggregationConfig config ) {
        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSubSetService.createSubSetsByCellType( expressionExperiment, singleCellExperimentSubSetsCreationConfig );
        int longestSubsetName = subsets.stream().map( ExpressionExperimentSubSet::getName ).mapToInt( String::length ).max().orElse( 0 );
        log.info( String.format( "Aggregating over %d subsets of %s, one per cell type:%n\t%s", subsets.size(), expressionExperiment,
                subsets.stream().map( subset -> StringUtils.rightPad( subset.getName(), longestSubsetName ) + "\t" + entityUrlBuilder.fromHostUrl().entity( subset ).web().toUri() ).collect( Collectors.joining( "\n\t" ) ) ) );
        List<BioAssay> cellBAs = new ArrayList<>();
        for ( ExpressionExperimentSubSet subset : subsets ) {
            subset.getBioAssays().stream()
                    .sorted( Comparator.comparing( BioAssay::getName ) )
                    .forEach( cellBAs::add );
        }
        QuantitationType newQt = singleCellExpressionExperimentAggregateService.aggregateVectorsByCellType( expressionExperiment, cellBAs, config );
        log.info( "Aggregated single-cell data for the preferred single-cell QT into pseudo-bulks with quantitation type " + newQt + "." );
        return newQt;
    }

    /** See {@link #createSubSetsAndAggregateByCellType} for why this is {@code Propagation.NEVER}. */
    @Override
    @Transactional(propagation = Propagation.NEVER)
    public QuantitationType createSubSetsAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt, CellLevelCharacteristics clc, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f, SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig, SingleCellAggregationConfig config ) {
        SingleCellDimension scd = singleCellExpressionExperimentService.getSingleCellDimensionWithoutCellIds( expressionExperiment, scQt );
        if ( scd == null ) {
            throw new IllegalStateException( "No single-cell dimension found for " + expressionExperiment + " and " + scQt + "." );
        }
        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSubSetService.createSubSets( expressionExperiment, scd, clc, cellTypeFactor, c2f, singleCellExperimentSubSetsCreationConfig );
        int longestSubsetName = subsets.stream().map( ExpressionExperimentSubSet::getName ).mapToInt( String::length ).max().orElse( 0 );
        log.info( String.format( "Aggregating over %d subsets of %s, one per cell type:%n\t%s", subsets.size(), expressionExperiment,
                subsets.stream().map( subset -> StringUtils.rightPad( subset.getName(), longestSubsetName ) + "\t" + entityUrlBuilder.fromHostUrl().entity( subset ).web().toUri() ).collect( Collectors.joining( "\n\t" ) ) ) );

        List<BioAssay> cellBAs = new ArrayList<>( subsets.size() * clc.getCharacteristics().size() );
        for ( ExpressionExperimentSubSet subset : subsets ) {
            subset.getBioAssays().stream()
                    .sorted( Comparator.comparing( BioAssay::getName ) )
                    .forEach( cellBAs::add );
        }

        QuantitationType newQt = singleCellExpressionExperimentAggregateService.aggregateVectors( expressionExperiment, scQt, cellBAs, clc, cellTypeFactor, c2f, config );
        log.info( "Aggregated single-cell data for " + scQt + " into pseudo-bulks with quantitation type " + newQt + "." );
        return newQt;
    }

    @Override
    @Transactional
    public QuantitationType redoAggregateByCellType( ExpressionExperiment expressionExperiment, BioAssayDimension dimension, @Nullable QuantitationType previousQt, SingleCellAggregationConfig config ) {
        if ( previousQt != null ) {
            // when removing a previous QT, check if we should keep its dimension for re-aggregating
            BioAssayDimension previousBad = expressionExperimentService.getBioAssayDimension( expressionExperiment, previousQt, RawExpressionDataVector.class );
            if ( previousBad == null ) {
                log.warn( "No BioAssayDimension found for " + previousQt );
            }
            boolean keepDimension = previousBad != null && previousBad.equals( dimension );
            singleCellExpressionExperimentAggregateService.removeAggregatedVectors( expressionExperiment, previousQt, keepDimension );
        }
        dimension = bioAssayDimensionService.thaw( dimension );
        List<BioAssay> cellBAs = dimension.getBioAssays();
        QuantitationType newQt = singleCellExpressionExperimentAggregateService.aggregateVectorsByCellType( expressionExperiment, cellBAs, config );
        log.info( "Aggregated single-cell data for the preferred single-cell QT into pseudo-bulks with quantitation type " + newQt + "." );
        return newQt;
    }

    @Override
    @Transactional
    public QuantitationType redoAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt, CellLevelCharacteristics clc, ExperimentalFactor factor, Map<Characteristic, FactorValue> c2f, BioAssayDimension dimension, @Nullable QuantitationType previousQt, SingleCellAggregationConfig config ) {
        if ( previousQt != null ) {
            // when removing a previous QT, check if we should keep its dimension for re-aggregating
            BioAssayDimension previousBad = expressionExperimentService.getBioAssayDimension( expressionExperiment, previousQt, RawExpressionDataVector.class );
            if ( previousBad == null ) {
                log.warn( "No BioAssayDimension found for " + previousQt );
            }
            boolean keepDimension = previousBad != null && previousBad.equals( dimension );
            singleCellExpressionExperimentAggregateService.removeAggregatedVectors( expressionExperiment, previousQt, keepDimension );
        }
        dimension = bioAssayDimensionService.thaw( dimension );
        List<BioAssay> cellBAs = dimension.getBioAssays();
        QuantitationType newQt = singleCellExpressionExperimentAggregateService.aggregateVectors( expressionExperiment, scQt, cellBAs, clc, factor, c2f, config );
        log.info( "Aggregated single-cell data for " + scQt + " into pseudo-bulks with quantitation type " + newQt + "." );
        return newQt;
    }
}
