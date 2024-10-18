package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
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
public interface DifferentialExpressionAnalysisResultListFileService {

    /**
     * Writes a list of DE results with additional columns for mapping them to source experiments and analyzed experiments (for subsets).
     * <p>
     * The following additional columns are added:
     * - source experiment ID
     * - experiment analyzed ID
     */
    void writeTsv( List<DifferentialExpressionAnalysisResult> payload, @Nullable Gene gene, @Nullable Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, @Nullable Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, @Nullable Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, Writer writer ) throws IOException;
}
