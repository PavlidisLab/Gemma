package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.assertj.core.api.Assertions;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.*;

@ContextConfiguration
public class AuditEventDaoTest extends BaseDatabaseTest {

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
        Date before = new Date();
        ExpressionExperiment auditable = new ExpressionExperiment();
        sessionFactory.getCurrentSession().persist( auditable );
        assertNull( auditEventDao.getLastEvent( auditable, BatchInformationFetchingEvent.class ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.C, null, null, null, new BatchInformationFetchingEvent() ) );
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( new Date(), AuditAction.U, null, null, null, new BatchInformationFetchingEvent() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
        Assertions.assertThat( auditEventDao.getNewSinceDate( ExpressionExperiment.class, before ) )
                .hasSize( 1 )
                .contains( auditable );
    }
}
