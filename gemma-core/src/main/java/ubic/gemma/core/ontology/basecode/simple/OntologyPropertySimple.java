/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.simple;

import ubic.gemma.core.ontology.basecode.model.OntologyProperty;

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
