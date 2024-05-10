package ubic.gemma.rest;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.TestComponent;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.BaseJerseyTest;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.rest.util.Assertions.assertThat;

@ContextConfiguration
public class OpenApiTest extends BaseJerseyTest {

    @Import(JacksonConfig.class)
    @Configuration
    @TestComponent
    static class OpenApiTestContextConfiguration {

        @Bean
        public CustomModelResolver customModelResolver( SearchService searchService ) {
            return new CustomModelResolver( Json.mapper(), searchService );
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
            return mock( SearchService.class );
        }

        @Bean
        public AnalyticsProvider analyticsProvider() {
            return mock( AnalyticsProvider.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }

        private static <S extends Identifiable, T extends EntityArgService<S, ?>> T mockFilteringService( Class<T> clazz, Class<S> elementClass ) {
            T ees = mock( clazz );
            when( ees.getElementClass() ).thenAnswer( a -> elementClass );
            when( ees.getFilterableProperties() ).thenReturn( Collections.emptySet() );
            return ees;
        }
    }

    @Autowired
    private CustomModelResolver customModelResolver;

    @Autowired
    private SearchService searchService;

    @Autowired
    @Qualifier("swaggerObjectMapper")
    private ObjectMapper objectMapper;

    private OpenAPI spec;

    @Before
    public void setUpSpec() throws IOException {
        when( searchService.getSupportedResultTypes() ).thenReturn( Collections.singleton( ExpressionExperiment.class ) );
        when( searchService.getFields( ExpressionExperiment.class ) ).thenReturn( Collections.singleton( "shortName" ) );
        // FIXME: this is normally initialized in the servlet
        ModelConverters.getInstance().addConverter( customModelResolver );
        Response response = target( "/openapi.json" ).request().get();
        assertThat( response )
                .hasStatus( Response.Status.OK )
                .hasEncoding( "gzip" );
        spec = objectMapper.readValue( response.readEntity( InputStream.class ), OpenAPI.class );
    }

    @Test
    public void testInfoMatchContentOfOpenApiConfiguration() throws IOException {
        OpenApiConfiguration config;
        try ( InputStream is = new ClassPathResource( "/openapi-configuration.yaml" ).getInputStream() ) {
            config = Yaml.mapper().readValue( is, OpenApiConfiguration.class );
        }
        assertThat( config.getResourcePackages() )
                .containsExactly( getClass().getPackage().getName() );
        assertThat( spec.getInfo().getVersion() )
                .isNotBlank()
                .isEqualTo( config.getOpenAPI().getInfo().getVersion() );
    }

    @Data
    private static class OpenApiConfiguration {
        private String[] resourcePackages;
        private OpenAPI openAPI;
    }

    @Test
    public void testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection() {
        assertThat( spec.getPaths() ).allSatisfy( ( path, operations ) -> {
            assertThat( operations.getGet().getResponses() )
                    .describedAs( "GET %s", path )
                    .hasEntrySatisfying( new Condition<>( entry -> {
                        if ( entry.getKey().startsWith( "3" ) ) {
                            // a redirection, no need for a default response
                        } else {
                            assertThat( entry.getKey() ).isEqualTo( "default" );
                            assertThat( entry.getValue().getContent() )
                                    .describedAs( "GET %s -> default", path )
                                    .isNotEmpty()
                                    .doesNotContainKey( "*/*" );
                        }
                        return true;
                    }, "" ) );
        } );
    }

    @Test
    public void testEnsureThatAllErrorResponsesUseResponseErrorObjectWithJsonMediaType() {
        assertThat( spec.getPaths() ).allSatisfy( ( path, operations ) -> {
            assertThat( operations.getGet().getResponses() ).allSatisfy( ( code, response ) -> {
                if ( code.startsWith( "4" ) || code.startsWith( "5" ) ) {
                    assertThat( response.getContent() )
                            .describedAs( "GET %s -> %s", path, code )
                            .hasEntrySatisfying( "application/json", content -> {
                                assertThat( content.getSchema().get$ref() ).isEqualTo( "#/components/schemas/ResponseErrorObject" );
                            } );
                }
            } );
        } );
    }

    @Test
    public void testGetDatasetsCategories() {
        assertThat( spec.getPaths().get( "/datasets/categories" ).getGet().getResponses() )
                .hasEntrySatisfying( "default", response -> {
                    assertThat( response.getContent().get( "application/json" ).getSchema().get$ref() )
                            .isEqualTo( "#/components/schemas/QueriedAndFilteredResponseDataObjectCategoryWithUsageStatisticsValueObject" );
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
}
