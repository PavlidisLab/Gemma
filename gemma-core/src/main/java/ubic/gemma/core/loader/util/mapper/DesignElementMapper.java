package ubic.gemma.core.loader.util.mapper;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Maps gene identifiers to {@link CompositeSequence}.
 * @author poirigui
 */
public interface DesignElementMapper extends EntityMapper<CompositeSequence> {

    /**
     * Create a mapper for the design elements of a given platform.
     */
    default StatefulEntityMapper<CompositeSequence> forCandidates( ArrayDesign platform ) {
        return forCandidates( platform.getCompositeSequences() );
    }
}
