package ubic.gemma.persistence.service.expression.experiment;

import gemma.gsec.SecurityService;
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
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
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

        @Bean
        public CoexpressionService coexpressionService() {
            return mock();
        }
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CoexpressionService coexpressionService;

    @Autowired
    private SecurityService securityService;

    @After
    public void tearDown() {
        reset( ontologyService, coexpressionService, securityService );
    }

    @Test
    public void testGetEnhancedFilters() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerms( eq( Collections.singleton( "http://example.com/T00001" ) ), anyLong(), any() ) ).thenReturn( Collections.singleton( term ) );
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        Filters inferredFilters = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters ).hasToString( "any(c.valueUri = http://example.com/T00001)" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com/T00001" ) ), longThat( l -> l > 0 && l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000L ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetEnhancedFiltersWhenANegativeQueryIsPerformed() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerms( eq( Collections.singleton( "http://example.com/T00001" ) ), anyLong(), any() ) ).thenReturn( Collections.singleton( term ) );
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.notEq, "http://example.com/T00001", "characteristics.valueUri" );
        Filters inferredFilters = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters ).hasToString( "none(c.valueUri = http://example.com/T00001)" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com/T00001" ) ), longThat( l -> l > 0 && l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000L ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testGetEnhancedFiltersWhenAPredicateOrObjectIsUsed() throws TimeoutException {
        Filters f = Filters.by( "ac", "object", String.class, Filter.Operator.eq, "http://example.com/T00001", "allCharacteristics.object" );
        Filters inferredFilter = expressionExperimentService.getEnhancedFilters( f, null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilter )
                .hasToString( "ac.object = http://example.com/T00001 or ac.secondObject = http://example.com/T00001" );
        assertThat( inferredFilter.toOriginalString() )
                .isEqualTo( "allCharacteristics.object = http://example.com/T00001" );
        assertThat( inferredFilter )
                .singleElement()
                .satisfies( subClause -> {
                    assertThat( subClause ).hasSize( 2 )
                            .extracting( Filter::getPropertyName )
                            .containsExactly( "object", "secondObject" );
                } );
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
        expressionExperimentService.getAnnotationsUsageFrequency( Filters.empty(), null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( null, null, -1, 0, null, null, null, null, false, false );
        verifyNoMoreInteractions( expressionExperimentDao );
    }

    @Test
    public void testGetAnnotationsUsageFrequencyWithFilters() throws TimeoutException {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        expressionExperimentService.getAnnotationsUsageFrequency( f, null, null, null, null, 0, null, -1, false, false, 5000, TimeUnit.MILLISECONDS );
        verify( expressionExperimentDao ).loadIdsWithCache( f, null );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( Collections.emptyList(), null, -1, 0, null, null, null, null, false, false );
        verifyNoMoreInteractions( expressionExperimentDao );
    }

    @Test
    public void testRemoveDatasetWithCoexpressionLinks() {
        ExpressionExperiment ee = new ExpressionExperiment();
        when( coexpressionService.hasLinks( ee ) ).thenReturn( true );
        when( securityService.isEditableByCurrentUser( ee ) ).thenReturn( true );
        assertThatThrownBy( () -> expressionExperimentService.remove( ee ) )
                .isInstanceOf( IllegalStateException.class );
    }
}
