package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicUtils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
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

    /**
     * Verbatim provenance backing this statement — a JSON array of {@code {quote, source, location, …}} items
     * the curation agents emitted. Gemma stores and serves it opaquely; the agents repo owns the schema.
     * <p>
     * A {@link Statement} is a {@link ubic.gemma.model.common.description.Characteristic}, so the storage
     * (the {@code SUPPORTING_EVIDENCE} column) has always existed — it was simply never surfaced here, which
     * left the design read path unable to answer "where did this factor value's term come from" even for rows
     * that recorded it. Null means "nothing recorded", the expected reading for most rows.
     * <p>
     * Provenance rather than identity, so it is excluded from equals/hashCode and from {@link #COMPARATOR}:
     * the same statement with and without recorded evidence is the same statement, and the comparator's
     * ordering is relied upon to assign annotation ids.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode.Exclude
    @Schema(description = "Verbatim provenance backing this statement — a JSON array of {quote, source, location} items the curation agents emitted. Null when none is recorded.")
    private JsonNode supportingEvidence;

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
        this.supportingEvidence = CharacteristicUtils.parseSupportingEvidence( s.getSupportingEvidence() );
    }

    @Override
    public int compareTo( @NonNull StatementValueObject other ) {
        return COMPARATOR.compare( this, other );
    }
}
