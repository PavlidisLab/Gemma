package ubic.gemma.analysis.sequence;

import java.util.Collection;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

public interface ArrayDesignMapResultService {

    /**
     * @param arrayDesign
     * @return
     */
    public abstract Collection<CompositeSequenceMapSummary> summarizeMapResults( ArrayDesign arrayDesign );

    /**
     * @param arrayDesign
     * @return
     */
    public abstract Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( ArrayDesign arrayDesign );

    /**
     * Version of objects that retains less information.
     * 
     * @param sequenceData
     * @return
     */
    public abstract Collection<CompositeSequenceMapValueObject> getSmallerSummaryMapValueObjects(
            Collection<Object[]> sequenceData );

    /**
     * FIXME this is only public so we can use it in the DesignElementController; need refactoring (see
     * CompositeSequenceService) Function to get a collection of CompositeSequenceMapValueObjects that contain
     * information about a composite sequence and related tables.
     * 
     * @param rawSummaryData - raw results from SQL query to get CS information.
     * @return
     */
    public abstract Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects(
            Collection<Object[]> sequenceData );

    /**
     * Non-HQL version of the composite sequence data summary query. Returns a summary of the composite sequence data
     * and related tables.
     * 
     * @param compositeSequences
     * @return
     */
    public abstract Collection<CompositeSequenceMapSummary> summarizeMapResults(
            Collection<CompositeSequence> compositeSequences );

}