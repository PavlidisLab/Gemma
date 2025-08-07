package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeDaoImpl;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class ProcessedExpressionDataVectorDaoTest extends BaseDatabaseTest {

    private static final int NUM_PROBES = 100;

    @Configuration
    @TestComponent
    static class ProcessedExpressionDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ProcessedDataVectorByGeneCache processedDataVectorCache() {
            return mock( ProcessedDataVectorByGeneCache.class );
        }

        @Bean
        public ProcessedExpressionDataVectorDao processedExpressionDataVectorDao( SessionFactory sessionFactory ) {
            return new ProcessedExpressionDataVectorDaoImpl( sessionFactory );
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public QuantitationTypeDao quantitationTypeDao( SessionFactory sessionFactory ) {
            return new QuantitationTypeDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Before
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
    }


    @Test
    public void testGetProcessedVectors() {
        Session session = sessionFactory.getCurrentSession();
        Taxon taxon = new Taxon();
        session.persist( taxon );
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( taxon );
        session.persist( platform );
        ExpressionExperiment ee = new ExpressionExperiment();
        List<CompositeSequence> probes = new ArrayList<>();
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            CompositeSequence probe = new CompositeSequence();
            probe.setArrayDesign( platform );
            session.persist( probe );
            probes.add( probe );
        }
        BioAssayDimension bad = new BioAssayDimension();
        session.persist( bad );
        // create 10000 vectors
        Set<ProcessedExpressionDataVector> vectors = new HashSet<>();
        for ( int i = 0; i < NUM_PROBES; i++ ) {
            ProcessedExpressionDataVector vector = new ProcessedExpressionDataVector();
            vector.setExpressionExperiment( ee );
            vector.setBioAssayDimension( bad );
            vector.setDesignElement( probes.get( i ) );
            vector.setData( new byte[8 * 8] );
            vectors.add( vector );
        }
        ee.setProcessedExpressionDataVectors( vectors );
        ee.setNumberOfDataVectors( vectors.size() );
        session.persist( ee );
        session.flush();
        session.clear();
        Collection<ProcessedExpressionDataVector> reloadedVectors = processedExpressionDataVectorDao.getProcessedVectors( ee );
        assertEquals( NUM_PROBES, reloadedVectors.size() );
    }
}