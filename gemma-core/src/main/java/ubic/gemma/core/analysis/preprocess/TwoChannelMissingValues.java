package ubic.gemma.core.analysis.preprocess;

import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.util.Collection;

public interface TwoChannelMissingValues {

    double DEFAULT_SIGNAL_TO_NOISE_THRESHOLD = 1.5;

    /**
     * @param ee The expression experiment to analyze. The quantitation types to use are selected automatically. If
     *           you want more control use other computeMissingValues methods.
     * @return collection of raw expression data vectors
     */
    @SuppressWarnings("UnusedReturnValue")
    // Possible external use
    Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment ee );

    /**
     * @param ee                          The expression experiment to analyze. The quantitation types to use are selected automatically.
     * @param signalToNoiseThreshold      A value such as 1.5 or 2.0; only spots for which at least ONE of the channel signal
     *                                    is more than signalToNoiseThreshold*background (and the preferred data are not missing) will be considered
     *                                    present.
     * @param extraMissingValueIndicators Values that should be considered missing. For example, some data sets use '0'.
     *                                    This can be null or empty and it will be ignored.
     * @return DesignElementDataVectors corresponding to a new PRESENTCALL quantitation type for the experiment.
     */
    Collection<RawExpressionDataVector> computeMissingValues( ExpressionExperiment ee, double signalToNoiseThreshold,
            @Nullable Collection<Double> extraMissingValueIndicators );

}