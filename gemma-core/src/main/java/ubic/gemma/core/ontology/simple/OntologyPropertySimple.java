package ubic.gemma.core.ontology.simple;

import ubic.gemma.core.ontology.model.OntologyProperty;

import javax.annotation.Nullable;

/**
 * Simple in-memory implementation of {@link OntologyProperty}.
 * @author poirigui
 */
public class OntologyPropertySimple extends AbstractOntologyResourceSimple implements OntologyProperty {

    public OntologyPropertySimple( @Nullable String uri, @Nullable String label ) {
        this( uri, null, label );
    }

    /**
     *
     * @param uri   an URI or null if this is a free-text property
     * @param label a label for the property
     */
    public OntologyPropertySimple( @Nullable String uri, @Nullable String localName, @Nullable String label ) {
        super( uri, localName, label );
    }

    @Nullable
    @Override
    public String getComment() {
        return null;
    }

    @Override
    public boolean isObsolete() {
        return false;
    }

    @Override
    public boolean isFunctional() {
        return false;
    }
}
