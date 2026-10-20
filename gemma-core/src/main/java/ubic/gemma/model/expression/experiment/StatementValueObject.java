package ubic.gemma.model.expression.experiment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicUtils;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import java.util.Comparator;

import static ubic.gemma.model.common.description.CharacteristicUtils.compareTerm;

/**
 * Represents a VO for a {@link Statement}, typically part of a {@link FactorValueBasicValueObject}.
 * <p>
 * The REST representation was settled by <a href="https://github.com/PavlidisLab/Gemma/issues/814">#814</a>,
 * closed in {@code dff752727c}: the {@code predicate*} / {@code object*} slots are public, and a
 * compound statement's second clause reaches clients flattened by
 * {@code AbstractFactorValueValueObjectSerializer} rather than through the {@code second*} fields —
 * see those fields for why they stay off the wire. This javadoc previously said the question was
 * still open; it has not been since November 2023.
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

    /**
     * The predicate and object halves are absent rather than null when nothing was said.
     * <p>
     * This is the rule {@code AbstractFactorValueValueObjectSerializer#writeStatement} already applies
     * when it emits the same VO by hand on the factor-value path — "null reads as 'this was cleared',
     * and a subject-only statement has nothing to clear" — and these four annotations make the bean
     * path agree with it. Before, the two serializations of one type disagreed about the commonest
     * statement there is.
     * <p>
     * The subject and category halves keep their nulls, on the same serializer's reasoning: they
     * describe a term that IS there, and {@code subjectUri: null} says it is ungrounded.
     * <p>
     * Measured on {@code GET /datasets/3937/samples}: {@code predicate}, {@code predicateUri},
     * {@code object}, {@code objectUri}, {@code objectId}, {@code subjectId} and {@code id} were null
     * on all 5,291 statements in the response, costing 587,301 of 5,265,852 bytes — 11.2% — to say
     * nothing seven times over.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String predicate;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String predicateUri;

    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String object;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String objectUri;

    /**
     * The second clause of a compound statement (e.g. the {@code "for 12 weeks"} of
     * {@code "HFD for 12 weeks"}).
     * <p>
     * These four are withheld because {@code AbstractFactorValueValueObjectSerializer} already puts
     * them on the wire, flattened: a statement with a second object is emitted as <em>two</em>
     * entries in the {@code statements} array sharing one subject, the second carrying this clause
     * under the generic {@code predicate} / {@code object} keys. Serializing the raw fields as well
     * would publish the same clause twice under two names. That flattening arrived with the fields'
     * exposure in {@code dff752727c} ("Serialize statements", fix #814), which is why the first four
     * slots are public here and these are not.
     * <p>
     * {@link ubic.gemma.model.common.description.AnnotationValueObject} exposes its own
     * {@code second*} fields directly, and that is not an inconsistency: it is serialized as a plain
     * bean with no flattener, so direct fields are the only way to carry the compound shape there.
     */
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "AbstractFactorValueValueObjectSerializer flattens the second clause into an extra statements[] entry under the generic predicate/object keys")
    private String secondPredicate;
    @Nullable
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "AbstractFactorValueValueObjectSerializer flattens the second clause into an extra statements[] entry under the generic predicate/object keys")
    private String secondPredicateUri;

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "AbstractFactorValueValueObjectSerializer flattens the second clause into an extra statements[] entry under the generic predicate/object keys")
    private String secondObject;
    @Nullable
    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "AbstractFactorValueValueObjectSerializer flattens the second clause into an extra statements[] entry under the generic predicate/object keys")
    private String secondObjectUri;

    /**
     * A unique ontology identifier (i.e. IRI) for this subject.
     * <p>
     * Assigned by {@code AbstractFactorValueValueObjectSerializer}, which is the only thing that
     * populates it; nothing sets it on the bean path, so it is null on every statement
     * {@code GET /datasets/{id}/samples} returns. Absent rather than null for that reason.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @GemmaRestOnly
    private String subjectId;
    /**
     * A unique ontology identifier (i.e. IRI) for this object. Assigned and omitted on the same terms
     * as {@link #subjectId}.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
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

    /**
     * How this statement was arrived at, as a {@link ubic.gemma.model.association.GOEvidenceCode} name
     * ({@code IC}, {@code IEA}, {@code IIA}, {@code TAS}, …). The uppercase enum name is the wire form
     * this field carries everywhere it appears — {@code AnnotationValueObject#evidenceCode} and
     * {@code PublicationAssociationValueObject#evidenceCode} spell it the same way.
     * <p>
     * Storage is the {@code EVIDENCE_CODE} column {@link Statement} inherits from
     * {@link ubic.gemma.model.common.description.Characteristic}; like {@link #supportingEvidence} it was
     * never surfaced here, so a design write could not say who decided and a design read could not tell a
     * curator's call from a program's.
     * <p>
     * Provenance rather than identity, so it is excluded from equals/hashCode and from {@link #COMPARATOR}
     * for the same reason {@link #supportingEvidence} is: the comparator's ordering assigns annotation ids.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @EqualsAndHashCode.Exclude
    @Schema(description = "How this statement was arrived at, as a GOEvidenceCode name (IC, IEA, IIA, TAS, …). Null when none is recorded.")
    private String evidenceCode;

    public StatementValueObject() {
        super();
    }

    public StatementValueObject( Statement s ) {
        super( s );
        this.category = s.getCategory();
        this.categoryUri = s.getCategoryUri();
        // All three value slots go through the canonicaliser (CharacteristicUtils#canonicalUri),
        // a read-time stand-in for the parked migration. Predicates and categories deliberately
        // do NOT: Gemma 1.0 reads categories while it is live, and the predicate vocabulary is
        // already constrained by Relation.terms.txt, so neither is where duplicates live.
        this.subjectUri = CharacteristicUtils.canonicalUri( s.getSubjectUri() );
        this.subject = CharacteristicUtils.canonicalLabel( s.getSubjectUri(), s.getSubject() );
        this.predicate = s.getPredicate();
        this.predicateUri = s.getPredicateUri();
        this.objectUri = CharacteristicUtils.canonicalUri( s.getObjectUri() );
        this.object = CharacteristicUtils.canonicalLabel( s.getObjectUri(), s.getObject() );
        this.secondPredicate = s.getSecondPredicate();
        this.secondPredicateUri = s.getSecondPredicateUri();
        this.secondObjectUri = CharacteristicUtils.canonicalUri( s.getSecondObjectUri() );
        this.secondObject = CharacteristicUtils.canonicalLabel( s.getSecondObjectUri(), s.getSecondObject() );
        this.supportingEvidence = CharacteristicUtils.parseSupportingEvidence( s.getSupportingEvidence() );
        this.evidenceCode = s.getEvidenceCode() != null ? s.getEvidenceCode().name() : null;
    }

    @Override
    public int compareTo( @NonNull StatementValueObject other ) {
        return COMPARATOR.compare( this, other );
    }
}
