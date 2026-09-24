package ubic.gemma.rest;


import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.Data;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.concurrent.FutureUtils;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.swagger.resolver.CustomModelResolver;
import ubic.gemma.rest.util.OpenApiFactory;
import ubic.gemma.rest.util.args.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class OpenApiTest extends BaseTest5 implements InitializingBean {

    @Configuration
    @TestComponent
    static class OpenApiTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer properties() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=https://gemma.msl.ubc.ca" );
        }

        @Bean
        public OpenApiFactory openApi( CustomModelResolver customModelResolver ) {
            OpenApiFactory factory = new OpenApiFactory( "ubic.gemma.rest.OpenApiTest" );
            factory.setModelConverters( Collections.singletonList( customModelResolver ) );
            return factory;
        }

        @Bean
        public CustomModelResolver customModelResolver() {
            return new CustomModelResolver();
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

        /**
         * Needed since {@code GET /platforms/{platform}/elements} gained a {@code ?filter=}
         * argument: {@code CustomModelResolver.resolveAvailableProperties} looks up an
         * {@link EntityArgService} per {@code FilterArg<T>} element type to enumerate the
         * filterable properties for the spec, and throws when none is registered.
         */
        @Bean
        public CompositeSequenceArgService compositeSequenceArgService() {
            return mockFilteringService( CompositeSequenceArgService.class, CompositeSequence.class );
        }

        @Bean
        public TaxonArgService taxonService() {
            return mockFilteringService( TaxonArgService.class, Taxon.class );
        }

        @Bean
        public SearchService searchService() {
            SearchService searchService = mock( SearchService.class );
            when( searchService.getSupportedResultTypes() ).thenReturn( Collections.singleton( ExpressionExperiment.class ) );
            when( searchService.getFields( ExpressionExperiment.class, SearchSettings.SearchMode.ACCURATE ) )
                    .thenReturn( Collections.singleton( "shortName" ) );
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
    private BeanFactory beanFactory;

    private OpenAPI spec;

    @Override
    public void afterPropertiesSet() {
        spec = FutureUtils.get( ( Future<OpenAPI> ) beanFactory.getBean( "openApi", Future.class ) );
    }

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
            PathItem pathItem = spec.getPaths().get( path );
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet() ) {
                PathItem.HttpMethod method = opEntry.getKey();
                Operation operation = opEntry.getValue();
                assertions.assertThat( operation.getResponses() )
                        .describedAs( "%s %s (%s)", method, path, operation.getOperationId() )
                        .hasKeySatisfying( new Condition<>( entry -> entry.equals( "200" )
                                || entry.equals( "201" )
                                || entry.equals( "202" )
                                || entry.equals( "204" )
                                || entry.startsWith( "3" ),
                                "has at least a default response or is a redirection" ) )
                        .allSatisfy( ( responseCode, content ) -> {
                            if ( responseCode.startsWith( "3" ) ) {
                                // a redirection, no need for a default responses
                                assertThat( content.getContent() )
                                        .describedAs( "%s %s -> %s (%s)", method, path, responseCode, operation.getOperationId() )
                                        .isNull();
                            } else if ( responseCode.equals( "201" ) ) {
                                // created
                                assertThat( content.getContent() )
                                        .describedAs( "%s %s -> %s (%s)", method, path, responseCode, operation.getOperationId() )
                                        .doesNotContainKey( "*/*" );
                            } else if ( responseCode.equals( "204" ) ) {
                                // no content
                                assertThat( content.getContent() )
                                        .describedAs( "%s %s -> %s (%s)", method, path, responseCode, operation.getOperationId() )
                                        .isNull();
                            } else {
                                assertThat( content.getContent() )
                                        .describedAs( "%s %s -> %s (%s)", method, path, responseCode, operation.getOperationId() )
                                        .isNotEmpty()
                                        .doesNotContainKey( "*/*" );
                            }
                        } );
            }
        }
        assertions.assertAll();
    }

    @Test
    public void testEnsureThatAllErrorResponsesUseResponseErrorObjectWithJsonMediaType() {
        SoftAssertions assertions = new SoftAssertions();
        for ( Map.Entry<String, PathItem> entry : spec.getPaths().entrySet() ) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathItem.readOperationsMap().entrySet() ) {
                PathItem.HttpMethod method = opEntry.getKey();
                Operation operation = opEntry.getValue();
                for ( Map.Entry<String, ApiResponse> e : operation.getResponses().entrySet() ) {
                    String code = e.getKey();
                    ApiResponse response = e.getValue();
                    if ( code.startsWith( "4" ) || code.startsWith( "5" ) ) {
                        // PUT /datasets/{dataset}/design intentionally returns a DesignPreflightReport
                        // on 400 (blockers) and 409 (force required) so admins can act on the cascade.
                        if ( method == PathItem.HttpMethod.PUT
                                && "/datasets/{dataset}/design".equals( path )
                                && ( "400".equals( code ) || "409".equals( code ) ) ) {
                            continue;
                        }
                        // GET /health intentionally returns the same HealthValueObject body on 503
                        // (any component DOWN) so external uptime tools can react without a separate
                        // error-shape parser. The 200 and 503 share the HealthValueObject schema.
                        if ( method == PathItem.HttpMethod.GET
                                && "/health".equals( path )
                                && "503".equals( code ) ) {
                            continue;
                        }
                        // POST /preboarded 409 intentionally returns a richer body
                        // (error, accession, existing_id, existing_type) so callers can act on the conflict.
                        if ( method == PathItem.HttpMethod.POST
                                && "/preboarded".equals( path )
                                && "409".equals( code ) ) {
                            continue;
                        }
                        // POST /preboarded/{id}/promote 409 intentionally returns a richer body
                        // (error, preboardedId) so callers can act on the already-promoted state.
                        if ( method == PathItem.HttpMethod.POST
                                && "/preboarded/{id}/promote".equals( path )
                                && "409".equals( code ) ) {
                            continue;
                        }
                        // PUT /datasets/{id}/workflow 409 intentionally returns a richer body
                        // (error, currentState, targetState, allowedNextStates) so the UI can
                        // re-render the transition picker without a second round-trip.
                        if ( method == PathItem.HttpMethod.PUT
                                && "/datasets/{id}/workflow".equals( path )
                                && "409".equals( code ) ) {
                            continue;
                        }
                        // Mirror the original hasEntrySatisfying("application/json", ...) semantics:
                        // vacuously satisfied when the response has no application/json content block.
                        // Inlined here to surface ALL violations as soft-assertion failures rather than
                        // NPE-on-first-miss (lambda inside hasEntrySatisfying threw on null schema).
                        if ( response.getContent() != null && response.getContent().containsKey( "application/json" ) ) {
                            io.swagger.v3.oas.models.media.MediaType jsonContent = response.getContent().get( "application/json" );
                            assertions.assertThat( jsonContent.getSchema() )
                                    .describedAs( "%s %s -> %s application/json schema", method, path, code )
                                    .isNotNull();
                            if ( jsonContent.getSchema() != null ) {
                                assertions.assertThat( jsonContent.getSchema().get$ref() )
                                        .describedAs( "%s %s -> %s application/json schema $ref", method, path, code )
                                        .isEqualTo( "#/components/schemas/ResponseErrorObject" );
                            }
                        }
                    }
                }
            }
        }
        assertions.assertAll();
    }

    /**
     * {@code GET /resultSets/{id}/pvalueDistribution} serves the stored histogram. The spec has to say
     * so: the default column is the uncorrected one, {@code corrected} is not on the menu at all, and
     * {@code bins} is capped at the stored bin count instead of the old 1..1000 range.
     */
    @Test
    public void testPvalueDistributionAdvertisesTheStoredHistogramContract() {
        Operation op = spec.getPaths().get( "/resultSets/{resultSet}/pvalueDistribution" ).getGet();
        assertThat( op ).isNotNull();

        Parameter column = op.getParameters().stream()
                .filter( p -> "column".equals( p.getName() ) )
                .findFirst().orElseThrow( () -> new AssertionError( "no 'column' parameter" ) );
        assertThat( column.getSchema().getDefault() ).isEqualTo( "raw" );
        assertThat( column.getSchema().getEnum() ).containsExactly( "raw" );

        Parameter bins = op.getParameters().stream()
                .filter( p -> "bins".equals( p.getName() ) )
                .findFirst().orElseThrow( () -> new AssertionError( "no 'bins' parameter" ) );
        assertThat( String.valueOf( bins.getSchema().getDefault() ) ).isEqualTo( "20" );
        assertThat( bins.getSchema().getMaximum() ).isEqualByComparingTo( "100" );

        assertThat( op.getDescription() )
                .contains( "1, 2, 4, 5, 10, 20, 25, 50" )
                .doesNotContain( "1000" );
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
        assertThat( spec.getPaths().get( "/datasets" ).getGet().getParameters() )
                .anySatisfy( p -> {
                    assertThat( p.getName() ).isEqualTo( "query" );
                    assertThat( p.getSchema().get$ref() ).isEqualTo( "#/components/schemas/QueryArg" );
                    assertThat( p.getDescription() ).isEqualTo( "If specified, `sort` will default to `-searchResult.score` instead of `+id`. Note that sorting by `searchResult.score` is only valid if a query is specified." );
                } );
        assertThat( spec.getComponents().getSchemas().get( "QueryArg" ) ).satisfies( s -> {
            assertThat( s.getType() ).isEqualTo( "string" );
            assertThat( s.getDescription() ).startsWith( "Filter results matching the given full-text query.\n\nThe search query accepts the following syntax:" );
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

    /**
     * The wire speaks one language, and it is camelCase.
     *
     * <p>Every property name in every published schema, and every query parameter, must be
     * camelCase. This is the guard for {@code c4d2d4ceb9} / {@code 8b2c8b09ff}, which collapsed
     * the two conventions the API used to serve at once. It exists because the first sweep
     * grepped {@code gemma-rest} and missed {@code GeoScrapeDryRunCandidate} — a response class
     * that lives in gemma-core but is serialized by a gemma-rest resource. A half-done rename is
     * worse than either end state, and the half left undone was the response a downstream
     * screening script consumed. Reading the spec instead of the source catches that class of
     * miss regardless of which module the class lives in.</p>
     *
     * <p>Enum VALUES are deliberately not checked: {@code expression_experiment},
     * {@code gemma_intake} and friends are data, not keys, and renaming them would stop matching
     * what is stored. This walks property names and parameter names only.</p>
     */
    @Test
    public void testWireNamesAreCamelCaseEverywhere() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;

        Map<String, Schema> schemas = spec.getComponents().getSchemas();
        for ( Map.Entry<String, Schema> e : schemas.entrySet() ) {
            Map<String, Schema> props = e.getValue().getProperties();
            if ( props == null ) continue;
            for ( String prop : props.keySet() ) {
                inspected++;
                if ( prop.indexOf( '_' ) >= 0 ) {
                    offenders.add( "schema " + e.getKey() + "." + prop );
                }
            }
        }

        for ( Map.Entry<String, PathItem> e : spec.getPaths().entrySet() ) {
            for ( Operation op : e.getValue().readOperations() ) {
                if ( op.getParameters() == null ) continue;
                for ( Parameter param : op.getParameters() ) {
                    String name = param.getName();
                    if ( name == null ) continue;
                    inspected++;
                    if ( name.indexOf( '_' ) >= 0 ) {
                        offenders.add( "parameter " + e.getKey() + " ?" + name );
                    }
                }
            }
        }

        // Guard against the guard going vacuous: if the spec ever stops exposing property and
        // parameter names, this test would pass by inspecting nothing.
        assertThat( inspected )
                .withFailMessage( "expected the spec to expose many wire names; inspected only %d", inspected )
                .isGreaterThan( 200 );

        assertThat( offenders )
                .withFailMessage( "snake_case on the wire (the API serves camelCase only): %s", offenders )
                .isEmpty();
    }
}
