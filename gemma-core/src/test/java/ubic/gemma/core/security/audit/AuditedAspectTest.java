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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.auditAndSecurity.AbstractAuditable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.SampleRemovalEvent;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-level test for {@link AuditedAspect}: the aspect intercepts the
 * {@link Audited} annotation, locates the first {@link Auditable} argument
 * and optional {@link AuditEventPayload} argument, serialises the payload to
 * JSON, delegates to {@link AuditTrailService#addUpdateEventWithPayload}, and
 * publishes a Spring {@link AuditedEvent}.
 *
 * <p>Phase A round-trip verification for {@code AUDIT_SYSTEM_AUDIT.md}.
 */
@ContextConfiguration
public class AuditedAspectTest extends BaseTest {

    /**
     * A locally-defined payload record. Real Phase B payloads will live next
     * to their event types under {@code ubic.gemma.core.security.audit} or a
     * dedicated package. {@code @JsonTypeName} is required on nested records
     * because Jackson's default discriminator includes the enclosing
     * class (e.g. {@code AuditedAspectTest$SamplePayload}); top-level Phase B
     * records won't need the annotation since their simple class name
     * already discriminates uniquely.
     */
    @JsonTypeName( "SamplePayload" )
    public record SamplePayload( int removed, String reason ) implements AuditEventPayload {}

    @Configuration
    @TestComponent
    @EnableAspectJAutoProxy
    static class TestContextConfiguration {

        @Bean
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public AuditedAspect auditedAspect( AuditTrailService auditTrailService, ApplicationEventPublisher publisher ) {
            return new AuditedAspect( auditTrailService, publisher );
        }

        @Bean
        public AnnotatedService annotatedService() {
            return new AnnotatedService();
        }

        @Bean
        public AuditedEventCollector auditedEventCollector() {
            return new AuditedEventCollector();
        }
    }

    @Service
    static class AnnotatedService {

        /** No payload arg — just the typed audit row. */
        @Audited( value = SampleRemovalEvent.class, message = "removed by curator" )
        public String simpleRemove( FakeAuditable target ) {
            return "ok-" + target.getId();
        }

        /** Payload arg present — must be serialised. */
        @Audited( value = SampleRemovalEvent.class )
        public void removeWithPayload( FakeAuditable target, SamplePayload payload ) {
            // no-op; the aspect does the work
        }

        /** No Auditable arg at all — aspect should WARN and skip. */
        @Audited( SampleRemovalEvent.class )
        public void noAuditableArg( String onlyArg ) {
            // no-op
        }

        /**
         * Dynamic note built from a method parameter via SpEL. The expression
         * references the parameter by name ({@code #reason}); this only works
         * because javac is run with {@code -parameters} project-wide (see the
         * parent pom). Exercises the Phase B-2 SpEL path in AuditedAspect.
         */
        @Audited( value = SampleRemovalEvent.class,
                messageSpel = "'Removed sample because: ' + #reason" )
        public void removeWithSpelMessage( FakeAuditable target, String reason ) {
            // no-op; the aspect resolves the note from #reason
        }

        /**
         * SpEL referencing the return value via {@code #result}. The aspect
         * is {@code @AfterReturning} so {@code #result} is fully populated.
         */
        @Audited( value = SampleRemovalEvent.class,
                messageSpel = "'Removed ' + #result + ' samples.'" )
        public int removeAndCount( FakeAuditable target, int count ) {
            return count;
        }

        /**
         * Malformed SpEL — aspect must NOT drop the audit row; it falls back
         * to the literal {@code message()} attribute (which here is set).
         */
        @Audited( value = SampleRemovalEvent.class,
                message = "fallback literal",
                messageSpel = "#nonexistent.foo.bar()" )
        public void brokenSpelFallsBack( FakeAuditable target ) {
            // no-op
        }
    }

    static class AuditedEventCollector {
        final List<AuditedEvent> received = new ArrayList<>();

        @EventListener
        public void onAudited( AuditedEvent ev ) {
            received.add( ev );
        }
    }

    /**
     * Minimal in-memory Auditable that doesn't require Hibernate or any of
     * the gemma-core entity machinery. Inherits the {@code auditTrail}
     * initialiser from {@link AbstractAuditable}.
     */
    public static class FakeAuditable extends AbstractAuditable {
        public FakeAuditable( long id ) {
            setId( id );
        }
        @Override
        public Long getId() {
            return super.getId();
        }
        @Override
        public boolean equals( Object o ) {
            return this == o;
        }
        @Override
        public int hashCode() {
            return System.identityHashCode( this );
        }
    }

    @Autowired
    private AnnotatedService annotatedService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private AuditedEventCollector collector;

    private final AuditEvent stubEvent = new AuditEvent();

    @Before
    public void setUp() {
        reset( auditTrailService );
        collector.received.clear();
        when( auditTrailService.addUpdateEventWithPayload( any(), any(), any(), any() ) )
                .thenReturn( stubEvent );
    }

    @Test
    public void simpleRemove_writesTypedEventWithLiteralMessage_andPublishesSpringEvent() {
        FakeAuditable target = new FakeAuditable( 42L );

        String result = annotatedService.simpleRemove( target );

        assertThat( result ).isEqualTo( "ok-42" );
        verify( auditTrailService ).addUpdateEventWithPayload(
                eq( target ),
                eq( SampleRemovalEvent.class ),
                eq( "removed by curator" ),
                eq( null ) ); // no payload
        assertThat( collector.received ).hasSize( 1 );
        AuditedEvent ev = collector.received.get( 0 );
        assertThat( ev.getTarget() ).isSameAs( target );
        assertThat( ev.getEventType() ).isInstanceOf( SampleRemovalEvent.class );
        assertThat( ev.getPayload() ).isNull();
        assertThat( ev.getAuditEvent() ).isSameAs( stubEvent );
    }

    @Test
    public void removeWithPayload_serialisesJsonAndIncludesTypeDiscriminator() throws Exception {
        FakeAuditable target = new FakeAuditable( 7L );
        SamplePayload payload = new SamplePayload( 3, "low-quality" );

        annotatedService.removeWithPayload( target, payload );

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass( String.class );
        verify( auditTrailService ).addUpdateEventWithPayload(
                eq( target ),
                eq( SampleRemovalEvent.class ),
                eq( null ), // no message() set on the annotation
                payloadCaptor.capture() );

        String json = payloadCaptor.getValue();
        assertThat( json ).isNotNull();

        // Re-parse to confirm the discriminator is present and round-trips.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode parsed = ( ObjectNode ) mapper.readTree( json );
        assertThat( parsed.get( "@type" ).asText() )
                .as( "Jackson must emit the @type discriminator from @JsonTypeInfo on AuditEventPayload" )
                .isEqualTo( "SamplePayload" );
        assertThat( parsed.get( "removed" ).asInt() ).isEqualTo( 3 );
        assertThat( parsed.get( "reason" ).asText() ).isEqualTo( "low-quality" );

        // Polymorphic round-trip back to the interface. The interface is
        // intentionally not sealed in Phase A (no concrete production
        // subtypes yet), so callers must register the subtype with their
        // own ObjectMapper. Phase B will switch this to a sealed interface
        // with permits, at which point Jackson can auto-discover subtypes.
        mapper.registerSubtypes( SamplePayload.class );
        AuditEventPayload roundtripped = mapper.readValue( json, AuditEventPayload.class );
        assertThat( roundtripped ).isInstanceOf( SamplePayload.class );
        assertThat( ( ( SamplePayload ) roundtripped ).removed() ).isEqualTo( 3 );

        // And the Spring event carried the original typed payload.
        assertThat( collector.received ).hasSize( 1 );
        assertThat( collector.received.get( 0 ).getPayload() ).isSameAs( payload );
    }

    @Test
    public void noAuditableArg_logsWarnAndDoesNotCallService() {
        annotatedService.noAuditableArg( "just a string" );

        verifyNoInteractions( auditTrailService );
        assertThat( collector.received ).isEmpty();
    }

    /**
     * Phase B-2: {@code messageSpel} must be evaluated against the method
     * arguments (by parameter name) and the resolved string passed through
     * to {@link AuditTrailService#addUpdateEventWithPayload}.
     */
    @Test
    public void testMessageSpelEvaluation() {
        FakeAuditable target = new FakeAuditable( 100L );

        annotatedService.removeWithSpelMessage( target, "low-quality" );

        verify( auditTrailService ).addUpdateEventWithPayload(
                eq( target ),
                eq( SampleRemovalEvent.class ),
                eq( "Removed sample because: low-quality" ),
                eq( null ) );
        assertThat( collector.received ).hasSize( 1 );
    }

    @Test
    public void messageSpel_canReferenceReturnValueWithHashResult() {
        FakeAuditable target = new FakeAuditable( 101L );

        int returned = annotatedService.removeAndCount( target, 5 );

        assertThat( returned ).isEqualTo( 5 );
        verify( auditTrailService ).addUpdateEventWithPayload(
                eq( target ),
                eq( SampleRemovalEvent.class ),
                eq( "Removed 5 samples." ),
                eq( null ) );
    }

    @Test
    public void brokenSpel_fallsBackToLiteralMessageAndStillWritesAuditRow() {
        FakeAuditable target = new FakeAuditable( 102L );

        annotatedService.brokenSpelFallsBack( target );

        verify( auditTrailService ).addUpdateEventWithPayload(
                eq( target ),
                eq( SampleRemovalEvent.class ),
                eq( "fallback literal" ),
                eq( null ) );
    }

}
