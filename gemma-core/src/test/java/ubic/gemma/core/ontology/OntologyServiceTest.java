package ubic.gemma.core.ontology;

import org.junit.After;
import org.junit.Test;
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
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.providers.ChebiOntologyService;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class OntologyServiceTest extends BaseTest {

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

        @Deprecated
        @Bean
        public FMAOntologyService fmaOntologyService() {
            return mock();
        }

        @Deprecated
        @Bean
        public NIFSTDOntologyService nifstdOntologyService() {
            return mock();
        }

        @Bean
        public ObiService obiService() {
            return mock();
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
    private SearchService searchService;

    @Autowired
    private CharacteristicService characteristicService;

    @After
    public void tearDown() {
        reset( chebiOntologyService, obiService, searchService );
    }

    @Test
    public void testFindTermInexact() throws OntologySearchException, SearchException {
        SearchService.SearchResultMap srm = mock();
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.emptyList() );
        when( searchService.search( any() ) ).thenReturn( srm );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        ontologyService.findTermsInexact( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", 5000, null, 5000, TimeUnit.MILLISECONDS );
        verify( characteristicService ).findByValueUriOrValueLike( eq( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol" ), eq( Arrays.asList( ExpressionExperiment.class, ExperimentalDesign.class, FactorValue.class, BioMaterial.class ) ) );
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
