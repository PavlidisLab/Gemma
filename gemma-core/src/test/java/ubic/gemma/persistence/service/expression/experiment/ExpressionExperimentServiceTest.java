package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.core.security.gsec.SecurityService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorDao;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author poirigui
 */
@ContextConfiguration
public class ExpressionExperimentServiceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class ExpressionExperimentServiceTestContextConfiguration {

        @Bean
        public ExpressionExperimentService expressionExperimentService( ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentServiceImpl( expressionExperimentDao );
        }

        @Bean
        public ExpressionExperimentReadService expressionExperimentReadService() {
            return mock( ExpressionExperimentReadService.class );
        }

        @Bean
        public ExpressionExperimentWriteService expressionExperimentWriteService() {
            return mock( ExpressionExperimentWriteService.class );
        }

        @Bean
        public ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService() {
            return mock( ExpressionExperimentSubSetReadService.class );
        }

        @Bean
        public ExpressionExperimentDataVectorService expressionExperimentDataVectorService() {
            return mock( ExpressionExperimentDataVectorService.class );
        }

        @Bean
        public ExpressionExperimentFilterRewriteHelperService expressionExperimentFilterInferenceHelperService( OntologyService ontologyService ) {
            return new ExpressionExperimentFilterRewriteHelperService( ontologyService );
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

        @Bean
        public CharacteristicService characteristicService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private ExpressionExperimentReadService expressionExperimentReadService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private SecurityService securityService;

    @After
    public void tearDown() {
        reset( ontologyService, securityService, expressionExperimentReadService );
    }

    @Test
    public void testGetEnhancedFilters() throws TimeoutException {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        Filters expected = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        when( expressionExperimentReadService.getEnhancedFilters( eq( f ), any(), any(), anyLong(), any() ) ).thenReturn( expected );
        Filters inferredFilters = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters ).isSameAs( expected );
        verify( expressionExperimentReadService ).getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
    }

    @Test
    public void testGetEnhancedFiltersWhenANegativeQueryIsPerformed() throws TimeoutException {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.notEq, "http://example.com/T00001", "characteristics.valueUri" );
        Filters expected = Filters.by( "c", "valueUri", String.class, Filter.Operator.notEq, "http://example.com/T00001", "characteristics.valueUri" );
        when( expressionExperimentReadService.getEnhancedFilters( eq( f ), any(), any(), anyLong(), any() ) ).thenReturn( expected );
        Filters inferredFilters = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters ).isSameAs( expected );
        verify( expressionExperimentReadService ).getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
    }

    @Test
    public void testGetEnhancedFiltersWhenAPredicateOrObjectIsUsed() throws TimeoutException {
        Filters f = Filters.by( "ac", "object", String.class, Filter.Operator.eq, "http://example.com/T00001", "allCharacteristics.object" );
        Filters expected = Filters.by( "ac", "object", String.class, Filter.Operator.eq, "http://example.com/T00001", "allCharacteristics.object" );
        when( expressionExperimentReadService.getEnhancedFilters( eq( f ), any(), any(), anyLong(), any() ) ).thenReturn( expected );
        Filters inferredFilter = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilter ).isSameAs( expected );
        verify( expressionExperimentReadService ).getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
    }

    @Test
    public void testGetFiltersWithCategories() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerms( eq( Collections.singleton( "http://example.com/T00001" ) ), anyLong(), any() ) ).thenReturn( Collections.singleton( term ) );
        Filters f = Filters.by( "c", "categoryUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.categoryUri" );
        expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        verifyNoInteractions( ontologyService );
    }

    @Test
    public void testGetAnnotationsUsageFrequency() throws TimeoutException {
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> expected = Collections.emptyList();
        when( expressionExperimentReadService.getAnnotationsUsageFrequency( any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) ).thenReturn( expected );
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> result =
                expressionExperimentService.getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
        assertThat( result ).isSameAs( expected );
        verify( expressionExperimentReadService ).getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
    }

    @Test
    public void testGetAnnotationsUsageFrequencyWithFilters() throws TimeoutException {
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> expected = Collections.emptyList();
        when( expressionExperimentReadService.getAnnotationsUsageFrequency( any(), any(), any(), any(), any(), anyInt(), any(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) ).thenReturn( expected );
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        List<ExpressionExperimentService.CharacteristicWithUsageStatisticsAndOntologyTerm> result =
                expressionExperimentService.getAnnotationsUsageFrequency( f, null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
        assertThat( result ).isSameAs( expected );
        verify( expressionExperimentReadService ).getAnnotationsUsageFrequency( f, null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
    }
}
