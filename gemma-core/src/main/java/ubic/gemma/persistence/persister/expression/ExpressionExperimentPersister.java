package ubic.gemma.persistence.persister.expression;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.persister.CachingPersister;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

/**
 * Extension of {@link Persister} to handle special cases relating to {@link ExpressionExperiment}.
 */
public interface ExpressionExperimentPersister extends CachingPersister<ExpressionExperiment> {

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
