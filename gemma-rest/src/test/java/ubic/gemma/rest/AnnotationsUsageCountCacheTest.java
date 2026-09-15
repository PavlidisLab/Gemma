package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.BaseJerseyTest5;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.args.TaxonArgService;

import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.Mockito.*;
import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * The per-URI corpus usage-count cache behind {@code /annotations/search?rank=composite}.
 * <p>
 * These live in their own class because the sibling {@link AnnotationsWebServiceTest} context
 * deliberately wires no {@link CacheManager}, which disables every cache in the endpoint. That is
 * the right default there — it keeps those tests measuring the endpoint rather than a cache — but
 * it also means the caching path ships untested unless something opts in, which is what this does.
 * <p>
 * What is worth pinning is not that a cache exists but that it is keyed per URI. A typeahead sends
 * a new query on nearly every keystroke and each one proposes a candidate set overlapping the last;
 * keyed per result set, every keystroke would miss and the cache would buy nothing.
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AnnotationsUsageCountCacheTest extends BaseJerseyTest5 {

    private static final String CACHE_NAME = "AnnotationsUsageCountCache";

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class ContextConfig {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080",
                    "annotation.category.prefixes=",
                    "annotation.category.excludedPrefixes=" );
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager( CACHE_NAME );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        /**
         * Required by {@code AnnotationsWebService} since the relation endpoints landed; without it the
         * context fails to build and these cache tests error before asserting anything.
         */
        @Bean
        public ubic.gemma.persistence.service.common.description.AnnotationRelationService annotationRelationService() {
            return mock( ubic.gemma.persistence.service.common.description.AnnotationRelationService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock( CharacteristicService.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock( ExpressionExperimentService.class );
        }

        @Bean
        public TaxonService taxonService() {
            return mock( TaxonService.class );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public DatasetArgService datasetArgService( ExpressionExperimentService service, SearchService searchService ) {
            return new DatasetArgService( service, searchService, mock( ArrayDesignService.class ),
                    mock( BioAssayService.class ), mock( ubic.gemma.core.analysis.preprocess.OutlierDetectionService.class ),
                    mock( ubic.gemma.persistence.service.common.description.PublicationAssociationService.class ) );
        }

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService, GeneService geneService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), geneService );
        }

        @Bean
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                DatasetArgService datasetArgService, TaxonArgService taxonArgService, GeneService geneService ) {
            Map<String, ubic.gemma.rest.ranking.AnnotationSearchRankingStrategy> strategies = new HashMap<>();
            strategies.put( "lucene", new ubic.gemma.rest.ranking.LuceneOrderRankingStrategy() );
            strategies.put( "composite", new ubic.gemma.rest.ranking.CompositeRankingStrategy( 0.5, 0.3, 0.2 ) );
            return new AnnotationsWebService( ontologyService, searchService, characteristicService,
                    expressionExperimentService, datasetArgService, taxonArgService, geneService, strategies );
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
        public Future<OpenAPI> openApi() {
            return constantFuture( mock() );
        }

        @Bean
        public BuildInfo buildInfo() {
            return mock();
        }
    }

    private static final String DIABETES = "http://example.com/diabetes";
    private static final String OBESITY = "http://example.com/obesity";
    private static final String UNUSED = "http://example.com/unused";

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    public void clearCache() {
        reset( ontologyService, characteristicService );
        Objects.requireNonNull( cacheManager.getCache( CACHE_NAME ) ).clear();
    }

    /** Candidates the ontology proposes for a query, in the shape the endpoint consumes. */
    private void ontologyProposes( String query, String... uris ) throws SearchException, TimeoutException {
        List<CharacteristicValueObject> hits = new ArrayList<>();
        for ( String uri : uris ) {
            hits.add( new CharacteristicValueObject( uri.substring( uri.lastIndexOf( '/' ) + 1 ), uri, "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( query ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( hits );
    }

    private Response search( String query ) {
        return target( "/annotations/search" )
                .queryParam( "query", query )
                .queryParam( "rank", "composite" )
                .request().get();
    }

    /**
     * The point of the cache: a keystroke that re-proposes a URI already counted must not send that
     * URI back to the database. Only the newly-proposed one does.
     */
    @Test
    @WithMockUser(username = "curator")
    public void testOnlyTheNewlyProposedUriReachesTheDatabase() throws SearchException, TimeoutException {
        ontologyProposes( "diabet", DIABETES );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( DIABETES, 7L ) );

        assertThat( search( "diabet" ) ).hasStatus( Response.Status.OK );
        verify( characteristicService ).countExperimentsByUris( eq( Collections.singleton( DIABETES ) ),
                anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );

        // second keystroke: the candidate set grows, it does not start over
        ontologyProposes( "diabete", DIABETES, OBESITY );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( OBESITY, 3L ) );

        assertThat( search( "diabete" ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                // the cached count is still reported, not lost because it was not re-queried
                .anySatisfy( a -> assertThat( a )
                        .containsEntry( "valueUri", DIABETES )
                        .containsEntry( "usageCount", 7 ) )
                .anySatisfy( a -> assertThat( a )
                        .containsEntry( "valueUri", OBESITY )
                        .containsEntry( "usageCount", 3 ) );

        // the already-counted URI was NOT asked for again
        verify( characteristicService ).countExperimentsByUris( eq( Collections.singleton( OBESITY ) ),
                anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
        verify( characteristicService, never() ).countExperimentsByUris(
                argThat( ( Set<String> s ) -> s.size() > 1 ), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
    }

    /**
     * A URI nothing uses is the common case in a broad candidate set, and it is the one a naive
     * cache drops: with only non-zero counts stored, every uninformative candidate goes back to the
     * database on the next keystroke, which is most of them.
     */
    @Test
    @WithMockUser(username = "curator")
    public void testAUriNothingUsesIsCachedToo() throws SearchException, TimeoutException {
        ontologyProposes( "unused", UNUSED );
        // the aggregate omits a URI nothing references rather than returning zero for it
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( search( "unused" ) ).hasStatus( Response.Status.OK );
        verify( characteristicService, times( 1 ) ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );

        assertThat( search( "unused" ) )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "usageCount", 0 ) );

        // still exactly one database call: the zero was remembered
        verify( characteristicService, times( 1 ) ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
    }

    /**
     * The tally is ACL-restricted, so an entry computed for one reader must not be reachable by
     * another. Asserting on the key rather than on a second request's result because the alternative
     * — driving the same endpoint under two security contexts and watching for a wrong number — only
     * fails if the counts happen to differ, and would pass silently the day they match.
     */
    @Test
    @WithMockUser(username = "curator")
    public void testCacheEntriesAreScopedToTheReader() throws SearchException, TimeoutException {
        ontologyProposes( "diabet", DIABETES );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( DIABETES, 7L ) );

        assertThat( search( "diabet" ) ).hasStatus( Response.Status.OK );

        ConcurrentMapCache cache = ( ConcurrentMapCache ) cacheManager.getCache( CACHE_NAME );
        Set<String> keys = new HashSet<>();
        for ( Object k : Objects.requireNonNull( cache ).getNativeCache().keySet() ) {
            keys.add( String.valueOf( k ) );
        }
        org.assertj.core.api.Assertions.assertThat( keys ).hasSize( 1 );
        // Deliberately not asserting the separator: what matters is that the reader's identity is
        // in the key and the URI is in the key, not which byte joins them.
        org.assertj.core.api.Assertions.assertThat( keys.iterator().next() )
                .as( "the reader's identity is part of the key, so anonymous cannot read this entry" )
                .startsWith( "u:curator" )
                .endsWith( DIABETES );
    }
}
