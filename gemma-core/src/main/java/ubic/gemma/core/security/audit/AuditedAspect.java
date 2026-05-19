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
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
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

    @AfterReturning( "@annotation(audited)" )
    public void afterAuditedMethod( JoinPoint joinPoint, Audited audited ) {
        Object[] args = joinPoint.getArgs();
        Auditable target = findAuditable( args );
        if ( target == null ) {
            log.warn( "@Audited method {} has no Auditable argument; no audit event will be written.",
                    joinPoint.getSignature() );
            return;
        }
        AuditEventPayload payload = findPayload( args );
        String payloadJson = serialisePayload( payload, joinPoint );
        String note = resolveMessage( audited, joinPoint );
        AuditEvent ev;
        try {
            ev = auditTrailService.addUpdateEventWithPayload( target, audited.value(), note, payloadJson );
        } catch ( RuntimeException e ) {
            log.error( "Failed to persist audit event for @Audited method {} (type={}); rethrowing.",
                    joinPoint.getSignature(), audited.value().getSimpleName(), e );
            throw e;
        }
        AuditEventType resolvedType = eventTypeCache.computeIfAbsent( audited.value(), AuditedAspect::instantiateEventType );
        try {
            eventPublisher.publishEvent( new AuditedEvent( this, target, resolvedType, payload, ev ) );
        } catch ( RuntimeException e ) {
            // Publishing failure must NOT roll back the audit row. The row is
            // already in the AuditTrail bag and will be flushed at commit.
            log.warn( "AuditedEvent listener threw for {} on {}; audit row is unaffected.",
                    audited.value().getSimpleName(), target, e );
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
    private String resolveMessage( Audited audited, JoinPoint joinPoint ) {
        if ( !audited.messageSpel().isEmpty() ) {
            // Phase A intentionally ships without SpEL support: spring-expression
            // is on the gemma-core classpath only at runtime scope, and Phase A's
            // pilot call sites all pass static notes that fit the literal
            // message() attribute. Phase B will (a) widen spring-expression to
            // compile scope and (b) wire a SpelExpressionParser+
            // StandardEvaluationContext here if/when a dynamic-note caller is
            // migrated. We log a warning so misuse is loud rather than silent.
            log.warn( "@Audited.messageSpel='{}' set on {} but SpEL is not yet supported in Phase A; falling back to literal message().",
                    audited.messageSpel(), joinPoint.getSignature() );
        }
        String literal = audited.message();
        return literal.isEmpty() ? null : literal;
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
