package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

/**
 * @author poirigui
 */
@ContextConfiguration
public class ExpressionExperimentServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ExpressionExperimentServiceTestContextConfiguration {

        @Bean
        public ExpressionExperimentService expressionExperimentService( ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentServiceImpl( expressionExperimentDao );
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao() {
            return mock( ExpressionExperimentDao.class );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
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
        public RawExpressionDataVectorDao rawExpressionDataVectorDao() {
            return mock( RawExpressionDataVectorDao.class );
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
        public BioMaterialService bioMaterialService() {
            return mock();
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
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private OntologyService ontologyService;

    @After
    public void tearDown() {
        reset( ontologyService );
    }

    @Test
    public void testGetFiltersWithInferredAnnotations() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerms( Collections.singleton( "http://example.com/T00001" ) ) ).thenReturn( Collections.singleton( term ) );
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        Filters inferredFilters = expressionExperimentService.getFiltersWithInferredAnnotations( f, null, null, 30, TimeUnit.SECONDS );
        verify( ontologyService ).getTerms( Collections.singleton( "http://example.com/T00001" ) );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000L ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetFiltersWithCategories() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerms( Collections.singleton( "http://example.com/T00001" ) ) ).thenReturn( Collections.singleton( term ) );
        Filters f = Filters.by( "c", "categoryUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.categoryUri" );
        expressionExperimentService.getFiltersWithInferredAnnotations( f, null, null, 30, TimeUnit.SECONDS );
        verifyNoInteractions( ontologyService );
    }

    @Test
    public void testGetAnnotationsUsageFrequency() throws TimeoutException {
        expressionExperimentService.getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, -1 );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( null, null, -1, 0, null, null, null, null );
        verifyNoMoreInteractions( expressionExperimentDao );
    }

    @Test
    public void testGetAnnotationsUsageFrequencyWithFilters() throws TimeoutException {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        expressionExperimentService.getAnnotationsUsageFrequency( f, null, null, null, null, 0, null, -1 );
        verify( expressionExperimentDao ).loadIdsWithCache( f, null );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( Collections.emptyList(), null, -1, 0, null, null, null, null );
        verifyNoMoreInteractions( expressionExperimentDao );
    }
}
