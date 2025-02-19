package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class RawAndProcessedExpressionDataVectorDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class RawAndProcessedExpressionDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public RawExpressionDataVectorDao rawExpressionDataVectorDao() {
            return mock( RawExpressionDataVectorDao.class );
        }

        @Bean
        public ProcessedExpressionDataVectorDao processedExpressionDataVectorDao() {
            return mock( ProcessedExpressionDataVectorDao.class );
        }

        @Bean
        public RawAndProcessedExpressionDataVectorDao rawAndProcessedExpressionDataVectorDao( SessionFactory sessionFactory ) {
            return new RawAndProcessedExpressionDataVectorDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private RawAndProcessedExpressionDataVectorDao rawAndProcessedExpressionDataVectorDao;

    @Autowired
    private RawExpressionDataVectorDao rawExpressionDataVectorDao;

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @After
    public void tearDown() {
        reset( rawExpressionDataVectorDao, processedExpressionDataVectorDao );
    }

    @Test
    public void testFind() {
        RawExpressionDataVector ev = new RawExpressionDataVector();
        assertNull( rawAndProcessedExpressionDataVectorDao.find( ev ) );
        verify( rawExpressionDataVectorDao ).find( ev );
        verifyNoInteractions( processedExpressionDataVectorDao );
    }

    @Test
    public void testRemoveByCompositeSequence() {
        Session session = sessionFactory.getCurrentSession();
        Taxon taxon = Taxon.Factory.newInstance( "test" );
        session.persist( taxon );
        ArrayDesign ad = ArrayDesign.Factory.newInstance( "test", taxon );
        ad.setPrimaryTaxon( taxon );
        session.persist( ad );
        CompositeSequence cs = CompositeSequence.Factory.newInstance( "test", ad );
        session.persist( cs );
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        session.persist( bad );
        RawExpressionDataVector ev = new RawExpressionDataVector();
        ev.setDesignElement( cs );
        ev.setBioAssayDimension( bad );
        ev.setData( new byte[0] );
        ProcessedExpressionDataVector pv = new ProcessedExpressionDataVector();
        pv.setDesignElement( cs );
        pv.setBioAssayDimension( bad );
        pv.setData( new byte[0] );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ev.setExpressionExperiment( ee );
        pv.setExpressionExperiment( ee );
        ee.setRawExpressionDataVectors( Collections.singleton( ev ) );
        ee.setProcessedExpressionDataVectors( Collections.singleton( pv ) );
        ee.setNumberOfDataVectors( 1 );
        sessionFactory.getCurrentSession().persist( ee );
        assertEquals( 2, rawAndProcessedExpressionDataVectorDao.findByExpressionExperiment( ee ).size() );
        assertEquals( 2, rawAndProcessedExpressionDataVectorDao.removeByCompositeSequence( cs ) );
    }
}