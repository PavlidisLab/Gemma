package ubic.gemma.core.loader.expression;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;

public interface DataUpdater {
    void addAffyDataFromAPTOutput( ExpressionExperiment ee, String pathToAptOutputFile ) throws IOException;

    void addCountData( ExpressionExperiment ee, ArrayDesign targetArrayDesign,
            DoubleMatrix<String, String> countMatrix, DoubleMatrix<String, String> rpkmMatrix, @Nullable Integer readLength,
            @Nullable Boolean isPairedReads, boolean allowMissingSamples );

    void log2cpmFromCounts( ExpressionExperiment ee, QuantitationType qt );

    void replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform, QuantitationType qt,
            DoubleMatrix<String, String> data );

    void reprocessAffyDataFromCel( ExpressionExperiment ee );

    void addData( ExpressionExperiment ee, ArrayDesign targetPlatform, ExpressionDataDoubleMatrix data );

    void replaceData( ExpressionExperiment ee, ArrayDesign targetPlatform,
            ExpressionDataDoubleMatrix data );
}
