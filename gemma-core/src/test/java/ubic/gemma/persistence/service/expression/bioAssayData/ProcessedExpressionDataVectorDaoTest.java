package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.analysis.preprocess.detect.QuantitationMismatchException;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.randomExpressionMatrix;
import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
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
    }

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    @Before
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
    }

    @Test
    @WithMockUser
    public void testCreateProcessedDataVectors() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new LogNormalDistribution( 9, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LINEAR, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        assertEquals( NUM_PROBES, processedExpressionDataVectorDao.createProcessedDataVectors( ee, false ) );
        assertThat( ee.getQuantitationTypes() ).hasSize( 1 ).first().satisfies( qt -> {
            assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isFalse();
        } );
    }

    @Test
    @WithMockUser
    public void testCreateProcessedDataVectorsFromLog2Data() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 15, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        assertEquals( NUM_PROBES, processedExpressionDataVectorDao.createProcessedDataVectors( ee, false ) );
    }

    @Test
    @WithMockUser
    public void testCreateProcessedDataVectorsFromLog2RatiometricData() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 4, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        assertEquals( NUM_PROBES, processedExpressionDataVectorDao.createProcessedDataVectors( ee, false ) );
    }

    @Test
    @WithMockUser
    public void testThaw() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( NUM_PROBES, 8, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( NUM_PROBES );
        assertEquals( NUM_PROBES, processedExpressionDataVectorDao.createProcessedDataVectors( ee, false ) );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        Collection<ProcessedExpressionDataVector> reloadedVectors;

        // thaw a single vector
        reloadedVectors = processedExpressionDataVectorDao.getProcessedVectors( ee );
        ProcessedExpressionDataVector oneVector = reloadedVectors.iterator().next();
        checkVectorInitializationBeforeThaw( oneVector );
        processedExpressionDataVectorDao.thaw( oneVector );
        checkVectorInitializationAfterThaw( oneVector );

        sessionFactory.getCurrentSession().clear();

        // thaw all vectors in bulk
        reloadedVectors = processedExpressionDataVectorDao.getProcessedVectors( ee );
        assertThat( reloadedVectors ).allSatisfy( ProcessedExpressionDataVectorDaoTest::checkVectorInitializationBeforeThaw );
        processedExpressionDataVectorDao.thaw( reloadedVectors );
        assertThat( reloadedVectors )
                .allSatisfy( ProcessedExpressionDataVectorDaoTest::checkVectorInitializationAfterThaw );
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

    private static void checkVectorInitializationBeforeThaw( ProcessedExpressionDataVector vector ) {
        assertThat( Hibernate.isInitialized( vector ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getExpressionExperiment() ) ).isFalse();
        assertThat( Hibernate.isInitialized( vector.getBioAssayDimension() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getBiologicalCharacteristic() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getQuantitationType() ) ).isTrue();
    }

    private static void checkVectorInitializationAfterThaw( ProcessedExpressionDataVector vector ) {
        assertThat( Hibernate.isInitialized( vector ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getExpressionExperiment() ) ).isTrue();
        assertThat( vector.getExpressionExperiment().getBioAssays() )
                .allMatch( Hibernate::isInitialized );
        assertThat( vector.getExpressionExperiment().getBioAssays() ).allSatisfy( ba -> {
            assertThat( Hibernate.isInitialized( ba.getSampleUsed() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getSampleUsed().getFactorValues() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getArrayDesignUsed() ) ).isTrue();
            assertThat( Hibernate.isInitialized( ba.getOriginalPlatform() ) ).isTrue();
        } );
        assertThat( Hibernate.isInitialized( vector.getBioAssayDimension() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getArrayDesign() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getDesignElement().getBiologicalCharacteristic() ) ).isTrue();
        assertThat( Hibernate.isInitialized( vector.getQuantitationType() ) ).isTrue();
    }


    private ExpressionExperiment getTestExpressionExperimentForRawExpressionMatrix( double[][] matrix, ScaleType scaleType, boolean isRatio ) {
        ExpressionExperiment ee = new ExpressionExperiment();

        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );

        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad.setTechnologyType( TechnologyType.SEQUENCING );
        sessionFactory.getCurrentSession().persist( ad );

        QuantitationType qt = new QuantitationType();
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( scaleType );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( scaleType == ScaleType.COUNT ? StandardQuantitationType.COUNT : StandardQuantitationType.AMOUNT );
        qt.setIsRatio( isRatio );
        qt.setIsPreferred( true );
        sessionFactory.getCurrentSession().persist( qt );
        List<BioMaterial> bioMaterials = new ArrayList<>();
        for ( int i = 0; i < matrix[0].length; i++ ) {
            BioMaterial bm = new BioMaterial();
            bm.setSourceTaxon( taxon );
            sessionFactory.getCurrentSession().persist( bm );
            bioMaterials.add( bm );
        }
        List<BioAssay> bas = new ArrayList<>();
        for ( int i = 0; i < matrix[0].length; i++ ) {
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setSampleUsed( bioMaterials.get( i ) );
            sessionFactory.getCurrentSession().persist( ba );
            bas.add( ba );
        }
        ee.getBioAssays().addAll( bas );

        BioAssayDimension bad = new BioAssayDimension();
        bad.setBioAssays( bas );
        sessionFactory.getCurrentSession().persist( bad );

        List<CompositeSequence> probes = new ArrayList<>();
        for ( int i = 0; i < matrix.length; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setArrayDesign( ad );
            sessionFactory.getCurrentSession().persist( cs );
            probes.add( cs );
        }

        Set<RawExpressionDataVector> vectors = new HashSet<>();
        int i = 0;
        for ( double[] row : matrix ) {
            RawExpressionDataVector ev = new RawExpressionDataVector();
            ev.setBioAssayDimension( bad );
            ev.setDesignElement( probes.get( i ) );
            ev.setData( doubleArrayToBytes( row ) );
            ev.setExpressionExperiment( ee );
            ev.setQuantitationType( qt );
            vectors.add( ev );
            i++;
        }

        ee.setRawExpressionDataVectors( vectors );
        sessionFactory.getCurrentSession().persist( ee );
        return ee;
    }
}