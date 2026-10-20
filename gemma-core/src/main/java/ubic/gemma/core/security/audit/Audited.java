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
 * Method-level annotation requesting that an {@link
 * ubic.gemma.model.common.auditAndSecurity.AuditEvent} of the declared type be
 * written to the audit trail of the first {@link
 * ubic.gemma.model.common.auditAndSecurity.Auditable} argument after the method
 * returns normally.
 *
 * <p>The method may additionally declare an {@link AuditEventPayload} parameter
 * (anywhere in the argument list). When present, the aspect serializes the
 * payload to JSON via Jackson and stores the string in the new
 * {@code AUDIT_EVENT.PAYLOAD} column. Type information for polymorphic
 * deserialization is carried by Jackson's
 * {@code @JsonTypeInfo(use = NAME, property = "@type")} discriminator.
 *
 * <p>Replaces the (now-retired) {@code AuditAdvice} generic auto-UPDATE aspect,
 * which fired on every DAO mutation regardless of intent and recorded every
 * event with a {@code null} event type. See {@code AUDIT_SYSTEM_AUDIT.md}
 * Phase A; the terminal Phase C step (see {@code AUDIT_ADVICE_RETIREMENT_PLAN.md})
 * deleted {@code AuditAdvice} and the blanket DAO pointcuts that drove it.
 *
 * <p>This annotation runs through {@code AuditedAspect}; it is the supported
 * mechanism for emitting typed UPDATE rows from service-layer methods. Generic
 * (eventType=null) auto-UPDATE rows are no longer produced by the framework.
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.METHOD )
@Documented
public @interface Audited {

    /**
     * The concrete {@link AuditEventType} subclass to record. Must be
     * instantiable via a public no-arg constructor (the convention for every
     * AndroMDA-generated event type in {@code ubic.gemma.model.common.auditAndSecurity.eventType}).
     *
     * <p>May be left at the default ({@link AuditEventType}.class — the
     * abstract base) when {@link #valueSpel()} is used to choose the event
     * class at runtime. The aspect treats the default as "no compile-time
     * choice"; emission is skipped unless {@code valueSpel} resolves to a
     * concrete subclass.
     */
    Class<? extends AuditEventType> value() default AuditEventType.class;

    /**
     * Optional SpEL expression evaluated against the join-point to choose the
     * {@link AuditEventType} subclass at runtime. The expression must resolve
     * to a {@code Class<? extends AuditEventType>} — either a class literal
     * ({@code T(com.example.FooEvent)}) or a helper invocation
     * ({@code T(com.example.Helper).pick(#arg)}).
     *
     * <p>Evaluation context matches {@link #messageSpel()}: method parameters
     * resolve by name, the return value is exposed as {@code #result}, and
     * {@code #root.args[i]} provides positional access.
     *
     * <p>When both {@link #value()} and {@code valueSpel()} are non-default,
     * the aspect logs a WARN and prefers {@code valueSpel} (runtime choice
     * wins). When {@code valueSpel} evaluates to {@code null}, is non-Class,
     * or fails to evaluate, the aspect logs ERROR and falls back to
     * {@link #value()}; if {@code value()} is also at its default, no audit
     * row is written.
     *
     * <p>Motivated by inventory #15/#16 in {@code AUDIT_RESIDUAL_INVENTORY.md}:
     * {@code ExpressionExperimentWriteServiceImpl.updateQuantitationType}
     * emits one of two {@link AuditEventType} subclasses depending on the
     * vector type of the affected {@code QuantitationType}.
     */
    String valueSpel() default "";

    /**
     * Optional plain (non-SpEL) note string. Stored verbatim in
     * {@code AUDIT_EVENT.NOTE}. Empty default = no note.
     *
     * <p>If both {@link #message()} and {@link #messageSpel()} are set, the
     * SpEL expression wins.
     */
    String message() default "";

    /**
     * Optional SpEL expression evaluated against the join-point. Resolves
     * method arguments by name (e.g. {@code #ee}, {@code #arrayDesign}) —
     * requires {@code -parameters} compile flag, which is enabled
     * project-wide in the parent pom. Positional access via
     * {@code #root.args[i]} also works because the root object is the
     * args array. The method's return value is exposed as {@code #result}
     * (so {@code @AfterReturning}-only — the expression sees a fully
     * populated return).
     *
     * <p>The result is converted to {@code String} via Spring's
     * {@code ConversionService} default behaviour and stored in
     * {@code AUDIT_EVENT.NOTE}. If the SpEL evaluator throws, the failure
     * is logged at {@code ERROR} and {@link #message()} is used as the
     * fallback (the audit row is never dropped over a formatting glitch).
     */
    String messageSpel() default "";
}
