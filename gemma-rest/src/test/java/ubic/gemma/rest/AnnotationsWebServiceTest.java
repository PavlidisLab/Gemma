package ubic.gemma.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.context.web.WebAppConfiguration;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.*;
import ubic.gemma.rest.util.args.DatasetArgService;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.SortValueObject;
import ubic.gemma.rest.util.args.*;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author poirigui
 */
@WebAppConfiguration
@ContextConfiguration
public class AnnotationsWebServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    public static class AnnotationsWebServiceContextConfiguration {

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
        public DatasetArgService datasetRestService( ExpressionExperimentService service ) {
            return new DatasetArgService( service );
        }

        @Bean
        public TaxonArgService taxonArgService( TaxonService taxonService ) {
            return new TaxonArgService( taxonService );
        }

        @Bean
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                DatasetArgService datasetRestService, TaxonArgService taxonArgService ) {
            return new AnnotationsWebService( ontologyService, searchService, characteristicService, expressionExperimentService, datasetRestService, taxonArgService );
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

    @Before
    public void setUp() {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setId( 1L );
        when( taxonService.findByCommonName( "human" ) ).thenReturn( taxon );
    }

    @After
    public void tearDown() {
        reset( searchService, taxonService );
    }

    @Test
    public void testSearchTaxonDatasets() throws SearchException {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setId( 1L );
        SearchService.SearchResultMap srm = mock( SearchService.SearchResultMap.class );
        when( srm.get( ExpressionExperiment.class ) ).thenReturn( Collections.singletonList( SearchResult.from( ExpressionExperiment.class, ee, "test object" ) ) );
        when( searchService.search( any( SearchSettings.class ), eq( false ), eq( false ) ) )
                .thenReturn( srm );
        when( taxonService.getFilter( eq( "commonName" ), eq( Filter.Operator.eq ), any( String.class ) ) ).thenAnswer( a -> Filter.by( "t", "commonName", String.class, Filter.Operator.eq, a.getArgument( 2, String.class ), a.getArgument( 0 ) ) );
        when( taxonService.getFilter( eq( "scientificName" ), eq( Filter.Operator.eq ), any( String.class ) ) ).thenAnswer( a -> Filter.by( "t", "scientificName", String.class, Filter.Operator.eq, a.getArgument( 2, String.class ), a.getArgument( 0 ) ) );
        when( expressionExperimentService.getIdentifierPropertyName() ).thenReturn( "id" );
        when( expressionExperimentService.getFilter( "id", Filter.Operator.in, Collections.singletonList( "1" ) ) ).thenReturn( Filter.by( "ee", "id", Long.class, Filter.Operator.in, Collections.singleton( 1L ), "id" ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) );
        when( expressionExperimentService.loadValueObjectsPreFilter( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ), eq( 0 ), eq( 20 ) ) )
                .thenAnswer( a -> new Slice<>( Collections.singletonList( new ExpressionExperimentValueObject( ee ) ), a.getArgument( 1 ), a.getArgument( 2, Integer.class ), a.getArgument( 3, Integer.class ), 10000L ) );
        PaginatedResponseDataObject<ExpressionExperimentValueObject> payload = annotationsWebService.searchTaxonDatasets(
                TaxonArg.valueOf( "human" ),
                StringArrayArg.valueOf( "bipolar" ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        assertThat( payload )
                .hasFieldOrPropertyWithValue( "filter", "ee.id in (1) and (t.commonName = human or t.scientificName = human)" )
                .hasFieldOrPropertyWithValue( "sort", new SortValueObject( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ) )
                .hasFieldOrPropertyWithValue( "offset", 0 )
                .hasFieldOrPropertyWithValue( "limit", 20 )
                .hasFieldOrPropertyWithValue( "totalElements", 10000L );
        verify( searchService ).search( any( SearchSettings.class ), eq( false ), eq( false ) );
        verify( taxonService ).getFilter( "commonName", Filter.Operator.eq, "human" );
        verify( taxonService ).getFilter( "scientificName", Filter.Operator.eq, "human" );
        verify( expressionExperimentService ).getFilter( "id", Filter.Operator.in, Collections.singletonList( "1" ) );
        verify( expressionExperimentService ).getSort( "id", Sort.Direction.ASC );
        verify( expressionExperimentService ).loadValueObjectsPreFilter( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC, "id" ) ), eq( 0 ), eq( 20 ) );
    }

}