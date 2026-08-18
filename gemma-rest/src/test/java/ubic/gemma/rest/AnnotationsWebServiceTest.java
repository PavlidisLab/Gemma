package ubic.gemma.rest;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.assertj.core.api.InstanceOfAssertFactories.map;
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
            // The category preference / exclusion tables are @Value-injected, so without them here
            // neither promotion nor exclusion can be exercised at all and both would ship on live
            // verification only.
            return new TestPropertyPlaceholderConfigurer( "gemma.hosturl=http://localhost:8080",
                    "annotation.category.prefixes=treatment:CHEBI_,EFO_;genotype:TGEMO_,GENO_,EFO_",
                    "annotation.category.excludedPrefixes=genotype:MONDO_" );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        /**
         * Required by {@code AnnotationsWebService} since the relation endpoints landed; without it
         * every test here fails on context init rather than on anything it asserts. A mock, because
         * nothing in this class exercises a relation — {@code AnnotationRelationDaoTest} does.
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
        public DatasetArgService datasetRestService( ExpressionExperimentService service, SearchService searchService ) {
            return new DatasetArgService( service, searchService, mock( ArrayDesignService.class ), mock( BioAssayService.class ), mock( OutlierDetectionService.class ),
                    mock( ubic.gemma.persistence.service.common.description.PublicationAssociationService.class ) );
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
            // Register the real strategies rather than passing null: with null the service falls
            // back to lucene alone, so ?rank= is untestable and the interaction between a strategy
            // and the category promotion cannot be pinned.
            java.util.Map<String, ubic.gemma.rest.ranking.AnnotationSearchRankingStrategy> strategies = new HashMap<>();
            strategies.put( "lucene", new ubic.gemma.rest.ranking.LuceneOrderRankingStrategy() );
            strategies.put( "composite", new ubic.gemma.rest.ranking.CompositeRankingStrategy( 0.5, 0.3, 0.2 ) );
            return new AnnotationsWebService( ontologyService, searchService, characteristicService,
                    expressionExperimentService, datasetRestService, taxonArgService, geneService, strategies );
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

        Map<String, Long> counts = new HashMap<>();
        counts.put( "http://example.com/parentA", 2L );
        counts.put( "http://example.com/parentB", 1L );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( counts );

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

        verify( characteristicService ).countExperimentsByUris(
                argThat( ( Set<String> s ) -> s.containsAll( Arrays.asList( "http://example.com/parentA", "http://example.com/parentB" ) ) ),
                eq( true ), eq( true ), eq( true ), isNull(), eq( Collections.emptySet() ) );
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

        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( "http://example.com/child", 1L ) );

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
        // the tally reports nothing for this URI → usageCount falls back to 0
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        verify( characteristicService, never() ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
    }

    @Test
    public void testSearchAnnotationsPopulatesUsageCount() throws SearchException, TimeoutException {
        CharacteristicValueObject hit = new CharacteristicValueObject( "diabetes", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );

        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( "http://example.com/diabetes", 2L ) );

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

    /**
     * A designation query that retrieves only near-matches must report WHICH terms it ruled out,
     * not merely that nothing matched. An empty {@code data} array is indistinguishable from "the
     * ontology wasn't loaded" or "this call never ran", and on its own it does not stop a
     * downstream stage from proposing one of these very terms from its own index.
     */
    @Test
    public void testSuppressedDesignationReportsWhatItRuledOut() throws SearchException, TimeoutException {
        // The MK-8722 case: everything that comes back is a different compound.
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MK-8722" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList(
                        new CharacteristicValueObject( "mk-8353", "http://purl.obolibrary.org/obo/CHEBI_167664", "treatment", null ),
                        new CharacteristicValueObject( "ganoderic acid mk", "http://purl.obolibrary.org/obo/CHEBI_176105", "treatment", null ) ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "MK-8722" )
                .queryParam( "suppress_near_matches", "true" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .satisfies( body -> {
                    // Nothing we stand behind — and the ruled-out terms are NOT smuggled into data,
                    // so a client reading data[0] can never pick up a term we just rejected.
                    assertThat( body ).extracting( "data", list( Map.class ) ).isEmpty();
                    assertThat( body ).extracting( "negativeEvidence", map( String.class, Object.class ) )
                            .containsEntry( "query", "MK-8722" )
                            .containsEntry( "solidMatch", false )
                            .containsEntry( "ruledOutTruncated", false );
                    //noinspection unchecked
                    List<Map<String, Object>> ruledOut = (List<Map<String, Object>>)
                            ( (Map<String, Object>) ( (Map<String, Object>) body ).get( "negativeEvidence" ) ).get( "ruledOut" );
                    assertThat( ruledOut ).hasSize( 2 );
                    assertThat( ruledOut ).extracting( r -> r.get( "valueUri" ) )
                            .containsExactlyInAnyOrder( "http://purl.obolibrary.org/obo/CHEBI_167664",
                                    "http://purl.obolibrary.org/obo/CHEBI_176105" );
                } );
    }

    /**
     * {@code exact_label} filters the POSITIVE list; {@code negativeEvidence} reports the VERDICT.
     * Passing both must still yield the verdict — the agents send {@code exact_label=true} on every
     * call, so an interaction that swallows the signal means they never receive it at all, while
     * appearing to have asked for it.
     */
    @Test
    public void testExactLabelDoesNotSuppressNegativeEvidence() throws SearchException, TimeoutException {
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MK-8722" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList(
                        new CharacteristicValueObject( "mk-8353", "http://purl.obolibrary.org/obo/CHEBI_167664", "treatment", null ) ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "MK-8722" )
                .queryParam( "suppress_near_matches", "true" )
                .queryParam( "exact_label", "true" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .satisfies( body -> {
                    assertThat( body ).extracting( "data", list( Map.class ) ).isEmpty();
                    assertThat( body ).extracting( "negativeEvidence", map( String.class, Object.class ) )
                            .containsEntry( "solidMatch", false );
                } );
    }

    /**
     * The confident negative must not be claimed when identity matching never ran — a descriptive
     * query keeps its near-matches, so absence of a match there says nothing.
     */
    @Test
    public void testNegativeEvidenceAbsentForDescriptiveQuery() throws SearchException, TimeoutException {
        when( ontologyService.findExperimentsCharacteristicTags( eq( "high fat diet" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList(
                        new CharacteristicValueObject( "high fat diet regimen", "http://example.com/hfd", "treatment", null ) ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "high fat diet" )
                .queryParam( "suppress_near_matches", "true" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .satisfies( body -> {
                    // The near-match survives, and no confident negative is asserted.
                    assertThat( body ).extracting( "data", list( Map.class ) ).hasSize( 1 );
                    assertThat( ( (Map<String, Object>) body ).get( "negativeEvidence" ) ).isNull();
                } );
    }

    /**
     * A ranking strategy must not be able to discard the category promotion. `composite` weights
     * label coverage heavily, so for `FTC` it ranked the MGI gene (whose label IS the query) above
     * the CHEBI compound that only matches via a synonym — silently undoing the preference the
     * caller asked for. The category is a constraint; the strategy is a relevance heuristic.
     */
    /**
     * Reported by CAB 2026-08-11: `H1` is a declared exact synonym of EFO_0003042 ("H1-hESC", 18
     * corpus uses), but the relevance tiers read the hit's LABEL only, so on "h1-hesc" it scored
     * as a prefix match and came back at rank 7 -- behind two CHEBI histamine-receptor ligands
     * with no corpus use at all. A caller cannot act on that: the row it wants is
     * indistinguishable from the noise it has to filter. An exact synonym is an exact match, and
     * now earns the exact tier.
     */
    @Test
    public void testExactSynonymEarnsTheExactTier() throws Exception {
        CharacteristicValueObject ligand = new CharacteristicValueObject( "h1-receptor antagonist",
                "http://purl.obolibrary.org/obo/CHEBI_37955", "treatment", null );
        CharacteristicValueObject hesc = new CharacteristicValueObject( "h1-hesc",
                "http://www.ebi.ac.uk/efo/EFO_0003042", "cell line", null );
        // Order from the ontology puts the ligand first; on labels alone both are mere prefix
        // matches for "H1" and the ligand keeps that lead.
        when( ontologyService.findExperimentsCharacteristicTags( eq( "H1" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( ligand, hesc ) );

        OntologyTerm h1hesc = mock( OntologyTerm.class );
        when( h1hesc.getLabel() ).thenReturn( "H1-hESC" );
        when( h1hesc.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        AnnotationProperty syn = mock( AnnotationProperty.class );
        when( syn.getContents() ).thenReturn( "H1" );
        when( h1hesc.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ) )
                .thenReturn( Collections.singletonList( syn ) );
        when( ontologyService.getTerm( eq( "http://www.ebi.ac.uk/efo/EFO_0003042" ), anyLong(), any() ) )
                .thenReturn( h1hesc );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "H1" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .first()
                .satisfies( top -> {
                    assertThat( top ).containsEntry( "valueUri", "http://www.ebi.ac.uk/efo/EFO_0003042" );
                    // and the caller is told WHY, which is the half it acts on
                    assertThat( top ).containsEntry( "matchedVia", "exact_synonym" );
                } );
    }

    @Test
    public void testLexicalCatalogueHitIsWorthOneTierLessThanAnOntologyHit() throws Exception {
        // The measured shape of the FTC failure, without the string: a flat lexical catalogue
        // (MGI names) carries a row whose LABEL equals the query, so on label exactness alone it
        // took position 0 ahead of every conventional-ontology candidate. gemma-core already ranks
        // these sources below conventional ones; the tiers here used to see only exactness.
        CharacteristicValueObject catalogueExact = new CharacteristicValueObject( "ftc",
                "https://www.informatics.jax.org/strain/MGI:2667754", null, null );
        catalogueExact.setSupplementary( true );
        CharacteristicValueObject ontologyPrefix = new CharacteristicValueObject( "ftc-133 cell",
                "http://purl.obolibrary.org/obo/CLO_0003402", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "FTC" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( catalogueExact, ontologyPrefix ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "FTC" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( data -> {
                    // demoted one tier: exact(0)+1 == prefix(1), and the conventional source wins
                    // the tie, so the ontology term leads
                    assertThat( data.get( 0 ) ).containsEntry( "valueUri",
                            "http://purl.obolibrary.org/obo/CLO_0003402" );
                    // ...and still returned rather than banished. These sources are backups for
                    // names the ontologies lack, so dropping them defeats the reason they load.
                    assertThat( data.get( 1 ) ).containsEntry( "valueUri",
                            "https://www.informatics.jax.org/strain/MGI:2667754" );
                } );
    }

    @Test
    public void testLexicalCatalogueHitAlsoSinksBelowANonExactOntologyHit() throws Exception {
        // The half the two-candidate test above cannot see, pinned from live behaviour rather than
        // from intent. The synonym-exact pass sorts on a BINARY key (exact / not exact), so adding
        // the demotion to it carries a supplementary exact match across the bucket boundary: it
        // ends up behind every non-exact conventional hit, not one tier down. Measured on gemma2
        // 2026-08-13, `FTC` with no category put the MGI row at position 7, below CHEBI rows
        // reached only through a related synonym.
        //
        // This is stronger than the comment at the sort site originally claimed and is kept on
        // purpose -- it is what demotes the catalogue row below the ontology, and it costs nothing
        // measurable (lucene on the 400-pair TUNE fold is identical to three decimals before and
        // after). Guarded so that softening the demotion is a deliberate act with a red test, not
        // a silent side effect of touching either sort.
        CharacteristicValueObject catalogueExact = new CharacteristicValueObject( "ftc",
                "https://www.informatics.jax.org/strain/MGI:2667754", null, null );
        catalogueExact.setSupplementary( true );
        CharacteristicValueObject ontologyBySynonym = new CharacteristicValueObject( "ferroptocide",
                "http://purl.obolibrary.org/obo/CHEBI_173106", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "FTC" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( catalogueExact, ontologyBySynonym ) );
        // ferroptocide names the query only through a RELATED synonym -- a weaker attribution than
        // the catalogue row's preferred-label match, which is the point.
        OntologyTerm ferroptocide = mock( OntologyTerm.class );
        when( ferroptocide.getLabel() ).thenReturn( "ferroptocide" );
        when( ferroptocide.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        AnnotationProperty related = mock( AnnotationProperty.class );
        when( related.getContents() ).thenReturn( "FTC" );
        when( ferroptocide.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym" ) )
                .thenReturn( Collections.singletonList( related ) );
        when( ontologyService.getTerm( eq( "http://purl.obolibrary.org/obo/CHEBI_173106" ), anyLong(), any() ) )
                .thenReturn( ferroptocide );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "FTC" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .satisfies( data -> assertThat( data.get( 0 ) )
                        .as( "a weaker ontology match still precedes an exact catalogue name" )
                        .containsEntry( "valueUri", "http://purl.obolibrary.org/obo/CHEBI_173106" ) );
    }

    @Test
    public void testCategoryPromotionSurvivesTheRankingStrategy() throws Exception {
        CharacteristicValueObject gene = new CharacteristicValueObject( "ftc",
                "https://www.informatics.jax.org/strain/MGI:2667754", "genotype", null );
        CharacteristicValueObject chem = new CharacteristicValueObject( "emtricitabine",
                "http://purl.obolibrary.org/obo/CHEBI_31536", "treatment", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "FTC" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( gene, chem ) );
        // emtricitabine is reachable from "FTC" only through a synonym, so it is solid but scores
        // poorly on label coverage — exactly the shape composite mis-ranks.
        //
        // 🛑 The synonym scope below is HYPOTHETICAL and the test says so on purpose. Live, ChEBI
        // files `FTC` as a RELATED synonym (on ferroptocide) and emtricitabine's string is
        // `(-)-FTC`, so no CHEBI candidate is promotable and this query does NOT behave this way
        // against the real ontology — see AnnotationsWebServiceSolidMatchTest#theRealFtcShapeIsNotSolid.
        // What is under test here is that promotion SURVIVES strategy.rank(), which needs some
        // solid preferred-namespace hit to exist; it is not a claim about FTC.
        OntologyTerm emtricitabine = mock( OntologyTerm.class );
        when( emtricitabine.getLabel() ).thenReturn( "emtricitabine" );
        when( emtricitabine.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        AnnotationProperty syn = mock( AnnotationProperty.class );
        when( syn.getContents() ).thenReturn( "FTC" );
        when( emtricitabine.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ) )
                .thenReturn( Collections.singletonList( syn ) );
        when( ontologyService.getTerm( eq( "http://purl.obolibrary.org/obo/CHEBI_31536" ), anyLong(), any() ) )
                .thenReturn( emtricitabine );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "FTC" )
                .queryParam( "category", "treatment" )
                .queryParam( "rank", "composite" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .first()
                .satisfies( top -> assertThat( top )
                        .containsEntry( "valueUri", "http://purl.obolibrary.org/obo/CHEBI_31536" ) );
    }

    /**
     * The exact tier has the same problem the category promotion had: it is established on the
     * candidate list and then thrown away by {@code strategy.rank()}, which re-sorts everything on
     * its own score. Measured on frink 2026-08-16 before the fix, {@code Myelopathy} returned
     * {@code spinal cord injury} above {@code myelopathy} — the latter matched on its PREFERRED
     * LABEL and lost anyway, because the former carries far more corpus usage and composite weights
     * usage. An exact match losing to a lexical neighbour is not a ranking preference, it is the
     * ranking being wrong.
     */
    @Test
    public void testExactMatchTierSurvivesTheRankingStrategy() throws Exception {
        // The neighbour reaches the query through a RELATED synonym, so it scores full token
        // coverage like the exact hit does -- coverage cannot separate them -- and it carries heavy
        // corpus usage, which is what lets composite put it on top. RELATED is deliberately not an
        // exact attribution, so only the tier distinguishes these two.
        CharacteristicValueObject neighbour = new CharacteristicValueObject( "spinal cord injury",
                "http://purl.obolibrary.org/obo/MONDO_0002542", "disease", null );
        // The answer: the query IS its label, and it has no corpus usage to trade on.
        CharacteristicValueObject exact = new CharacteristicValueObject( "myelopathy",
                "http://purl.obolibrary.org/obo/HP_0002196", "phenotype", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "myelopathy" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( neighbour, exact ) );

        OntologyTerm neighbourTerm = mock( OntologyTerm.class );
        when( neighbourTerm.getLabel() ).thenReturn( "spinal cord injury" );
        when( neighbourTerm.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        AnnotationProperty related = mock( AnnotationProperty.class );
        when( related.getContents() ).thenReturn( "myelopathy" );
        when( neighbourTerm.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym" ) )
                .thenReturn( Collections.singletonList( related ) );
        when( ontologyService.getTerm( eq( "http://purl.obolibrary.org/obo/MONDO_0002542" ), anyLong(), any() ) )
                .thenReturn( neighbourTerm );

        // Corpus usage for the neighbour only, which is what composite's usage term rewards.
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( "http://purl.obolibrary.org/obo/MONDO_0002542", 60L ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "myelopathy" )
                .queryParam( "rank", "composite" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .first()
                .satisfies( top -> assertThat( top )
                        .as( "an exact label match must not lose to a high-usage token overlap" )
                        .containsEntry( "valueUri", "http://purl.obolibrary.org/obo/HP_0002196" ) );
    }

    /**
     * An excluded namespace leaves {@code data} but is REPORTED, not deleted — a gene symbol
     * answered with the disease it causes is the measured failure, and an over-firing rule has to
     * be visible rather than silent.
     */
    @Test
    public void testCategoryExclusionRemovesAndReportsOutOfCategoryHits() throws Exception {
        CharacteristicValueObject disease = new CharacteristicValueObject( "retinoblastoma",
                "http://purl.obolibrary.org/obo/MONDO_0008380", "disease", null );
        CharacteristicValueObject genotype = new CharacteristicValueObject( "RB1",
                "http://gemma.msl.ubc.ca/ont/TGEMO_00166", "genotype", null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "RB1" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( disease, genotype ) );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "RB1" )
                .queryParam( "category", "genotype" )
                .queryParam( "includeGenes", "false" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .satisfies( body -> {
                    assertThat( body ).extracting( "data", list( Map.class ) )
                            .allSatisfy( row -> assertThat( ( String ) ( ( Map<?, ?> ) row ).get( "valueUri" ) )
                                    .doesNotContain( "MONDO_" ) );
                    //noinspection unchecked
                    List<Map<String, Object>> ruled = (List<Map<String, Object>>)
                            ( (Map<String, Object>) ( (Map<String, Object>) body ).get( "negativeEvidence" ) ).get( "ruledOut" );
                    assertThat( ruled ).anySatisfy( r -> {
                        assertThat( r.get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/MONDO_0008380" );
                        assertThat( r.get( "reason" ) ).isEqualTo( "out_of_category" );
                    } );
                } );
    }

    @Test
    public void testSearchAnnotationsBatchResolvesEachLabelIndependently() throws SearchException, TimeoutException {
        CharacteristicValueObject diabetes = new CharacteristicValueObject( "diabetes", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        CharacteristicValueObject liver = new CharacteristicValueObject( "liver", "http://example.com/liver", "organism part", "http://example.com/part" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( diabetes ) );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "liver" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( liver ) );

        String body = "{\"queries\":[{\"query\":\"diabetes\",\"category\":\"disease\"},"
                + "{\"query\":\"liver\",\"category\":\"organism part\"}],\"includeGenes\":false}";
        assertThat( target( "/annotations/search/batch" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( entries -> {
                    // Each item resolves to its own list — NOT unioned — and echoes its query for correlation.
                    assertThat( entries.get( 0 ) )
                            .containsEntry( "query", "diabetes" )
                            .containsEntry( "error", null );
                    assertThat( ( List<?> ) entries.get( 0 ).get( "results" ) ).hasSize( 1 );
                    assertThat( entries.get( 1 ) )
                            .containsEntry( "query", "liver" );
                    assertThat( ( List<?> ) entries.get( 1 ).get( "results" ) ).hasSize( 1 );
                } );
        // includeGenes=false → the gene fan-out is skipped for every item.
        verify( geneService, never() ).findByOfficialSymbol( anyString() );
    }

    @Test
    public void testSearchAnnotationsBatchEmptyReturns400() {
        assertThat( target( "/annotations/search/batch" ).request().post( Entity.json( "{\"queries\":[]}" ) ) )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void testSearchAnnotationsIncludeGenesFalseSkipsGeneFanout() throws SearchException, TimeoutException {
        when( ontologyService.findExperimentsCharacteristicTags( eq( "stat5b" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );

        // Default (includeGenes=true): the three gene probes fire.
        assertThat( target( "/annotations/search" ).queryParam( "query", "stat5b" ).request().get() )
                .hasStatus( Response.Status.OK );
        verify( geneService, atLeastOnce() ).findByOfficialSymbol( "stat5b" );

        // includeGenes=false: none of the gene probes fire.
        reset( geneService );
        assertThat( target( "/annotations/search" ).queryParam( "query", "stat5b" )
                .queryParam( "includeGenes", "false" ).request().get() )
                .hasStatus( Response.Status.OK );
        verify( geneService, never() ).findByOfficialSymbol( anyString() );
        verify( geneService, never() ).findByOfficialName( anyString() );
        verify( geneService, never() ).findByAlias( anyString() );
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

        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( "http://example.com/diabetes", 2L ) );

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
        verify( characteristicService ).countExperimentsByUris(
                eq( Collections.singleton( "http://example.com/diabetes" ) ),
                eq( true ), eq( true ), eq( true ), isNull(), eq( Collections.emptySet() ) );
    }

    @Test
    public void testGetAnnotationTermIncludesSynonymsAndAlternativeIds() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/diabetes" );
        when( term.getLabel() ).thenReturn( "diabetes" );
        AnnotationProperty exact = mock( AnnotationProperty.class );
        when( exact.getContents() ).thenReturn( "diabetes mellitus" );
        AnnotationProperty generic = mock( AnnotationProperty.class );
        when( generic.getContents() ).thenReturn( "DM" );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasExactSynonym" ) )
                .thenReturn( Collections.singletonList( exact ) );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasSynonym" ) )
                .thenReturn( Collections.singletonList( generic ) );
        when( term.getAlternativeIds() ).thenReturn( Arrays.asList( "OMIM:125853", "DOID:9351" ) );
        AnnotationProperty xrefMesh = mock( AnnotationProperty.class );
        when( xrefMesh.getContents() ).thenReturn( "MESH:D003920" );
        AnnotationProperty xrefUmls = mock( AnnotationProperty.class );
        when( xrefUmls.getContents() ).thenReturn( "UMLS:C0011860" );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasDbXref" ) )
                .thenReturn( Arrays.asList( xrefMesh, xrefUmls ) );
        when( ontologyService.getTerm( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getVersion( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( "2024-05-29" );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        Response response = target( "/annotations/term" ).queryParam( "uri", "http://example.com/diabetes" ).request().get();
        assertThat( response ).hasStatus( Response.Status.OK );
        assertThat( response )
                .entity()
                .hasFieldOrPropertyWithValue( "data.ontologyVersion", "2024-05-29" );
        assertThat( response )
                .entity()
                .extracting( "data.synonyms", list( Map.class ) )
                .satisfiesExactlyInAnyOrder(
                        s -> assertThat( s ).containsEntry( "value", "diabetes mellitus" ).containsEntry( "type", "exact_synonym" ),
                        // generic hasSynonym collapses to related_synonym
                        s -> assertThat( s ).containsEntry( "value", "DM" ).containsEntry( "type", "related_synonym" ) );
        assertThat( response )
                .entity()
                .extracting( "data.alternativeIds", list( String.class ) )
                .containsExactlyInAnyOrder( "OMIM:125853", "DOID:9351" );
        assertThat( response )
                .entity()
                .extracting( "data.dbXrefs", list( String.class ) )
                .containsExactlyInAnyOrder( "MESH:D003920", "UMLS:C0011860" );
    }

    /**
     * 🛑 Literature citations are withheld from {@code dbXrefs} unless asked for, and always counted.
     *
     * <p>Measured on {@code CHEBI_45783 imatinib}: 63 cross-references, of which 51 are {@code pubmed:}
     * and every identifier that names a record — {@code cas}, {@code drugbank}, {@code drugcentral},
     * {@code kegg.drug} — appears exactly once. The citations push the clickable ones off any bounded
     * view, so uib was capping the list client-side and every other consumer would have written the
     * same rule.</p>
     *
     * <p>Counted rather than dropped silently: a caller has to be able to tell "cites nothing" from
     * "cites fifty-one things you did not ask for".</p>
     */
    @Test
    public void testGetAnnotationTermWithholdsLiteratureCitationsUnlessAsked() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/CHEBI_45783";
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( uri );
        when( term.getLabel() ).thenReturn( "imatinib" );
        List<AnnotationProperty> xrefs = new ArrayList<>();
        for ( String x : Arrays.asList( "drugbank:DB00619", "cas:152459-95-5", "pubmed:22891806",
                "pubmed:17457302", "doi:10.1021/jm9903837" ) ) {
            AnnotationProperty ap = mock( AnnotationProperty.class );
            when( ap.getContents() ).thenReturn( x );
            xrefs.add( ap );
        }
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#hasDbXref" ) ).thenReturn( xrefs );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        Response byDefault = target( "/annotations/term" ).queryParam( "uri", uri ).request().get();
        assertThat( byDefault ).hasStatus( Response.Status.OK );
        assertThat( byDefault ).entity()
                .extracting( "data.dbXrefs", list( String.class ) )
                .as( "the identifiers a curator would click, and nothing else" )
                .containsExactlyInAnyOrder( "drugbank:DB00619", "cas:152459-95-5" );
        assertThat( byDefault ).entity()
                .hasFieldOrPropertyWithValue( "data.citationXrefCount", 3 );

        Response asked = target( "/annotations/term" ).queryParam( "uri", uri )
                .queryParam( "includeCitationXrefs", "true" ).request().get();
        assertThat( asked ).entity()
                .extracting( "data.dbXrefs", list( String.class ) )
                .hasSize( 5 );
        assertThat( asked ).entity()
                .as( "the count reports what there is, whether or not it was returned" )
                .hasFieldOrPropertyWithValue( "data.citationXrefCount", 3 );
    }

    /**
     * The EFO case that motivated the field (uib, 2026-08-16): {@code EFO_0000408 obsolete_disease}
     * names {@code MONDO_0000001} as its successor, and EFO's OWL carries a label for that class, so
     * no second lookup is needed.
     */
    @Test
    public void testGetAnnotationTermExposesReplacementForObsoleteTerm() throws TimeoutException {
        String uri = "http://www.ebi.ac.uk/efo/EFO_0000408";
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( uri );
        when( term.getLabel() ).thenReturn( "obsolete_disease" );
        when( term.isObsolete() ).thenReturn( true );
        AnnotationProperty replacedBy = mock( AnnotationProperty.class );
        when( replacedBy.getValueUri() ).thenReturn( "http://purl.obolibrary.org/obo/MONDO_0000001" );
        when( replacedBy.getContents() ).thenReturn( "disease" );
        when( term.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0100001" ) ).thenReturn( replacedBy );
        AnnotationProperty obsoletedIn = mock( AnnotationProperty.class );
        when( obsoletedIn.getContents() ).thenReturn( "3.88.0" );
        when( term.getAnnotation( "http://www.ebi.ac.uk/efo/obsoleted_in_version" ) ).thenReturn( obsoletedIn );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getVersion( eq( uri ), anyLong(), any() ) ).thenReturn( "3.92.0" );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/term" ).queryParam( "uri", uri ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.obsolete", true )
                .hasFieldOrPropertyWithValue( "data.termReplacedBy", "http://purl.obolibrary.org/obo/MONDO_0000001" )
                .hasFieldOrPropertyWithValue( "data.termReplacedByLabel", "disease" )
                .hasFieldOrPropertyWithValue( "data.obsoletedInVersion", "3.88.0" )
                .hasFieldOrPropertyWithValue( "data.ontologyVersion", "3.92.0" );

        // the deprecating model had the label, so no successor lookup was needed
        verify( ontologyService, times( 1 ) ).getTerm( anyString(), anyLong(), any() );
    }

    /**
     * Same axiom written as a literal rather than an {@code rdf:resource} — the OBO→OWL conversions
     * disagree on this, and reading only the resource form empties the field for whole ontologies.
     * With no in-model label, the successor's label comes from resolving it through the service.
     */
    @Test
    public void testGetAnnotationTermResolvesLiteralReplacementAndItsLabel() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/CLO_0000021";
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( uri );
        when( term.getLabel() ).thenReturn( "obsolete immortal cat cell line cell" );
        when( term.isObsolete() ).thenReturn( true );
        AnnotationProperty replacedBy = mock( AnnotationProperty.class );
        // literal-valued: no value URI, and getContents() hands back the IRI itself, not a label
        when( replacedBy.getValueUri() ).thenReturn( null );
        when( replacedBy.getContents() ).thenReturn( "http://purl.obolibrary.org/obo/CLO_0000457" );
        when( term.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0100001" ) ).thenReturn( replacedBy );
        OntologyTerm successor = mock( OntologyTerm.class );
        when( successor.getLabel() ).thenReturn( "immortal cat cell line cell" );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getTerm( eq( "http://purl.obolibrary.org/obo/CLO_0000457" ), anyLong(), any() ) )
                .thenReturn( successor );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/term" ).queryParam( "uri", uri ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.termReplacedBy", "http://purl.obolibrary.org/obo/CLO_0000457" )
                .hasFieldOrPropertyWithValue( "data.termReplacedByLabel", "immortal cat cell line cell" )
                // CLO declares no release stamp; null rather than invented
                .hasFieldOrPropertyWithValue( "data.obsoletedInVersion", null );
    }

    /** A CURIE-valued literal ({@code CLO:0000457}) is canonicalised, not dropped. */
    @Test
    public void testGetAnnotationTermExpandsCurieValuedReplacement() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/CLO_0000021";
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( uri );
        when( term.isObsolete() ).thenReturn( true );
        AnnotationProperty replacedBy = mock( AnnotationProperty.class );
        when( replacedBy.getContents() ).thenReturn( "CLO:0000457" );
        when( term.getAnnotation( "http://purl.obolibrary.org/obo/IAO_0100001" ) ).thenReturn( replacedBy );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/term" ).queryParam( "uri", uri ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .hasFieldOrPropertyWithValue( "data.termReplacedBy", "http://purl.obolibrary.org/obo/CLO_0000457" );
    }

    /** A term that was split rather than merged names candidates, not a replacement. */
    @Test
    public void testGetAnnotationTermExposesConsiderCandidates() throws TimeoutException {
        String uri = "http://www.ebi.ac.uk/efo/EFO_0000001";
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( uri );
        when( term.isObsolete() ).thenReturn( true );
        AnnotationProperty first = mock( AnnotationProperty.class );
        when( first.getValueUri() ).thenReturn( "http://www.ebi.ac.uk/efo/EFO_0000002" );
        when( first.getContents() ).thenReturn( "first candidate" );
        AnnotationProperty second = mock( AnnotationProperty.class );
        when( second.getValueUri() ).thenReturn( "http://www.ebi.ac.uk/efo/EFO_0000003" );
        when( term.getAnnotations( "http://www.geneontology.org/formats/oboInOwl#consider" ) )
                .thenReturn( Arrays.asList( first, second ) );
        when( ontologyService.getTerm( eq( uri ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        Response response = target( "/annotations/term" ).queryParam( "uri", uri ).request().get();
        assertThat( response ).hasStatus( Response.Status.OK );
        assertThat( response )
                .entity()
                .hasFieldOrPropertyWithValue( "data.termReplacedBy", null );
        assertThat( response )
                .entity()
                .extracting( "data.consider", list( Map.class ) )
                .satisfiesExactlyInAnyOrder(
                        c -> assertThat( c ).containsEntry( "uri", "http://www.ebi.ac.uk/efo/EFO_0000002" )
                                .containsEntry( "label", "first candidate" ),
                        // no label in the model; the URI is the identity, the label is decoration
                        c -> assertThat( c ).containsEntry( "uri", "http://www.ebi.ac.uk/efo/EFO_0000003" )
                                .containsEntry( "label", null ) );
    }

    /**
     * A live term is never probed for obsolescence axioms — it declares none, and probing would cost a
     * successor lookup per call for a field that is always null.
     */
    @Test
    public void testGetAnnotationTermSkipsObsolescenceLookupForLiveTerm() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/diabetes" );
        when( term.getLabel() ).thenReturn( "diabetes" );
        when( term.isObsolete() ).thenReturn( false );
        when( ontologyService.getTerm( eq( "http://example.com/diabetes" ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        Response response = target( "/annotations/term" ).queryParam( "uri", "http://example.com/diabetes" ).request().get();
        assertThat( response ).hasStatus( Response.Status.OK );
        assertThat( response )
                .entity()
                .hasFieldOrPropertyWithValue( "data.termReplacedBy", null )
                .hasFieldOrPropertyWithValue( "data.termReplacedByLabel", null )
                .hasFieldOrPropertyWithValue( "data.obsoletedInVersion", null );
        assertThat( response )
                .entity()
                .extracting( "data.consider", list( Map.class ) )
                .isEmpty();

        verify( term, never() ).getAnnotation( anyString() );
        verify( term, never() ).getAnnotations( "http://www.geneontology.org/formats/oboInOwl#consider" );
    }

    @Test
    public void testGetAnnotationTermReportsZeroWhenNoExperimentsMatch() throws TimeoutException {
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/orphan" );
        when( term.getLabel() ).thenReturn( "orphan" );
        when( ontologyService.getTerm( eq( "http://example.com/orphan" ), anyLong(), any() ) ).thenReturn( term );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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

        verify( characteristicService, never() ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
    }

    @Test
    public void testGetAnnotationTermNotFound() throws TimeoutException {
        when( ontologyService.getTerm( eq( "http://example.com/missing" ), anyLong(), any() ) ).thenReturn( null );

        assertThat( target( "/annotations/term" ).queryParam( "uri", "http://example.com/missing" ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );

        verify( ontologyService ).getTerm( eq( "http://example.com/missing" ), anyLong(), any() );
        verifyNoMoreInteractions( ontologyService );
        verify( characteristicService, never() ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
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

        verify( characteristicService, never() ).countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() );
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( raw );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsLimitBelow1Returns400() throws SearchException, TimeoutException {
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "limit", "0" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() );
    }

    @Test
    public void testSearchAnnotationsAttributesPreferredLabelMatch() throws SearchException, TimeoutException {
        // Hit's preferred label EQUALS the query (case- + whitespace-normalised) →
        // matchedVia=preferred_label, matchedText=label. No synonyms wired on the term mock;
        // the back-compute fast-paths on the preferred-label match.
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "Diabetes mellitus", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes mellitus" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "pancreatic cell" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/CL_0000169" );
        when( term.getLabel() ).thenReturn( "type b pancreatic cell" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "ammon horn" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "alzhei" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( ontologyService.getDefinition( anyString(), anyLong(), any() ) ).thenReturn( null );
        OntologyTerm term = mock( OntologyTerm.class );
        when( term.getUri() ).thenReturn( "http://example.com/DOID_10652" );
        when( term.getLabel() ).thenReturn( "alzheimer's disease" );
        when( term.getAnnotations( anyString() ) ).thenReturn( Collections.emptyList() );
        when( ontologyService.getTerm( anyString(), anyLong(), any() ) ).thenReturn( term );
        when( ontologyService.getParents( anySet(), eq( true ), eq( true ), anyLong(), any() ) )
                .thenReturn( Collections.emptySet() );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "haptoglobin" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
                        .containsEntry( "matchedVia", "search:gene_name" ) );
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "tp53" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
                        .containsEntry( "matchedVia", "search:gene_alias" ) );
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "il10" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( ontologyHit ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "HP" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.emptyList() );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "HP" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 );
    }

    @Test
    public void testSearchAnnotationsExactOntologyLabelOutranksGeneAliasCollision() throws SearchException, TimeoutException {
        // The "age" regression: NCBI Renbp carries the historical alias "AGE", so the gene
        // fan-out's findByAlias probe returned it. Before this fix the alias-matched gene was
        // unconditionally prepended above the tier-sorted ontology hits, so a resolver-style
        // caller inspecting the top hit saw value="Renbp renin binding protein" and concluded
        // "no high-confidence hit for the bare label 'age'" — even though PATO:0000011 and
        // EFO:0000246 both carry preferred_label="age". Alias-only hits must now land BELOW
        // the tier-0 ontology rows.
        CharacteristicValueObject pato = new CharacteristicValueObject(
                "age", "http://purl.obolibrary.org/obo/PATO_0000011", null, null );
        CharacteristicValueObject efo = new CharacteristicValueObject(
                "age", "http://www.ebi.ac.uk/efo/EFO_0000246", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "age" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( pato, efo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );
        ubic.gemma.model.genome.Gene renbp = mock( ubic.gemma.model.genome.Gene.class );
        when( renbp.getId() ).thenReturn( 19703L );
        when( renbp.getOfficialSymbol() ).thenReturn( "Renbp" );
        when( renbp.getOfficialName() ).thenReturn( "renin binding protein" );
        when( renbp.getNcbiGeneId() ).thenReturn( 19703 );
        when( geneService.findByOfficialSymbol( "age" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByOfficialName( "age" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByAlias( "age" ) ).thenReturn( Collections.singletonList( renbp ) );

        assertThat( target( "/annotations/search" ).queryParam( "query", "age" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 3 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/PATO_0000011" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0000246" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 2 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.org/commons/record/ncbi_gene/19703" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 2 ) ).get( "matchedVia" ) ).isEqualTo( "search:gene_alias" );
                } );
    }

    @Test
    public void testSearchAnnotationsPopulatesPriorCategoriesFromCorpusHistory() throws SearchException, TimeoutException {
        // Resolver tiebreaker signal: a URI's prior-category breakdown should land on the wire
        // so a downstream resolver can choose "cell line" when the URI has been tagged 14× as
        // a cell line and 1× as a protein, regardless of which ontology label happened to match.
        CharacteristicValueObject clo = new CharacteristicValueObject(
                "mec-2 cell", "http://purl.obolibrary.org/obo/CLO_0037182", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MEC-2" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( clo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );
        Map<String, Map<String, Long>> priorByUri = new HashMap<>();
        Map<String, Long> mec2Categories = new HashMap<>();
        mec2Categories.put( "cell line", 14L );
        mec2Categories.put( "protein", 1L );
        priorByUri.put( "http://purl.obolibrary.org/obo/CLO_0037182", mec2Categories );
        when( characteristicService.findEeCountsByUriGroupedByCategory( anySet() ) ).thenReturn( priorByUri );

        assertThat( target( "/annotations/search" ).queryParam( "query", "MEC-2" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> priorCategories = ( Map<String, Integer> ) a.get( "priorCategories" );
                    assertThat( priorCategories ).containsEntry( "cell line", 14 ).containsEntry( "protein", 1 );
                } );
    }

    @Test
    public void testSearchAnnotationsHyphenInsensitiveMec2FindsCloCellLine() throws SearchException, TimeoutException {
        // The MEC2 / MEC-2 regression: bro 1's resolver hit /annotations/search?query=MEC2 and got
        // back EFO_0006285 ("mec2", a protein), missing CLO_0037182 ("mec-2 cell") entirely because
        // the CLO label has both a hyphen AND the " cell" suffix the query lacks. With the canonical-
        // form tier match (lowercase + strip cell suffix + strip hyphens), both labels canonicalise
        // to "mec2" so both reach tier 0; the CLO-preference tiebreaker then promotes CLO ahead of
        // EFO because the strip-and-hyphen normalisation earned the match.
        CharacteristicValueObject efo = new CharacteristicValueObject( "mec2", "http://www.ebi.ac.uk/efo/EFO_0006285", null, null );
        CharacteristicValueObject clo = new CharacteristicValueObject( "mec-2 cell", "http://purl.obolibrary.org/obo/CLO_0037182", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MEC2" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "MEC2" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CLO_0037182" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0006285" );
                } );
    }

    @Test
    public void testSearchAnnotationsHyphenInsensitiveMec2DashFindsEfoBareLabel() throws SearchException, TimeoutException {
        // Reverse direction of the MEC2 case: query has the hyphen, EFO has the bare form. The
        // ranker must still reach tier 0 on EFO so a resolver that received MEC-2 from a curator
        // can see both candidates. CLO still leads (canonical-form match earned via strip);
        // EFO follows.
        CharacteristicValueObject efo = new CharacteristicValueObject( "mec2", "http://www.ebi.ac.uk/efo/EFO_0006285", null, null );
        CharacteristicValueObject clo = new CharacteristicValueObject( "mec-2 cell", "http://purl.obolibrary.org/obo/CLO_0037182", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MEC-2" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "MEC-2" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CLO_0037182" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0006285" );
                } );
    }

    @Test
    public void testSearchAnnotationsExactLabelFilterRespectsHyphenAndCellSuffixCanonicalForm() throws SearchException, TimeoutException {
        // The resolver-style call: ?query=MEC2&exact_label=true&prefixes=CLO_,CL_. Before this
        // fix the exact_label filter applied literal toLowerCase().equals(); "mec-2 cell" !=
        // "mec2" so the filter dropped CLO_0037182 and the resolver got zero hits. The filter
        // must apply the same canonical form the tier function uses.
        CharacteristicValueObject clo = new CharacteristicValueObject( "mec-2 cell", "http://purl.obolibrary.org/obo/CLO_0037182", null, null );
        CharacteristicValueObject efo = new CharacteristicValueObject( "mec2", "http://www.ebi.ac.uk/efo/EFO_0006285", null, null );
        CharacteristicValueObject unrelated = new CharacteristicValueObject( "mec2-related thing", "http://example.com/x", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "MEC2" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo, unrelated ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" )
                .queryParam( "query", "MEC2" )
                .queryParam( "exact_label", "true" )
                .request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    // CLO first (canonical-form strip earned the match); EFO follows; the
                    // "mec2-related thing" row drops because its canonical form is
                    // "mec2related thing", not "mec2".
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.obolibrary.org/obo/CLO_0037182" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://www.ebi.ac.uk/efo/EFO_0006285" );
                } );
    }

    @Test
    public void testSearchAnnotationsCanonicaliseDoesNotRegressNonHyphenatedQueries() throws SearchException, TimeoutException {
        // Guard: a non-identifier query like "diabetes" must rank the same as before. The
        // canonical-form check passes through cleanly when no hyphens or cell suffix are
        // involved on either side.
        CharacteristicValueObject diabetes = new CharacteristicValueObject(
                "diabetes mellitus", "http://www.ebi.ac.uk/efo/EFO_0000400", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( diabetes ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "valueUri", "http://www.ebi.ac.uk/efo/EFO_0000400" ) );
    }

    @Test
    public void testSearchAnnotationsSymbolMatchStillPrependsAboveOntologyHits() throws SearchException, TimeoutException {
        // Regression guard: an exact-symbol gene hit MUST still lead the response so a curator
        // typing a brand-new gene's symbol sees it ahead of any incidental ontology substring
        // match. Only alias-only hits got demoted.
        ubic.gemma.model.genome.Gene stat5b = mock( ubic.gemma.model.genome.Gene.class );
        when( stat5b.getId() ).thenReturn( 6777L );
        when( stat5b.getOfficialSymbol() ).thenReturn( "STAT5B" );
        when( stat5b.getOfficialName() ).thenReturn( "signal transducer and activator of transcription 5B" );
        when( stat5b.getNcbiGeneId() ).thenReturn( 6777 );
        when( geneService.findByOfficialSymbol( "STAT5B" ) ).thenReturn( Collections.singletonList( stat5b ) );
        when( geneService.findByOfficialName( "STAT5B" ) ).thenReturn( Collections.emptyList() );
        when( geneService.findByAlias( "STAT5B" ) ).thenReturn( Collections.emptyList() );
        // Incidental ontology hit so the test exercises the prepend ordering.
        CharacteristicValueObject incidental = new CharacteristicValueObject(
                "STAT5B related thing", "http://example.com/foo", null, null );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "STAT5B" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( incidental ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "STAT5B" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 2 )
                .satisfies( hits -> {
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "valueUri" ) ).isEqualTo( "http://purl.org/commons/record/ncbi_gene/6777" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 0 ) ).get( "matchedVia" ) ).isEqualTo( "search:gene_symbol" );
                    assertThat( ( ( Map<?, ?> ) hits.get( 1 ) ).get( "valueUri" ) ).isEqualTo( "http://example.com/foo" );
                } );
    }

    @Test
    public void testSearchAnnotationsAttributesSynonymMatch() throws SearchException, TimeoutException {
        // Hit's preferred label ("hippocampus") does NOT equal the query ("ammon's horn"); the
        // exact-synonym property does ("Ammon's horn", normalised). Back-compute must pick
        // exact_synonym + surface the matching synonym text.
        CharacteristicValueObject hit = new CharacteristicValueObject(
                "hippocampus", "http://example.com/UBERON_0002421", "organism part", "http://example.com/organism_part" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "ammon's horn" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
    public void testSearchAnnotationsLeavesEnrichmentNullForPostTopN() throws SearchException, TimeoutException {
        // Hits beyond the 25-deep enrichment window carry the lazy-load sentinel on the fields
        // that need an ontology lookup — definition and parents — even when the limit lets them
        // through.
        //
        // matchedVia is NOT one of those any more: label-level attribution is pure string work on
        // the row itself, so it is computed for every row regardless of the enrichment window (see
        // the free-text test below). The tail is null here because these rows are labelled
        // "term-N" and the query is "diabetes" — no label relationship — NOT because they missed
        // enrichment. Change the fixture labels and the tail would legitimately be attributed.
        List<CharacteristicValueObject> raw = new ArrayList<>();
        for ( int i = 0; i < 30; i++ ) {
            // Zero-padded suffix so URI lex sort matches numeric order — the endpoint
            // canonicalises hits by URI ASC before ranking, and un-padded t0..t59 would
            // shuffle (t10 sorts before t2 lex-wise) and break position-based assertions.
            raw.add( new CharacteristicValueObject( "term-" + i, String.format( "http://example.com/t%03d", i ), "disease", "http://example.com/disease" ) );
        }
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
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
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
                    // 25..29: the ontology-backed fields stay on the lazy-load sentinel.
                    for ( int i = 25; i < 30; i++ ) {
                        Map<?, ?> hit = ( Map<?, ?> ) hits.get( i );
                        assertThat( hit.get( "definition" ) )
                                .as( "post-top-25 hit %d should carry null definition", i ).isNull();
                        assertThat( hit.get( "parents" ) )
                                .as( "post-top-25 hit %d should carry null parents", i ).isNull();
                        // Null for want of any label relationship, not for want of enrichment.
                        assertThat( hit.get( "matchedVia" ) )
                                .as( "post-top-25 hit %d has no label relationship to the query", i ).isNull();
                        assertThat( hit.get( "matchedText" ) )
                                .as( "post-top-25 hit %d should carry null matchedText", i ).isNull();
                    }
                } );
    }

    @Test
    public void testSearchAnnotationsAttributesAFreeTextRowFromItsOwnLabel() throws SearchException, TimeoutException {
        // A curator's ungrounded tag: valueUri null, because the string was typed under a category
        // without being bound to a term. Enrichment keys on URI, so such a row can never be
        // enriched — and attribution used to be gated on having been enriched, so it reported
        // matchedVia=null however exactly its label matched. A client filtering on equality tiers
        // reads null as "weak" and discards, which silently demoted the row.
        //
        // Label-level attribution needs no ontology and no URI. The row says its own label IS the
        // query, and that is worth reporting. It stays ungrounded either way: valueUri is null on
        // the wire, which is the field that answers "can I adopt this" (CAB confirmed their
        // adoption gate reads valueUri, never matchedVia — handoff 2026-08-10).
        CharacteristicValueObject freeText = new CharacteristicValueObject(
                "N2a", null, "cell line", "http://purl.obolibrary.org/obo/CLO_0000031" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "N2a" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( freeText ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.emptyMap() );

        assertThat( target( "/annotations/search" ).queryParam( "query", "N2a" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a )
                        .containsEntry( "matchedVia", "preferred_label" )
                        .containsEntry( "matchedText", "N2a" )
                        // Still ungrounded, and still says so on the field that means that.
                        .containsEntry( "valueUri", null ) );
    }

    @Test
    public void testSearchAnnotationsUnknownRankReturns400() throws SearchException, TimeoutException {
        // No need to mock anything — the rank= validator fires before ontology lookup.
        assertThat( target( "/annotations/search" )
                .queryParam( "query", "diabetes" )
                .queryParam( "rank", "no-such-strategy" )
                .request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() );
    }

    /**
     * Counting one experiment once, however many annotation rows or levels carry the term, is now
     * the database's job — {@code countExperimentsByUris} does it with a {@code count(distinct ...)}
     * over the union of URI columns, so there is no per-class map left for this layer to collapse.
     * The dedup itself is pinned by
     * {@code CharacteristicDaoTest#testCountExperimentsByUrisCountsAnExperimentOnceAcrossColumns},
     * which exercises it against a real schema; what remains testable here is that the endpoint
     * asks for exactly the candidate URIs and reports the tally it is handed, unmodified.
     */
    @Test
    public void testSearchAnnotationsReportsTheTallyItIsGiven() throws SearchException, TimeoutException {
        CharacteristicValueObject hit = new CharacteristicValueObject( "diabetes", "http://example.com/diabetes", "disease", "http://example.com/disease" );
        when( ontologyService.findExperimentsCharacteristicTags( eq( "diabetes" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Collections.singletonList( hit ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
                .thenReturn( Collections.singletonMap( "http://example.com/diabetes", 1L ) );

        assertThat( target( "/annotations/search" ).queryParam( "query", "diabetes" ).queryParam( "rank", "composite" ).request().get() )
                .hasStatus( Response.Status.OK )
                .entity()
                .extracting( "data", list( Map.class ) )
                .hasSize( 1 )
                .first()
                .satisfies( a -> assertThat( a ).containsEntry( "usageCount", 1 ) );

        verify( characteristicService ).countExperimentsByUris(
                eq( Collections.singleton( "http://example.com/diabetes" ) ),
                eq( true ), eq( true ), eq( true ), isNull(), eq( Collections.emptySet() ) );
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "stable" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( zeta, mike, alpha ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "myeloid" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( mixed );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        verify( ontologyService, never() ).findExperimentsCharacteristicTags( anyString(), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() );
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "scoped" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( mixed );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "A549" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "A549 cell" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, clo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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
        when( ontologyService.findExperimentsCharacteristicTags( eq( "lung cancer" ), anyInt(), anyBoolean(), anyBoolean(), anyLong(), any() ) )
                .thenReturn( Arrays.asList( efo, mondo ) );
        when( characteristicService.countExperimentsByUris( anySet(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anySet() ) )
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

    /*
     * supportingEvidence on AnnotationDto -- the agent-writeback path.
     *
     * PUT /datasets/{id}/annotations could already carry evidence but emits a single aggregate event;
     * these two endpoints emit per-row Tag{Added,Removed}Event, which is what agent writeback needs.
     * Before this, the choice was attribution or evidence. See
     * handoffs/CAB_ASK_2026_08_12_CARRY_SUPPORTING_EVIDENCE_ON_ANNOTATION_DTO.md.
     */

    private static final String EVIDENCE_JSON =
            "[{\"quote\":\"Male C57BL/6J mice (8 weeks) were used throughout.\","
                    + "\"source\":\"paper\",\"location\":\"Methods, para 1\"}]";

    @Test
    @WithMockUser
    public void testAddDatasetAnnotationCarriesSupportingEvidence() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any() ) )
                .thenAnswer( a -> a.getArgument( 1 ) );
        String body = "{\"category\":\"strain\",\"value\":\"C57BL/6J\","
                + "\"valueUri\":\"http://www.ebi.ac.uk/efo/EFO_0004472\",\"evidenceCode\":\"IC\","
                + "\"supportingEvidence\":" + EVIDENCE_JSON + "}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        ArgumentCaptor<Characteristic> captor = ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        assertThat( captor.getValue().getSupportingEvidence() )
                .contains( "\"quote\":\"Male C57BL/6J mice (8 weeks) were used throughout.\"" )
                .contains( "\"source\":\"paper\"" )
                .contains( "\"location\":\"Methods, para 1\"" );
    }

    @Test
    @WithMockUser
    public void testAddDatasetAnnotationWithoutEvidenceLeavesSupportingEvidenceNull() {
        // Must be null, not "" or "[]" -- a blank would be indistinguishable from evidence that
        // serialized to nothing, and the read VO would start advertising empty provenance.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any() ) )
                .thenAnswer( a -> a.getArgument( 1 ) );
        String body = "{\"category\":\"organism part\",\"value\":\"liver\"}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        ArgumentCaptor<Characteristic> captor = ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        assertThat( captor.getValue().getSupportingEvidence() ).isNull();
    }

    @Test
    @WithMockUser
    public void testAddDatasetAnnotationCarriesSupportingEvidenceOnAStatement() {
        // Statement extends Characteristic, so the provenance slots are inherited -- but the mapper
        // builds Statement and Characteristic on separate branches, so the Statement branch needs
        // its own guard or it can silently lose the field.
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any() ) )
                .thenAnswer( a -> a.getArgument( 1 ) );
        String body = "{\"category\":\"treatment\",\"value\":\"HFD\","
                + "\"predicate\":\"has dose\",\"object\":\"10mg\","
                + "\"supportingEvidence\":" + EVIDENCE_JSON + "}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().post( Entity.json( body ) ) )
                .hasStatus( Response.Status.CREATED );
        ArgumentCaptor<Characteristic> captor = ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        assertThat( captor.getValue() ).isInstanceOf( Statement.class );
        assertThat( captor.getValue().getSupportingEvidence() ).contains( "\"source\":\"paper\"" );
    }

    @Test
    @WithMockUser
    public void testReplaceDatasetAnnotationsCarriesSupportingEvidence() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        ee.setCharacteristics( new HashSet<>() );
        when( expressionExperimentService.load( 1L ) ).thenReturn( ee );
        when( expressionExperimentService.addAnnotation( eq( ee ), any() ) )
                .thenAnswer( a -> a.getArgument( 1 ) );
        String body = "{\"annotations\":[{\"category\":\"strain\",\"value\":\"C57BL/6J\","
                + "\"supportingEvidence\":" + EVIDENCE_JSON + "}]}";
        assertThat( target( "/annotations/datasets/1/annotations" ).request().put( Entity.json( body ) ) )
                .hasStatus( Response.Status.OK );
        ArgumentCaptor<Characteristic> captor = ArgumentCaptor.forClass( Characteristic.class );
        verify( expressionExperimentService ).addAnnotation( eq( ee ), captor.capture() );
        assertThat( captor.getValue().getSupportingEvidence() ).contains( "\"source\":\"paper\"" );
    }
}