/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.simple;

import ubic.gemma.core.ontology.basecode.model.OntologyResource;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author poirigui
 */
public abstract class AbstractOntologyResourceSimple implements OntologyResource, Serializable {

    @Nullable
    private final String uri, localName, label;

    protected AbstractOntologyResourceSimple( @Nullable String uri, @Nullable String localName, @Nullable String label ) {
        this.uri = uri;
        this.localName = localName;
        this.label = label;
    }

    @Override
    @Nullable
    public String getUri() {
        return uri;
    }

    @Override
    @Nullable
    public String getLocalName() {
        return localName;
    }

    @Nullable
    @Override
    public String getLabel() {
        return label;
    }

    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( !( obj instanceof OntologyResource ) ) {
            return false;
        }
        final OntologyResource other = ( OntologyResource ) obj;
        if ( getUri() == null && other.getUri() == null ) {
            return Objects.equals( getLabel(), other.getLabel() );
        } else {
            return Objects.equals( getUri(), other.getUri() );
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash( uri );
    }
}
