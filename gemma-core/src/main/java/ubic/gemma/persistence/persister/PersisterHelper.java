package ubic.gemma.persistence.persister;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.CheckReturnValue;

/**
 * This interface contains a few extensions to the base {@link Persister} interface to handle special cases with
 * {@link ExpressionExperiment}.
 * <p>
 * You should not rely on this as it is mainly used for creating fixtures for tests.
 *
 * @author poirigui
 */
public interface PersisterHelper extends Persister {

    @Secured("GROUP_USER")
    @CheckReturnValue
    ExpressionExperiment persist( ExpressionExperiment ee, ArrayDesignsForExperimentCache cachedArrays );

    @Secured("GROUP_USER")
    @CheckReturnValue
    ArrayDesignsForExperimentCache prepare( ExpressionExperiment ee );
}
