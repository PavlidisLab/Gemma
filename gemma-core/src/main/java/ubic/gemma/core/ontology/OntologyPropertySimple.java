package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyProperty;
import ubic.gemma.core.lang.Nullable;

/**
 * Simple in-memory implementation of {@link OntologyProperty}.
 * TODO: move this in baseCode and share some of the implementation details with {@link ubic.basecode.ontology.model.OntologyTermSimple}
 * @author poirigui
 */
public class OntologyPropertySimple extends AbstractOntologyResourceSimple implements OntologyProperty {

    /**
     *
     * @param uri   an URI or null if this is a free-text property
     * @param label a label for the property
     */
    public OntologyPropertySimple( @Nullable String uri, String label ) {
        super( uri, label );
    }

    @Override
    public boolean isFunctional() {
        return false;
    }
}
