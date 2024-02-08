package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataAddedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataRemovedEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.DataReplacedEvent;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.only;

/**
 * Tests covering integration of single-cell.
 */
@ContextConfiguration
public class SingleCellExpressionExperimentServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class SingleCellExpressionExperimentServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return new SingleCellExpressionExperimentServiceImpl();
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock( AuditTrailService.class );
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock( BioAssayDimensionService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock( ExpressionExperimentSetService.class );
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock( ExpressionExperimentSubSetService.class );
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock( ExperimentalFactorService.class );
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock( FactorValueService.class );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        @Bean
        public PrincipalComponentAnalysisService principalComponentAnalysisService() {
            return mock( PrincipalComponentAnalysisService.class );
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public SecurityService securityService() {
            return mock( SecurityService.class );
        }

        @Bean
        public SVDService svdService() {
            return mock( SVDService.class );
        }

        @Bean
        public CoexpressionAnalysisService coexpressionAnalysisService() {
            return mock( CoexpressionAnalysisService.class );
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock( SampleCoexpressionAnalysisService.class );
        }

        @Bean
        public BlacklistedEntityService blacklistedEntityService() {
            return mock( BlacklistedEntityService.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }
    }

    @Autowired
    private SingleCellExpressionExperimentService scExpressionExperimentService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    private ArrayDesign ad;
    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 10; i++ ) {
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "test" + i );
            cs.setArrayDesign( ad );
            ad.getCompositeSequences().add( cs );
        }
        sessionFactory.getCurrentSession().persist( ad );
        ee = new ExpressionExperiment();
        ee.setTaxon( taxon );
        ee = expressionExperimentDao.create( ee );
    }

    @After
    public void resetMocks() {
        reset( auditTrailService );
    }

    @Test
    public void testAddSingleCellDataVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );

        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .contains( vectors.iterator().next().getQuantitationType() );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .allSatisfy( v -> assertThat( v.getId() ).isNotNull() );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .hasSize( 1 )
                .allSatisfy( scd -> {
                    assertThat( scd.getCellTypeLabel( 0 ) ).isEqualTo( "A" );
                    assertThat( scd.getCellTypeLabel( 50 ) ).isEqualTo( "B" );
                } );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors2.iterator().next().getQuantitationType(), vectors2 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        assertThat( scExpressionExperimentService.getSingleCellDimensions( ee ) )
                .hasSize( 2 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testAddSingleCellDataVectorsTwice() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        assertThatThrownBy( () -> scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "already have vectors for the quantitation type" );
        verify( auditTrailService, only() ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testAddSingleCellDataVectorsWhenThereIsAlreadyAPreferredSetOfVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors.iterator().next().getQuantitationType(), vectors );
        sessionFactory.getCurrentSession().flush();
        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( true );
        scExpressionExperimentService.addSingleCellDataVectors( ee, vectors2.iterator().next().getQuantitationType(), vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 2 )
                .satisfiesOnlyOnce( qt -> assertThat( qt.getIsPreferred() ).isTrue() );
        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
    }

    @Test
    public void testReplaceVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( qt );
        scExpressionExperimentService.replaceSingleCellDataVectors( ee, qt, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 )
                .doesNotContainAnyElementsOf( vectors )
                .containsAll( vectors2 );

        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataReplacedEvent.class ), any() );
    }

    @Test
    public void testRemoveVectors() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( false );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        scExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataRemovedEvent.class ), any() );
    }

    @Test
    public void testRemoveVectorsSharingADimension() {
        Collection<SingleCellExpressionDataVector> vectors = createSingleCellVectors( true );
        QuantitationType qt = vectors.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt, vectors );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        Collection<SingleCellExpressionDataVector> vectors2 = createSingleCellVectors( vectors.iterator().next().getSingleCellDimension() );
        QuantitationType qt2 = vectors2.iterator().next().getQuantitationType();
        scExpressionExperimentService.addSingleCellDataVectors( ee, qt2, vectors2 );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).contains( qt2 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 20 );

        scExpressionExperimentService.removeSingleCellDataVectors( ee, qt );
        sessionFactory.getCurrentSession().flush();
        assertThat( ee.getQuantitationTypes() ).doesNotContain( qt );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 10 );

        verify( auditTrailService, times( 2 ) ).addUpdateEvent( eq( ee ), eq( DataAddedEvent.class ), any() );
        verify( auditTrailService ).addUpdateEvent( eq( ee ), eq( DataRemovedEvent.class ), any() );
    }

    private SingleCellDimension createSingleCellDimension() {
        SingleCellDimension scd = new SingleCellDimension();
        scd.setCellIds( IntStream.range( 0, 100 ).mapToObj( i -> RandomStringUtils.randomAlphanumeric( 10 ) ).collect( Collectors.toList() ) );
        scd.setNumberOfCells( 100 );
        int[] ct = new int[100];
        for ( int i = 0; i < ct.length; i++ ) {
            ct[i] = i < 50 ? 0 : 1;
        }
        scd.setCellTypes( ct );
        scd.setCellTypeLabels( Arrays.asList( "A", "B" ) );
        scd.setNumberOfCellTypeLabels( 2 );
        return scd;
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( boolean preferred ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.LOG2 );
        qt.setIsPreferred( preferred );
        sessionFactory.getCurrentSession().persist( qt );
        return createSingleCellVectors( createSingleCellDimension(), qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( QuantitationType qt ) {
        return createSingleCellVectors( createSingleCellDimension(), qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( SingleCellDimension singleCellDimension ) {
        QuantitationType qt = new QuantitationType();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setScale( ScaleType.LOG2 );
        qt.setIsPreferred( false );
        sessionFactory.getCurrentSession().persist( qt );
        return createSingleCellVectors( singleCellDimension, qt );
    }

    private Collection<SingleCellExpressionDataVector> createSingleCellVectors( SingleCellDimension scd, QuantitationType qt ) {
        Collection<SingleCellExpressionDataVector> vectors = new HashSet<>();
        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            SingleCellExpressionDataVector v = new SingleCellExpressionDataVector();
            v.setDesignElement( cs );
            v.setSingleCellDimension( scd );
            v.setQuantitationType( qt );
            v.setData( new byte[8 * 100] );
            int[] ix = new int[8 * 100];
            for ( int i = 0; i < 800; i++ ) {
                ix[i] = i;
            }
            v.setDataIndices( ix );
            vectors.add( v );
        }
        return vectors;
    }
}
