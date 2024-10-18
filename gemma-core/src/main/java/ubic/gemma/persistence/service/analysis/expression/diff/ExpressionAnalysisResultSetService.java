package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.basecode.math.distribution.Histogram;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.service.analysis.AnalysisResultSetService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ExpressionAnalysisResultSetService extends AnalysisResultSetService<DifferentialExpressionAnalysisResult, ExpressionAnalysisResultSet>, FilteringVoEnabledService<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject> {

    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value );

    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, int offset, int limit );

    ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long value, double threshold, int offset, int limit );

    long countResults( ExpressionAnalysisResultSet ears );

    long countResults( ExpressionAnalysisResultSet ears, double threshold );

    @CheckReturnValue
    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet e );

    ExpressionAnalysisResultSet loadWithExperimentAnalyzed( Long id );

    DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet ears, boolean includeFactorValuesInContrasts, boolean queryByResult, boolean includeTaxonInGenes );

    Map<Long, Set<Gene>> loadResultIdToGenesMap( ExpressionAnalysisResultSet ears );

    Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> externalIds, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort );

    Baseline getBaseline( ExpressionAnalysisResultSet ears );

    Map<ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Set<ExpressionAnalysisResultSet> resultSets, boolean initializeFactorValues );

    Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> rsIds, boolean initializeFactorValues );

    @Nullable
    Histogram loadPvalueDistribution( ExpressionAnalysisResultSet resulSet );

}
