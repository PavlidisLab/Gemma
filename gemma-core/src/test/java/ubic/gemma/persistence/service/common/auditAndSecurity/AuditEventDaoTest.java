package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration
public class AuditEventDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class AuditEventDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public AuditEventDao auditEventDao( SessionFactory sessionFactory ) {
            return new AuditEventDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private AuditEventDao auditEventDao;

    @Test
    public void testFetch() {
        AuditEvent event = AuditEvent.Factory.newInstance( new Date(), AuditAction.C, null, null, null, null );
        event = auditEventDao.create( event );
        sessionFactory.getCurrentSession().evict( event );
        auditEventDao.load( event.getId() );
        assertTrue( Hibernate.isInitialized( event.getEventType() ) );
        assertTrue( Hibernate.isInitialized( event.getPerformer() ) );
    }

    @Test
    public void testGetEvents() {
        ExpressionExperiment auditable = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( auditable );
        assertNull( auditEventDao.getLastEvent( auditable, BatchInformationFetchingEvent.class ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Assertions.assertThat( auditEventDao.getEvents( auditable ) )
                .hasSize( 1 );
    }

    @Test
    public void testGetLastEvent() {
        ExpressionExperiment auditable = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( auditable );
        assertNull( auditEventDao.getLastEvent( auditable, BatchInformationFetchingEvent.class ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        // should also work on detached entities
        AuditEvent event = auditEventDao.getLastEvent( auditable, ExpressionExperimentAnalysisEvent.class );
        assertNotNull( event );
        assertTrue( Hibernate.isInitialized( event.getPerformer() ) );
        assertTrue( Hibernate.isInitialized( event.getEventType() ) );
        assertEquals( BatchInformationFetchingEvent.class, event.getEventType().getClass() );
    }

    @Test
    public void testGetLastEvents() {
        ExpressionExperiment auditable = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( auditable );
        assertNull( auditEventDao.getLastEvent( auditable, BatchInformationFetchingEvent.class ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        // should also work on detached entities
        Map<ExpressionExperiment, AuditEvent> events = auditEventDao.getLastEvents( ExpressionExperiment.class, ExpressionExperimentAnalysisEvent.class );
        Assertions.assertThat( events )
                .hasEntrySatisfying( auditable, ae -> {
                    Assertions.assertThat( ae.getEventType() ).isInstanceOf( BatchInformationFetchingEvent.class );
                } );
    }

    @Test
    public void testGetLastEventsByType() {
        ExpressionExperiment auditable = new ExpressionExperiment();
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().persist( auditable );
        ExpressionExperiment auditable2 = new ExpressionExperiment();
        auditable2.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new DataReplacedEvent() ) );
        sessionFactory.getCurrentSession().persist( auditable2 );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Map<ExpressionExperiment, AuditEvent> result = auditEventDao.getLastEvents( Arrays.asList( auditable, auditable2 ), ExpressionExperimentAnalysisEvent.class );
        Assertions.assertThat( result )
                .containsOnlyKeys( auditable, auditable2 );
    }

    @Test
    public void testGetLastEventsByType2() {
        ExpressionExperiment auditable = new ExpressionExperiment();
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().persist( auditable );
        ExpressionExperiment auditable2 = new ExpressionExperiment();
        auditable2.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new DataReplacedEvent() ) );
        sessionFactory.getCurrentSession().persist( auditable2 );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Map<ExpressionExperiment, AuditEvent> result = auditEventDao.getLastEvents( ExpressionExperiment.class, ExpressionExperimentAnalysisEvent.class );
        Assertions.assertThat( result )
                .containsOnlyKeys( auditable, auditable2 );
    }

    @Test
    public void testGetLastEventsWholeCorpusSqlSideMax() {
        // Regression for the SQL-side MAX(date) rewrite of getLastEvents(Class, Class).
        // Fixture: 5 trails x 3 events each with deliberately interleaved dates, plus a
        // 6th trail with two events on the same DATETIME(3) instant (tie-breaker case:
        // MAX(id) wins on equal date).
        long t0 = 1_700_000_000_000L; // arbitrary base epoch-ms

        // 5 trails: latest event has the largest date within each trail.
        ExpressionExperiment[] eeFive = new ExpressionExperiment[ 5 ];
        AuditEvent[] expectedLatest = new AuditEvent[ 5 ];
        for ( int i = 0; i < 5; i++ ) {
            ExpressionExperiment ee = new ExpressionExperiment();
            // Interleaved dates per trail: e.g. trail i gets events at (base+10i+0,
            // base+10i+5, base+10i+2) and the +5 one is the latest. By shuffling the
            // insertion order we guarantee the SQL aggregate (not insertion-order) is
            // doing the selection.
            AuditEvent older = AuditEvent.Factory.newInstance( new Date( t0 + 10L * i + 0 ),
                    AuditAction.U, "older", null, null, new BatchInformationFetchingEvent() );
            AuditEvent latest = AuditEvent.Factory.newInstance( new Date( t0 + 10L * i + 5 ),
                    AuditAction.U, "latest", null, null, new BatchInformationFetchingEvent() );
            AuditEvent middle = AuditEvent.Factory.newInstance( new Date( t0 + 10L * i + 2 ),
                    AuditAction.U, "middle", null, null, new BatchInformationFetchingEvent() );
            ee.getAuditTrail().getEvents().add( older );
            ee.getAuditTrail().getEvents().add( latest );
            ee.getAuditTrail().getEvents().add( middle );
            sessionFactory.getCurrentSession().persist( ee );
            eeFive[ i ] = ee;
            expectedLatest[ i ] = latest;
        }

        // 6th trail: two events at the exact same timestamp. MAX(id) should win.
        ExpressionExperiment eeTied = new ExpressionExperiment();
        Date tieDate = new Date( t0 + 99L );
        AuditEvent tieA = AuditEvent.Factory.newInstance( tieDate, AuditAction.U, "tieA", null, null, new BatchInformationFetchingEvent() );
        AuditEvent tieB = AuditEvent.Factory.newInstance( tieDate, AuditAction.U, "tieB", null, null, new BatchInformationFetchingEvent() );
        eeTied.getAuditTrail().getEvents().add( tieA );
        eeTied.getAuditTrail().getEvents().add( tieB );
        sessionFactory.getCurrentSession().persist( eeTied );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        Map<ExpressionExperiment, AuditEvent> result = auditEventDao.getLastEvents(
                ExpressionExperiment.class, ExpressionExperimentAnalysisEvent.class );

        // Exactly 6 trails should appear, one entry each (the contract: at most one event per
        // auditable). NOTE: other tests in this class may run in the same in-memory DB - we
        // assert presence of our trails and that each returned event is the expected latest,
        // not exact size, to be robust to setUp ordering.
        for ( int i = 0; i < 5; i++ ) {
            AuditEvent actual = result.get( eeFive[ i ] );
            assertNotNull( actual, "trail " + i + " should have a last event" );
            assertEquals( expectedLatest[ i ].getId(), actual.getId(),
                    "trail " + i + " latest event mismatch (expected the date-max event)" );
            assertTrue( Hibernate.isInitialized( actual.getEventType() ),
                    "eventType should be fetched eagerly" );
        }

        // Tie case: both events share the same DATETIME(3). The contract is "MAX(id) wins".
        AuditEvent actualTie = result.get( eeTied );
        assertNotNull( actualTie, "tied-date trail should have a last event" );
        long winningId = Math.max( tieA.getId(), tieB.getId() );
        assertEquals( winningId, actualTie.getId().longValue(),
                "on equal-date tie, the event with the larger id should win" );
    }

    @Test
    public void testGetNewSinceDate() {
        Date before = new Date();
        ExpressionExperiment auditable = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( auditable );
        assertNull( auditEventDao.getLastEvent( auditable, BatchInformationFetchingEvent.class ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.C, null, null, null, null ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Assertions.assertThat( auditEventDao.getNewSinceDate( ExpressionExperiment.class, before ) )
                .hasSize( 1 )
                .contains( auditable );
    }

    @Test
    public void testGetUpdatedSinceDate() {
        // "Updated" now means "received any typed AuditEvent in the window" — i.e. eventType IS NOT NULL.
        // See AUDIT_SYSTEM_AUDIT.md Section 5, risk #1.
        Date before = new Date();
        ExpressionExperiment withTyped = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( withTyped );
        // Typed update event — should be picked up.
        withTyped.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );

        ExpressionExperiment onlyGenericUpdate = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( onlyGenericUpdate );
        // Generic auto-UPDATE (eventType == null) — must NOT be picked up under the new semantics.
        onlyGenericUpdate.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, null ) );

        ExpressionExperiment onlyCreate = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( onlyCreate );
        // Plain create with no typed event — must NOT be picked up.
        onlyCreate.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.C, null, null, null, null ) );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Assertions.assertThat( auditEventDao.getUpdatedSinceDate( ExpressionExperiment.class, before ) )
                .hasSize( 1 )
                .contains( withTyped );
    }
}
