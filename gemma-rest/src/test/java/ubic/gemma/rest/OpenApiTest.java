package ubic.gemma.rest;


import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.Nullable;
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
import ubic.gemma.rest.annotations.Costly;
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
                            } else if ( "logout".equals( operation.getOperationId() ) ) {
                                // POST /logout answers 200 with an empty body. 204 would say that
                                // properly, but the status is part of its published contract, so the
                                // spec records what it does rather than what it should have done.
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
                        // GET /preboarded 501 intentionally returns a richer body
                        // (error, state, redirectTo) naming the endpoint that does serve the listing.
                        if ( method == PathItem.HttpMethod.GET
                                && "/preboarded".equals( path )
                                && "501".equals( code ) ) {
                            continue;
                        }
                        // GET /metrics is a Prometheus scrape target: it answers text/plain on every
                        // status, so an error there has no JSON body to check.
                        if ( "scrape".equals( operation.getOperationId() ) ) {
                            continue;
                        }
                        // Requiring the entry, not just checking it when present. Skipping the check for a
                        // response with no application/json block is how four 503s came to advertise a JSON
                        // error body under the method's text/tab-separated-values @Produces: the entry was
                        // missing, so nothing looked. Inlined rather than hasEntrySatisfying so every
                        // violation surfaces as a soft assertion instead of an NPE on the first miss.
                        assertions.assertThat( response.getContent() )
                                .describedAs( "%s %s -> %s offers no application/json error body", method, path, code )
                                .isNotNull()
                                .containsKey( "application/json" );
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
        // The key used to carry "; q=0.9", copied from the @Produces quality-of-source weight;
        // OpenApiFactory strips that now, so the media type is the one a client actually sends.
        assertThat( spec.getPaths().get( "/resultSets/{resultSet}" ).getGet().getResponses()
                .get( "200" )
                .getContent()
                .get( "text/tab-separated-values; charset=UTF-8" )
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

    /**
     * Every response container reachable from a path must say what its {@code data} payload holds.
     *
     * <p>An endpoint that supports both offset and cursor pagination returns {@link Object} and
     * declares its shapes with {@code @Schema(oneOf = {...})}. Java erases type arguments, so naming
     * a raw generic container there — {@code PaginatedResponseDataObject.class} — produces a schema
     * whose {@code data} is an array of bare {@code object}, and the payload type is gone. A client
     * generated from that deserializes to untyped dictionaries and reads nulls for every field
     * instead of failing, which is why this is worth a build failure rather than a code review.
     *
     * <p>The fix is to name a bound subclass from {@link ubic.gemma.rest.util.OpenApiResponseTypes}
     * instead. Since the method's return type is {@code Object}, nothing but this test checks that
     * the declared container is also the one the method actually builds.
     */
    @Test
    public void testPaginatedResponsesDeclareTheirPayloadType() {
        Map<String, Schema> schemas = spec.getComponents().getSchemas();
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Operation operation = opEntry.getValue();
                if ( operation.getResponses() == null ) {
                    continue;
                }
                for ( Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet() ) {
                    ApiResponse response = responseEntry.getValue();
                    if ( response.getContent() == null ) {
                        continue;
                    }
                    for ( io.swagger.v3.oas.models.media.MediaType media : response.getContent().values() ) {
                        for ( String schemaName : referencedSchemaNames( media.getSchema() ) ) {
                            Schema<?> container = schemas.get( schemaName );
                            if ( container == null || container.getProperties() == null ) {
                                continue;
                            }
                            Schema<?> data = ( Schema<?> ) container.getProperties().get( "data" );
                            if ( data == null || !"array".equals( data.getType() ) ) {
                                continue;
                            }
                            inspected++;
                            Schema<?> items = data.getItems();
                            boolean typed = items != null
                                    && ( items.get$ref() != null
                                    || ( items.getType() != null && !"object".equals( items.getType() ) ) );
                            if ( !typed ) {
                                offenders.add( String.format( "%s %s -> %s: %s has an untyped data array",
                                        opEntry.getKey(), pathEntry.getKey(), responseEntry.getKey(), schemaName ) );
                            }
                        }
                    }
                }
            }
        }

        // Guard against the guard going vacuous: if the spec ever stops emitting list containers,
        // this test would pass by inspecting nothing.
        assertThat( inspected )
                .withFailMessage( "expected the spec to expose many list-shaped responses; inspected only %d", inspected )
                .isGreaterThan( 50 );

        assertThat( offenders )
                .withFailMessage( "response containers that lost their payload type — name a bound subclass"
                        + " from OpenApiResponseTypes in the @Schema(oneOf = ...) instead of the raw generic: %s",
                        offenders )
                .isEmpty();
    }

    /**
     * Component schema names a response schema points at, following a {@code $ref} directly and each
     * branch of a {@code oneOf} / {@code anyOf}. Inline schemas contribute nothing.
     */
    private static List<String> referencedSchemaNames( @Nullable Schema<?> schema ) {
        if ( schema == null ) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        if ( schema.get$ref() != null ) {
            names.add( schema.get$ref().substring( schema.get$ref().lastIndexOf( '/' ) + 1 ) );
        }
        List<Schema> branches = schema.getOneOf() != null ? schema.getOneOf() : schema.getAnyOf();
        if ( branches != null ) {
            for ( Schema<?> branch : branches ) {
                names.addAll( referencedSchemaNames( branch ) );
            }
        }
        return names;
    }

    /**
     * Operations whose 503 is a standing condition rather than a schedule, so they send no
     * {@code Retry-After}: retrying at any particular moment is no more likely to work.
     */
    private static final List<String> NO_RETRY_AFTER_503 =
            Arrays.asList( "getDbPool", "getHealth", "scrape" );

    /**
     * Every 503 the API can schedule a retry for must declare the header carrying that schedule.
     *
     * <p>Two resource classes told callers in prose to "lookup the `Retry-After` header" while no
     * response anywhere declared one, so a generated client could not see it and gemmapy had to
     * write the parsing by hand. Everything that throws {@code ServiceUnavailableException} passes a
     * retry-after, and {@code CostlyEndpointFilter} sets the header itself, so the only 503s
     * legitimately without one are the standing conditions in {@link #NO_RETRY_AFTER_503}.
     */
    @Test
    public void testRetryableServiceUnavailableResponsesDeclareRetryAfter() {
        List<String> offenders = new ArrayList<>();
        Set<String> exemptionsSeen = new TreeSet<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Operation operation : pathEntry.getValue().readOperations() ) {
                if ( operation.getResponses() == null ) {
                    continue;
                }
                ApiResponse unavailable = operation.getResponses().get( "503" );
                if ( unavailable == null ) {
                    continue;
                }
                inspected++;
                String operationId = operation.getOperationId();
                if ( NO_RETRY_AFTER_503.contains( operationId ) ) {
                    exemptionsSeen.add( operationId );
                } else if ( unavailable.getHeaders() == null
                        || !unavailable.getHeaders().containsKey( "Retry-After" ) ) {
                    offenders.add( pathEntry.getKey() + " (" + operationId + ")" );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to document many 503s; inspected only %d", inspected )
                .isGreaterThan( 20 );
        assertThat( exemptionsSeen )
                .withFailMessage( "an operation exempted from the Retry-After rule no longer documents a 503;"
                        + " drop it from NO_RETRY_AFTER_503 rather than leaving the exemption to rot" )
                .containsExactlyElementsOf( NO_RETRY_AFTER_503 );
        assertThat( offenders )
                .withFailMessage( "503 responses that schedule a retry but do not declare the Retry-After header: %s",
                        offenders )
                .isEmpty();
    }

    /**
     * A {@link Costly} route can be refused when its budget is full, so its 503 belongs in the spec.
     *
     * <p>Eight of the fourteen did not document one. The check runs off the annotation rather than a
     * hand-kept list so a route marked {@code @Costly} later cannot quietly skip it; the operation is
     * matched by operationId, which is the method name unless {@code @Operation} overrides it.
     */
    @Test
    public void testCostlyEndpointsDocumentTheirCapacity503() {
        Set<String> costlyMethods = costlyMethodNames();
        assertThat( costlyMethods )
                .withFailMessage( "the @Costly scan found almost nothing, so this test would pass vacuously" )
                .hasSizeGreaterThan( 10 );

        List<String> offenders = new ArrayList<>();
        Set<String> matched = new TreeSet<>();
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Operation operation : pathEntry.getValue().readOperations() ) {
                String operationId = operation.getOperationId();
                if ( operationId == null || !costlyMethods.contains( operationId ) ) {
                    continue;
                }
                matched.add( operationId );
                ApiResponse unavailable = operation.getResponses() != null
                        ? operation.getResponses().get( "503" ) : null;
                if ( unavailable == null ) {
                    offenders.add( pathEntry.getKey() + " (" + operationId + ") documents no 503" );
                } else if ( unavailable.getHeaders() == null
                        || !unavailable.getHeaders().containsKey( "Retry-After" ) ) {
                    offenders.add( pathEntry.getKey() + " (" + operationId + ") 503 declares no Retry-After" );
                }
            }
        }

        assertThat( matched )
                .withFailMessage( "@Costly methods with no operation in the spec — an @Operation(operationId=...)"
                        + " override would break the match this test relies on: %s",
                        new TreeSet<>( CollectionUtils.subtract( costlyMethods, matched ) ) )
                .containsExactlyInAnyOrderElementsOf( costlyMethods );
        assertThat( offenders )
                .withFailMessage( "@Costly routes can be refused 503 by the budget but do not say so: %s", offenders )
                .isEmpty();
    }

    /**
     * Names of the methods annotated {@link Costly}, found by the same component scan the WAR runs.
     */
    private static Set<String> costlyMethodNames() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider( false );
        scanner.addIncludeFilter( new AnnotationTypeFilter( jakarta.ws.rs.Path.class ) );
        Set<String> names = new TreeSet<>();
        for ( BeanDefinition definition : scanner.findCandidateComponents( "ubic.gemma.rest" ) ) {
            Class<?> resource;
            try {
                resource = Class.forName( Objects.requireNonNull( definition.getBeanClassName() ) );
            } catch ( ClassNotFoundException e ) {
                throw new RuntimeException( e );
            }
            for ( Method method : resource.getDeclaredMethods() ) {
                if ( method.isAnnotationPresent( Costly.class ) ) {
                    names.add( method.getName() );
                }
            }
        }
        return names;
    }

    /**
     * swagger-core disambiguates two resource methods of the same name by appending {@code _1}, and
     * which of the pair gets the suffix depends on scan order — so the generated client's method name
     * for one of them is not stable across builds. Name both explicitly with
     * {@code @Operation(operationId = ...)} instead.
     */
    @Test
    public void testOperationIdsAreNotAutoDisambiguated() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Operation operation : pathEntry.getValue().readOperations() ) {
                String operationId = operation.getOperationId();
                if ( operationId == null ) {
                    offenders.add( pathEntry.getKey() + " has an operation with no operationId" );
                    continue;
                }
                inspected++;
                if ( operationId.matches( ".*_\\d+$" ) ) {
                    offenders.add( pathEntry.getKey() + " -> " + operationId );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to expose many operationIds; inspected only %d", inspected )
                .isGreaterThan( 200 );
        assertThat( offenders )
                .withFailMessage( "operationIds swagger-core had to disambiguate, or that are missing: %s", offenders )
                .isEmpty();
    }

    /**
     * The bearer scheme {@code POST /login} mints has to be declared, or a generated client cannot
     * express it and every caller hand-rolls the Authorization header.
     */
    @Test
    public void testBearerAuthIsDeclared() {
        assertThat( spec.getComponents().getSecuritySchemes() )
                .containsKeys( "basicAuth", "cookieAuth", "bearerAuth" );
        assertThat( spec.getComponents().getSecuritySchemes().get( "bearerAuth" ) ).satisfies( scheme -> {
            assertThat( scheme.getType() ).isEqualTo( SecurityScheme.Type.HTTP );
            assertThat( scheme.getScheme() ).isEqualTo( "bearer" );
        } );
        assertThat( spec.getSecurity() )
                .withFailMessage( "bearerAuth is defined but not offered in the global security list, so the"
                        + " spec says no endpoint accepts it" )
                .anySatisfy( requirement -> assertThat( requirement ).containsKey( "bearerAuth" ) );
    }

    /**
     * A {@code content} map is keyed by media type, and a media type has no {@code q} parameter —
     * that belongs in an {@code Accept} header. A key carrying one matches nothing a client sends.
     *
     * <p>They arrive from the JAX-RS quality-of-source weight: a resource serving both JSON and TSV
     * writes {@code @Produces(TEXT_TAB_SEPARATED_VALUES_UTF8 + ";qs=0.9")} to keep JSON the default,
     * and swagger-core copies that into the spec as {@code ; q=0.9}. The weight has to stay in the
     * annotation, so {@code OpenApiFactory} strips it on the way out; this pins that it happened.
     */
    @Test
    public void testResponseMediaTypesCarryNoQualityParameter() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Operation operation : pathEntry.getValue().readOperations() ) {
                if ( operation.getResponses() == null ) {
                    continue;
                }
                for ( Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet() ) {
                    if ( responseEntry.getValue().getContent() == null ) {
                        continue;
                    }
                    for ( String mediaType : responseEntry.getValue().getContent().keySet() ) {
                        inspected++;
                        if ( !mediaType.equals( OpenApiFactory.stripQuality( mediaType ) ) ) {
                            offenders.add( pathEntry.getKey() + " -> " + responseEntry.getKey() + " " + mediaType );
                        }
                    }
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare many response media types; inspected only %d", inspected )
                .isGreaterThan( 200 );
        assertThat( offenders )
                .withFailMessage( "response media types carrying a q/qs parameter: %s", offenders )
                .isEmpty();
    }

    /**
     * A non-JSON response body has to say what it is. {@code @Content} with a {@code mediaType} and no
     * {@code schema} defaults to {@code type: object}, which for a TSV download says the body is a JSON
     * object — three of them did. The right declaration is {@code @Schema(type = "string")}.
     */
    @Test
    public void testNonJsonResponseBodiesAreTyped() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Operation operation : pathEntry.getValue().readOperations() ) {
                if ( operation.getResponses() == null ) {
                    continue;
                }
                for ( Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet() ) {
                    if ( responseEntry.getValue().getContent() == null ) {
                        continue;
                    }
                    for ( Map.Entry<String, io.swagger.v3.oas.models.media.MediaType> media
                            : responseEntry.getValue().getContent().entrySet() ) {
                        if ( media.getKey().startsWith( "application/json" ) ) {
                            continue;
                        }
                        inspected++;
                        Schema<?> schema = media.getValue().getSchema();
                        if ( schema != null && "object".equals( schema.getType() ) && schema.get$ref() == null
                                && schema.getProperties() == null ) {
                            offenders.add( pathEntry.getKey() + " -> " + responseEntry.getKey()
                                    + " " + media.getKey() );
                        }
                    }
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare several non-JSON bodies; inspected only %d", inspected )
                .isGreaterThan( 15 );
        assertThat( offenders )
                .withFailMessage( "non-JSON response bodies left as an untyped object — declare"
                        + " @Schema(type = \"string\") on the @Content: %s", offenders )
                .isEmpty();
    }

    /**
     * A successful JSON response must say what its body is.
     *
     * <p>{@code testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection} already requires a
     * content block, but a content block with a null schema satisfies it — which is what seven
     * {@code Response}-returning GETs had. Returning raw {@code jakarta.ws.rs.core.Response} tells
     * swagger-core nothing about the entity, so unless the method declares an {@code @ApiResponse}
     * with a schema the spec publishes the media type and stops there, and a generated client hands
     * back an untyped blob.
     *
     * <p>The write surface was exempted behind a shrink-only list when this was written; the list is
     * gone because it reached zero.
     */
    @Test
    public void testSuccessfulJsonResponsesDeclareASchema() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Operation operation = opEntry.getValue();
                if ( operation.getResponses() == null ) {
                    continue;
                }
                for ( Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet() ) {
                    if ( !responseEntry.getKey().startsWith( "2" ) || responseEntry.getValue().getContent() == null ) {
                        continue;
                    }
                    io.swagger.v3.oas.models.media.MediaType json =
                            responseEntry.getValue().getContent().get( "application/json" );
                    if ( json == null ) {
                        continue;
                    }
                    inspected++;
                    if ( json.getSchema() == null ) {
                        offenders.add( opEntry.getKey() + " " + pathEntry.getKey() + " -> " + responseEntry.getKey()
                                + " (" + operation.getOperationId() + ")" );
                    }
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare many successful JSON responses; inspected only %d", inspected )
                .isGreaterThan( 150 );
        assertThat( offenders )
                .withFailMessage( "successful JSON responses with no schema — a method returning raw Response"
                        + " needs an @ApiResponse that names the entity: %s", offenders )
                .isEmpty();
    }

    /**
     * {@code description} is REQUIRED on a Response Object in OpenAPI 3.0, so a document with one
     * missing is invalid. 184 responses had none and another 59 carried swagger-core's "default
     * response" placeholder, which is the same thing wearing a hat.
     *
     * <p>Two exclusions. {@code GET /genes/probes/refresh} publishes {@code *&#47;*} with no content
     * swagger-core can attach a description to — swagger-api/swagger-core#4693, which
     * {@code testEnsureThatAllEndpointHaveADefaultGetResponseOrIsARedirection} already skips for the
     * same reason. The {@code /custom} paths are Jersey test fixtures from
     * {@code UnknownQueryParameterFilterTest} and friends: they are on this module's test classpath,
     * so the scan picks them up here, and they are not in the deployed spec.
     */
    @Test
    public void testEveryResponseHasADescription() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            if ( pathEntry.getKey().startsWith( "/custom" ) ) {
                continue;
            }
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Operation operation = opEntry.getValue();
                if ( operation.getResponses() == null || "refreshGenesProbes".equals( operation.getOperationId() ) ) {
                    continue;
                }
                for ( Map.Entry<String, ApiResponse> responseEntry : operation.getResponses().entrySet() ) {
                    inspected++;
                    String description = responseEntry.getValue().getDescription();
                    if ( description == null || description.trim().isEmpty()
                            || "default response".equals( description ) ) {
                        offenders.add( opEntry.getKey() + " " + pathEntry.getKey() + " -> " + responseEntry.getKey()
                                + " (" + operation.getOperationId() + ")" );
                    }
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare many responses; inspected only %d", inspected )
                .isGreaterThan( 500 );
        assertThat( offenders )
                .withFailMessage( "responses with no description, or still on swagger-core's placeholder."
                        + " OpenAPI 3.0 requires one, so the document is invalid without it: %s", offenders )
                .isEmpty();
    }

    /**
     * Every operation must carry a tag, because a tag is how a generated client is organised.
     *
     * <p>204 of 286 operations had none, so swagger-codegen put them all in one {@code DefaultApi}
     * — a 170-method class with no structure, which is what gemmapy generates against today. Tags
     * split that into one class per resource.
     *
     * <p>The {@code /custom} paths are excluded for the same reason as in
     * {@link #testEveryResponseHasADescription}: they are Jersey fixtures on this module's test
     * classpath, not deployed routes.
     */
    @Test
    public void testEveryOperationIsTagged() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            if ( pathEntry.getKey().startsWith( "/custom" ) ) {
                continue;
            }
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Operation operation = opEntry.getValue();
                inspected++;
                if ( operation.getTags() == null || operation.getTags().isEmpty() ) {
                    offenders.add( opEntry.getKey() + " " + pathEntry.getKey()
                            + " (" + operation.getOperationId() + ")" );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare many operations; inspected only %d", inspected )
                .isGreaterThan( 250 );
        assertThat( offenders )
                .withFailMessage( "operations with no tag — they would all land in the generated client's"
                        + " DefaultApi. Put a class-level @Tag on the resource: %s", offenders )
                .isEmpty();
    }

    /**
     * No Hibernate entity may appear in the published specification.
     *
     * <p>{@code AdminPipelineWebService} used to return {@code PipelineJobBatch} directly. Its jobs
     * are a lazy {@code @OneToMany}, and each job holds a lazy {@code @ManyToOne} to
     * {@code ExpressionExperiment} — so following one field pulled in the experiment graph and 44
     * entity schemas with 466 properties arrived in the document through it and nothing else.
     * Nothing had marked them {@code @JsonIgnore}, and nothing initialized them either, so the same
     * field was a serialization hazard as well as specification noise.
     *
     * <p>The rule is the general one rather than a list of those 44: an entity reaching the wire is
     * a missing value object, whichever entity it is.
     */
    @Test
    public void testNoHibernateEntityIsPublishedAsASchema() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider( false );
        scanner.addIncludeFilter( new AnnotationTypeFilter( jakarta.persistence.Entity.class ) );
        Set<String> entities = new TreeSet<>();
        for ( BeanDefinition definition : scanner.findCandidateComponents( "ubic.gemma.model" ) ) {
            String name = Objects.requireNonNull( definition.getBeanClassName() );
            entities.add( name.substring( name.lastIndexOf( '.' ) + 1 ) );
        }

        assertThat( entities )
                .withFailMessage( "the @Entity scan found almost nothing, so this test would pass vacuously" )
                .hasSizeGreaterThan( 50 );

        Set<String> published = new TreeSet<>( entities );
        published.retainAll( spec.getComponents().getSchemas().keySet() );
        assertThat( published )
                .withFailMessage( "Hibernate entities published as schemas. An endpoint is returning an"
                        + " entity where it should return a value object — the entity's lazy associations"
                        + " become both specification bloat and a serialization hazard: %s", published )
                .isEmpty();
    }

    /**
     * Every parameter must be described. A parameter is what a caller actually sets, so an
     * undescribed one is the gap they hit first — 412 of 652 had nothing.
     *
     * <p>Only parameters the specification publishes are covered, which is the right scope: a
     * {@code @Parameter(hidden = true)} legacy alias never reaches a client and has nothing to
     * document. The {@code /custom} paths are test fixtures, excluded as elsewhere.
     */
    @Test
    public void testEveryParameterHasADescription() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            if ( pathEntry.getKey().startsWith( "/custom" ) ) {
                continue;
            }
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Operation operation = opEntry.getValue();
                if ( operation.getParameters() == null ) {
                    continue;
                }
                for ( Parameter parameter : operation.getParameters() ) {
                    inspected++;
                    if ( parameter.getDescription() == null || parameter.getDescription().trim().isEmpty() ) {
                        offenders.add( opEntry.getKey() + " " + pathEntry.getKey() + " ?" + parameter.getName()
                                + " (" + operation.getOperationId() + ")" );
                    }
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected the spec to declare many parameters; inspected only %d", inspected )
                .isGreaterThan( 500 );
        assertThat( offenders )
                .withFailMessage( "parameters with no description: %s", offenders )
                .isEmpty();
    }

    /**
     * Ceiling on undescribed schema properties. It may only go down.
     *
     * <p>3018 of 3177 properties had no description when this was measured. Removing the leaked
     * Hibernate entities took 466 of them off the board, and describing the response envelopes took
     * another 450 — 39 annotations on the shared containers, which every parameterised container
     * inherits. What is left is roughly 450 value objects in gemma-core, and hand-writing those is
     * its own piece of work rather than something to finish in passing.
     *
     * <p>A ratchet rather than a rule, because the rule would fail today. Lower the number when you
     * describe more; the test fails if it rises, so a new value object cannot arrive undocumented
     * and disappear into the pile.
     *
     * <p>Counts only properties that <em>can</em> be described. A property resolving to a bare
     * {@code $ref} cannot: OpenAPI 3.0 discards keywords beside a {@code $ref}, so its description
     * belongs on the schema it points at. Including them would move this number for reasons nobody
     * can act on — adding two legitimate, fully-described containers raises the bare-$ref count by
     * two, because each carries a {@code sort} that is one.
     */
    private static final int UNDESCRIBED_PROPERTY_BUDGET = 1721;

    /**
     * @see #UNDESCRIBED_PROPERTY_BUDGET
     */
    @Test
    public void testUndescribedSchemaPropertiesDoNotIncrease() {
        int undescribed = 0, total = 0, bareRef = 0;
        for ( Schema<?> schema : spec.getComponents().getSchemas().values() ) {
            if ( schema.getProperties() == null ) {
                continue;
            }
            for ( Object value : schema.getProperties().values() ) {
                Schema<?> property = ( Schema<?> ) value;
                total++;
                if ( property.getDescription() != null && !property.getDescription().trim().isEmpty() ) {
                    continue;
                }
                // a property that resolves to a bare $ref cannot carry a description: OpenAPI 3.0
                // discards keywords sitting beside a $ref. Its description belongs on the schema it
                // points at, which is where it would be read from anyway, so it is not counted here.
                if ( property.get$ref() != null ) {
                    bareRef++;
                } else {
                    undescribed++;
                }
            }
        }

        assertThat( total )
                .withFailMessage( "expected the spec to declare many properties; inspected only %d", total )
                .isGreaterThan( 2000 );
        assertThat( undescribed )
                .withFailMessage( "describable schema properties left undescribed rose to %d, over the budget of %d"
                                + " (a further %d are bare $refs, which cannot carry one)."
                                + " Describe the new properties, or lower the budget if you have described others.",
                        undescribed, UNDESCRIBED_PROPERTY_BUDGET, bareRef )
                .isLessThanOrEqualTo( UNDESCRIBED_PROPERTY_BUDGET );
    }

    /**
     * A published operation must declare every parameter the endpoint behind it accepts.
     *
     * <p>Two JAX-RS methods can serve one (verb, path) and differ only in {@code @Produces} —
     * {@code GET /datasets/{dataset}/design} does, one for JSON and one for TSV. swagger-core keeps a
     * single operation for the pair, and it takes the winner's <em>signature</em> as well as its
     * annotation. The JSON method wins there and accepts only {@code {dataset}}, so
     * {@code ?quantitationType=} and {@code ?useProcessedQuantitationType=} — read by the TSV branch
     * at runtime — were absent from the spec with nothing to indicate it.
     *
     * <p>The file guards the response half of that merge with a comment asking that the two
     * {@code @Operation} annotations stay identical. A comment is not enforcement, and it said
     * nothing about parameters. This is the enforcement, and it is written as the general rule:
     * whatever an endpoint reads, the operation says so, however many methods implement it.
     *
     * <p>Parameters marked {@code @Parameter(hidden = true)} are excluded — a deliberately
     * unpublished legacy alias is not a gap.
     */
    @Test
    public void testCollapsedRoutesDeclareEveryParameterTheyAccept() {
        Map<String, Set<String>> accepted = acceptedParametersByRoute();
        assertThat( accepted )
                .withFailMessage( "the resource scan found almost no routes, so this test would pass vacuously" )
                .hasSizeGreaterThan( 200 );

        List<String> offenders = new ArrayList<>();
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            if ( pathEntry.getKey().startsWith( "/custom" ) ) {
                continue;
            }
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                Set<String> expected = accepted.get( opEntry.getKey() + " " + pathEntry.getKey() );
                if ( expected == null ) {
                    continue;
                }
                Set<String> declared = new TreeSet<>();
                if ( opEntry.getValue().getParameters() != null ) {
                    for ( Parameter parameter : opEntry.getValue().getParameters() ) {
                        declared.add( parameter.getName() );
                    }
                }
                Set<String> missing = new TreeSet<>( expected );
                missing.removeAll( declared );
                if ( !missing.isEmpty() ) {
                    offenders.add( opEntry.getKey() + " " + pathEntry.getKey() + " accepts but does not declare "
                            + missing );
                }
            }
        }

        assertThat( offenders )
                .withFailMessage( "parameters an endpoint reads but its operation does not publish. Where two"
                        + " methods share a (verb, path), declare the loser's parameters on the winner's"
                        + " @Operation(parameters = ...): %s", offenders )
                .isEmpty();
    }

    /**
     * Every {@code VERB /path} the resource classes serve, mapped to the union of the JAX-RS parameter
     * names its methods accept. Keyed the way {@link PathItem.HttpMethod} prints, so it joins onto the
     * spec directly.
     */
    private static Map<String, Set<String>> acceptedParametersByRoute() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider( false );
        scanner.addIncludeFilter( new AnnotationTypeFilter( jakarta.ws.rs.Path.class ) );
        Map<String, Set<String>> byRoute = new TreeMap<>();
        for ( BeanDefinition definition : scanner.findCandidateComponents( "ubic.gemma.rest" ) ) {
            Class<?> resource;
            try {
                resource = Class.forName( Objects.requireNonNull( definition.getBeanClassName() ) );
            } catch ( ClassNotFoundException e ) {
                throw new RuntimeException( e );
            }
            jakarta.ws.rs.Path classPath = resource.getAnnotation( jakarta.ws.rs.Path.class );
            for ( Method method : resource.getDeclaredMethods() ) {
                String verb = httpMethodOf( method );
                if ( verb == null ) {
                    continue;
                }
                jakarta.ws.rs.Path methodPath = method.getAnnotation( jakarta.ws.rs.Path.class );
                String path = normalizePath( ( classPath != null ? classPath.value() : "" )
                        + "/" + ( methodPath != null ? methodPath.value() : "" ) );
                byRoute.computeIfAbsent( verb + " " + path, k -> new TreeSet<>() )
                        .addAll( parameterNamesOf( method ) );
            }
        }
        return byRoute;
    }

    /** The verb a resource method serves, read off whichever annotation is meta-annotated {@code @HttpMethod}. */
    @Nullable
    private static String httpMethodOf( Method method ) {
        for ( java.lang.annotation.Annotation annotation : method.getAnnotations() ) {
            jakarta.ws.rs.HttpMethod httpMethod =
                    annotation.annotationType().getAnnotation( jakarta.ws.rs.HttpMethod.class );
            if ( httpMethod != null ) {
                return httpMethod.value();
            }
        }
        return null;
    }

    /** Named JAX-RS parameters, skipping any the resource deliberately hides. */
    private static Set<String> parameterNamesOf( Method method ) {
        Set<String> names = new TreeSet<>();
        for ( java.lang.reflect.Parameter parameter : method.getParameters() ) {
            io.swagger.v3.oas.annotations.Parameter documented =
                    parameter.getAnnotation( io.swagger.v3.oas.annotations.Parameter.class );
            if ( documented != null && documented.hidden() ) {
                continue;
            }
            jakarta.ws.rs.QueryParam query = parameter.getAnnotation( jakarta.ws.rs.QueryParam.class );
            if ( query != null ) {
                names.add( query.value() );
            }
            jakarta.ws.rs.PathParam path = parameter.getAnnotation( jakarta.ws.rs.PathParam.class );
            if ( path != null ) {
                names.add( path.value() );
            }
        }
        return names;
    }

    /**
     * A JAX-RS path as the specification spells it: single slashes, no trailing slash, and the regex
     * stripped out of a template like <code>{type:json|yaml}</code>.
     */
    private static String normalizePath( String path ) {
        String collapsed = ( "/" + path ).replaceAll( "/+", "/" );
        if ( collapsed.length() > 1 && collapsed.endsWith( "/" ) ) {
            collapsed = collapsed.substring( 0, collapsed.length() - 1 );
        }
        return collapsed.replaceAll( "\\{\\s*(\\w+)\\s*:[^}]*}", "{$1}" );
    }

    /**
     * Wire names clients depend on. A name listed here may not disappear.
     *
     * <p>{@code AnnotationValueObject} renamed four fields — {@code className} to {@code category},
     * {@code classUri} to {@code categoryUri}, {@code termName} to {@code value}, {@code termUri} to
     * {@code valueUri} — with no deprecation, no alias and no changelog entry. Downstream found out
     * when every column came back null, because a wrapper reading the old names off the new payload
     * gets nulls rather than an error. Nothing in the build would have objected.
     *
     * <p>This is the objection. It pins only the value objects clients actually consume, and only
     * as a subset check: adding a field is backward compatible and passes, while removing or
     * renaming one fails and has to be a deliberate act with a changelog entry to match.
     */
    private static Map<String, String[]> pinnedWireNames() {
        Map<String, String[]> pinned = new TreeMap<>();
        pinned.put( "AnnotationValueObject", new String[] {
                "category", "categoryUri", "evidenceCode", "id", "object", "objectClass", "objectUri",
                "predicate", "predicateUri", "secondObject", "secondObjectUri", "secondPredicate",
                "secondPredicateUri", "supportingEvidence", "value", "valueUri" } );
        pinned.put( "ArrayDesignValueObject", new String[] {
                "color", "curationNote", "description", "expressionExperimentCount", "externalReferences",
                "geneCountsLastUpdated", "id", "isMerged", "isMergee", "lastNeedsAttentionEvent",
                "lastNoteUpdateEvent", "lastTroubledEvent", "lastUpdated", "mergedInto", "mergees", "name",
                "needsAttention", "numberOfExpressionExperiments", "numberOfGenes",
                "numberOfMappedElements", "numberOfSwitchedExpressionExperiments", "releaseUrl",
                "releaseVersion", "shortName", "taxon", "taxonID", "technologyType", "troubleDetails",
                "troubled" } );
        pinned.put( "BioAssayValueObject", new String[] {
                "accession", "arrayDesign", "description", "extractedMolecule", "id", "librarySelection",
                "libraryStrategy", "metadata", "name", "numberOfCells", "numberOfCellsByDesignElements",
                "numberOfDesignElements", "originalPlatform", "outlier", "predictedOutlier",
                "processingDate", "sample", "sequencePairedReads", "sequenceReadCount",
                "sequenceReadLength", "shortName", "sourceBioAssayId", "userFlaggedOutlier" } );
        pinned.put( "CharacteristicValueObject", new String[] {
                "category", "categoryUri", "id", "originalValue", "supportingEvidence", "value", "valueId",
                "valueUri" } );
        pinned.put( "CompositeSequenceValueObject", new String[] {
                "arrayDesign", "description", "geneMappingSummaries", "genes", "id", "name", "sequence",
                "sequenceLength" } );
        pinned.put( "DifferentialExpressionAnalysisResultSetValueObject", new String[] {
                "analysis", "baselineGroup", "experimentalFactors", "id", "results", "secondBaselineGroup",
                "taxa" } );
        pinned.put( "ExperimentalFactorValueObject", new String[] {
                "baselineRelevance", "baselineRelevanceReason", "category", "categoryUri", "description",
                "factorValues", "id", "name", "type", "values" } );
        pinned.put( "ExpressionExperimentValueObject", new String[] {
                "accession", "batchConfound", "batchEffect", "batchEffectStatistics", "bioAssayCount",
                "characteristics", "curationNote", "dateCreated", "description", "doi", "externalDatabase",
                "externalDatabaseUri", "externalLabel", "externalUri", "extractedMolecules", "geeq", "id",
                "isPublic", "isSingleCell", "lastNeedsAttentionEvent", "lastNoteUpdateEvent",
                "lastTroubledEvent", "lastUpdated", "librarySelections", "libraryStrategies", "metadata",
                "name", "needsAttention", "numberOfArrayDesigns", "numberOfBioAssays", "numberOfCellIds",
                "numberOfCells", "numberOfProcessedExpressionVectors", "originalPlatforms", "otherParts",
                "platforms", "pubmedId", "shortName", "source", "taxon", "taxonId", "technologyType",
                "troubleDetails", "troubled" } );
        pinned.put( "GeneValueObject", new String[] {
                "accessions", "aliases", "associatedExperimentCount", "compositeSequenceCount",
                "ensemblId", "geneSets", "homologues", "id", "matchType", "multifunctionalityRank",
                "ncbiId", "ncbiUri", "numGoTerms", "officialName", "officialSymbol", "platformCount",
                "taxon", "taxonId" } );
        pinned.put( "GeeqValueObject", new String[] {
                "batchCorrected", "corrMatIssues", "id", "lastComputed", "noVectors", "publicQualityScore",
                "qScoreBatchInfo", "qScoreOutliers", "qScorePlatformsTech", "qScorePublicBatchConfound",
                "qScorePublicBatchEffect", "qScoreReplicates", "qScoreSampleCorrelationVariance",
                "qScoreSampleMeanCorrelation", "qScoreSampleMedianCorrelation", "replicatesIssues" } );
        pinned.put( "QuantitationTypeValueObject", new String[] {
                "description", "generalType", "id", "isBackground", "isBackgroundSubtracted",
                "isBatchCorrected", "isMaskedPreferred", "isNormalized", "isPreferred", "isRatio",
                "isRecomputedFromRawData", "isSingleCellPreferred", "name", "representation", "scale",
                "type", "vectorType" } );
        pinned.put( "TaxonValueObject", new String[] {
                "commonName", "externalDatabase", "id", "ncbiId", "scientificName" } );
        return pinned;
    }

    /**
     * @see #pinnedWireNames()
     */
    @Test
    public void testPublicWireNamesAreNotRenamedOrRemoved() {
        Map<String, Schema> schemas = spec.getComponents().getSchemas();
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, String[]> entry : pinnedWireNames().entrySet() ) {
            Schema<?> schema = schemas.get( entry.getKey() );
            if ( schema == null ) {
                offenders.add( entry.getKey() + " is no longer published at all" );
                continue;
            }
            Set<String> present = schema.getProperties() != null
                    ? schema.getProperties().keySet() : Collections.emptySet();
            for ( String name : entry.getValue() ) {
                inspected++;
                if ( !present.contains( name ) ) {
                    offenders.add( entry.getKey() + "." + name );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected to pin many wire names; inspected only %d", inspected )
                .isGreaterThan( 150 );
        assertThat( offenders )
                .withFailMessage( "wire names that clients depend on have gone. If the rename is intended,"
                        + " update this list AND add a CHANGELOG.md entry — that is the whole point of the"
                        + " check, since the last rename shipped without one: %s", offenders )
                .isEmpty();
    }

    /**
     * Everything under {@code /admin} and {@code /internal} must be reachable as internal surface.
     *
     * <p>gemmapy prunes Gemma's specification before generating, and its filter is "drop the
     * non-GET operations" — which keeps every admin <em>read</em>, so its published SDK carries
     * admin_api, admin_pipeline_api and observability_api. Dropping writes is not the same as
     * dropping admin surface, and there was no marker to drop the right thing by.
     *
     * <p>There is one now: the Admin, Admin/Pipeline and Internal/Pipeline tags carry
     * {@code x-internal: true}, so a consumer collects the internal tag names from the document's
     * own {@code tags} list and drops the operations carrying them — no hardcoded tag names, no
     * path prefixes. This pins that every operation under those prefixes is actually covered, so a
     * new admin endpoint cannot slip out of the marked set.
     *
     * <p>swagger-codegen's own {@code -Dapis=<Tag>} is not a substitute. It selects API classes but
     * generates no models unless {@code -Dmodels} is also passed, and passing both selected no APIs
     * at all when tried — and it cannot drop the schemas that only the excluded operations reach.
     */
    @Test
    public void testAdminAndInternalSurfaceIsMarkedInternal() {
        Set<String> internalTags = new TreeSet<>();
        if ( spec.getTags() != null ) {
            for ( io.swagger.v3.oas.models.tags.Tag tag : spec.getTags() ) {
                // parseValue = true makes swagger store this as a Jackson BooleanNode rather than a
                // java.lang.Boolean, so Boolean.TRUE.equals() misses it. The emitted YAML is a real
                // boolean either way, which is what a consumer reads.
                Object internal = tag.getExtensions() != null ? tag.getExtensions().get( "x-internal" ) : null;
                if ( internal != null && "true".equals( String.valueOf( internal ) ) ) {
                    internalTags.add( tag.getName() );
                }
            }
        }
        assertThat( internalTags )
                .withFailMessage( "no tag carries x-internal, so a consumer has nothing to prune by" )
                .isNotEmpty();

        List<String> unmarked = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, PathItem> pathEntry : spec.getPaths().entrySet() ) {
            if ( !pathEntry.getKey().startsWith( "/admin" ) && !pathEntry.getKey().startsWith( "/internal" ) ) {
                continue;
            }
            for ( Map.Entry<PathItem.HttpMethod, Operation> opEntry : pathEntry.getValue().readOperationsMap().entrySet() ) {
                inspected++;
                List<String> tags = opEntry.getValue().getTags();
                if ( tags == null || Collections.disjoint( tags, internalTags ) ) {
                    unmarked.add( opEntry.getKey() + " " + pathEntry.getKey()
                            + " (" + opEntry.getValue().getOperationId() + ")" );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected many admin/internal operations; inspected only %d", inspected )
                .isGreaterThan( 30 );
        assertThat( unmarked )
                .withFailMessage( "admin or internal operations a downstream prune would keep, because no"
                        + " x-internal tag covers them: %s", unmarked )
                .isEmpty();
    }

    /**
     * A property cannot be both required and nullable without saying nothing.
     *
     * <p>{@code required} means the key is always present; {@code nullable} means its value may be
     * null. Both together is legal OpenAPI and occasionally meaningful, but in this specification it
     * is almost always a mistake — a field marked required because it "is always set" while also
     * carrying {@code @Nullable} means one of the two annotations is wrong about the wire.
     *
     * <p>The required declarations here are the ones that can be proved rather than judged: a Java
     * primitive cannot be null and Jackson always serializes it, so the key is always present. That
     * rule is deliberately not applied wholesale — {@code required} is direction-agnostic in OpenAPI
     * 3.0, and {@code ExperimentalDesignValueObject} and everything under it is accepted as a request
     * body by {@code PUT /datasets/{dataset}/design}, where marking a field required would start
     * demanding it from clients.
     */
    @Test
    public void testNothingIsBothRequiredAndNullable() {
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        for ( Map.Entry<String, Schema> entry : spec.getComponents().getSchemas().entrySet() ) {
            Schema<?> schema = entry.getValue();
            if ( schema.getRequired() == null || schema.getProperties() == null ) {
                continue;
            }
            for ( String name : schema.getRequired() ) {
                inspected++;
                Schema<?> property = ( Schema<?> ) schema.getProperties().get( name );
                if ( property != null && Boolean.TRUE.equals( property.getNullable() ) ) {
                    offenders.add( entry.getKey() + "." + name );
                }
            }
        }

        assertThat( inspected )
                .withFailMessage( "expected many required properties; inspected only %d", inspected )
                .isGreaterThan( 100 );
        assertThat( offenders )
                .withFailMessage( "properties declared both required and nullable — one of the two is wrong"
                        + " about the wire: %s", offenders )
                .isEmpty();
    }
}
