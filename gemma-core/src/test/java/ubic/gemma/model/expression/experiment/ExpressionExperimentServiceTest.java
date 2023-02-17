package ubic.gemma.model.expression.experiment;

import gemma.gsec.SecurityService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventDao;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.bioAssayData.RawExpressionDataVectorDao;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;

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
        public AuditEventDao auditEventDao() {
            return mock( AuditEventDao.class );
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
        public ExperimentalFactorDao experimentalFactorDao() {
            return mock( ExperimentalFactorDao.class );
        }

        @Bean
        public FactorValueDao factorValueDao() {
            return mock( FactorValueDao.class );
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
    }

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    private OntologyService ontologyService;

    @Test
    public void testGetFiltersWithInferredAnnotations() {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        expressionExperimentService.getFiltersWithInferredAnnotations( f );
        verify( ontologyService ).getTerm( "http://example.com/T00001" );
    }

    @Test
    public void testGetAnnotationsUsageFrequency() {
        expressionExperimentService.getAnnotationsUsageFrequency( Filters.empty(), -1 );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( null, null, -1 );
        verifyNoMoreInteractions( expressionExperimentDao );
    }

    @Test
    public void testGetAnnotationsUsageFrequencyWithFilters() {
        Filters f = Filters.by( "c", "valueUri", String.class, Filter.Operator.eq, "http://example.com/T00001", "characteristics.valueUri" );
        expressionExperimentService.getAnnotationsUsageFrequency( f, -1 );
        verify( expressionExperimentDao ).loadIds( f, null );
        verify( expressionExperimentDao ).getAnnotationsUsageFrequency( Collections.emptyList(), null, -1 );
        verifyNoMoreInteractions( expressionExperimentDao );
    }
}
