package ubic.gemma.model.expression.experiment;

import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.annotations.GemmaWebOnly;

import javax.annotation.Nullable;

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
public class StatementValueObject extends IdentifiableValueObject<Statement> {

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
}
