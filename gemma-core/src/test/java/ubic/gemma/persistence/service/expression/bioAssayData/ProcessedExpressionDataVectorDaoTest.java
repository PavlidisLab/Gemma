package ubic.gemma.persistence.service.expression.bioAssayData;

import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.core.datastructure.matrix.QuantitationMismatchException;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils.randomExpressionMatrix;

@ContextConfiguration
public class ProcessedExpressionDataVectorDaoTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class ProcessedExpressionDataVectorDaoTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ProcessedDataVectorCache processedDataVectorCache() {
            return mock( ProcessedDataVectorCache.class );
        }

        @Bean
        public ProcessedExpressionDataVectorDao processedExpressionDataVectorDao( SessionFactory sessionFactory, ProcessedDataVectorCache cache ) {
            return new ProcessedExpressionDataVectorDaoImpl( sessionFactory, cache );
        }
    }

    @Autowired
    private ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;

    private final ByteArrayConverter bac = new ByteArrayConverter();

    @Before
    public void setUp() {
        RandomExpressionDataMatrixUtils.setSeed( 123L );
    }

    @Test
    public void testCreateProcessedDataVectors() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( 100, 4, new LogNormalDistribution( 10, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LINEAR, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( 100 );
        Set<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorDao.createProcessedDataVectors( ee, false );
        assertThat( vectors ).hasSize( 100 );
        assertThat( ee.getQuantitationTypes() ).hasSize( 1 ).first().satisfies( qt -> {
            assertThat( qt.getGeneralType() ).isEqualTo( GeneralType.QUANTITATIVE );
            assertThat( qt.getScale() ).isEqualTo( ScaleType.LOG2 );
            assertThat( qt.getIsRatio() ).isFalse();
        } );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2Data() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( 100, 4, new NormalDistribution( 15, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, false );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( 100 );
        Set<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorDao.createProcessedDataVectors( ee, false );
        assertThat( vectors ).hasSize( 100 );
    }

    @Test
    public void testCreateProcessedDataVectorsFromLog2RatiometricData() throws QuantitationMismatchException {
        double[][] matrix = randomExpressionMatrix( 100, 4, new NormalDistribution( 0, 1 ) );
        ExpressionExperiment ee = getTestExpressionExperimentForRawExpressionMatrix( matrix, ScaleType.LOG2, true );
        assertThat( ee.getProcessedExpressionDataVectors() ).isEmpty();
        assertThat( ee.getRawExpressionDataVectors() ).hasSize( 100 );
        Set<ProcessedExpressionDataVector> vectors = processedExpressionDataVectorDao.createProcessedDataVectors( ee, false );
        assertThat( vectors ).hasSize( 100 );
    }


    private ExpressionExperiment getTestExpressionExperimentForRawExpressionMatrix( double[][] matrix, ScaleType scaleType, boolean isRatio ) {
        ExpressionExperiment ee = new ExpressionExperiment();

        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );

        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        ad.setAuditTrail( new AuditTrail() );
        sessionFactory.getCurrentSession().persist( ad );

        QuantitationType qt = new QuantitationTypeImpl();
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( scaleType );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( scaleType == ScaleType.COUNT ? StandardQuantitationType.COUNT : StandardQuantitationType.AMOUNT );
        qt.setIsRatio( isRatio );
        qt.setIsPreferred( true );
        sessionFactory.getCurrentSession().persist( qt );

        BioAssayDimension bad = new BioAssayDimension();
        List<BioAssay> bas = new ArrayList<>();
        for ( int i = 0; i < matrix[0].length; i++ ) {
            BioMaterial bm = new BioMaterial();
            bm.setSourceTaxon( taxon );
            sessionFactory.getCurrentSession().persist( bm );
            BioAssay ba = new BioAssay();
            ba.setArrayDesignUsed( ad );
            ba.setSampleUsed( bm );
            sessionFactory.getCurrentSession().persist( ba );
            bas.add( ba );
        }
        bad.setBioAssays( bas );
        sessionFactory.getCurrentSession().persist( bad );

        Set<RawExpressionDataVector> vectors = new HashSet<>();
        for ( double[] row : matrix ) {
            RawExpressionDataVector ev = new RawExpressionDataVector();
            ev.setBioAssayDimension( bad );
            CompositeSequence cs = new CompositeSequence();
            cs.setArrayDesign( ad );
            sessionFactory.getCurrentSession().persist( cs );
            ev.setDesignElement( cs );
            ev.setData( bac.doubleArrayToBytes( row ) );
            ev.setExpressionExperiment( ee );
            ev.setQuantitationType( qt );
            vectors.add( ev );
        }

        ee.setRawExpressionDataVectors( vectors );
        ee.setAuditTrail( new AuditTrail() );
        sessionFactory.getCurrentSession().persist( ee );
        return ee;
    }
}