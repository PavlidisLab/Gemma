package ubic.gemma.model.expression.experiment;

import ubic.gemma.model.common.description.Characteristic;

import javax.annotation.Nullable;
import javax.persistence.Transient;

/**
 * A special kind of characteristic that act as a statement.
 * <p>
 * It can relate to up to two other terms, essentially forming two statements.
 * <p/>
 * This is a limited form of RDF-style triplet with the main limitation that the subject is the statement itself.
 * @author poirigui
 */
public class Statement extends Characteristic {

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
    public String toString() {
        StringBuilder b = new StringBuilder( super.toString() );
        if ( predicateUri != null ) {
            b.append( " Predicate URI=" ).append( predicateUri );
        }
        if ( object != null ) {
            b.append( " Object=" ).append( object.getId() );
        }
        if ( secondPredicateUri != null ) {
            b.append( " Second Predicate URI=" ).append( secondPredicateUri );
        }
        if ( secondObject != null ) {
            b.append( " Second Object=" ).append( secondObject.getId() );
        }
        return b.toString();
    }
}
