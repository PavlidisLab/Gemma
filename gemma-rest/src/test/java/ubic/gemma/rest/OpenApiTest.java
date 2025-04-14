package ubic.gemma.rest;


import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.OpenApiFactory;
import ubic.gemma.rest.util.args.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class OpenApiTest extends BaseTest {

    @Configuration
    @TestComponent
    static class OpenApiTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer properties() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=https://gemma.msl.ubc.ca" );
        }

        @Bean
        public FactoryBean<OpenAPI> openApi( CustomModelResolver customModelResolver ) {
            OpenApiFactory factory = new OpenApiFactory( "ubic.gemma.rest.OpenApiTest" );
            factory.setModelConverters( Collections.singletonList( customModelResolver ) );
            return factory;
        }

        @Bean
        public CustomModelResolver customModelResolver( SearchService searchService ) {
            return new CustomModelResolver( searchService );
        }

        @Bean
        public DatasetArgService datasetArgService() {
            return mockFilteringService( DatasetArgService.class, ExpressionExperiment.class );
        }

        @Bean
        public ExpressionAnalysisResultSetArgService expressionAnalysisResultSetArgService() {
            return mockFilteringService( ExpressionAnalysisResultSetArgService.class, ExpressionAnalysisResultSet.class );
        }

        @Bean
        public PlatformArgService platformArgService() {
            return mockFilteringService( PlatformArgService.class, ArrayDesign.class );
        }

        @Bean
        public TaxonArgService taxonService() {
            return mockFilteringService( TaxonArgService.class, Taxon.class );
        }

        @Bean
        public SearchService searchService() {
            SearchService searchService = mock( SearchService.class );
            when( searchService.getSupportedResultTypes() ).thenReturn( Collections.singleton( ExpressionExperiment.class ) );
            when( searchService.getFields( ExpressionExperiment.class ) ).thenReturn( Collections.singleton( "shortName" ) );
            return searchService;
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock( AnalyticsProvider.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }

        private static <S extends Identifiable, T extends EntityArgService<S, ?>> T mockFilteringService( Class<T> clazz, Class<S> elementClass ) {
            T ees = mock( clazz );
            when( ees.getElementClass() ).thenAnswer( a -> elementClass );
            when( ees.getFilterableProperties() ).thenReturn( Collections.emptySet() );
            return ees;
        }
    }

    @Autowired
    private OpenAPI spec;

    @Test
    public void testExternalDocumentationUrlIsReplaced() {
        assertThat( spec.getComponents().getSchemas().get( "FilterArgExpressionExperiment" ).getExternalDocs().getUrl() )
                .isEqualTo( "https://gemma.msl.ubc.ca/resources/apidocs/ubic/gemma/rest/util/args/FilterArg.html" );
    }

    @Test
    public void testInfoMatchContentOfOpenApiConfiguration() throws IOException {
        OpenApiConfiguration config;
        try ( InputStream is = new ClassPathResource( "/openapi-configuration.yaml" ).getInputStream() ) {
            config = Yaml.mapper().readValue( is, OpenApiConfiguration.class );
        }
        assertThat( config.getResourcePackages() )
                .containsExactly( getClass().getPackage().getName() );
        assertThat( config.getDefaultResponseCode() )
                .isEqualTo( "200" );
        assertThat( spec.getInfo().getVersion() )
                .isNotBlank()
                .isEqualTo( config.getOpenAPI().getInfo().getVersion() );
    }

    @Data
    private static class OpenApiConfiguration {
        private String[] resourcePackages;
        private String defaultResponseCode;
        private OpenAPI openAPI;
    }

    @Test
    public void testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection() {
        SoftAssertions assertions = new SoftAssertions();
        for ( String path : spec.getPaths().keySet() ) {
            if ( path.equals( "/genes/probes/refresh" ) ) {
                // FIXME: this is broken, see https://github.com/swagger-api/swagger-core/issues/4693
                continue;
            }
            PathItem operations = spec.getPaths().get( path );
            assertions.assertThat( operations.getGet().getResponses() )
                    .describedAs( "GET %s (%s)", path, operations.getGet().getOperationId() )
                    .hasKeySatisfying( new Condition<>( entry -> entry.equals( "200" )
                            || entry.equals( "201" )
                            || entry.equals( "204" )
                            || entry.startsWith( "3" ),
                            "has at least a default GET response or is a redirection" ) )
                    .allSatisfy( ( responseCode, content ) -> {
                        if ( responseCode.startsWith( "3" ) ) {
                            // a redirection, no need for a default responses
                            assertThat( content.getContent() )
                                    .describedAs( "GET %s -> %s (%s)", path, responseCode, operations.getGet().getOperationId() )
                                    .isNull();
                        } else if ( responseCode.equals( "201" ) ) {
                            // created
                            assertThat( content.getContent() )
                                    .describedAs( "GET %s -> %s (%s)", path, responseCode, operations.getGet().getOperationId() )
                                    .doesNotContainKey( "*/*" );
                        } else if ( responseCode.equals( "204" ) ) {
                            // no content
                            assertThat( content.getContent() )
                                    .describedAs( "GET %s -> %s (%s)", path, responseCode, operations.getGet().getOperationId() )
                                    .isNull();
                        } else {
                            assertThat( content.getContent() )
                                    .describedAs( "GET %s -> %s (%s)", path, responseCode, operations.getGet().getOperationId() )
                                    .isNotEmpty()
                                    .doesNotContainKey( "*/*" );
                        }
                    } );
        }
        assertions.assertAll();
    }

    @Test
    public void testEnsureThatAllErrorResponsesUseResponseErrorObjectWithJsonMediaType() {
        SoftAssertions assertions = new SoftAssertions();
        for ( Map.Entry<String, PathItem> entry : spec.getPaths().entrySet() ) {
            String path = entry.getKey();
            PathItem operations = entry.getValue();
            for ( Map.Entry<String, ApiResponse> e : operations.getGet().getResponses().entrySet() ) {
                String code = e.getKey();
                ApiResponse response = e.getValue();
                if ( code.startsWith( "4" ) || code.startsWith( "5" ) ) {
                    assertions.assertThat( response.getContent() )
                            .describedAs( "GET %s -> %s", path, code )
                            .hasEntrySatisfying( "application/json", content -> {
                                assertThat( content.getSchema().get$ref() ).isEqualTo( "#/components/schemas/ResponseErrorObject" );
                            } );
                }
            }
        }
        assertions.assertAll();
    }

    @Test
    public void testGetDatasetsCategories() {
        assertThat( spec.getPaths().get( "/datasets/categories" ).getGet().getResponses() )
                .hasEntrySatisfying( "200", response -> {
                    assertThat( response.getContent().get( "application/json" ).getSchema().get$ref() )
                            .isEqualTo( "#/components/schemas/QueriedAndFilteredAndInferredAndLimitedResponseDataObjectCategoryWithUsageStatisticsValueObject" );
                } )
                .hasEntrySatisfying( "503", response -> {
                    Assertions.assertThat( response.getContent().get( "application/json" ).getSchema().get$ref() )
                            .isEqualTo( "#/components/schemas/ResponseErrorObject" );
                } );
    }

    @Test
    public void testFilterArgSchemas() {
        assertThat( spec.getComponents().getSchemas() )
                // FIXME: remove the dangling 'Filter'
                // .doesNotContainKey( "Filter" )
                .containsKeys( "FilterArgExpressionExperiment", "FilterArgArrayDesign", "FilterArgExpressionAnalysisResultSet" );
        Schema<?> schema = spec.getComponents().getSchemas().get( "FilterArgExpressionAnalysisResultSet" );
        assertThat( schema.getType() )
                .isEqualTo( "string" );
        assertThat( schema.getProperties() )
                .isNull();
        assertThat( schema.getDescription() ).contains( "Available properties:" );
    }

    @Test
    public void testSortArgSchemas() {
        assertThat( spec.getComponents().getSchemas() )
                // FIXME: remove the dangling 'Sort'
                // .doesNotContainKey( "Sort" )
                .containsKeys( "SortArgExpressionExperiment", "SortArgArrayDesign", "SortArgExpressionAnalysisResultSet" );
        Schema<?> schema = spec.getComponents().getSchemas().get( "SortArgExpressionExperiment" );
        assertThat( schema.getType() )
                .isEqualTo( "string" );
        assertThat( schema.getDescription() ).contains( "Available properties:" );
    }

    @Test
    public void testLimitArgIs5000ForGetDatasetsAnnotations() {
        assertThat( spec.getPaths().get( "/datasets/annotations" ).getGet().getParameters() )
                .anySatisfy( p -> {
                    assertThat( p.getSchema().getType() ).isEqualTo( "integer" );
                    assertThat( p.getSchema().getMinimum() ).isEqualTo( "1" );
                    assertThat( p.getSchema().getMaximum() ).isEqualTo( "5000" );
                } );
    }

    @Test
    public void testSearchableProperties() {
        assertThat( spec.getPaths().get( "/search" ).getGet().getParameters() )
                .anySatisfy( p -> {
                    assertThat( p.getName() ).isEqualTo( "query" );
                    assertThat( p.getSchema().get$ref() ).isEqualTo( "#/components/schemas/QueryArg" );
                } );
        assertThat( spec.getComponents().getSchemas().get( "QueryArg" ) ).satisfies( s -> {
            assertThat( s.getType() ).isEqualTo( "string" );
            //noinspection unchecked
            assertThat( s.getExtensions() )
                    .isNotNull()
                    .containsEntry( "x-gemma-searchable-properties", Collections.singletonMap( ExpressionExperiment.class.getName(), Collections.singletonList( "shortName" ) ) );
            assertThat( s.getExternalDocs().getUrl() )
                    .isEqualTo( "https://lucene.apache.org/core/3_6_2/queryparsersyntax.html" );
        } );
    }

    @Test
    public void testExamplesFromClasspath() throws IOException {
        assertThat( spec.getPaths().get( "/resultSets/{resultSet}" ).getGet().getResponses()
                .get( "200" )
                .getContent()
                .get( "text/tab-separated-values; charset=UTF-8; q=0.9" )
                .getExample() )
                .isEqualTo( IOUtils.resourceToString( "/restapidocs/examples/result-set.tsv", StandardCharsets.UTF_8 ) );
    }
}
