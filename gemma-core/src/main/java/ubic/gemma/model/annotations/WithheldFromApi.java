package ubic.gemma.model.annotations;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Withhold a property from the RESTful API, and say why.
 * <p>
 * This is {@link JsonIgnore} underneath, so an annotated field or getter is absent from the
 * serialized response — and, because swagger-core introspects through Jackson, absent from the
 * generated OpenAPI document too. It is a serialization control, not a documentation hint.
 *
 * <h2>Why the reason is required</h2>
 *
 * This replaces {@code @GemmaWebOnly}, which asserted "exclusively used for Gemma Web". Gemma Web
 * was deleted in {@code bb154eee88}, which made that sentence false at all 79 of its call sites and
 * left exactly one argument available to anyone reading it: <em>Gemma Web is gone, so this hiding is
 * vestigial, so remove it.</em>
 * <p>
 * That argument is correct for most of those members. Applied to
 * {@code ExpressionExperimentValueObject.getCurrentUserIsOwner()} it would publish per-caller
 * authorization state onto a response carrying {@code @CacheControl(maxAge = 1200)}. The old marker
 * gave you no way to tell the two apart, so {@link #value()} has no default: a suppression that does
 * not state its reason is the thing that created this problem, and a marker that cannot be applied
 * without one cannot recreate it.
 * <p>
 * {@code WithheldFromApiInventoryTest} pins every application of this annotation, so adding,
 * removing or re-reasoning one is a deliberate, reviewed edit rather than a silent API change. It
 * also asserts that nothing marked {@link Reason#CALLER_IDENTITY} or {@link Reason#DISCLOSURE} can
 * reach the wire, and that the {@link Reason#UNTRIAGED} population only ever shrinks.
 *
 * <pre>
 * &#64;WithheldFromApi(value = Reason.CALLER_IDENTITY,
 *         comment = "per-principal ownership on a response cached by URL")
 * public boolean getCurrentUserIsOwner() { ... }
 * </pre>
 *
 * The bucketing that produced the initial reasons is in {@code docs/recce/GEMMA_WEB_ONLY_AUDIT.md}.
 * <p>
 * There was briefly a {@code PUBLIC_PROJECTION_EXISTS} reason, for a member whose safe subset a
 * parallel value object published. It was removed: its only claimed instance,
 * {@code PublicGeeqValueObject}, turned out to serialize a payload identical to the VO it was
 * supposedly projecting, and once the "unfiltered internal form" half of its meaning moved to
 * {@link Reason#INTERNAL_ONLY}, what remained said only "the data is published elsewhere" —
 * which is {@link Reason#REDUNDANT}.
 *
 * @author paul
 * @see GemmaRestOnly for the inverse — properties exclusive to Gemma REST
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonIgnore
public @interface WithheldFromApi {

    /**
     * Why this member is withheld. Required — see the type javadoc.
     */
    Reason value();

    /**
     * The specific hazard, the projection to use instead, or what to check before exposing this.
     * <p>
     * Strongly encouraged on {@link Reason#UNTRIAGED}, where it is the note the next person needs.
     */
    String comment() default "";

    enum Reason {
        /**
         * The value depends on <em>who is asking</em> rather than on the entity — an authorization
         * flag, an ownership check, a per-principal count.
         * <p>
         * Serializing one onto a response cached by URL is the classic cross-user leak. The dataset
         * endpoints do carry {@code @CacheControl(isPrivate = true, authorities = {"GROUP_USER"})}
         * alongside their {@code maxAge}, which is what keeps this safe today; that is a second,
         * independent control, and this marker exists so correctness does not rest on it alone.
         * <p>
         * Never remove one of these without replacing it with something stronger.
         */
        CALLER_IDENTITY,
        /**
         * Not per-caller, but still should not leave the building: internal filesystem paths, raw
         * instrument headers, submitter-local identifiers, internal free-text notes.
         */
        DISCLOSURE,
        /**
         * Nothing is being withheld: this datum is already on the wire under another name, or is
         * trivially derivable from what is. A denormalized copy, a flattened convenience accessor
         * beside the object it flattens, or a member whose content a separate projection already
         * publishes.
         * <p>
         * This is the one reason that asserts <em>no</em> hazard, so it is deliberately exempt from
         * the suppression enforcement in {@code WithheldFromApiInventoryTest} — a property of the
         * same name serializing elsewhere confirms the claim rather than contradicting it. Do not
         * reach for it when the member is merely useless; that is {@link #INTERNAL_ONLY}, and the
         * distinction is what decides whether the guard watches the member or ignores it.
         * <p>
         * Safe to delete outright. Name the member or projection that carries the data in
         * {@link #comment()}, so the claim can be rechecked.
         */
        REDUNDANT,
        /**
         * The member cannot mean anything to a client, so publishing it would mislead rather than
         * inform. Three shapes recur:
         * <ul>
         * <li>nothing populates it, so it would serialize a permanent {@code 0} / {@code null} /
         * {@code ""} that reads as data;</li>
         * <li>its shape is lossy — a category-keyed map that silently drops colliding entries, say —
         * so the honest form of the same data is elsewhere;</li>
         * <li>it lives on a value object nothing serves, or exists purely as scaffolding for an
         * editor that is gone.</li>
         * </ul>
         * Unlike {@link #REDUNDANT}, the suppression here is doing real work, so it <em>is</em>
         * enforced: exposing one of these publishes a falsehood, not a duplicate.
         * <p>
         * "Nothing populates it" is a claim about today's code and can quietly stop being true. Say
         * in {@link #comment()} which shape applies and how it was established, and prefer deleting
         * the member over annotating it — for a field nobody writes, this annotation is a placeholder
         * for that deletion.
         */
        INTERNAL_ONLY,
        /**
         * Withheld by curation or editorial policy rather than by a technical hazard. The data is not
         * dangerous; we have decided not to publish it. Say whose decision in {@link #comment()} so
         * it can be revisited by the right person.
         */
        POLICY,
        /**
         * Real data that a client might legitimately want, withheld only because Gemma Web was once
         * the consumer, and not yet traced far enough to expose safely.
         * <p>
         * This is debt, not a verdict, and it is the one reason that should never be chosen for a new
         * member — it exists to carry the residue of the {@code @GemmaWebOnly} migration. Retiring
         * one means tracing where it is populated and whether null is meaningful, then un-hiding it
         * with a {@code NON_NULL} guard and a serialization test — the process
         * {@code CharacteristicValueObject.originalValue} went through in {@code 3646111985}.
         */
        UNTRIAGED
    }
}
