package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;

import java.util.Comparator;

import static ubic.gemma.model.common.description.CharacteristicUtils.compareTerm;

/**
 * Represents a VO for a {@link Statement}, typically part of a {@link FactorValueBasicValueObject}.
 * <p>
 * Most of the fields in here are reserved for Gemma Web and we are still discussing the best way to represent these for
 * the REST API in <a href="https://github.com/PavlidisLab/Gemma/issues/814">#814</a>.
 * @see Statement
 * @see FactorValueBasicValueObject
 * @author poirigui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({ "id" })
public class StatementValueObject extends IdentifiableValueObject<Statement> implements Comparable<StatementValueObject> {

    /**
     * It is critical that the order of the fields in the comparator is the same as the order of the fields in the
     * {@link Statement} comparator since this is used to assign IDs to annotations (i.e. subjects and objects).
     */
    private static final Comparator<StatementValueObject> COMPARATOR = Comparator
            .comparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getCategory(), c1.getCategoryUri(), c2.getCategory(), c2.getCategoryUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getSubject(), c1.getSubjectUri(), c2.getSubject(), c2.getSubjectUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getPredicate(), c1.getPredicateUri(), c2.getPredicate(), c2.getPredicateUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getObject(), c1.getObjectUri(), c2.getObject(), c2.getObjectUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getSecondPredicate(), c1.getSecondPredicateUri(), c2.getSecondPredicate(), c2.getSecondPredicateUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getSecondObject(), c1.getSecondObjectUri(), c2.getSecondObject(), c2.getSecondObjectUri() ) )
            .thenComparing( StatementValueObject::getId, Comparator.nullsLast( Comparator.naturalOrder() ) );

    private String category;
    @Nullable
    private String categoryUri;

    private String subject;
    @Nullable
    private String subjectUri;

    private String predicate;
    @Nullable
    private String predicateUri;

    private String object;
    @Nullable
    private String objectUri;

    @GemmaWebOnly
    private String secondPredicate;
    @Nullable
    @GemmaWebOnly
    private String secondPredicateUri;

    @GemmaWebOnly
    private String secondObject;
    @Nullable
    @GemmaWebOnly
    private String secondObjectUri;

    /**
     * A unique ontology identifier (i.e. IRI) for this subject.
     */
    @GemmaRestOnly
    private String subjectId;
    /**
     * A unique ontology identifier (i.e. IRI) for this object.
     */
    @GemmaRestOnly
    private String objectId;

    public StatementValueObject() {
        super();
    }

    public StatementValueObject( Statement s ) {
        super( s );
        this.category = s.getCategory();
        this.categoryUri = s.getCategoryUri();
        this.subject = s.getSubject();
        this.subjectUri = s.getSubjectUri();
        this.predicate = s.getPredicate();
        this.predicateUri = s.getPredicateUri();
        this.object = s.getObject();
        this.objectUri = s.getObjectUri();
        this.secondPredicate = s.getSecondPredicate();
        this.secondPredicateUri = s.getSecondPredicateUri();
        this.secondObject = s.getSecondObject();
        this.secondObjectUri = s.getSecondObjectUri();
    }

    @Override
    public int compareTo( StatementValueObject other ) {
        return COMPARATOR.compare( this, other );
    }
}
