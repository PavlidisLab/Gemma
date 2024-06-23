package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.genome.Gene;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * File service for arbitrary list of {@link DifferentialExpressionAnalysisResult}.
 * <p>
 * Because results may originate from different result sets, the contrasts are mangled into a single column in the
 * tabular output.
 * <p>
 * Use {@link DifferentialExpressionAnalysisResultListFileService} for producing results from a single result set.
 * @author poirigui
 */
public interface DifferentialExpressionAnalysisResultListFileService extends TsvFileService<List<DifferentialExpressionAnalysisResult>> {

    void writeTsv( List<DifferentialExpressionAnalysisResult> payload, Gene gene, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Writer writer ) throws IOException;
}
