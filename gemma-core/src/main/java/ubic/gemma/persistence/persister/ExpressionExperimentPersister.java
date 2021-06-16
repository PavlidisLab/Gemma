package ubic.gemma.persistence.persister;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

/**
 * Extension of {@link Persister} to handle special cases relating to {@link ExpressionExperiment}.
 */
public interface ExpressionExperimentPersister<T> extends Persister<T> {

    /**
     * Special case for experiments.
     *
     * @param  ee experiment
     * @param  c  array design cache (see caller)
     * @return persisted experiment
     */
    @Secured({ "GROUP_USER" })
    ExpressionExperiment persist( ExpressionExperiment ee, ArrayDesignsForExperimentCache c );

    @Secured({ "GROUP_USER" })
    ArrayDesignsForExperimentCache prepare( ExpressionExperiment entity );
}
