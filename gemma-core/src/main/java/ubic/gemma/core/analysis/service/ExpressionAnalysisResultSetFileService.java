package ubic.gemma.core.analysis.service;

import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.genome.Gene;

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
     *
     * The format is borrowed from {@link ExpressionDataFileService} and contains the following columns:
     *
     *  - result id
     *  - probe id
     *  - probe name
     *  - biological sequence name
     *  - pvalue
     *  - corrected pvalue (a.k.a. qvalue)
     *  - rank
     *
     */
    void writeTsvToAppendable( ExpressionAnalysisResultSet analysisResultSet, Map<Long, List<Gene>> result2Genes, Writer writer ) throws IOException;
}
