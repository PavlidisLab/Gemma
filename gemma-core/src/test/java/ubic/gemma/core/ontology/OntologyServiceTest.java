package ubic.gemma.core.ontology;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.ontology.providers.ChebiOntologyService;
import ubic.gemma.core.ontology.providers.ExperimentalFactorOntologyService;
import ubic.gemma.core.ontology.providers.ObiService;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.CellosaurusOntologyService;
import ubic.gemma.core.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.search.OntologySearchResult;
import ubic.gemma.core.ontology.simple.OntologyTermSimple;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.description.CharacteristicReadService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class OntologyServiceTest extends BaseTest5 {

    @Configuration
    @TestComponent
    static class OntologyServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "load.ontologies=false" );
        }

        @Bean
        public OntologyService ontologyService() {
            return new OntologyServiceImpl();
        }

        @Bean
        public ChebiOntologyService chebiOntologyService() {
            return mock( ChebiOntologyService.class );
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock();
        }

        @Bean
        public CharacteristicReadService characteristicReadService() {
            return mock( CharacteristicReadService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock();
        }

        @Bean
        public GeneOntologyService geneOntologyService() {
            return mock();
        }

        @Bean
        public GeneService geneService() {
            return mock();
        }

        @Bean
        public AsyncTaskExecutor taskExecutor() {
            return new SimpleAsyncTaskExecutor();
        }

        @Bean
        public ExperimentalFactorOntologyService experimentalFactorOntologyService() {
            return mock();
        }

        @Bean
        public ObiService obiService() {
            return mock();
        }

        @Bean
        public CellosaurusOntologyService cellosaurusOntologyService() {
            return mock( CellosaurusOntologyService.class );
        }

        @Bean
        @Qualifier("ontologyTaskExecutor")
        public TaskExecutor ontologyTaskExecutor() {
            return mock();
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private ChebiOntologyService chebiOntologyService;

    @Autowired
    private ObiService obiService;

    @Autowired
    private CellosaurusOntologyService cellosaurusOntologyService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private CharacteristicReadService characteristicReadService;

    @AfterEach
    public void tearDown() {
        reset( chebiOntologyService, obiService, cellosaurusOntologyService, searchService );
    }

    /**
     * A supplementary source scores against its own Lucene index and applies a large exact-name boost, so its
     * raw score dwarfs a conventional ontology's. Ranking must not be decided by that number: every
     * conventional hit comes first, and the supplementary hit is appended below.
     */
    @Test
    public void testSupplementarySourceRanksBelowConventionalOntologies() throws Exception {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.findTerm( "HeLa", 100 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "http://purl.obolibrary.org/obo/CLO_0003684", "HeLa cell" ), 1.5 ) ) );

        when( cellosaurusOntologyService.isSupplementary() ).thenReturn( true );
        when( cellosaurusOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( cellosaurusOntologyService.findTerm( "HeLa", 100 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "https://www.cellosaurus.org/CVCL_0030", "HeLa" ), 137.0 ) ) );

        List<OntologySearchResult<OntologyTerm>> results =
                new ArrayList<>( ontologyService.findTerms( "HeLa", 100, 5000, TimeUnit.MILLISECONDS ) );

        assertEquals( 2, results.size() );
        assertEquals( "http://purl.obolibrary.org/obo/CLO_0003684", results.get( 0 ).getResult().getUri() );
        assertEquals( "https://www.cellosaurus.org/CVCL_0030", results.get( 1 ).getResult().getUri() );
    }

    /**
     * The gap-fill case: when no conventional ontology matches, the supplementary hit is still returned and is
     * still first. Ranking below must not degrade into suppression.
     */
    @Test
    public void testSupplementarySourceStillSurfacesWhenOntologiesFindNothing() throws Exception {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.findTerm( "KOLF2.1J", 100 ) ).thenReturn( Collections.emptyList() );

        when( cellosaurusOntologyService.isSupplementary() ).thenReturn( true );
        when( cellosaurusOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( cellosaurusOntologyService.findTerm( "KOLF2.1J", 100 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "https://www.cellosaurus.org/CVCL_B5P3", "KOLF2.1J" ), 137.0 ) ) );

        List<OntologySearchResult<OntologyTerm>> results =
                new ArrayList<>( ontologyService.findTerms( "KOLF2.1J", 100, 5000, TimeUnit.MILLISECONDS ) );

        assertEquals( 1, results.size() );
        assertEquals( "https://www.cellosaurus.org/CVCL_B5P3", results.get( 0 ).getResult().getUri() );
    }

    /**
     * GO is consulted only when the other ontologies come up empty. That test must read the conventional
     * ontologies alone — otherwise enabling a supplementary catalogue would quietly switch the GO fallback off
     * for every query the catalogue happens to match.
     */
    @Test
    public void testSupplementaryHitDoesNotSuppressTheGeneOntologyFallback() throws Exception {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.findTerm( "pregnancy", 100 ) ).thenReturn( Collections.emptyList() );

        when( cellosaurusOntologyService.isSupplementary() ).thenReturn( true );
        when( cellosaurusOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( cellosaurusOntologyService.findTerm( "pregnancy", 100 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "https://www.cellosaurus.org/CVCL_9999", "pregnancy" ), 137.0 ) ) );

        when( geneOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( geneOntologyService.findTerm( "pregnancy", 100 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "http://purl.obolibrary.org/obo/GO_0007565", "female pregnancy" ), 2.0 ) ) );

        List<OntologySearchResult<OntologyTerm>> results =
                new ArrayList<>( ontologyService.findTerms( "pregnancy", 100, 5000, TimeUnit.MILLISECONDS ) );

        verify( geneOntologyService ).findTerm( "pregnancy", 100 );
        assertEquals( 2, results.size() );
        assertEquals( "http://purl.obolibrary.org/obo/GO_0007565", results.get( 0 ).getResult().getUri() );
        assertEquals( "https://www.cellosaurus.org/CVCL_9999", results.get( 1 ).getResult().getUri() );
    }

    @Test
    public void testFindTermInexact() throws OntologySearchException, SearchException {
        SearchService.SearchResultMap srm = mock();
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.emptyList() );
        when( searchService.search( any() ) ).thenReturn( srm );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        ontologyService.findTermsInexact( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", 5000, null, 5000, TimeUnit.MILLISECONDS );
        verify( characteristicReadService ).findByValueUriOrValueStartingWith( eq( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol" ), eq( Arrays.asList( ExpressionExperiment.class, ExperimentalDesign.class, FactorValue.class, BioMaterial.class ) ), eq( false ) );
        ArgumentCaptor<SearchSettings> captor = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService ).search( captor.capture() );
        SearchSettings settings = captor.getValue();
        assertEquals( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", settings.getQuery() );
        assertTrue( settings.getResultTypes().contains( Gene.class ) );
        assertTrue( settings.isFillResults() );
        verify( chebiOntologyService ).isOntologyLoaded();
        verify( chebiOntologyService ).findTerm( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", 5000 );
    }

    @Test
    public void testTermLackingLabelIsIgnored() throws TimeoutException {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );

        when( chebiOntologyService.getTerm( "http://test" ) ).thenReturn( new OntologyTermSimple( "http://test", null ) );
        assertNull( ontologyService.getTerm( "http://test", 5000, TimeUnit.MILLISECONDS ) );

        // provide the term from another ontology, but with a label this time
        when( obiService.isOntologyLoaded() ).thenReturn( true );
        when( obiService.getTerm( "http://test" ) ).thenReturn( new OntologyTermSimple( "http://test", "this is a test term" ) );
        assertNotNull( ontologyService.getTerm( "http://test", 5000, TimeUnit.MILLISECONDS ) );
    }
}
