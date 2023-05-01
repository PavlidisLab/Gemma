package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.CurationDetailsDao;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ExpressionExperimentDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public CurationDetailsDao curationDetailsDao() {
            return mock( CurationDetailsDao.class );
        }
    }

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Test
    public void testThawTransientEntity() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setExperimentalDesign( new ExperimentalDesign() );
        ee.setBioAssays( Collections.singleton( new BioAssay() ) );
        ee.setRawExpressionDataVectors( Collections.singleton( new RawExpressionDataVector() ) );
        ee.setProcessedExpressionDataVectors( Collections.singleton( new ProcessedExpressionDataVector() ) );
        expressionExperimentDao.thaw( ee );
        expressionExperimentDao.thawBioAssays( ee );
        expressionExperimentDao.thawWithoutVectors( ee );
        expressionExperimentDao.thawForFrontEnd( ee );
    }

    @Test
    public void testThaw() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thaw( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawBioAssays() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawBioAssays( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        assertTrue( Hibernate.isInitialized( ee.getBioAssays() ) );
    }

    @Test
    public void testThawForFrontEnd() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawForFrontEnd( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testThawWithoutVectors() {
        ExpressionExperiment ee = createExpressionExperiment();
        ee = reload( ee );
        assertFalse( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
        expressionExperimentDao.thawWithoutVectors( ee );
        assertTrue( Hibernate.isInitialized( ee.getExperimentalDesign() ) );
    }

    @Test
    public void testLoadReference() {
        ExpressionExperiment ee = createExpressionExperiment();
        assertSame( ee, expressionExperimentDao.loadReference( ee.getId() ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee );
        ExpressionExperiment proxy = expressionExperimentDao.loadReference( ee.getId() );
        assertFalse( Hibernate.isInitialized( proxy ) );
        assertEquals( ee.getId(), proxy.getId() );
        assertFalse( Hibernate.isInitialized( proxy ) );
    }

    @Test
    public void testLoadMultipleReferences() {
        ExpressionExperiment ee1 = createExpressionExperiment();
        ExpressionExperiment ee2 = createExpressionExperiment();
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( ee1 );
        sessionFactory.getCurrentSession().evict( ee2 );
        Collection<ExpressionExperiment> ees = expressionExperimentDao.loadReference( Arrays.asList( ee1.getId(), ee2.getId() ) );
        for ( ExpressionExperiment ee : ees ) {
            assertFalse( Hibernate.isInitialized( ee ) );
        }
    }

    private ExpressionExperiment reload( ExpressionExperiment e ) {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( e );
        return expressionExperimentDao.load( e.getId() );
    }

    private ExpressionExperiment createExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setAuditTrail( new AuditTrail() );
        ee.setExperimentalDesign( new ExperimentalDesign() );
        return expressionExperimentDao.create( ee );
    }
}