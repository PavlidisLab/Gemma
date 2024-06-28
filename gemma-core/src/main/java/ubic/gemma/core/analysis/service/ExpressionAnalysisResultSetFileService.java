package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.genome.Gene;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

/**
 * @author poirigui
 */
public interface ExpressionAnalysisResultSetFileService extends TsvFileService<ExpressionAnalysisResultSet> {

    /**
     * Write the analysis result set to an {@link Appendable} using a tabular format.
     * <p>
     * The format is borrowed from {@link ExpressionDataFileService} and contains the following columns:
     * <p>
     *  - result id
     *  - probe id
     *  - probe name
     *  - pvalue
     *  - corrected pvalue (a.k.a. qvalue)
     *  - rank
     *  Then for each contrast, a column with the {@code contrast_} prefix:
     *  - coefficient
     *  - log2 fold-change
     *  - t statistic
     *  - pvalue
     */
    @Override
    void writeTsv( ExpressionAnalysisResultSet entity, Writer writer ) throws IOException;

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
    void writeTsv( ExpressionAnalysisResultSet analysisResultSet, @Nullable Baseline baseline, Map<Long, List<Gene>> result2Genes, Writer writer ) throws IOException;
}
