package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.NotTroubledStatusFlagEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;
import ubic.gemma.core.context.TestComponent;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ContextConfiguration
public class CuratableDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class CuratableDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ArrayDesignDaoImpl( sessionFactory );
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        // PERF_PROBE_REPORT_ROUND4 B1: EE DAO field-injects SingleCellDimensionExperimentDao.
        @Bean
        public ubic.gemma.persistence.service.expression.experiment.SingleCellDimensionExperimentDao singleCellDimensionExperimentDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.experiment.SingleCellDimensionExperimentDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Test
    public void testAddTroubledEventOnPlatform() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign curatable = new ArrayDesign();
        curatable.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( curatable );
        assertNotNull( curatable.getId() );
        assertNotNull( curatable.getCurationDetails().getId() );
        AuditEvent event = createEventOfType( TroubledStatusFlagEvent.class );
        arrayDesignDao.updateCurationDetailsFromAuditEvent( curatable, event );
        assertTrue( curatable.getCurationDetails().getTroubled() );
        assertNotNull( curatable.getCurationDetails().getLastTroubledEvent() );
        assertEquals( event.getDate(), curatable.getCurationDetails().getLastTroubledEvent().getDate() );
        assertEquals( event.getEventType(), curatable.getCurationDetails().getLastTroubledEvent().getEventType() );
    }

    @Test
    public void testAddTroubledEventOnAlreadyTroubledEntity() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign curatable = new ArrayDesign();
        curatable.setPrimaryTaxon( taxon );
        curatable.getCurationDetails().setTroubled( true );
        sessionFactory.getCurrentSession().persist( curatable );
        IllegalArgumentException e = assertThrows( IllegalArgumentException.class, () -> arrayDesignDao.updateCurationDetailsFromAuditEvent( curatable, createEventOfType( TroubledStatusFlagEvent.class ) ) );
        assertTrue( e.getMessage().contains( "already troubled" ) );
        assertTrue( curatable.getCurationDetails().getTroubled() );
        // same for non-troubled
        curatable.getCurationDetails().setTroubled( false );
        e = assertThrows( IllegalArgumentException.class, () -> arrayDesignDao.updateCurationDetailsFromAuditEvent( curatable, createEventOfType( NotTroubledStatusFlagEvent.class ) ) );
        assertTrue( e.getMessage().contains( "already non-troubled" ) );
        assertFalse( curatable.getCurationDetails().getTroubled() );
    }

    private AuditEvent createEventOfType( Class<? extends AuditEventType> type ) {
        try {
            return AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, "nothing special, just testing",
                    null, null, type.newInstance() );
        } catch ( InstantiationException | IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }
}