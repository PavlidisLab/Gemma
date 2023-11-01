package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyResource;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

/**
 * Simple in-memory implementation of {@link OntologyProperty}.
 * TODO: move this in baseCode and share some of the implementation details with {@link ubic.basecode.ontology.model.OntologyTermSimple}
 * @author poirigui
 */
public class OntologyPropertySimple implements OntologyProperty, Serializable {

    private final String label;
    @Nullable
    private final String uri;

    /**
     *
     * @param uri   an URI or null if this is a free-text property
     * @param label a label for the property
     */
    public OntologyPropertySimple( @Nullable String uri, String label ) {
        this.label = label;
        this.uri = uri;
    }

    @Override
    public boolean isFunctional() {
        return false;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Nullable
    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public boolean isObsolete() {
        return false;
    }

    @Nullable
    @Override
    public Double getScore() {
        return null;
    }

    @Override
    public int compareTo( OntologyResource other ) {
        return Objects.compare( getUri(), other.getUri(), Comparator.nullsLast( Comparator.naturalOrder() ) );
    }


    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        final OntologyResource other = ( OntologyResource ) obj;
        if ( getLabel() == null ) {
            if ( other.getLabel() != null ) return false;
        } else if ( !getLabel().equals( other.getLabel() ) ) return false;
        if ( getUri() == null ) {
            if ( other.getUri() != null ) return false;
        } else if ( !getUri().equals( other.getUri() ) ) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash( label, uri );
    }
}
