package ubic.gemma.model.expression.experiment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
public class StatementValueObject extends IdentifiableValueObject<Statement> implements Comparable<StatementValueObject> {

    private static final Comparator<StatementValueObject> COMPARATOR = Comparator
            .comparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getCategory(), c1.getCategoryUri(), c2.getCategory(), c2.getCategoryUri() ) )
            .thenComparing( ( StatementValueObject c ) -> c, ( c1, c2 ) -> compareTerm( c1.getValue(), c1.getValueUri(), c2.getValue(), c2.getValueUri() ) )
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

    // for backward-compatibility because FactorValueBasicValueObject characteristics used to be CharacteristicBasicValueObject

    public String getValue() {
        return subject;
    }

    public void setValue( String value ) {
        this.subject = value;
    }

    public String getValueUri() {
        return subjectUri;
    }

    public void setValueUri( String valueUri ) {
        this.subjectUri = valueUri;
    }

    @Override
    public int compareTo( @Nonnull StatementValueObject other ) {
        return COMPARATOR.compare( this, other );
    }
}
