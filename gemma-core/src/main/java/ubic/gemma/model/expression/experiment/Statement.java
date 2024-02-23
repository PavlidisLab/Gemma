package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.Objects;

import static org.apache.commons.lang3.StringUtils.stripToNull;

/**
 * A special kind of characteristic that act as a statement.
 * <p>
 * It can relate to up to two other objects, essentially forming two statements. This is a limited form of RDF-style
 * triplet with the main limitation that a given subject can have up to two predicates and objects.
 * @author poirigui
 */
public class Statement extends Characteristic {

    private static final Comparator<Statement> COMPARATOR = Comparator
            .comparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.getCategory(), s1.getCategoryUri(), s2.getCategory(), s2.getCategoryUri() ) )
            .thenComparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.getSubject(), s1.getSubjectUri(), s2.getSubject(), s2.getSubjectUri() ) )
            .thenComparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.predicate, s1.predicateUri, s2.predicate, s2.predicateUri ) )
            .thenComparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.object, s1.objectUri, s2.object, s2.objectUri ) )
            .thenComparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.secondPredicate, s1.secondPredicateUri, s2.secondPredicate, s2.secondPredicateUri ) )
            .thenComparing( ( Statement s ) -> s, ( s1, s2 ) -> CharacteristicUtils.compareTerm( s1.secondObject, s1.secondObjectUri, s2.secondObject, s2.secondObjectUri ) )
            .thenComparing( Statement::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    public static class Factory {
        public static Statement newInstance() {
            return new Statement();
        }

        public static Statement newInstance( String category, @Nullable String categoryUri, String subject, @Nullable String subjectUri ) {
            final Statement entity = new Statement();
            entity.setCategory( category );
            entity.setCategoryUri( stripToNull( categoryUri ) );
            entity.setSubject( subject );
            entity.setSubjectUri( stripToNull( subjectUri ) );
            return entity;
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
    @Field
    public String getObject() {
        return object;
    }

    public void setObject( @Nullable String object ) {
        this.object = object;
    }

    @Nullable
    @Field(analyze = Analyze.NO)
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
    @Field
    public String getSecondObject() {
        return secondObject;
    }

    public void setSecondObject( @Nullable String secondObject ) {
        this.secondObject = secondObject;
    }

    @Nullable
    @Field(analyze = Analyze.NO)
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
        return super.equals( object )
                && CharacteristicUtils.equals( predicate, predicateUri, that.predicate, that.predicateUri )
                && CharacteristicUtils.equals( this.object, objectUri, that.object, that.objectUri )
                && CharacteristicUtils.equals( secondPredicate, secondPredicateUri, that.secondPredicate, that.secondPredicateUri )
                && CharacteristicUtils.equals( secondObject, secondObjectUri, that.secondObject, that.secondObjectUri );
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
