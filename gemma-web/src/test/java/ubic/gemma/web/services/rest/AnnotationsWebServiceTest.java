package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletResponse;
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
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;
import ubic.gemma.web.services.rest.util.args.*;

import javax.ws.rs.NotFoundException;
import java.util.Collections;

import static org.mockito.Mockito.*;

/**
 * @author poirigui
 */
@WebAppConfiguration
@ContextConfiguration
public class AnnotationsWebServiceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
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
        public AnnotationsWebService annotationsWebService( OntologyService ontologyService, SearchService searchService,
                CharacteristicService characteristicService, ExpressionExperimentService expressionExperimentService,
                TaxonService taxon ) {
            return new AnnotationsWebService( ontologyService, searchService, characteristicService, expressionExperimentService, taxon );
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
        when( searchService.search( any( SearchSettings.class ), eq( false ), eq( false ) ) )
                .thenReturn( Collections.singletonMap( ExpressionExperiment.class, Collections.singletonList( new SearchResult<>( ee, "test object" ) ) ) );
        when( expressionExperimentService.getSort( "id", Sort.Direction.ASC ) ).thenReturn( Sort.by( "ee", "id", Sort.Direction.ASC ) );
        when( expressionExperimentService.loadValueObjectsPreFilter( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC ) ), eq( 0 ), eq( 20 ) ) )
                .thenReturn( Slice.fromList( Collections.singletonList( new ExpressionExperimentValueObject( ee ) ) ) );
        annotationsWebService.searchTaxonDatasets(
                TaxonArg.valueOf( "human" ),
                StringArrayArg.valueOf( "bipolar" ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        verify( searchService ).search( any( SearchSettings.class ), eq( false ), eq( false ) );
        verify( expressionExperimentService ).getSort( "id", Sort.Direction.ASC );
        verify( expressionExperimentService ).loadValueObjectsPreFilter( any( Filters.class ), eq( Sort.by( "ee", "id", Sort.Direction.ASC ) ), eq( 0 ), eq( 20 ) );
    }

}