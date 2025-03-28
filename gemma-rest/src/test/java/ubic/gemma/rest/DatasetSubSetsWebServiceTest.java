package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.DatasetArgService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
public class DatasetSubSetsWebServiceTest extends BaseJerseyTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    static class DatasetSubSetsWebServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public DatasetArgService datasetArgService() {
            return mock();
        }

        @Bean
        public OpenAPI openApi() {
            return mock();
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock();
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }
    }

    @Autowired
    private DatasetArgService datasetArgService;

    @Test
    public void testGetDatasetSubSetGroups() {
        ExpressionExperiment ee = new ExpressionExperiment();
        BioAssayDimension bad = new BioAssayDimension();
        List<ExpressionExperimentSubSet> subsets = Collections.singletonList( ExpressionExperimentSubSet.Factory.newInstance( "test", ee ) );
        when( datasetArgService.getEntity( any() ) ).thenReturn( ee );
        when( expressionExperimentService.getSubSetsByDimension( ee ) ).thenReturn( Collections.singletonMap( bad, new HashSet<>( subsets ) ) );
        ExperimentalFactor factor = new ExperimentalFactor();
        FactorValue fv = FactorValue.Factory.newInstance( factor );
        when( expressionExperimentService.getSubSetsByFactorValue( ee, bad ) ).thenReturn( Collections.singletonMap( factor, Collections.singletonMap( fv, subsets.iterator().next() ) ) );
        assertThat( target( "/datasets/1/subSetGroups" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }
}