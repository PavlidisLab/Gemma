package ubic.gemma.core.ontology;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class OntologyServiceTest extends AbstractJUnit4SpringContextTests {

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
            return mock();
        }
    }

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private ChebiOntologyService chebiOntologyService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private CharacteristicService characteristicService;

    @Test
    public void testFindTermInexact() throws OntologySearchException, SearchException {
        SearchService.SearchResultMap srm = mock();
        when( srm.getByResultObjectType( Gene.class ) ).thenReturn( Collections.emptyList() );
        when( searchService.search( any() ) ).thenReturn( srm );
        when( chebiOntologyService.isOntologyLoaded() ).thenReturn( true );
        ontologyService.findTermsInexact( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", null );
        verify( characteristicService ).findCharacteristicsByValueUriOrValueLike( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol" );
        ArgumentCaptor<SearchSettings> captor = ArgumentCaptor.forClass( SearchSettings.class );
        verify( searchService ).search( captor.capture() );
        SearchSettings settings = captor.getValue();
        assertEquals( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol", settings.getQuery() );
        assertTrue( settings.getResultTypes().contains( Gene.class ) );
        assertTrue( settings.isFillResults() );
        verify( chebiOntologyService ).isOntologyLoaded();
        verify( chebiOntologyService ).findTerm( "9-chloro-5-phenyl-3-prop-2-enyl-1,2,4,5-tetrahydro-3-benzazepine-7,8-diol" );
    }
}
