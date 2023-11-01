package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang.StringUtils;
import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.Objects;

/**
 * A special kind of characteristic that act as a statement.
 * <p>
 * It can relate to up to two other objects, essentially forming two statements. This is a limited form of RDF-style
 * triplet with the main limitation that a given subject can have up to two predicates and objects.
 * @author poirigui
 */
public class Statement extends Characteristic {

    private static final Comparator<Statement> COMPARATOR = Comparator
            .comparing( ( Statement s ) -> s.getCategoryUri() != null ? s.getCategoryUri() : s.getCategory(), Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( Statement s ) -> s.getSubjectUri() != null ? s.getSubjectUri() : s.getSubject(), Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( Statement s ) -> s.predicateUri != null ? s.predicateUri : s.predicate, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( Statement s ) -> s.objectUri != null ? s.objectUri : s.object, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( Statement s ) -> s.secondPredicateUri != null ? s.secondPredicateUri : s.secondPredicate, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( ( Statement s ) -> s.secondObjectUri != null ? s.secondObjectUri : s.secondObject, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) )
            .thenComparing( Statement::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

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
    private String object;

    @Nullable
    private String objectUri;

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
    private String secondObject;

    @Nullable
    private String secondObjectUri;

    /**
     * @deprecated use {@link #getSubject()} instead
     */
    @Override
    @Deprecated
    public String getValue() {
        return super.getValue();
    }

    /**
     * @deprecated use {@link #setSubject(String)} instead
     */
    @Override
    @Deprecated
    public void setValue( String value ) {
        super.setValue( value );
    }

    /**
     * @deprecated use {@link #getSubjectUri()} instead
     */
    @Override
    @Deprecated
    public String getValueUri() {
        return super.getValueUri();
    }

    /**
     * @deprecated use {@link #setSubjectUri(String)} instead
     */
    @Override
    @Deprecated
    public void setValueUri( @Nullable String uri ) {
        super.setValueUri( uri );
    }

    /**
     * Obtain the subject of the statement.
     */
    @Transient
    public String getSubject() {
        return super.getValue();
    }

    public void setSubject( String subject ) {
        super.setValue( subject );
    }

    /**
     * Obtain the subject URI of the statement.
     */
    @Transient
    public String getSubjectUri() {
        return super.getValueUri();
    }

    public void setSubjectUri( String subject ) {
        super.setValueUri( subject );
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
    public String getObject() {
        return object;
    }

    public void setObject( @Nullable String object ) {
        this.object = object;
    }

    @Nullable
    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri( @Nullable String objectUri ) {
        this.objectUri = objectUri;
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
    public String getSecondObject() {
        return secondObject;
    }

    public void setSecondObject( @Nullable String secondObject ) {
        this.secondObject = secondObject;
    }

    @Nullable
    public String getSecondObjectUri() {
        return secondObjectUri;
    }

    public void setSecondObjectUri( @Nullable String secondObjectUri ) {
        this.secondObjectUri = secondObjectUri;
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
        if ( predicateUri != null ^ that.predicateUri != null )
            return false;
        if ( objectUri != null ^ that.objectUri != null )
            return false;
        if ( secondPredicateUri != null ^ that.secondPredicateUri != null )
            return false;
        if ( secondObjectUri != null ^ that.secondObjectUri != null )
            return false;
        return super.equals( object )
                && ( predicateUri != null ? StringUtils.equalsIgnoreCase( predicateUri, that.predicateUri ) : StringUtils.equalsIgnoreCase( predicate, that.predicate ) )
                && ( objectUri != null ? StringUtils.equalsIgnoreCase( this.objectUri, that.objectUri ) : StringUtils.equalsIgnoreCase( this.object, that.object ) )
                && ( secondPredicateUri != null ? StringUtils.equalsIgnoreCase( secondPredicateUri, that.secondPredicateUri ) : StringUtils.equalsIgnoreCase( secondPredicate, that.secondPredicate ) )
                && ( secondObjectUri != null ? StringUtils.equalsIgnoreCase( secondObjectUri, that.secondObjectUri ) : StringUtils.equalsIgnoreCase( secondObject, that.secondObject ) );
    }

    @Override
    public int hashCode() {
        if ( this.getId() != null )
            return super.hashCode();
        // don't both hashing labels unless the URI is null
        return super.hashCode() + 31 * Objects.hash(
                StringUtils.lowerCase( predicateUri != null ? predicateUri : predicate ),
                StringUtils.lowerCase( objectUri != null ? objectUri : object ),
                StringUtils.lowerCase( secondPredicateUri != null ? secondPredicateUri : secondPredicate ),
                StringUtils.lowerCase( secondObjectUri != null ? secondObjectUri : secondObject ) );
    }

    @Override
    public int compareTo( @Nonnull Characteristic characteristic ) {
        if ( characteristic instanceof Statement ) {
            return COMPARATOR.compare( this, ( Statement ) characteristic );
        }
        return super.compareTo( characteristic );
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
            b.append( " Object=" ).append( object );
            if ( objectUri != null ) {
                b.append( " [" ).append( objectUri ).append( "]" );
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
            b.append( " Second Object=" ).append( secondObject );
            if ( secondObjectUri != null ) {
                b.append( " [" ).append( secondObjectUri ).append( "]" );
            }
        }
        return b.toString();
    }
}
