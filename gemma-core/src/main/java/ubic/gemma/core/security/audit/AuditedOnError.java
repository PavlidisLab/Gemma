/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.security.audit;

import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Throwing cousin of {@link Audited} / {@link AuditedConditional}. Method-level
 * annotation that asks {@code AuditedAspect} to emit an audit event of the
 * declared {@link AuditEventType} when the method exits by throwing.
 *
 * <p>Motivated by {@code AUDIT_PHASE_C_RECCE.md} bucket 2e: a cluster of
 * imperative {@code auditTrailService.addUpdateEvent(target, FailedXEvent.class,
 * e.getMessage(), e)} callers sit inside {@code catch} blocks and re-throw the
 * exception after writing the audit row. The canonical shape is:
 * <pre>{@code
 *   try {
 *       doWork( ee );
 *   } catch ( Exception e ) {
 *       auditTrailService.addUpdateEvent( ee, FailedFooEvent.class, e.getMessage(), e );
 *       throw e;
 *   }
 * }</pre>
 * With this annotation, the body collapses to a plain {@code doWork(ee)} call
 * on a method annotated {@code @AuditedOnError(FailedFooEvent.class)}.
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@code @AfterThrowing} only — methods that return normally record
 *       nothing through this annotation. Pair with {@link Audited} or
 *       {@link AuditedConditional} on the same method to also record success.</li>
 *   <li>The thrown {@link Throwable} is re-thrown verbatim after the audit
 *       row is written — the aspect does NOT swallow exceptions.</li>
 *   <li>The audit row is written through
 *       {@code AuditTrailService.addUpdateEvent(Auditable, Class, String, Throwable)},
 *       which carries {@code @Transactional(propagation = REQUIRES_NEW)} so
 *       the row survives the rollback of the surrounding transaction. The
 *       full stack trace is persisted in {@code AUDIT_EVENT.DETAIL}.</li>
 *   <li>The evaluation context for {@link #messageSpel()} is identical to
 *       {@link Audited#messageSpel()} EXCEPT that {@code #result} is undefined
 *       (the method threw, there is no return value) and the caught throwable
 *       is exposed as {@code #exception}. Method parameters resolve by name
 *       (requires {@code -parameters}, enabled project-wide).</li>
 *   <li>If the {@link #messageSpel()} SpEL fails to evaluate, the aspect falls
 *       back to {@link #message()} (a literal). If both are empty the audit
 *       row is still written with {@code #exception.message} as the note
 *       (the default for {@link #messageSpel()}), so a thrown event never
 *       loses its note silently. Contrast with {@link AuditedConditional},
 *       whose {@code when} predicate failure SKIPS emission.</li>
 * </ul>
 *
 * <p>A method may carry {@link Audited} (or {@link AuditedConditional}) AND
 * {@code @AuditedOnError} simultaneously — the success annotation fires on
 * normal return, this one fires on throw, and they share no advice path.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface AuditedOnError {

    /**
     * The concrete {@link AuditEventType} subclass to record. Must be
     * instantiable via a public no-arg constructor. Conventionally a
     * {@code Failed…Event} subtype.
     */
    Class<? extends AuditEventType> value();

    /**
     * Optional plain (non-SpEL) note string. Stored verbatim in
     * {@code AUDIT_EVENT.NOTE} when {@link #messageSpel()} is empty or fails
     * to evaluate. Empty default means "fall through to the SpEL default".
     */
    String message() default "";

    /**
     * Optional SpEL expression for the note, evaluated against the
     * post-throw context. The caught {@link Throwable} is exposed as
     * {@code #exception}; method parameters resolve by name; the root object
     * is the args array (so {@code #root.args[i]} also works). The default
     * {@code "#exception.message"} matches the most common imperative
     * pattern: {@code addUpdateEvent(ee, type, e.getMessage(), e)}.
     *
     * <p>If evaluation throws, the aspect falls back to {@link #message()};
     * the audit row is still written so a thrown exception is never
     * silently lost from the audit log.
     */
    String messageSpel() default "#exception.message";
}
