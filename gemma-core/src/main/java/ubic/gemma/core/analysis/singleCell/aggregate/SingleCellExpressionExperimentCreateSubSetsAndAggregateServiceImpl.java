package ubic.gemma.core.analysis.singleCell.aggregate;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommonsLog
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

    @Override
    @Transactional
    public QuantitationType createSubSetsAndAggregateByCellType( ExpressionExperiment expressionExperiment, SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig, SingleCellAggregationConfig config ) {
        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSubSetService.createSubSetsByCellType( expressionExperiment, singleCellExperimentSubSetsCreationConfig );
        int longestSubsetName = subsets.stream().map( ExpressionExperimentSubSet::getName ).mapToInt( String::length ).max().orElse( 0 );
        log.info( String.format( "Created %d subsets of %s for each cell type:\n\t%s", subsets.size(), expressionExperiment,
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

    @Override
    @Transactional
    public QuantitationType createSubSetsAndAggregate( ExpressionExperiment expressionExperiment, QuantitationType scQt, CellLevelCharacteristics clc, ExperimentalFactor cellTypeFactor, Map<Characteristic, FactorValue> c2f, SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig, SingleCellAggregationConfig config ) {
        List<ExpressionExperimentSubSet> subsets = singleCellExpressionExperimentSubSetService.createSubSets( expressionExperiment, clc, cellTypeFactor, c2f, singleCellExperimentSubSetsCreationConfig );
        int longestSubsetName = subsets.stream().map( ExpressionExperimentSubSet::getName ).mapToInt( String::length ).max().orElse( 0 );
        log.info( String.format( "Created %d subsets of %s for each cell type:\n\t%s", subsets.size(), expressionExperiment,
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
