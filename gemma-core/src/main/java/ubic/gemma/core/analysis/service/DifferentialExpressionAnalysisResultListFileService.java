package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * File service for arbitrary list of {@link DifferentialExpressionAnalysisResult}.
 * <p>
 * Use {@link DifferentialExpressionAnalysisResultListFileService} for producing results from a single result set.
 * @author poirigui
 */
public interface DifferentialExpressionAnalysisResultListFileService extends TsvFileService<List<DifferentialExpressionAnalysisResult>> {

    /**
     * {@inheritDoc}
     * <p>
     * The format is similar to {@link ExpressionAnalysisResultSetFileService#writeTsv(ExpressionAnalysisResultSet, Writer)}
     * except for how contrasts are encoded.
     * <p>
     * Because results may originate from different result sets, the contrasts are mangled into a single column in the
     * tabular output and a column is added for the result set ID.
     */
    @Override
    void writeTsv( List<DifferentialExpressionAnalysisResult> entity, Writer writer ) throws IOException;

    /**
     * Writes a list of DE results with additional columns for mapping them to source experiments and analyzed experiments (for subsets).
     * <p>
     * The following additional columns are added:
     * - source experiment ID
     * - experiment analyzed ID
     */
    void writeTsv( List<DifferentialExpressionAnalysisResult> payload, Gene gene, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, Writer writer ) throws IOException;
}
