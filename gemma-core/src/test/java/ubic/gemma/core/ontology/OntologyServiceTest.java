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
import ubic.gemma.core.ontology.model.AnnotationProperty;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.description.CharacteristicValueObject;
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
import java.util.stream.Collectors;
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
        // characteristicReadService belongs here too: it was the one collaborator left holding
        // invocations across tests, so a `never()` verification against it reported the PREVIOUS
        // test's call and failed on test order rather than on behaviour.
        reset( chebiOntologyService, obiService, cellosaurusOntologyService, searchService,
                characteristicReadService );
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
     * Ranking below must not mean "only if there is room left over". The supplementary tier used to get
     * {@code maxResults - ranked.size()}, which is exactly zero whenever the conventional ontologies fill
     * the window — the common case, since a partial-token match on a word like "syndrome" saturates a
     * default limit of 20 on its own. The catalogues were loaded, searched, and then discarded.
     */
    @Test
    public void testSupplementaryTierIsReachableWhenConventionalHitsFillTheWindow() throws Exception {
        List<OntologySearchResult<OntologyTerm>> conventional = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            conventional.add( new OntologySearchResult<>(
                    new OntologyTermSimple( "http://purl.obolibrary.org/obo/MONDO_" + i, "syndrome " + i ), 2.0 ) );
        }
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.findTerm( "Gorlin Goltz Syndrome", 10 ) ).thenReturn( conventional );

        when( cellosaurusOntologyService.isSupplementary() ).thenReturn( true );
        when( cellosaurusOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( cellosaurusOntologyService.findTerm( "Gorlin Goltz Syndrome", 10 ) ).thenReturn( Collections.singletonList(
                new OntologySearchResult<>( new OntologyTermSimple( "https://www.cellosaurus.org/CVCL_XXXX", "Gorlin Goltz" ), 137.0 ) ) );

        List<OntologySearchResult<OntologyTerm>> results =
                new ArrayList<>( ontologyService.findTerms( "Gorlin Goltz Syndrome", 10, 5000, TimeUnit.MILLISECONDS ) );

        // The window is still full, and the supplementary hit is in it rather than starved out.
        assertEquals( 10, results.size() );
        assertTrue( results.stream().anyMatch( r -> r.getResult().getUri().startsWith( "https://www.cellosaurus.org/" ) ),
                "supplementary hit was starved by a full conventional window" );
        // Still ranked below: every conventional hit that made the cut precedes it.
        int supplementaryIndex = -1;
        for ( int i = 0; i < results.size(); i++ ) {
            if ( results.get( i ).getResult().getUri().startsWith( "https://www.cellosaurus.org/" ) ) {
                supplementaryIndex = i;
                break;
            }
        }
        assertEquals( results.size() - 1, supplementaryIndex );
    }

    /**
     * Reserving a slice must not cost a result when there is nothing to put in it: with no supplementary
     * source contributing, the conventional hits still fill the whole window.
     */
    @Test
    public void testReservedSliceIsReturnedToConventionalHitsWhenUnused() throws Exception {
        List<OntologySearchResult<OntologyTerm>> conventional = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            conventional.add( new OntologySearchResult<>(
                    new OntologyTermSimple( "http://purl.obolibrary.org/obo/MONDO_" + i, "term " + i ), 2.0 ) );
        }
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.findTerm( "myelopathy", 10 ) ).thenReturn( conventional );

        List<OntologySearchResult<OntologyTerm>> results =
                new ArrayList<>( ontologyService.findTerms( "myelopathy", 10, 5000, TimeUnit.MILLISECONDS ) );

        assertEquals( 10, results.size() );
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

    /**
     * A two-character query is a real term name — H1, H7 and H9 are among the most-used human
     * embryonic stem cell lines, and EFO_0003042 (H1-hESC) carries `H1` as an exact synonym. The
     * floor used to sit at three and return an empty set with no log line, so those queries were
     * indistinguishable from an ontology that had not been loaded.
     */
    @Test
    public void testTwoCharacterQueryReachesTheOntologies() throws Exception {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( characteristicReadService.findByValueLike( any(), any(), any(), anyBoolean(), anyInt() ) )
                .thenReturn( Collections.emptyList() );

        ontologyService.findExperimentsCharacteristicTags( "H1", 100, false, false, 5000, TimeUnit.MILLISECONDS );

        verify( chebiOntologyService ).findTerm( "H1", 100 );
    }

    /**
     * One character stays out: that is where the candidate set stops being a name and becomes a
     * scan of the index, and nothing we annotate is designated by a single character.
     */
    @Test
    public void testSingleCharacterQueryIsStillRefused() throws Exception {
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );

        assertTrue( ontologyService.findExperimentsCharacteristicTags( "H", 100, false, false, 5000, TimeUnit.MILLISECONDS ).isEmpty() );

        verify( chebiOntologyService, never() ).findTerm( anyString(), anyInt() );
        verify( characteristicReadService, never() ).findByValueLike( any(), any(), any(), anyBoolean(), anyInt() );
    }

    /**
     * The cap has to keep the BEST candidates, not merely the same ones every time.
     *
     * <p>`H1 cell line` overflows the candidate cap with chemicals whose labels contain the
     * substring, and EFO_0003042 -- the stem cell line actually being asked for -- is a poor match
     * on its LABEL and a strong one on its declared synonym. Cutting alphabetically, or in whatever
     * order a HashSet iterated, buried it: the term was absent from a hundred rows while scoring
     * far above everything that displaced it. Relevance has to survive as far as the cut, so the
     * search score is carried out of the ontology fan-out instead of being dropped at the value
     * object.</p>
     */
    @Test
    public void testTheCapKeepsTheBestCandidatesNotTheAlphabeticalOnes() throws Exception {
        when( characteristicReadService.findByValueLike( any(), any(), any(), anyBoolean(), anyInt() ) )
                .thenReturn( Collections.emptyList() );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        // Nine weak matches whose URIs sort FIRST, and the strong one whose URI sorts last.
        List<OntologySearchResult<OntologyTerm>> hits = new ArrayList<>();
        for ( int i = 0; i < 9; i++ ) {
            hits.add( new OntologySearchResult<>( new OntologyTermSimple(
                    String.format( "http://purl.obolibrary.org/obo/AAA_%04d", i ), "zzz weak " + i ), 0.5 ) );
        }
        OntologyTermSimple best = new OntologyTermSimple( "http://purl.obolibrary.org/obo/ZZZ_9999", "zzz best" );
        hits.add( new OntologySearchResult<>( best, 42.0 ) );
        when( chebiOntologyService.findTerm( eq( "zzz" ), anyInt() ) ).thenReturn( hits );

        List<CharacteristicValueObject> capped = new ArrayList<>(
                ontologyService.findExperimentsCharacteristicTags( "zzz", 3, false, false, 5000, TimeUnit.MILLISECONDS ) );

        assertEquals( 3, capped.size() );
        // The high scorer survives a cap of three despite sorting last alphabetically.
        assertEquals( "http://purl.obolibrary.org/obo/ZZZ_9999", capped.get( 0 ).getValueUri() );
        // ...and the cut is still reproducible.
        List<CharacteristicValueObject> again = new ArrayList<>(
                ontologyService.findExperimentsCharacteristicTags( "zzz", 3, false, false, 5000, TimeUnit.MILLISECONDS ) );
        assertEquals( capped.stream().map( CharacteristicValueObject::getValueUri ).collect( Collectors.toList() ),
                again.stream().map( CharacteristicValueObject::getValueUri ).collect( Collectors.toList() ) );
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

    /**
     * CLO does not use the OBO definition property. It writes what it knows about a cell line into
     * {@code rdfs:comment} — {@code CLO_0008127} (NCI-H929) carries "disease: plasmacytoma;   myeloma" and no
     * OBO definition at all — so probing only {@code IAO_0000115} returned null for exactly the terms whose
     * description is the point. That disease is a property of the line, and nobody should have to curate it
     * onto an experiment when an ontology already loaded here asserts it.
     */
    @Test
    public void testDefinitionFallsBackToCommentWhenTheOntologyUsesOne() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/CLO_0008127";
        OntologyTerm term = mock();
        when( term.getLabel() ).thenReturn( "NCI-H929 cell" );
        when( term.getAnnotation( OntologyUtils.DEFINITION_URI ) ).thenReturn( null );
        when( term.getComment() ).thenReturn( "disease: plasmacytoma;   myeloma" );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.getTerm( uri ) ).thenReturn( term );

        assertEquals( "disease: plasmacytoma;   myeloma",
                ontologyService.getDefinition( uri, 5000, TimeUnit.MILLISECONDS ) );
    }

    /**
     * The fallback is a fallback: an ontology that states a definition properly still wins, so a MONDO or
     * UBERON term never reports an editorial comment as its definition.
     */
    @Test
    public void testOboDefinitionWinsOverComment() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/MONDO_0004975";
        AnnotationProperty definition = mock();
        when( definition.getContents() ).thenReturn( "A progressive form of dementia." );
        OntologyTerm term = mock();
        when( term.getLabel() ).thenReturn( "Alzheimer disease" );
        when( term.getAnnotation( OntologyUtils.DEFINITION_URI ) ).thenReturn( definition );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.getTerm( uri ) ).thenReturn( term );

        assertEquals( "A progressive form of dementia.",
                ontologyService.getDefinition( uri, 5000, TimeUnit.MILLISECONDS ) );
        verify( term, never() ).getComment();
    }

    /**
     * A term with neither still reports nothing, rather than an empty string a caller would render as a
     * definition that exists and is blank.
     */
    @Test
    public void testDefinitionIsNullWhenTheTermDescribesItselfNowhere() throws TimeoutException {
        String uri = "http://purl.obolibrary.org/obo/CLO_0000019";
        OntologyTerm term = mock();
        when( term.getLabel() ).thenReturn( "immortal cell line cell" );
        when( term.getAnnotation( OntologyUtils.DEFINITION_URI ) ).thenReturn( null );
        when( term.getComment() ).thenReturn( "   " );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        when( chebiOntologyService.getTerm( uri ) ).thenReturn( term );

        assertNull( ontologyService.getDefinition( uri, 5000, TimeUnit.MILLISECONDS ) );
    }
}
