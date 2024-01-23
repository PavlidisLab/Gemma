package ubic.gemma.core.ontology;

import ubic.basecode.ontology.model.OntologyResource;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public abstract class AbstractOntologyResourceSimple implements OntologyResource, Serializable {

    private final String uri, label;

    protected AbstractOntologyResourceSimple( @Nullable String uri, String label ) {
        this.uri = uri;
        this.label = label;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    @Nullable
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
