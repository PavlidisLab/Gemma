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
 * Conditional cousin of {@link Audited}. Method-level annotation that asks
 * {@code AuditedAspect} to emit an audit event of the declared
 * {@link AuditEventType} only when the {@link #when()} SpEL predicate
 * evaluates to {@code true} on the post-invocation context.
 *
 * <p>Motivated by {@code AUDIT_PHASE_C_RECCE.md} bucket 2c: a cluster of
 * imperative {@code auditTrailService.addUpdateEvent(...)} callers sit
 * behind an early-return guard (e.g.
 * <pre>{@code
 *   if ( dimension.getCellTypeAssignments().stream().noneMatch( CellTypeAssignment::isPreferred ) ) {
 *       return PreferredCellTypeAssignmentChangeOutcome.UNCHANGED;
 *   }
 *   ...
 *   auditTrailService.addUpdateEvent( ee, FooEvent.class, "..." );
 * }</pre>
 * Plain {@code @Audited} would unconditionally fire on every successful
 * return, which is wrong (the guard branch is a no-op). This annotation
 * lets the aspect skip the no-op branch by evaluating a SpEL predicate
 * against the same context used for {@link Audited#messageSpel()}:
 * method arguments resolve by name, the return value is exposed as
 * {@code #result}, and positional access via {@code #root.args[i]} also
 * works.
 *
 * <p>Common predicate shapes:
 * <ul>
 *   <li>{@code when = "!#result.isEmpty()"} — fire only when a collection-returning
 *       method actually produced something.</li>
 *   <li>{@code when = "#result.name() != 'UNCHANGED'"} — fire only when an
 *       enum-returning outcome method actually changed state.</li>
 *   <li>{@code when = "#result > 0"} — fire only when a count-returning method
 *       did non-zero work.</li>
 *   <li>{@code when = "!#toRemove.isEmpty() || !#toAdd.isEmpty()"} — fire only
 *       when the input deltas would actually result in a change (parameters
 *       resolved by name).</li>
 * </ul>
 *
 * <p>Semantics:
 * <ul>
 *   <li>{@code @AfterReturning} only — throwing methods record nothing
 *       (same as {@link Audited}).</li>
 *   <li>If the {@link #when()} SpEL fails to evaluate or returns {@code null},
 *       <strong>no audit row is written</strong> and the failure is logged at
 *       {@code ERROR}. This is the safe default: when the predicate is unclear,
 *       err on the side of not emitting (no false positives in the audit log).
 *       Contrast with {@link Audited#messageSpel()}, where a SpEL failure falls
 *       back to {@link Audited#message()} because dropping the row would be
 *       worse than a bad note.</li>
 *   <li>Otherwise the emission path is identical to {@link Audited}: locate
 *       the first {@link ubic.gemma.model.common.auditAndSecurity.Auditable}
 *       argument, optional {@link AuditEventPayload}, resolve note via
 *       {@link #message()} / {@link #messageSpel()}, delegate to
 *       {@code AuditTrailService.addUpdateEventWithPayload}, publish an
 *       {@link AuditedEvent}.</li>
 * </ul>
 *
 * <p>A method may carry either {@link Audited} or {@code @AuditedConditional}
 * but not both — the aspect's two {@code @AfterReturning} advices match on
 * the respective annotation and would otherwise double-fire.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface AuditedConditional {

    /**
     * The concrete {@link AuditEventType} subclass to record. Must be
     * instantiable via a public no-arg constructor.
     */
    Class<? extends AuditEventType> value();

    /**
     * SpEL predicate evaluated against the post-invocation context. The audit
     * row is emitted only when this expression evaluates to {@code Boolean.TRUE}.
     *
     * <p>The evaluation context is identical to {@link Audited#messageSpel()}:
     * method parameters resolve by name (e.g. {@code #ee}, {@code #dimension})
     * — requires {@code -parameters} compile flag, enabled project-wide in the
     * parent pom; positional access via {@code #root.args[i]}; the method's
     * return value is exposed as {@code #result}.
     *
     * <p>The default {@code "true"} makes the annotation a strict superset of
     * {@link Audited} (an always-fire conditional). Practical usage will
     * always supply a non-trivial predicate.
     */
    String when() default "true";

    /**
     * Optional plain (non-SpEL) note string. Stored verbatim in
     * {@code AUDIT_EVENT.NOTE}. Empty default = no note. See
     * {@link Audited#message()}.
     */
    String message() default "";

    /**
     * Optional SpEL expression for the note, evaluated only when {@link #when()}
     * is {@code true}. Semantics match {@link Audited#messageSpel()}: failure
     * to evaluate falls back to {@link #message()}; the audit row is still
     * written. (The {@code when} predicate is the only gate on emission.)
     */
    String messageSpel() default "";
}
