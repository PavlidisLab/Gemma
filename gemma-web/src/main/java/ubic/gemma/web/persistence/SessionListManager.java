package ubic.gemma.web.persistence;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.core.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

public interface SessionListManager {

    SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo );

    SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo, boolean modified );

    SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo );

    SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo, boolean modified );

    Collection<SessionBoundExpressionExperimentSetValueObject> getAllExperimentSets();

    Collection<SessionBoundGeneSetValueObject> getAllGeneSets();

    Collection<SessionBoundGeneSetValueObject> getAllGeneSets( Long taxonId );


    Collection<Long> getExperimentIdsInSet( Long id );

    /**
     * Get the session-bound group using the group's id
     * 

     */
    SessionBoundExpressionExperimentSetValueObject getExperimentSetById( Long id );


    Collection<ExpressionExperimentDetailsValueObject> getExperimentsInSet( Long id );

    /**
     * Get the session-bound group using the group's id
     *
     */
    SessionBoundGeneSetValueObject getGeneSetById( Long id );

    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     *
     */
    Collection<GeneValueObject> getGenesInGroup( Long groupId );

    Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets();

    Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets( Long taxonId );

    Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets();

    Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets( Long taxonId );

    Long incrementAndGetLargestExperimentSetSessionId();

    Long incrementAndGetLargestGeneSetSessionId();

    void removeExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo );

    void removeGeneSet( SessionBoundGeneSetValueObject gsvo );

    void updateExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo );

    void updateGeneSet( SessionBoundGeneSetValueObject gsvo );

}