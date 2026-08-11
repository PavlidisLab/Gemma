package ubic.gemma.model.annotations;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a property is withheld from the RESTful API because exposing it would be
 * <em>wrong</em> — not merely because no REST client happens to want it.
 * <p>
 * Mechanically identical to {@link GemmaWebOnly}: both compose {@link JsonIgnore}, so swapping one
 * for the other on a given member changes nothing on the wire. The difference is entirely in what
 * the annotation asserts, and that assertion is the point.
 *
 * <h2>Why this exists</h2>
 *
 * {@code @GemmaWebOnly} says "exclusively used for Gemma Web". Gemma Web was deleted in
 * {@code bb154eee88}, so that sentence is now false everywhere it appears — which invites exactly
 * one argument: <em>Gemma Web is gone, therefore this hiding is vestigial, therefore remove it.</em>
 * <p>
 * That argument is correct for most of the annotated members, and a security bug for a few. It was
 * applied correctly once already, to {@code CharacteristicValueObject.originalValue}, which was real
 * submitter data withheld from clients for no surviving reason. The identical reasoning applied to
 * {@code ExpressionExperimentValueObject.getCurrentUserIsOwner()} would publish per-caller
 * authorization state onto a response carrying {@code @CacheControl(maxAge = 1200)}.
 * <p>
 * Nothing in {@code @GemmaWebOnly} distinguishes those two cases. This annotation is the proposed
 * fix: move the members that must stay hidden onto a marker that says so, leaving
 * {@code @GemmaWebOnly} to mean only what its name claims. A future "gemma-web is gone, delete this"
 * cleanup then becomes mechanical rather than a judgement call about each member.
 * <p>
 * The full bucketing of all 79 current {@code @GemmaWebOnly} sites — which are candidates for this
 * annotation, which are inert, and which are genuinely vestigial — is in
 * {@code docs/recce/GEMMA_WEB_ONLY_AUDIT.md}.
 *
 * <h2>Using it</h2>
 *
 * {@link #value()} is required on purpose. An unexplained suppression is the thing that created this
 * problem; a marker that cannot be applied without stating a reason cannot recreate it. Use
 * {@link #comment()} for anything the enum cannot carry, such as which VO to use instead.
 *
 * <pre>
 * &#64;NotForPublicApi(value = NotForPublicApi.Reason.CALLER_IDENTITY,
 *         comment = "depends on the authenticated principal; the response is cacheable")
 * public boolean getCurrentUserIsOwner() { ... }
 * </pre>
 *
 * @author paul
 * @see GemmaWebOnly for genuinely vestigial Gemma Web display state
 * @see GemmaRestOnly for the inverse — properties exclusive to Gemma REST
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonIgnore
public @interface NotForPublicApi {

    /**
     * Why this member is withheld. Required — see the class javadoc.
     */
    Reason value();

    /**
     * Optional detail: the specific hazard, or the VO a client should use instead.
     */
    String comment() default "";

    enum Reason {
        /**
         * The value depends on <em>who is asking</em> rather than on the entity — an authorization
         * flag, an ownership check, a per-principal count.
         * <p>
         * Serializing one onto a response that is cached by URL is the classic cross-user leak. The
         * dataset endpoints do carry {@code @CacheControl(isPrivate = true, authorities = ...)},
         * which is what keeps this safe today; that is a second, independent control, and this
         * marker exists so correctness does not rest on it alone.
         */
        CALLER_IDENTITY,
        /**
         * The value is not per-caller but still should not leave the building: internal filesystem
         * paths, raw instrument headers, submitter-local identifiers, internal free-text notes.
         */
        DISCLOSURE,
        /**
         * A separate value object already exposes the safe subset of this data to REST, and this
         * member is the unfiltered internal form. Removing the suppression here would not add a
         * capability — it would duplicate one and bypass whatever the public projection excludes.
         * <p>
         * Name the projection in {@link #comment()}.
         */
        PUBLIC_PROJECTION_EXISTS,
        /**
         * Withheld by curation or editorial policy rather than by a technical hazard. The data is
         * not dangerous; we have decided not to publish it. Say whose decision in
         * {@link #comment()} so it can be revisited by the right person.
         */
        POLICY
    }
}
