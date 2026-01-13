package ubic.gemma.core.analysis.service;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * High-level service for deleting an {@link ExpressionExperiment} and all associated data files.
 *
 * @author poirigui
 */
public interface ExpressionExperimentDeleterService {

    /**
     * Delete an experiment and all associated data files.
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void delete( ExpressionExperiment ee );
}
