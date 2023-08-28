package ubic.gemma.model.expression.experiment;

import org.hibernate.Hibernate;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Objects;

/**
 * A special kind of characteristic that act as a statement.
 * <p>
 * It can relate to up to two other terms, essentially forming two statements.
 * <p/>
 * This is a limited form of RDF-style triplet with the main limitation that the subject is the statement itself.
 * @author poirigui
 */
public class Statement extends Characteristic {

    public static class Factory {
        public static Statement newInstance() {
            return new Statement();
        }
    }

    /**
     * The predicate of the statement.
     */
    @Nullable
    private String predicate;

    /**
     * The predicate URI of the statement.
     */
    @Nullable
    private String predicateUri;

    /**
     * The object of the statement.
     */
    @Nullable
    private Characteristic object;

    /**
     * The second predicate.
     */
    @Nullable
    private String secondPredicate;

    @Nullable
    private String secondPredicateUri;

    /**
     * The second object.
     */
    @Nullable
    private Characteristic secondObject;

    /**
     * Obtain the subject of the statement.
     */
    @Transient
    public Characteristic getSubject() {
        return this;
    }

    @Nullable
    public String getPredicate() {
        return predicate;
    }

    public void setPredicate( @Nullable String predicate ) {
        this.predicate = predicate;
    }

    @Nullable
    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri( @Nullable String predicateUri ) {
        this.predicateUri = predicateUri;
    }

    @Nullable
    public Characteristic getObject() {
        return object;
    }

    public void setObject( @Nullable Characteristic object ) {
        this.object = object;
    }

    @Nullable
    public String getSecondPredicate() {
        return secondPredicate;
    }

    public void setSecondPredicate( @Nullable String secondPredicate ) {
        this.secondPredicate = secondPredicate;
    }

    @Nullable
    public String getSecondPredicateUri() {
        return secondPredicateUri;
    }

    public void setSecondPredicateUri( @Nullable String secondPredicateUri ) {
        this.secondPredicateUri = secondPredicateUri;
    }

    @Nullable
    public Characteristic getSecondObject() {
        return secondObject;
    }

    public void setSecondObject( @Nullable Characteristic secondObject ) {
        this.secondObject = secondObject;
    }

    @Override
    public boolean equals( Object object ) {
        if ( object == null )
            return false;
        if ( this == object )
            return true;
        if ( !( object instanceof Statement ) )
            return false;
        Statement that = ( Statement ) object;
        if ( this.getId() != null && that.getId() != null )
            return super.equals( that );
        // if both URIs are non-null, we can compare them directly
        boolean comparePredicateUris = predicateUri != null && that.predicateUri != null;
        boolean compareSecondPredicateUris = secondPredicateUri != null && that.secondPredicateUri != null;
        return super.equals( object )
                && ( comparePredicateUris ? Objects.equals( predicateUri, that.predicateUri ) : Objects.equals( predicate, that.predicate ) )
                && Objects.equals( this.object, that.object )
                && ( compareSecondPredicateUris ? Objects.equals( secondPredicateUri, that.secondPredicateUri ) : Objects.equals( secondPredicate, that.secondPredicate ) )
                && Objects.equals( secondObject, that.secondObject );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return super.hashCode();
        // don't both hashing labels unless the URI is null
        return super.hashCode() + 31 * Objects.hash(
                predicateUri != null ? predicateUri : predicate,
                object,
                secondPredicateUri != null ? secondPredicateUri : secondPredicate,
                secondObject );
    }


    @Override
    public String toString() {
        StringBuilder b = new StringBuilder( super.toString() );
        if ( predicate != null ) {
            b.append( " Predicate=" ).append( predicate );
            if ( predicateUri != null ) {
                b.append( " [" ).append( predicateUri ).append( "]" );
            }
        } else if ( predicateUri != null ) {
            b.append( " Predicate URI=" ).append( predicateUri );
        }
        if ( object != null ) {
            if ( Hibernate.isInitialized( object ) ) {
                b.append( " Object=[" ).append( object ).append( "]" );
            } else {
                b.append( " Object=" ).append( object.getId() );
            }
        }
        if ( secondPredicate != null ) {
            b.append( " Second Predicate=" ).append( secondPredicate );
            if ( secondPredicateUri != null ) {
                b.append( " [" ).append( secondPredicateUri ).append( "]" );
            }
        } else if ( secondPredicateUri != null ) {
            b.append( " Second Predicate URI=" ).append( secondPredicateUri );
        }
        if ( secondObject != null ) {
            if ( Hibernate.isInitialized( secondObject ) ) {
                b.append( " Second Object=[" ).append( secondObject ).append( "]" );
            } else {
                b.append( " Second Object=" ).append( secondObject.getId() );
            }
        }
        return b.toString();
    }
}
