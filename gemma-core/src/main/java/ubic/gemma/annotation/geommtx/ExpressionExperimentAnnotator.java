package ubic.gemma.annotation.geommtx;

import java.util.Collection;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface ExpressionExperimentAnnotator {

    public static final String MMTX_ACTIVATION_PROPERTY_KEY = "mmtxOn";

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator#annotate(ubic.gemma.model.expression.experiment.
     * ExpressionExperiment, boolean)
     */
    public abstract Collection<Characteristic> annotate( ExpressionExperiment e, boolean force );

}