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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Spring AOP aspect that backs the {@link Audited} annotation.
 *
 * <p>On successful return from any {@code @Audited(SomeEventType.class)}
 * method, this aspect:
 * <ol>
 *   <li>scans the method arguments left-to-right for the first
 *       {@link Auditable} — that is the target;</li>
 *   <li>scans again for an optional {@link AuditEventPayload} and, if
 *       present, serialises it to JSON via Jackson;</li>
 *   <li>resolves the note string (literal {@link Audited#message()} or SpEL
 *       {@link Audited#messageSpel()});</li>
 *   <li>delegates to {@link AuditTrailService} to write the
 *       {@link AuditEvent} row (so the existing curation-details side
 *       effect, abbreviation, security annotations and transaction
 *       semantics are reused verbatim);</li>
 *   <li>publishes an {@link AuditedEvent} for chain-style downstream
 *       consumers (transactional event listeners, cache eviction, etc).</li>
 * </ol>
 *
 * <p>If no {@link Auditable} argument is found the method runs but no audit
 * event is written (WARN logged).
 *
 * <p>{@code @AfterReturning} — not {@code @Before} or {@code @Around} — so a
 * throwing method records nothing.
 *
 * <p>Phase A of {@code AUDIT_SYSTEM_AUDIT.md}. The aspect is independent of
 * (and coexists with) the legacy generic {@code AuditAdvice}. Phase C will
 * retire that legacy aspect once Phase B has swept all 77 typed-hardcoded
 * imperative callers to {@code @Audited}.
 */
@Aspect
@Component
@Order(5)
@Slf4j
public class AuditedAspect {

    private final AuditTrailService auditTrailService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    /** Cache resolved event-type instances; concrete types are effectively singletons. */
    private final ConcurrentMap<Class<? extends AuditEventType>, AuditEventType> eventTypeCache = new ConcurrentHashMap<>();
    /** Parsed SpEL expressions are cached by source string; expressions are method-stable. */
    private final ConcurrentMap<String, Expression> spelCache = new ConcurrentHashMap<>();
    private final ExpressionParser spelParser = new SpelExpressionParser();

    @Autowired
    public AuditedAspect( AuditTrailService auditTrailService, ApplicationEventPublisher eventPublisher ) {
        this.auditTrailService = auditTrailService;
        this.eventPublisher = eventPublisher;
        // Build a private ObjectMapper rather than autowiring a shared bean: the
        // shared one is tuned for the REST surface and we don't want its
        // serialisation customizations bleeding into audit payloads. The
        // discriminator is carried by @JsonTypeInfo on AuditEventPayload, so
        // no per-mapper subtype registration is required for sealed permits.
        this.objectMapper = new ObjectMapper();
    }

    @AfterReturning( pointcut = "@annotation(audited)", returning = "result" )
    public void afterAuditedMethod( JoinPoint joinPoint, Audited audited, @Nullable Object result ) {
        emit( joinPoint, audited.value(), audited.message(), audited.messageSpel(), result, "@Audited" );
    }

    /**
     * Phase C: {@link AuditedConditional} fires only when its SpEL
     * {@link AuditedConditional#when()} predicate evaluates to {@code true}
     * against the post-invocation context. Otherwise the method is a no-op
     * from the audit-log's perspective.
     *
     * <p>Predicate-evaluation failure → log ERROR and skip emission (do NOT
     * fall back to "always fire"; an undecidable predicate is closer to the
     * no-op branch than to the emission branch — false negatives in the
     * audit log are worse than missing rows in the message-string).
     */
    @AfterReturning( pointcut = "@annotation(auditedConditional)", returning = "result" )
    public void afterAuditedConditionalMethod( JoinPoint joinPoint, AuditedConditional auditedConditional, @Nullable Object result ) {
        String whenSpel = auditedConditional.when();
        Boolean fire;
        try {
            fire = evaluateSpel( whenSpel, joinPoint, result, null, Boolean.class );
        } catch ( RuntimeException e ) {
            log.error( "Failed to evaluate @AuditedConditional.when='{}' on {}; SKIPPING emission (safe default).",
                    whenSpel, joinPoint.getSignature(), e );
            return;
        }
        if ( !Boolean.TRUE.equals( fire ) ) {
            // Predicate false (or null) → caller intentionally signalled "no audit on this branch".
            return;
        }
        emit( joinPoint, auditedConditional.value(), auditedConditional.message(),
                auditedConditional.messageSpel(), result, "@AuditedConditional" );
    }

    /**
     * Phase C: {@link AuditedOnError} fires when the annotated method exits
     * by throwing. The audit row is written via the 4-arg {@code Throwable}
     * overload of {@link AuditTrailService#addUpdateEvent}, which carries
     * {@code @Transactional(propagation = REQUIRES_NEW)} so the row survives
     * the rollback of the wrapping transaction and the full stack trace is
     * persisted in {@code AUDIT_EVENT.DETAIL}.
     *
     * <p>{@code @AfterThrowing} does NOT swallow the throwable — Spring AOP
     * re-throws it after the advice returns.
     *
     * <p>SpEL context here exposes the caught throwable as {@code #exception};
     * {@code #result} is undefined (the method did not return). A
     * SpEL-evaluation failure falls back to the literal {@link
     * AuditedOnError#message()} but the audit row is STILL written so that a
     * thrown exception never silently leaves the audit log empty.
     */
    @AfterThrowing( pointcut = "@annotation(auditedOnError)", throwing = "ex" )
    public void afterAuditedOnErrorMethod( JoinPoint joinPoint, AuditedOnError auditedOnError, Throwable ex ) {
        // Apply the exception-class filter: a single un-filtered declaration
        // (default Throwable.class) matches everything (back-compat). A
        // narrower filter SKIPs when the throwable is not an instanceof —
        // Spring AOP still re-throws the original by virtue of @AfterThrowing
        // not swallowing.
        if ( !auditedOnError.exception().isInstance( ex ) ) {
            return;
        }
        emitOnError( joinPoint, auditedOnError, ex );
    }

    /**
     * Phase C: repeated {@link AuditedOnError} declarations (wrapped by the
     * compiler in {@link AuditedOnErrors}) — multi-catch shape. The aspect
     * picks the MOST-SPECIFIC matching declaration (the one whose
     * {@code exception()} class is the deepest in the instanceof chain for
     * the thrown throwable) and emits at most ONE audit row per throw. A
     * default {@code Throwable.class} declaration acts as the catch-all
     * fallback. Ties are resolved by declaration order (first wins) but
     * ties are not expected in normal use.
     *
     * <p>When NO declaration matches the throwable (every {@code exception()}
     * filter excludes it), nothing is recorded — same behaviour as a Java
     * multi-catch where the thrown type isn't one of the listed catch
     * branches.
     */
    @AfterThrowing( pointcut = "@annotation(auditedOnErrors)", throwing = "ex" )
    public void afterAuditedOnErrorsMethod( JoinPoint joinPoint, AuditedOnErrors auditedOnErrors, Throwable ex ) {
        AuditedOnError best = pickMostSpecific( auditedOnErrors.value(), ex );
        if ( best == null ) {
            return;
        }
        emitOnError( joinPoint, best, ex );
    }

    /**
     * Pick the most-specific {@link AuditedOnError} declaration for
     * {@code ex}: highest-depth {@code exception()} class wins; ties are
     * resolved by declaration order (first match wins).
     */
    @Nullable
    private static AuditedOnError pickMostSpecific( AuditedOnError[] decls, Throwable ex ) {
        AuditedOnError best = null;
        int bestDepth = -1;
        for ( AuditedOnError d : decls ) {
            Class<? extends Throwable> filter = d.exception();
            if ( !filter.isInstance( ex ) ) {
                continue;
            }
            int depth = 0;
            for ( Class<?> c = filter; c != null && c != Object.class; c = c.getSuperclass() ) {
                depth++;
            }
            if ( depth > bestDepth ) {
                best = d;
                bestDepth = depth;
            }
        }
        return best;
    }

    /**
     * Shared emission path for {@link AuditedOnError} (singular or selected
     * from a repeated set). Writes the audit row via the 4-arg Throwable
     * overload (REQUIRES_NEW) and publishes an {@link AuditedEvent} for
     * downstream listeners.
     */
    private void emitOnError( JoinPoint joinPoint, AuditedOnError auditedOnError, Throwable ex ) {
        Object[] args = joinPoint.getArgs();
        Auditable target = findAuditable( args );
        if ( target == null ) {
            log.warn( "@AuditedOnError method {} has no Auditable argument; no audit event will be written.",
                    joinPoint.getSignature() );
            return;
        }
        Class<? extends AuditEventType> eventType = auditedOnError.value();
        String note = resolveMessageOnError( auditedOnError.message(), auditedOnError.messageSpel(), joinPoint, ex );
        AuditEvent ev;
        try {
            // The 4-arg Throwable overload of addUpdateEvent uses
            // REQUIRES_NEW: the audit row commits even though the
            // surrounding transaction is rolling back. Stack trace is
            // persisted to AUDIT_EVENT.DETAIL via ExceptionUtils.
            ev = auditTrailService.addUpdateEvent( target, eventType, note, ex );
        } catch ( RuntimeException e ) {
            // Don't mask the original exception with an audit-row failure;
            // log loudly and let the AfterThrowing re-throw the original.
            log.error( "Failed to persist @AuditedOnError event for {} (type={}, original cause: {}); " +
                            "the original exception will still be propagated.",
                    joinPoint.getSignature(), eventType.getSimpleName(), ex, e );
            return;
        }
        AuditEventType resolvedType = eventTypeCache.computeIfAbsent( eventType, AuditedAspect::instantiateEventType );
        try {
            // No AuditEventPayload on the throwing path (the method didn't
            // get to populate one); pass null.
            eventPublisher.publishEvent( new AuditedEvent( this, target, resolvedType, null, ev ) );
        } catch ( RuntimeException e ) {
            log.warn( "AuditedEvent listener threw for @AuditedOnError({}) on {}; audit row is unaffected.",
                    eventType.getSimpleName(), target, e );
        }
    }

    /**
     * Shared emission path for both {@link Audited} and
     * {@link AuditedConditional}. The {@code label} is purely for log
     * messages (so a hot stack trace tells you which annotation triggered).
     */
    private void emit( JoinPoint joinPoint, Class<? extends AuditEventType> eventType,
            String literalMessage, String messageSpel, @Nullable Object result, String label ) {
        Object[] args = joinPoint.getArgs();
        Auditable target = findAuditable( args );
        if ( target == null ) {
            log.warn( "{} method {} has no Auditable argument; no audit event will be written.",
                    label, joinPoint.getSignature() );
            return;
        }
        AuditEventPayload payload = findPayload( args );
        String payloadJson = serialisePayload( payload, joinPoint );
        String note = resolveMessage( literalMessage, messageSpel, joinPoint, result );
        AuditEvent ev;
        try {
            ev = auditTrailService.addUpdateEventWithPayload( target, eventType, note, payloadJson );
        } catch ( RuntimeException e ) {
            log.error( "Failed to persist audit event for {} method {} (type={}); rethrowing.",
                    label, joinPoint.getSignature(), eventType.getSimpleName(), e );
            throw e;
        }
        AuditEventType resolvedType = eventTypeCache.computeIfAbsent( eventType, AuditedAspect::instantiateEventType );
        try {
            eventPublisher.publishEvent( new AuditedEvent( this, target, resolvedType, payload, ev ) );
        } catch ( RuntimeException e ) {
            // Publishing failure must NOT roll back the audit row. The row is
            // already in the AuditTrail bag and will be flushed at commit.
            log.warn( "AuditedEvent listener threw for {} on {}; audit row is unaffected.",
                    eventType.getSimpleName(), target, e );
        }
    }

    @Nullable
    private static Auditable findAuditable( Object[] args ) {
        for ( Object a : args ) {
            if ( a instanceof Auditable ) {
                return ( Auditable ) a;
            }
        }
        return null;
    }

    @Nullable
    private static AuditEventPayload findPayload( Object[] args ) {
        for ( Object a : args ) {
            if ( a instanceof AuditEventPayload ) {
                return ( AuditEventPayload ) a;
            }
        }
        return null;
    }

    @Nullable
    private String serialisePayload( @Nullable AuditEventPayload payload, JoinPoint joinPoint ) {
        if ( payload == null ) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString( payload );
        } catch ( JsonProcessingException e ) {
            // Don't drop the audit row over a serialisation hiccup, but make
            // it impossible to miss in the logs.
            log.error( "Failed to JSON-serialise AuditEventPayload {} for @Audited method {}; audit row will be written with null payload.",
                    payload.getClass().getName(), joinPoint.getSignature(), e );
            return null;
        }
    }

    @Nullable
    private String resolveMessage( String literalMessage, String messageSpel, JoinPoint joinPoint, @Nullable Object returnValue ) {
        if ( !messageSpel.isEmpty() ) {
            // Phase B-2: SpEL support. spring-expression was widened to compile
            // scope in gemma-core/pom.xml so we can wire a SpelExpressionParser
            // + StandardEvaluationContext directly. Method parameters resolve by
            // name (javac -parameters is enabled, see parent pom). Positional
            // access via #root.args[i] also works (root object is the args array).
            // The method's return value is exposed as #result.
            try {
                String evaluated = evaluateSpel( messageSpel, joinPoint, returnValue, null, String.class );
                if ( evaluated != null && !evaluated.isEmpty() ) {
                    return evaluated;
                }
            } catch ( RuntimeException e ) {
                // Don't drop the audit row over a SpEL hiccup, but make it loud.
                log.error( "Failed to evaluate messageSpel='{}' on {}; falling back to literal message().",
                        messageSpel, joinPoint.getSignature(), e );
            }
        }
        return literalMessage.isEmpty() ? null : literalMessage;
    }

    /**
     * Phase C ({@link AuditedOnError}): resolve the note for the throwing
     * path. Differs from {@link #resolveMessage} in that the SpEL context
     * binds {@code #exception} (the caught throwable) and {@code #result} is
     * undefined. SpEL-evaluation failure falls back to the literal
     * {@link AuditedOnError#message()}; the audit row is still written
     * (unlike {@link AuditedConditional#when()}, a broken note must never
     * SKIP emission on the throwing path — losing a Failed* row would hide
     * the error).
     */
    @Nullable
    private String resolveMessageOnError( String literalMessage, String messageSpel, JoinPoint joinPoint, Throwable exception ) {
        if ( !messageSpel.isEmpty() ) {
            try {
                String evaluated = evaluateSpel( messageSpel, joinPoint, null, exception, String.class );
                if ( evaluated != null && !evaluated.isEmpty() ) {
                    return evaluated;
                }
            } catch ( RuntimeException e ) {
                log.error( "Failed to evaluate @AuditedOnError messageSpel='{}' on {}; falling back to literal message().",
                        messageSpel, joinPoint.getSignature(), e );
            }
        }
        return literalMessage.isEmpty() ? null : literalMessage;
    }

    /**
     * Evaluate a SpEL expression against the join-point context: root object
     * is the args array (so positional access via {@code #root.args[i]} works),
     * named parameters are bound as variables (requires javac {@code -parameters}),
     * the method's return value is bound as {@code #result}, and an optional
     * caught throwable is bound as {@code #exception} (used by
     * {@link AuditedOnError}). Caches the parsed expression keyed by source.
     * Any thrown exception is propagated; the caller is responsible for the
     * fall-back / skip policy.
     */
    @Nullable
    private <T> T evaluateSpel( String spel, JoinPoint joinPoint, @Nullable Object returnValue,
            @Nullable Throwable exception, Class<T> desiredType ) {
        MethodSignature sig = ( MethodSignature ) joinPoint.getSignature();
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        ctx.setRootObject( args );
        ctx.setVariable( "result", returnValue );
        ctx.setVariable( "exception", exception );
        String[] paramNames = sig.getParameterNames();
        if ( paramNames != null ) {
            for ( int i = 0; i < paramNames.length && i < args.length; i++ ) {
                if ( paramNames[i] != null ) {
                    ctx.setVariable( paramNames[i], args[i] );
                }
            }
        }
        Expression expr = spelCache.computeIfAbsent( spel, spelParser::parseExpression );
        return expr.getValue( ctx, desiredType );
    }

    private static AuditEventType instantiateEventType( Class<? extends AuditEventType> type ) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch ( ReflectiveOperationException e ) {
            throw new IllegalArgumentException( "Cannot instantiate " + type.getName() + " — does it have a public no-arg constructor?", e );
        }
    }

    /**
     * Reflection helper not currently used by the aspect itself but provided
     * for diagnostic / future use: returns the declared {@link Method} of the
     * join-point, even when the bytecode signature lost details.
     */
    @SuppressWarnings( "unused" )
    private static Method method( JoinPoint joinPoint ) {
        return ( ( MethodSignature ) joinPoint.getSignature() ).getMethod();
    }

}
