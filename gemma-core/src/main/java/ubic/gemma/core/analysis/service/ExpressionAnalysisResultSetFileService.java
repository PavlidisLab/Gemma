package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

/**
 * @author poirigui
 */
public interface ExpressionAnalysisResultSetFileService {

    /**
     * Write the analysis result set with result-to-gene mappings.
     * <p>
     * The tabular format has the following additional columns:
     * - gene id
     * - gene name
     * - gene NCBI ID
     * - gene official symbol
     * - gene official name
     * @param baseline    baseline to include in the TSV header
     * @param result2Genes mapping of results to genes
     *
     */
    void writeTsv( ExpressionAnalysisResultSet analysisResultSet, @Nullable Baseline baseline, Map<Long, Set<Gene>> result2Genes, Writer writer ) throws IOException;
}
