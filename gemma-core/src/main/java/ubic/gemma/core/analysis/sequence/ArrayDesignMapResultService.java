package ubic.gemma.core.analysis.sequence;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.Collection;

public interface ArrayDesignMapResultService {

    Collection<CompositeSequenceMapSummary> summarizeMapResults( ArrayDesign arrayDesign );

    Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( ArrayDesign arrayDesign );

    /**
     * FIXME this is only public so we can use it in the DesignElementController; need refactoring (see
     * CompositeSequenceService) Function to get a collection of CompositeSequenceMapValueObjects that contain
     * information about a composite sequence and related tables.
     */
    Collection<CompositeSequenceMapValueObject> getSummaryMapValueObjects( Collection<Object[]> sequenceData );

    /**
     * Non-HQL version of the composite sequence data summary query. Returns a summary of the composite sequence data
     * and related tables.
     */
    Collection<CompositeSequenceMapSummary> summarizeMapResults( Collection<CompositeSequence> compositeSequences );

}