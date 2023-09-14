package ubic.gemma.core.loader.expression;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;

public interface DataUpdater {
    void addAffyDataFromAPTOutput( ExpressionExperiment ee, String pathToAptOutputFile ) throws IOException;

    /**
     * RNA-seq: Replaces data. Starting with the count data, we compute the log2cpm, which is the preferred quantitation
     * type we use internally. Counts and FPKM (if provided) are stored in addition.
     *
     * Rows (genes) that have all zero counts are ignored entirely.
     *
     * @param ee                  ee
     * @param targetArrayDesign   - this should be one of the "Generic" gene-based platforms. The data set will be
     *                            switched to use it.
     * @param countMatrix         Representing 'raw' counts (added after rpkm, if provided).
     * @param rpkmMatrix          Representing per-gene normalized data, optional (RPKM or FPKM)
     * @param readLength          read length
     * @param isPairedReads       is paired reads
     * @param allowMissingSamples if true, samples that are missing data will be deleted from the experiment.
     * @param skipLog2cpm         Only load the counts (and RPKM/FPKM if provided) - implemented to allow backfilling without updating log2cpm and preproc.
     */
    void addCountData( ExpressionExperiment ee, ArrayDesign targetArrayDesign,
            DoubleMatrix<String, String> countMatrix, DoubleMatrix<String, String> rpkmMatrix, Integer readLength,
            Boolean isPairedReads, boolean allowMissingSamples, boolean skipLog2cpm );

    void log2cpmFromCounts( ExpressionExperiment ee, QuantitationType qt );

    void replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform, QuantitationType qt,
            DoubleMatrix<String, String> data );

    void reprocessAffyDataFromCel( ExpressionExperiment ee );

    ExpressionExperiment addData( ExpressionExperiment ee, ArrayDesign targetPlatform, ExpressionDataDoubleMatrix data );

    ExpressionExperiment replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data );
}
