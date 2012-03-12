package ubic.gemma.web.persistence;

import java.util.Collection;

import ubic.gemma.expression.experiment.SessionBoundExpressionExperimentSetValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

public interface SessionListManager {

    public abstract Collection<SessionBoundGeneSetValueObject> getAllGeneSets();

    public abstract Collection<SessionBoundGeneSetValueObject> getAllGeneSets( Long taxonId );

    public abstract Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets();

    public abstract Collection<SessionBoundGeneSetValueObject> getModifiedGeneSets( Long taxonId );

    /**
     * Get the session-bound group using the group's id
     * 
     * @param reference
     * @return
     */
    public abstract SessionBoundExpressionExperimentSetValueObject getExperimentSetById( Long id );

    /**
     * Get the session-bound group using the group's id
     * 
     * @param reference
     * @return
     */
    public abstract SessionBoundGeneSetValueObject getGeneSetById( Long id );

    /**
     * AJAX If the current user has access to given gene group will return the gene ids in the gene group
     * 
     * @param groupId
     * @return
     */
    public abstract Collection<GeneValueObject> getGenesInGroup( Long groupId );

    /**
     * @param id
     * @return
     */
    public abstract Collection<ExpressionExperimentValueObject> getExperimentsInSet( Long id );

    /**
     * @param id
     * @return
     */
    public abstract Collection<Long> getExperimentIdsInSet( Long id );

    public abstract SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo );

    public abstract SessionBoundGeneSetValueObject addGeneSet( SessionBoundGeneSetValueObject gsvo, boolean modified );

    public abstract void removeGeneSet( SessionBoundGeneSetValueObject gsvo );

    public abstract void updateGeneSet( SessionBoundGeneSetValueObject gsvo );

    public abstract Long incrementAndGetLargestGeneSetSessionId();

    public abstract Collection<SessionBoundExpressionExperimentSetValueObject> getAllExperimentSets();

    public abstract Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets();

    public abstract Collection<SessionBoundExpressionExperimentSetValueObject> getModifiedExperimentSets( Long taxonId );

    public abstract SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo );

    public abstract SessionBoundExpressionExperimentSetValueObject addExperimentSet(
            SessionBoundExpressionExperimentSetValueObject eesvo, boolean modified );

    public abstract void removeExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo );

    public abstract void updateExperimentSet( SessionBoundExpressionExperimentSetValueObject eesvo );

    public abstract Long incrementAndGetLargestExperimentSetSessionId();

}