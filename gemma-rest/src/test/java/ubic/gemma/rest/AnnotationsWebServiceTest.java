package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.ontology.basecode.model.OntologyProperty;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.ChromosomeService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.rest.analytics.AnalyticsProvider;
import ubic.gemma.rest.util.BaseJerseyTest5;
import ubic.gemma.rest.util.JacksonConfig;
import ubic.gemma.rest.util.QueriedAndFilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.SortValueObject;
import ubic.gemma.rest.util.args.*;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.apache.commons.lang3.concurrent.ConcurrentUtils.constantFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.mockito.Mockito.*;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * @author poirigui
 */
@ContextConfiguration
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class AnnotationsWebServiceTest extends BaseJerseyTest5 {

    @Configuration
    @TestComponent
    @Import(JacksonConfig.class)
    public static class AnnotationsWebServiceContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer placeholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080" );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
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
        public DatasetArgService datasetRestService( ExpressionExperimentService service, SearchService searchService ) {
            return new DatasetArgService( service, searchService, mock( ArrayDesignService.class ), mock( BioAssayService.class ), mock( OutlierDetectionService.class ) );
        }

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), mock( GeneService.class ) );
        }

        @Bean
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                DatasetArgService datasetRestService, TaxonArgService taxonArgService ) {
            return new AnnotationsWebService( ontologyService, searchService, characteristicService, expressionExperimentService, datasetRestService, taxonArgService );
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

    @Autowired
    private AnnotationsWebService annotationsWebService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @BeforeEach
    public void setUpMocks() throws Exception {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setId( 1L );
        when( taxonService.findByCommonName( "human" ) ).thenReturn( taxon );
        // The /annotations/search endpoint memoises results in a static SEARCH_CACHE keyed by
        // normalised query. Multiple tests in this class issue ?query=diabetes with different
        // mocked usage-count payloads; without clearing the cache, the first run's result
        // poisons all subsequent ones. Drop the static map between tests.
        java.lang.reflect.Field f = AnnotationsWebService.class.getDeclaredField( "SEARCH_CACHE" );
        f.setAccessible( true );
        ( ( Map<?, ?> ) f.get( null ) ).clear();
    }

    @AfterEach
    public void resetMocks() {
        reset( searchService, taxonService, ontologyService, expressionExperimentService, characteristicService );
    }

    @Test
    @WithMockUser
    public void testSearchTaxonDatasets() throws SearchException, TimeoutException {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        SearchService.SearchResultMap mockedSrMap = mock( SearchService.SearchResultMap.class );
        when( mockedSrMap.getByResultObjectType( ExpressionExperiment.class ) )
                .thenReturn( Collections.singletonList( SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test object" ) ) );
        when( searchService.search( any( SearchSettings.class ) ) )
                .thenReturn( mockedSrMap );
        when( taxonService.getFilter( eq( "commonName" ), eq( String.class ), eq( Filter.Operator.eq ), any( String.class ) ) )
                .thenAnswer( a -> Filter.by( "t", "commonName", String.class, Filter.Operator.eq, a.getArgument( 3, String.class ), a.getArgument( 0 ) ) );
        when( taxonService.getFilter( eq( "scientificName" ), eq( String.class ), eq( Filter.Operator.eq ), any( String.class ) ) )
                .thenAnswer( a -> Filter.by( "t", "scientificName", String.class, Filter.Operator.eq, a.getArgument( 3, String.class ), a.getArgument( 0 ) ) );
        when( expressionExperimentService.getFilter( "id", Filter.Operator.eq, "1" ) ).thenReturn( Filter.by( "ee", "id", Long.class, Filter.Operator.in, Collections.singleton( 1L ), "id" ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) );
        when( expressionExperimentService.loadValueObjects( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) ), eq( 0 ), eq( 20 ) ) )
                .thenAnswer( a -> new Slice<>( Collections.singletonList( new ExpressionExperimentValueObject( ee ) ), a.getArgument( 1 ), a.getArgument( 2, Integer.class ), a.getArgument( 3, Integer.class ), 10000L ) );
        when( expressionExperimentService.getEnhancedFilters( any(), any(), any(), anyLong(), any() ) ).thenAnswer( a -> a.getArgument( 0 ) );
        QueriedAndFilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> payload = annotationsWebService.searchTaxonDatasets(
                TaxonArg.valueOf( "human" ),
                StringArrayArg.valueOf( "bipolar" ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        assertThat( payload )
                .hasFieldOrPropertyWithValue( "query", "bipolar" )
                .hasFieldOrPropertyWithValue( "filter", "commonName = human or scientificName = human" )
                .hasFieldOrPropertyWithValue( "sort", new SortValueObject( Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) ) )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 )
                .hasFieldOrPropertyWithValue( "totalElements", 10000L );
        verify( searchService ).search( any( SearchSettings.class ) );
        verify( taxonService ).getFilter( "commonName", String.class, Filter.Operator.eq, "human" );
        verify( taxonService ).getFilter( "scientificName", String.class, Filter.Operator.eq, "human" );
        verify( expressionExperimentService ).getFilter( "id", Filter.Operator.eq, "1" );
        verify( expressionExperimentService ).getSort( "id", Sort.Direction.ASC, Sort.NullMode.LAST );
        verify( expressionExperimentService ).loadValueObjects( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, Sort.NullMode.LAST, "id" ) ), eq( 0 ), eq( 20 ) );
    }

    @Test
    public void testParents() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( term );
        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( ontologyService ).getTerm( "http://example.com/test", 30000, TimeUnit.MILLISECONDS );
        verify( ontologyService ).getParents( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testParentsWhenTermIsNotFound() throws TimeoutException {
        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( ontologyService ).getTerm( "http://example.com/test", 30000, TimeUnit.MILLISECONDS );
        verifyNoMoreInteractions( ontologyService );
    }

    @Test
    public void testParentsWhenInferenceTimeout() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( any(), anyBoolean(), anyBoolean(), anyLong(), any() ) ).thenThrow( new TimeoutException( "Ontology inference timed out!" ) );
        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.SERVICE_UNAVAILABLE )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "error.code", 503 )
                .hasFieldOrPropertyWithValue( "error.message", "HTTP 503 Service Unavailable" );
        verify( ontologyService ).getTerm( "http://example.com/test", 30000, TimeUnit.MILLISECONDS );
        verify( ontologyService ).getParents( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    @Test
    public void testChildren() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( term );
        assertThat( target( "/annotations/children" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK );
        verify( ontologyService ).getTerm( "http://example.com/test", 30000, TimeUnit.MILLISECONDS );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
    }

    // ---------------------------------------------------------------------
    // Dataset annotation write endpoint tests (POST / DELETE / PUT)
    // ---------------------------------------------------------------------

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotation() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        ee.setCharacteristics( new LinkedHashSet<>() );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> {
                    Characteristic vc = a.getArgument( 1, Characteristic.class );
                    vc.setId( 42L );
                    return vc;
                } );
        String body = "{\"category\":\"organism part\",\"categoryUri\":\"http://purl.obolibrary.org/obo/UBERON_0000479\","
                + "\"value\":\"liver\",\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\","
                + "\"evidenceCode\":\"IEA\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), any( Characteristic.class ) );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationDuplicateReturns409() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        ee.setCharacteristics( new LinkedHashSet<>() );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenThrow( new IllegalArgumentException( "duplicate" ) );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\","
                + "\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CONFLICT );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationRejectsBlankCategory() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"category\":\"\",\"value\":\"liver\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).addAnnotation( any(), any() );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationRejectsBadEvidenceCode() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\",\"evidenceCode\":\"BOGUS\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( expressionExperimentService, never() ).addAnnotation( any(), any() );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testRemoveDatasetAnnotation() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        Characteristic c = Characteristic.Factory.newInstance();
        c.setId( 42L );
        c.setCategory( "organism part" );
        c.setValue( "liver" );
        when( expressionExperimentService.removeAnnotation( ee, 42L ) ).thenReturn( c );
        assertThat( target( "/annotations/datasets/1/annotations/42" ).request().delete() )
                .hasStatus( Response.Status.NO_CONTENT );
        verify( expressionExperimentService ).removeAnnotation( ee, 42L );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testRemoveDatasetAnnotationNotFound() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.removeAnnotation( ee, 999L ) ).thenReturn( null );
        assertThat( target( "/annotations/datasets/1/annotations/999" ).request().delete() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testReplaceDatasetAnnotationsIdempotentNoOp() {
        // EE already carries the desired tag; bulk PUT with the same set produces an empty diff
        // and emits no audit events. The handler returns 200 OK with empty added/removed lists.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        Characteristic existing = Characteristic.Factory.newInstance();
        existing.setId( 7L );
        existing.setCategory( "organism part" );
        existing.setCategoryUri( "http://purl.obolibrary.org/obo/UBERON_0000479" );
        existing.setValue( "liver" );
        existing.setValueUri( "http://purl.obolibrary.org/obo/UBERON_0002107" );
        LinkedHashSet<Characteristic> chars = new LinkedHashSet<>();
        chars.add( existing );
        ee.setCharacteristics( chars );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        String body = "{\"annotations\":[{\"category\":\"organism part\",\"categoryUri\":\"http://purl.obolibrary.org/obo/UBERON_0000479\","
                + "\"value\":\"liver\",\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}]}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
        verify( expressionExperimentService, never() ).addAnnotation( any(), any() );
        verify( expressionExperimentService, never() ).removeAnnotation( any(), any() );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testReplaceDatasetAnnotationsAppliesDiff() {
        // EE has one tag; desired set has a different tag. Bulk PUT should call removeAnnotation
        // for the existing tag and addAnnotation for the new tag — one event per mutation.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        Characteristic existing = Characteristic.Factory.newInstance();
        existing.setId( 7L );
        existing.setCategory( "organism part" );
        existing.setValue( "liver" );
        existing.setValueUri( "http://purl.obolibrary.org/obo/UBERON_0002107" );
        LinkedHashSet<Characteristic> chars = new LinkedHashSet<>();
        chars.add( existing );
        ee.setCharacteristics( chars );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.getAnnotations( ee ) ).thenReturn( Collections.emptySet() );
        when( expressionExperimentService.removeAnnotation( ee, 7L ) ).thenReturn( existing );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> {
                    Characteristic vc = a.getArgument( 1, Characteristic.class );
                    vc.setId( 88L );
                    return vc;
                } );
        String body = "{\"annotations\":[{\"category\":\"organism part\","
                + "\"value\":\"brain\",\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0000955\"}]}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        verify( expressionExperimentService ).removeAnnotation( ee, 7L );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), any( Characteristic.class ) );
    }

    // ---------------------------------------------------------------------
    // Usage-count / categories / predicates / term endpoint tests
    // ---------------------------------------------------------------------

    @Test
    public void testParentsPopulatesUsageCount() throws TimeoutException {
        OntologyTerm queried = mock( OntologyTerm.class );
        OntologyTerm parentA = mock( OntologyTerm.class );
        OntologyTerm parentB = mock( OntologyTerm.class );
        when( parentA.getUri() ).thenReturn( "http://example.com/parentA" );
        when( parentA.getLabel() ).thenReturn( "parent A" );
        when( parentB.getUri() ).thenReturn( "http://example.com/parentB" );
        when( parentB.getLabel() ).thenReturn( "parent B" );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( queried );
        // Use a LinkedHashSet to preserve iteration order for the assertion below.
        java.util.LinkedHashSet<OntologyTerm> parents = new java.util.LinkedHashSet<>();
        parents.add( parentA );
        parents.add( parentB );
        when( ontologyService.getParents( eq( Collections.singleton( queried ) ), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( parents );

        ExpressionExperiment ee1 = ExpressionExperiment.Factory.newInstance();
        ee1.setId( 1L );
        ExpressionExperiment ee2 = ExpressionExperiment.Factory.newInstance();
        ee2.setId( 2L );
        Map<String, Set<ExpressionExperiment>> perUri = new HashMap<>();
        perUri.put( "http://example.com/parentA", new HashSet<>( Arrays.asList( ee1, ee2 ) ) );
        perUri.put( "http://example.com/parentB", Collections.singleton( ee1 ) );
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = new HashMap<>();
        hits.put( ExpressionExperiment.class, perUri );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( hits );

        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfiesExactlyInAnyOrder(
                        a -> assertThat( a )
                                .containsEntry( "valueUri", "http://example.com/parentA" )
                                .containsEntry( "usageCount", 2 ),
                        a -> assertThat( a )
                                .containsEntry( "valueUri", "http://example.com/parentB" )
                                .containsEntry( "usageCount", 1 ) );

        verify( characteristicService ).findExperimentsByUris(
                argThat( ( Set<String> s ) -> s.containsAll( Arrays.asList( "http://example.com/parentA", "http://example.com/parentB" ) ) ),
                eq( true ), eq( true ), eq( true ), isNull(), eq( -1 ), eq( false ), eq( false ) );
    }

    @Test
    public void testChildrenPopulatesUsageCount() throws TimeoutException {
        OntologyTerm queried = mock( OntologyTerm.class );
        OntologyTerm child = mock( OntologyTerm.class );
        when( child.getUri() ).thenReturn( "http://example.com/child" );
        when( child.getLabel() ).thenReturn( "child" );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( queried );
        when( ontologyService.getChildren( eq( Collections.singleton( queried ) ), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singleton( child ) );

        ExpressionExperiment ee1 = ExpressionExperiment.Factory.newInstance();
        ee1.setId( 1L );
        // Two entries with same EE id across different Identifiable classes should still count once.
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = new HashMap<>();
        hits.put( ExpressionExperiment.class, Collections.singletonMap( "http://example.com/child", Collections.singleton( ee1 ) ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( hits );

        assertThat( target( "/annotations/children" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "valueUri", "http://example.com/child" )
                        .containsEntry( "usageCount", 1 ) );
    }

    @Test
    public void testParentsReportsZeroWhenNoExperimentsMatch() throws TimeoutException {
        OntologyTerm queried = mock( OntologyTerm.class );
        OntologyTerm parent = mock( OntologyTerm.class );
        when( parent.getUri() ).thenReturn( "http://example.com/parent" );
        when( parent.getLabel() ).thenReturn( "parent" );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( queried );
        when( ontologyService.getParents( eq( Collections.singleton( queried ) ), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singleton( parent ) );
        // findExperimentsByUris returns empty per-class map → usageCount falls back to 0
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "usageCount", 0 ) );
    }

    @Test
    public void testParentsSkipsCountLookupWhenAllTermsLackUris() throws TimeoutException {
        OntologyTerm queried = mock( OntologyTerm.class );
        OntologyTerm uriless = mock( OntologyTerm.class );
        when( uriless.getUri() ).thenReturn( null );
        when( uriless.getLabel() ).thenReturn( "uri-less" );
        when( ontologyService.getTerm( eq( "http://example.com/test" ), anyLong(), any() ) ).thenReturn( queried );
        when( ontologyService.getParents( eq( Collections.singleton( queried ) ), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singleton( uriless ) );

        assertThat( target( "/annotations/parents" ).queryParam( "uri", "http://example.com/test" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "usageCount", null ) );

        // No URIs to count → no DB call.
        verify( characteristicService, never() ).findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() );
    }

    @Test
    public void testSearchAnnotationsPopulatesUsageCount() throws SearchException, TimeoutException {
        CharacteristicValueObject hit = new CharacteristicValueObject( "diabetes", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );

        ExpressionExperiment ee1 = ExpressionExperiment.Factory.newInstance();
        ee1.setId( 1L );
        ExpressionExperiment ee2 = ExpressionExperiment.Factory.newInstance();
        ee2.setId( 2L );
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = new HashMap<>();
        hits.put( ExpressionExperiment.class, Collections.singletonMap( "http://example.com/diabetes", new HashSet<>( Arrays.asList( ee1, ee2 ) ) ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( hits );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "value", "diabetes" )
                        .containsEntry( "valueUri", "http://example.com/diabetes" )
                        .containsEntry( "usageCount", 2 ) );
    }

    @Test
    public void testGetAnnotationCategories() {
        OntologyTerm cellType = mock( OntologyTerm.class );
        when( cellType.getUri() ).thenReturn( "http://example.com/cellType" );
        when( cellType.getLabel() ).thenReturn( "cell type" );
        OntologyTerm disease = mock( OntologyTerm.class );
        when( disease.getUri() ).thenReturn( "http://example.com/disease" );
        when( disease.getLabel() ).thenReturn( "disease" );
        java.util.LinkedHashSet<OntologyTerm> categories = new java.util.LinkedHashSet<>();
        categories.add( cellType );
        categories.add( disease );
        when( ontologyService.getCategoryTerms() ).thenReturn( categories );

        assertThat( target( "/annotations/categories" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfiesExactlyInAnyOrder(
                        a -> assertThat( a )
                                .containsEntry( "uri", "http://example.com/cellType" )
                                .containsEntry( "label", "cell type" ),
                        a -> assertThat( a )
                                .containsEntry( "uri", "http://example.com/disease" )
                                .containsEntry( "label", "disease" ) );

        verify( ontologyService ).getCategoryTerms();
    }

    @Test
    public void testGetAnnotationCategoriesEmpty() {
        when( ontologyService.getCategoryTerms() ).thenReturn( Collections.emptySet() );

        assertThat( target( "/annotations/categories" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .isEmpty();
    }

    @Test
    public void testGetAnnotationPredicates() {
        OntologyProperty hasPart = mock( OntologyProperty.class );
        when( hasPart.getUri() ).thenReturn( "http://example.com/has_part" );
        when( hasPart.getLabel() ).thenReturn( "has part" );
        java.util.LinkedHashSet<OntologyProperty> predicates = new java.util.LinkedHashSet<>();
        predicates.add( hasPart );
        when( ontologyService.getRelationTerms() ).thenReturn( predicates );

        assertThat( target( "/annotations/predicates" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "uri", "http://example.com/has_part" )
                        .containsEntry( "label", "has part" ) );

        verify( ontologyService ).getRelationTerms();
    }

    @Test
    public void testGetAnnotationPredicatesEmpty() {
        when( ontologyService.getRelationTerms() ).thenReturn( Collections.emptySet() );

        assertThat( target( "/annotations/predicates" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .isEmpty();
    }

    @Test
    public void testGetAnnotationTerm() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/diabetes" );
        when( term.getLabel() ).thenReturn( "diabetes" );
        when( term.isObsolete() ).thenReturn( false );
        when( ontologyService.getTerm( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getDefinition( eq( "http://example.com/diabetes" ), anyLong(), any() ) )
                .thenReturn( "a metabolic disease" );

        ExpressionExperiment ee1 = ExpressionExperiment.Factory.newInstance();
        ee1.setId( 1L );
        ExpressionExperiment ee2 = ExpressionExperiment.Factory.newInstance();
        ee2.setId( 2L );
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = new HashMap<>();
        hits.put( ExpressionExperiment.class, Collections.singletonMap( "http://example.com/diabetes", new HashSet<>( Arrays.asList( ee1, ee2 ) ) ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( hits );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .hasFieldOrPropertyWithValue( "data.uri", "http://example.com/diabetes" )
                .hasFieldOrPropertyWithValue( "data.label", "diabetes" )
                .hasFieldOrPropertyWithValue( "data.definition", "a metabolic disease" )
                .hasFieldOrPropertyWithValue( "data.obsolete", false )
                .hasFieldOrPropertyWithValue( "data.usageCount", 2 );

        verify( ontologyService ).getTerm( eq( "http://example.com/diabetes" ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
        verify( ontologyService ).getDefinition( eq( "http://example.com/diabetes" ), longThat( l -> l <= 30000 ), eq( TimeUnit.MILLISECONDS ) );
        verify( characteristicService ).findExperimentsByUris(
                eq( Collections.singleton( "http://example.com/diabetes" ) ),
                eq( true ), eq( true ), eq( true ), isNull(), eq( -1 ), eq( false ), eq( false ) );
    }

    @Test
    public void testGetAnnotationTermReportsZeroWhenNoExperimentsMatch() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/orphan" );
        when( term.getLabel() ).thenReturn( "orphan" );
        when( ontologyService.getTerm( eq( "http://example.com/orphan" ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/orphan" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.usageCount", 0 );
    }

    @Test
    public void testGetAnnotationTermSkipsCountLookupWhenTermHasNoUri() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( null );
        when( term.getLabel() ).thenReturn( "uri-less" );
        when( ontologyService.getTerm( eq( "http://example.com/foo" ), anyLong(), any() ) ).thenReturn( term );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/foo" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.usageCount", null );

        verify( characteristicService, never() ).findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() );
    }

    @Test
    public void testGetAnnotationTermNotFound() throws TimeoutException {
        when( ontologyService.getTerm( eq( "http://example.com/missing" ), anyLong(), any() ) ).thenReturn( null );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/missing" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        verify( ontologyService ).getTerm( eq( "http://example.com/missing" ), anyLong(), any() );
        verifyNoMoreInteractions( ontologyService );
        verify( characteristicService, never() ).findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() );
    }

    @Test
    public void testGetAnnotationTermWithBlankUriIs400() throws TimeoutException {
        assertThat( target( "/annotations/term" ).queryParam( "uri", "" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        assertThat( target( "/annotations/term" ).queryParam( "uri", "   " ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).getTerm( any(), anyLong(), any() );
    }

    @Test
    public void testGetAnnotationTermWithMissingUriIs400() throws TimeoutException {
        assertThat( target( "/annotations/term" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).getTerm( any(), anyLong(), any() );
    }

    @Test
    public void testGetAnnotationTermWhenLookupTimesOut() throws TimeoutException {
        when( ontologyService.getTerm( eq( "http://example.com/slow" ), anyLong(), any() ) )
                .thenThrow( new TimeoutException( "ontology lookup timed out" ) );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/slow" ).request().get() )
                .hasStatus( Response.Status.SERVICE_UNAVAILABLE )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        verify( characteristicService, never() ).findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() );
    }

    @Test
    public void testSearchAnnotationsEnrichesTopNWithDefinitionAndParents() throws SearchException, TimeoutException {
        // Mock 27 hits — the first 25 should carry non-null definition + parents in the response,
        // hits 26+ carry nulls (lazy-load sentinel).
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 27; i++ ) {
            raw.add( new CharacteristicValueObject( "term-" + i, "http://example.com/t" + i, "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        // Definition lookup: per-URI canned response keyed by URI.
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) )
                .thenAnswer( a -> "def-for-" + a.getArgument( 0 ) );
        // Parents lookup: for each top-25 URI, the test resolves the term then asks for direct parents.
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) )
                .thenAnswer( a -> {
                    String uri = a.getArgument( 0 );
                    OntologyTerm t = mock( OntologyTerm.class );
                    when( t.getUri() ).thenReturn( uri );
                    when( t.getLabel() ).thenReturn( "term-" + uri );
                    return t;
                } );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenAnswer( a -> {
                    OntologyTerm parent = mock( OntologyTerm.class );
                    when( parent.getUri() ).thenReturn( "http://example.com/parent" );
                    when( parent.getLabel() ).thenReturn( "parent label" );
                    return Collections.singleton( parent );
                } );
        // No usage-count contribution needed for this test; mock returns empty.
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 27 )
                .satisfies( hits -> {
                    // First 25 hits: definition + parents populated.
                    for ( int i = 0; i < 25; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "definition" ) )
                                .as( "top-25 hit %d should carry definition", i )
                                .isEqualTo( "def-for-http://example.com/t" + i );
                        assertThat( hit.get( "parents" ) )
                                .as( "top-25 hit %d should carry parents", i )
                                .isInstanceOf( List.class );
                        assertThat( ( List<?> ) hit.get( "parents" ) ).hasSize( 1 );
                    }
                    // Hits 25 + 26: sentinel nulls.
                    for ( int i = 25; i < 27; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "definition" ) )
                                .as( "post-top-25 hit %d should carry null definition", i ).isNull();
                        assertThat( hit.get( "parents" ) )
                                .as( "post-top-25 hit %d should carry null parents", i ).isNull();
                    }
                } );
    }

    @Test
    public void testGetAnnotationTermPopulatesParents() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/diabetes" );
        when( term.getLabel() ).thenReturn( "diabetes" );
        when( ontologyService.getTerm( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getDefinition( eq( "http://example.com/diabetes" ), anyLong(), any() ) )
                .thenReturn( "metabolic disease" );
        OntologyTerm parent = mock( OntologyTerm.class );
        when( parent.getUri() ).thenReturn( "http://example.com/disease" );
        when( parent.getLabel() ).thenReturn( "disease" );
        when( ontologyService.getParents( eq( Collections.singleton( term ) ), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.singleton( parent ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.uri", "http://example.com/diabetes" )
                .extracting( "data.parents", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( p -> assertThat( p )
                        .containsEntry( "uri", "http://example.com/disease" )
                        .containsEntry( "label", "disease" ) );

        verify( ontologyService ).getParents( eq( Collections.singleton( term ) ), eq( true ), eq( true ), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsUnknownRankReturns400() throws SearchException, TimeoutException {
        // No need to mock anything — the rank= validator fires before ontology lookup.
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "rank", "no-such-strategy" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsCollapsesDuplicateEeIdsAcrossClasses() throws SearchException, TimeoutException {
        CharacteristicValueObject hit = new CharacteristicValueObject( "diabetes", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );

        ExpressionExperiment ee1 = ExpressionExperiment.Factory.newInstance();
        ee1.setId( 1L );
        ExpressionExperiment ee1Dup = ExpressionExperiment.Factory.newInstance();
        ee1Dup.setId( 1L );
        // Same EE id surfaces in two Identifiable buckets; should collapse to a distinct count of 1.
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = new HashMap<>();
        hits.put( ExpressionExperiment.class, Collections.singletonMap( "http://example.com/diabetes", Collections.singleton( ee1 ) ) );
        // Use a second concrete Identifiable class for the second bucket key.
        hits.put( CharacteristicValueObject.class, Collections.singletonMap( "http://example.com/diabetes", Collections.singleton( ee1Dup ) ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( hits );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "usageCount", 1 ) );
    }
}