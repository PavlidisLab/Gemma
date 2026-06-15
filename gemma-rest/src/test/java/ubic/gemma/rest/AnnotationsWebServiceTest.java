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
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.ontology.model.OntologyProperty;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.Statement;
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
        public TaxonArgService taxonArgService( TaxonService taxonService, GeneService geneService ) {
            return new TaxonArgService( taxonService, mock( ChromosomeService.class ), geneService );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                DatasetArgService datasetRestService, TaxonArgService taxonArgService, GeneService geneService ) {
            return new AnnotationsWebService( ontologyService, searchService, characteristicService,
                    expressionExperimentService, datasetRestService, taxonArgService, geneService, null );
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

    @Autowired
    private GeneService geneService;

    @BeforeEach
    public void setUpMocks() {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setId( 1L );
        when( taxonService.findByCommonName( "human" ) ).thenReturn( taxon );
        // The /annotations/search endpoint memoises results in a Spring CacheManager bean named
        // AnnotationsSearchResponseCache. This test context doesn't wire a CacheManager (the
        // field on AnnotationsWebService is @Autowired(required = false)), so caching is
        // implicitly disabled and there's nothing to clear between tests.
    }

    @AfterEach
    public void resetMocks() {
        reset( searchService, taxonService, ontologyService, expressionExperimentService, characteristicService, geneService );
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
    public void testAddDatasetAnnotationCreatesStatementWhenPredicateOrObjectSet() {
        // POST a compound annotation: "treatment HFD has_dose 30%". The conversion must
        // construct a Statement (not a plain Characteristic) and populate predicate +
        // object on it. Verifies the Gemma 2.0 EE Statement support — the underlying
        // service.addAnnotation signature already accepts any Characteristic subclass.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        ee.setCharacteristics( new LinkedHashSet<>() );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> {
                    Characteristic vc = a.getArgument( 1, Characteristic.class );
                    vc.setId( 99L );
                    return vc;
                } );
        String body = "{"
                + "\"category\":\"treatment\","
                + "\"categoryUri\":\"http://www.ebi.ac.uk/efo/EFO_0000727\","
                + "\"value\":\"high fat diet\","
                + "\"valueUri\":\"http://purl.obolibrary.org/obo/EFO_0002091\","
                + "\"predicate\":\"has_dose\","
                + "\"predicateUri\":\"http://purl.obolibrary.org/obo/RO_0002211\","
                + "\"object\":\"30%\","
                + "\"objectUri\":\"http://example.com/dose/30pct\""
                + "}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        org.mockito.ArgumentCaptor<Characteristic> captor = org.mockito.ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        Characteristic persisted = captor.getValue();
        assertThat( persisted ).isInstanceOf( Statement.class );
        Statement s = ( Statement ) persisted;
        assertThat( s.getCategory() ).isEqualTo( "treatment" );
        assertThat( s.getValue() ).isEqualTo( "high fat diet" );
        assertThat( s.getPredicate() ).isEqualTo( "has_dose" );
        assertThat( s.getPredicateUri() ).isEqualTo( "http://purl.obolibrary.org/obo/RO_0002211" );
        assertThat( s.getObject() ).isEqualTo( "30%" );
        assertThat( s.getObjectUri() ).isEqualTo( "http://example.com/dose/30pct" );
        assertThat( s.getSecondPredicate() ).isNull();
        assertThat( s.getSecondObject() ).isNull();
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationCreatesCharacteristicWhenNoStatementFieldsSet() {
        // Sanity: bodies without any predicate/object field still produce a plain
        // Characteristic (not an empty-Statement subclass). The hasStatementShape() guard
        // is what flips the conversion; this pins that the default path is unchanged.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setShortName( "GSE-test" );
        ee.setCharacteristics( new LinkedHashSet<>() );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any( Characteristic.class ) ) )
                .thenAnswer( a -> a.getArgument( 1, Characteristic.class ) );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\","
                + "\"valueUri\":\"http://purl.obolibrary.org/obo/UBERON_0002107\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        org.mockito.ArgumentCaptor<Characteristic> captor = org.mockito.ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        assertThat( captor.getValue() ).isNotInstanceOf( Statement.class );
    }

    @Test
    @WithMockUser(authorities = { "GROUP_CURATOR" })
    public void testAddDatasetAnnotationRejectsSecondPredicateWithoutFirst() {
        // Compound second-pair semantics only make sense relative to a first pair; a body
        // that supplies secondPredicate without ANY first predicate/object is malformed.
        // Catches client bugs that conflate the two pairs.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        String body = "{"
                + "\"category\":\"treatment\","
                + "\"value\":\"HFD\","
                + "\"secondPredicate\":\"for\","
                + "\"secondObject\":\"12 weeks\""
                + "}";
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
        // Mock 60 hits and request limit=50 — the first 25 (ENRICH_TOP_N) should carry non-null
        // definition + parents in the response, hits 26-50 carry nulls (lazy-load sentinel), and
        // hits 51-60 are truncated by the ?limit=50 cap.
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 60; i++ ) {
            // Zero-padded suffix so URI lex sort matches numeric order — the endpoint
            // canonicalises hits by URI ASC before ranking, and un-padded t0..t59 would
            // shuffle (t10 sorts before t2 lex-wise) and break position-based assertions.
            raw.add( new CharacteristicValueObject( "term-" + i, String.format( "http://example.com/t%03d", i ), "disease", "http://example.com/disease" ) );
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

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "50" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 50 )
                .satisfies( hits -> {
                    // First 25 hits: definition + parents populated.
                    for ( int i = 0; i < 25; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "definition" ) )
                                .as( "top-25 hit %d should carry definition", i )
                                .isEqualTo( String.format( "def-for-http://example.com/t%03d", i ) );
                        assertThat( hit.get( "parents" ) )
                                .as( "top-25 hit %d should carry parents", i )
                                .isInstanceOf( List.class );
                        assertThat( ( List<?> ) hit.get( "parents" ) ).hasSize( 1 );
                    }
                    // Hits 25..49: sentinel nulls (within limit but past enrichment top-N).
                    for ( int i = 25; i < 50; i++ ) {
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
    public void testSearchAnnotationsDefaultLimitIs20() throws SearchException, TimeoutException {
        // 30 raw hits → response truncated to 20 by the default ?limit=20.
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 30; i++ ) {
            // Zero-padded suffix so URI lex sort matches numeric order — the endpoint
            // canonicalises hits by URI ASC before ranking, and un-padded t0..t59 would
            // shuffle (t10 sorts before t2 lex-wise) and break position-based assertions.
            raw.add( new CharacteristicValueObject( "term-" + i, String.format( "http://example.com/t%03d", i ), "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 20 );
    }

    @Test
    public void testSearchAnnotationsRespectsExplicitLimit() throws SearchException, TimeoutException {
        // 60 raw hits → response truncated to the requested ?limit=50 (hard upper bound).
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 60; i++ ) {
            // Zero-padded suffix so URI lex sort matches numeric order — the endpoint
            // canonicalises hits by URI ASC before ranking, and un-padded t0..t59 would
            // shuffle (t10 sorts before t2 lex-wise) and break position-based assertions.
            raw.add( new CharacteristicValueObject( "term-" + i, String.format( "http://example.com/t%03d", i ), "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "50" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 50 );
    }

    @Test
    public void testSearchAnnotationsLimitOver100Returns400() throws SearchException, TimeoutException {
        // The validator fires before the ontology lookup — no need to mock anything.
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "101" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsLimitBelow1Returns400() throws SearchException, TimeoutException {
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "0" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsAttributesPreferredLabelMatch() throws SearchException, TimeoutException {
        // Hit's preferred label EQUALS the query (case- + whitespace-normalised) →
        // matchedVia=preferred_label, matchedText=label. No synonyms wired on the term mock;
        // the back-compute fast-paths on the preferred-label match.
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "Diabetes mellitus", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes mellitus" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( eq( "http://example.com/diabetes" ), anyLong(), any() ) )
                .thenReturn( "a metabolic disease" );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/diabetes" );
        when( term.getLabel() ).thenReturn( "Diabetes mellitus" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes mellitus" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "preferred_label" )
                        .containsEntry( "matchedText", "Diabetes mellitus" ) );
    }

    @Test
    public void testSearchAnnotationsAttributesLabelTokensForNonEqualPartialLabel() throws SearchException, TimeoutException {
        // Query "pancreatic cell" is a multi-token query; the label "type b pancreatic cell"
        // contains both content tokens as substrings but does not start with the query and is
        // not equal to it. Under the new attribution taxonomy this surfaces as
        // matchedVia=label_tokens — the relevant signal for the agents-side eval team that
        // multi-token coverage is the reason for the match (previously surfaced as null,
        // forcing them to guess).
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "type b pancreatic cell", "http://example.com/CL_0000169", "cell type", "http://example.com/cell_type" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "pancreatic cell" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/CL_0000169" );
        when( term.getLabel() ).thenReturn( "type b pancreatic cell" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "pancreatic cell" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "label_tokens" )
                        .containsEntry( "matchedText", "type b pancreatic cell" ) );
    }

    @Test
    public void testSearchAnnotationsAttributesSynonymTokensWhenLabelLacksTokens() throws SearchException, TimeoutException {
        // Query: "ammon horn" (multi-token). Label "hippocampus" contains neither content
        // token, but a synonym "Ammon's horn" covers both. Under the new attribution taxonomy
        // this surfaces as matchedVia=synonym_tokens — important for the agents-side eval
        // team: the strict-equality synonym tier (exact_synonym) needs the query to NORMALISE
        // equal to the synonym, which fails on token-level reorderings; synonym_tokens picks
        // up the slack so the bind has a reason set.
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "hippocampus", "http://example.com/UBERON_0002421", "organism part", "http://example.com/organism_part" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "ammon horn" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/UBERON_0002421" );
        when( term.getLabel() ).thenReturn( "hippocampus" );
        AnnotationProperty syn = mock( AnnotationProperty.class );
        when( syn.getContents() ).thenReturn( "Ammon's horn" );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ) )
                .thenReturn( Collections.singletonList( syn ) );
        when( term.getAnnotations( anyString() ) ).thenAnswer( a -> {
            String prop = a.getArgument( 0 );
            if ( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym".equals( prop ) ) {
                return Collections.singletonList( syn );
            }
            return Collections.emptyList();
        } );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "ammon horn" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "synonym_tokens" )
                        .containsEntry( "matchedText", "Ammon's horn" ) );
    }

    @Test
    public void testSearchAnnotationsAttributesLabelPrefixForTypeahead() throws SearchException, TimeoutException {
        // Typeahead: user typed "alzhei" and the label is "alzheimer's disease". This is
        // matchedVia=label_prefix (not exact, not token-coverage; the partial prefix is the
        // reason).
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "alzheimer's disease", "http://example.com/DOID_10652", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "alzhei" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/DOID_10652" );
        when( term.getLabel() ).thenReturn( "alzheimer's disease" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "alzhei" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "label_prefix" )
                        .containsEntry( "matchedText", "alzheimer's disease" ) );
    }

    @Test
    public void testSearchAnnotationsSurfacesGeneByOfficialName() throws SearchException, TimeoutException {
        // Gemma 1.0 parity: typing "haptoglobin" (the gene's official name, NOT its symbol)
        // surfaces the HP gene row. The previous symbol-only fan-out missed this case because
        // the official symbol is "HP", not "haptoglobin". Regression filed in screenshots
        // 2026-06-13.
        ubic.gemma.model.genome.Gene hp = mock( ubic.gemma.model.genome.Gene.class );
        when( hp.getId() ).thenReturn( 3240L );
        when( hp.getOfficialSymbol() ).thenReturn( "HP" );
        when( hp.getOfficialName() ).thenReturn( "haptoglobin" );
        when( hp.getNcbiGeneId() ).thenReturn( 3240 );
        when( geneService.findByOfficialSymbol( "haptoglobin" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByOfficialName( "haptoglobin" ) ).thenReturn( Collections.singletonList( hp ) );
        when( geneService.findByAlias( "haptoglobin" ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "haptoglobin" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "haptoglobin" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "category", "gene" )
                        .containsEntry( "value", "HP haptoglobin" )
                        .containsEntry( "valueUri", "http://purl.org/commons/record/ncbi_gene/3240" )
                        .containsEntry( "matchedVia", "search:gene" ) );
    }

    @Test
    public void testSearchAnnotationsSurfacesGeneByAlias() throws SearchException, TimeoutException {
        // Alias fan-out: a curator typing "Trp53" finds TRP53. The mouse-style alias resolves
        // to the human/mouse gene regardless of which spelling the curator used.
        ubic.gemma.model.genome.Gene tp53 = mock( ubic.gemma.model.genome.Gene.class );
        when( tp53.getId() ).thenReturn( 22059L );
        when( tp53.getOfficialSymbol() ).thenReturn( "Trp53" );
        when( tp53.getOfficialName() ).thenReturn( "transformation related protein 53" );
        when( tp53.getNcbiGeneId() ).thenReturn( 22059 );
        when( geneService.findByOfficialSymbol( "tp53" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByOfficialName( "tp53" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByAlias( "tp53" ) ).thenReturn( Collections.singletonList( tp53 ) );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "tp53" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "tp53" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "category", "gene" )
                        .containsEntry( "value", "Trp53 transformation related protein 53" )
                        .containsEntry( "valueUri", "http://purl.org/commons/record/ncbi_gene/22059" )
                        .containsEntry( "matchedVia", "search:gene" ) );
    }

    @Test
    public void testSearchAnnotationsDropsGeneFanoutWhenURIAlreadyInOntologyResults() throws SearchException, TimeoutException {
        // The IL10 regression from screenshots 2026-06-14: when the corpus already carries a
        // characteristic with the same NCBI Gene URI (a curator previously tagged an experiment
        // with the gene under category "genotype" → label "Il10 [mouse] interleukin 10",
        // usageCount > 0), the synthetic gene-fanout row would duplicate it with a bare label
        // and zero usage. Fix: URI-dedup at merge time. Ontology row wins (richer label + usage).
        CharacteristicValueObject ontologyHit = new CharacteristicValueObject(
                "Il10 [mouse] interleukin 10",
                "http://purl.org/commons/record/ncbi_gene/16153",
                "genotype",
                "http://www.ebi.ac.uk/efo/EFO_0000513" );
        ubic.gemma.model.genome.Gene il10 = mock( ubic.gemma.model.genome.Gene.class );
        when( il10.getId() ).thenReturn( 16153L );
        when( il10.getOfficialSymbol() ).thenReturn( "Il10" );
        when( il10.getOfficialName() ).thenReturn( "interleukin 10" );
        when( il10.getNcbiGeneId() ).thenReturn( 16153 );
        when( geneService.findByOfficialSymbol( "il10" ) ).thenReturn( Collections.singletonList( il10 ) );
        when( geneService.findByOfficialName( "il10" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByAlias( "il10" ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "il10" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( ontologyHit ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://purl.org/commons/record/ncbi_gene/16153" );
        when( term.getLabel() ).thenReturn( "Il10 [mouse] interleukin 10" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "il10" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "category", "genotype" )
                        .containsEntry( "value", "Il10 [mouse] interleukin 10" )
                        .containsEntry( "valueUri", "http://purl.org/commons/record/ncbi_gene/16153" ) );
    }

    @Test
    public void testSearchAnnotationsDedupesGenesAcrossProbes() throws SearchException, TimeoutException {
        // A single gene matching by multiple probes (symbol AND alias, e.g.) must emit one row.
        ubic.gemma.model.genome.Gene hp = mock( ubic.gemma.model.genome.Gene.class );
        when( hp.getId() ).thenReturn( 3240L );
        when( hp.getOfficialSymbol() ).thenReturn( "HP" );
        when( hp.getOfficialName() ).thenReturn( "haptoglobin" );
        when( hp.getNcbiGeneId() ).thenReturn( 3240 );
        // Imagine a hypothetical query "HP" that matches both the symbol AND an alias on the
        // same Gene row.
        when( geneService.findByOfficialSymbol( "HP" ) ).thenReturn( Collections.singletonList( hp ) );
        when( geneService.findByOfficialName( "HP" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByAlias( "HP" ) ).thenReturn( Collections.singletonList( hp ) );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "HP" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "HP" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 );
    }

    @Test
    public void testSearchAnnotationsAttributesSynonymMatch() throws SearchException, TimeoutException {
        // Hit's preferred label ("hippocampus") does NOT equal the query ("ammon's horn"); the
        // exact-synonym property does ("Ammon's horn", normalised). Back-compute must pick
        // exact_synonym + surface the matching synonym text.
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "hippocampus", "http://example.com/UBERON_0002421", "organism part", "http://example.com/organism_part" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "ammon's horn" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) )
                .thenReturn( "the part of the brain that..." );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/UBERON_0002421" );
        when( term.getLabel() ).thenReturn( "hippocampus" );
        AnnotationProperty syn = mock( AnnotationProperty.class );
        when( syn.getContents() ).thenReturn( "Ammon's horn" );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ) )
                .thenReturn( Collections.singletonList( syn ) );
        // Other synonym probes return empty.
        when( term.getAnnotations( anyString() ) ).thenAnswer( a -> {
            String prop = a.getArgument( 0 );
            if ( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym".equals( prop ) ) {
                return Collections.singletonList( syn );
            }
            return Collections.emptyList();
        } );
        when( ontologyService.getTerm( eq( "http://example.com/UBERON_0002421" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "ammon's horn" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "exact_synonym" )
                        .containsEntry( "matchedText", "Ammon's horn" ) );
    }

    @Test
    public void testSearchAnnotationsLeavesMatchAttributionNullForPostTopN() throws SearchException, TimeoutException {
        // Hits beyond the 25-deep enrichment window must carry matchedVia=null / matchedText=null
        // (lazy-load sentinel), even when the limit allows them through.
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 30; i++ ) {
            // Zero-padded suffix so URI lex sort matches numeric order — the endpoint
            // canonicalises hits by URI ASC before ranking, and un-padded t0..t59 would
            // shuffle (t10 sorts before t2 lex-wise) and break position-based assertions.
            raw.add( new CharacteristicValueObject( "term-" + i, String.format( "http://example.com/t%03d", i ), "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) )
                .thenAnswer( a -> {
                    String uri = a.getArgument( 0 );
                    OntologyTerm t = mock( OntologyTerm.class );
                    when( t.getUri() ).thenReturn( uri );
                    // Label EQUALS query — required for matchedVia=preferred_label under the
                    // strict-equality rule; matchedVia is the test's signal of "enriched".
                    when( t.getLabel() ).thenReturn( "diabetes" );
                    when( t.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
                    return t;
                } );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "30" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 30 )
                .satisfies( hits -> {
                    // Top-25: matchedVia populated (label equals query, picks preferred_label).
                    for ( int i = 0; i < 25; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "matchedVia" ) )
                                .as( "top-25 hit %d should carry matchedVia", i )
                                .isEqualTo( "preferred_label" );
                    }
                    // 25..29: nulls.
                    for ( int i = 25; i < 30; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "matchedVia" ) )
                                .as( "post-top-25 hit %d should carry null matchedVia", i ).isNull();
                        assertThat( hit.get( "matchedText" ) )
                                .as( "post-top-25 hit %d should carry null matchedText", i ).isNull();
                    }
                } );
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

    @Test
    public void testSearchAnnotationsCanonicaliesInputOrderByUri() throws SearchException, TimeoutException {
        // Underlying Lucene/Hibernate-Search can return tied-relevance hits in non-deterministic
        // order across requests. The endpoint should sort hits by URI ASC at strategy entry so
        // the final output is stable. Feed the mock a deliberately-shuffled order and assert
        // the response comes back URI-sorted under the default (lucene) strategy.
        CharacteristicValueObject zeta = new CharacteristicValueObject( "zeta term", "http://example.com/zeta", "cat", null );
        CharacteristicValueObject alpha = new CharacteristicValueObject( "alpha term", "http://example.com/alpha", "cat", null );
        CharacteristicValueObject mike = new CharacteristicValueObject( "mike term", "http://example.com/mike", "cat", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "stable" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( zeta, mike, alpha ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "stable" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 3 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://example.com/alpha" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://example.com/mike" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 2 ) ).get( "valueUri" ) ).isEqualTo( "http://example.com/zeta" );
                } );
    }

    @Test
    public void testSearchAnnotationsPrefixesFilterAppliesBeforeRanking() throws SearchException, TimeoutException {
        // `?prefixes=CL_,EFO_` should drop everything else BEFORE ranking + truncation. Feed a mix
        // of CL_, EFO_, and MP_ URIs; assert only CL_ + EFO_ survive and the count limit applies
        // to the filtered set (not the pre-filter set).
        List<CharacteristicValueObject> mixed = new ArrayList<>();
        mixed.add( new CharacteristicValueObject( "MP_1", "http://purl.obolibrary.org/obo/MP_0000001", "phenotype", null ) );
        mixed.add( new CharacteristicValueObject( "CL_1", "http://purl.obolibrary.org/obo/CL_0000001", "cell", null ) );
        mixed.add( new CharacteristicValueObject( "MP_2", "http://purl.obolibrary.org/obo/MP_0000002", "phenotype", null ) );
        mixed.add( new CharacteristicValueObject( "EFO_1", "http://www.ebi.ac.uk/efo/EFO_0000001", "disease", null ) );
        mixed.add( new CharacteristicValueObject( "MP_3", "http://purl.obolibrary.org/obo/MP_0000003", "phenotype", null ) );
        mixed.add( new CharacteristicValueObject( "CL_2", "http://purl.obolibrary.org/obo/CL_0000002", "cell", null ) );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "myeloid" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( mixed );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "myeloid" )
                .queryParam( "prefixes", "CL_,EFO_" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 3 )
                .satisfies( hits -> {
                    // URI-ASC canonical order: CL_0000001 < CL_0000002 < EFO_0000001 lex-wise on full URI.
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CL_0000001" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CL_0000002" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 2 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000001" );
                } );
    }

    @Test
    public void testSearchAnnotationsUpstreamReturns400WhenUrlUnset() throws SearchException, TimeoutException {
        // `?upstream=true` with no `gemma.upstream.annotationSearch.url` configured must 400.
        // The test bean's TestPropertyPlaceholderConfigurer doesn't set the url property, so this
        // exercises the unconfigured branch. The endpoint must not silently fall back to local.
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "anything" )
                .queryParam( "upstream", "true" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        // ontologyService must NOT be called when upstream=true and url is unset; the 400 should
        // fire before any work.
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsPrefixesFilterRespectsLimit() throws SearchException, TimeoutException {
        // The truncate-then-filter footgun: server returns top-N, client filters → maybe nothing
        // left. Pushdown should ensure ?limit=2 returns the top-2 OF the filtered set, not 2 from
        // the unfiltered set possibly trimmed down to 0 by client-side filtering.
        List<CharacteristicValueObject> mixed = new ArrayList<>();
        for ( int i = 0; i < 5; i++ ) {
            mixed.add( new CharacteristicValueObject( "MP_" + i, "http://purl.obolibrary.org/obo/MP_000000" + i, "phenotype", null ) );
        }
        for ( int i = 0; i < 5; i++ ) {
            mixed.add( new CharacteristicValueObject( "CL_" + i, "http://purl.obolibrary.org/obo/CL_000000" + i, "cell", null ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "scoped" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( mixed );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "scoped" )
                .queryParam( "prefixes", "CL_" )
                .queryParam( "limit", "2" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    // After CL_-only filter + URI-ASC canonical sort, top-2 of the CL set:
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CL_0000000" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CL_0000001" );
                } );
    }

    @Test
    public void testSearchAnnotationsPrefersCloOverEfoForBareCellLineQuery() throws SearchException, TimeoutException {
        // Bare cell-line query (no trailing " cell"): EFO labels the line as the bare name
        // ("a549"), CLO as "A549 cell". Without suffix-stripping, EFO wins at the exact-label
        // tier and CLO sinks to startsWith. With the strip, both reach tier 0 and the cell-line
        // tiebreaker promotes CLO ahead of EFO.
        CharacteristicValueObject efo = new CharacteristicValueObject( "a549", "http://www.ebi.ac.uk/efo/EFO_0001086", "cell line", null );
        CharacteristicValueObject clo = new CharacteristicValueObject( "A549 cell", "http://purl.obolibrary.org/obo/CLO_0001601", "cell line", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "A549" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "A549" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CLO_0001601" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0001086" );
                } );
    }

    @Test
    public void testSearchAnnotationsPrefersCloOverEfoForCellSuffixQuery() throws SearchException, TimeoutException {
        // Regression: query already carries the " cell" suffix. CLO's "A549 cell" is the exact
        // label match (tier 0); EFO's "a549" sinks to substring (tier 4). CLO must remain first.
        CharacteristicValueObject efo = new CharacteristicValueObject( "a549", "http://www.ebi.ac.uk/efo/EFO_0001086", "cell line", null );
        CharacteristicValueObject clo = new CharacteristicValueObject( "A549 cell", "http://purl.obolibrary.org/obo/CLO_0001601", "cell line", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "A549 cell" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "A549 cell" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CLO_0001601" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0001086" );
                } );
    }

    @Test
    public void testSearchAnnotationsCellLineTiebreakerDoesNotAffectNonCellLineQueries() throws SearchException, TimeoutException {
        // Guard: the CLO-preference tiebreaker is gated on the " cell" suffix strip earning the
        // match. A query that doesn't trigger the strip ("lung cancer", with hits whose labels
        // don't end in " cell") must fall back to URI-ASC, not get a hidden CLO boost.
        CharacteristicValueObject efo = new CharacteristicValueObject( "lung cancer", "http://www.ebi.ac.uk/efo/EFO_0001071", "disease", null );
        CharacteristicValueObject mondo = new CharacteristicValueObject( "lung cancer", "http://purl.obolibrary.org/obo/MONDO_0008903", "disease", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "lung cancer" ), anyInt(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, mondo ) );
        when( characteristicService.findExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyInt(), anyBoolean(), anyBoolean() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "lung cancer" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    // URI-ASC: "http://purl..." < "http://www...".
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/MONDO_0008903" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0001071" );
                } );
    }
}