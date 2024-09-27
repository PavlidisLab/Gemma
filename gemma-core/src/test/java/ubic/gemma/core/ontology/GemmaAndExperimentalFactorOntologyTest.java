package ubic.gemma.core.ontology;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.ObiService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * This test covers cases where inference is done across two distinct {@link ubic.basecode.ontology.providers.OntologyService}.
 * @author poirigui
 */
@Ignore("This test is timing out on the CI.")
@Category(SlowTest.class)
@ContextConfiguration
public class GemmaAndExperimentalFactorOntologyTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class GemmaOntologyAndEfoTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "load.ontologies=false" );
        }

        @Bean
        public ExperimentalFactorOntologyService experimentalFactorOntologyService() {
            ExperimentalFactorOntologyService ontology = new ExperimentalFactorOntologyService();
            ontology.setSearchEnabled( false );
            ontology.initialize( true, false );
            return ontology;
        }

        @Bean
        public GemmaOntologyService gemmaOntologyService() {
            GemmaOntologyService ontology = new GemmaOntologyService();
            ontology.setSearchEnabled( false );
            ontology.setProcessImports( false ); // FIXME: remove this once https://github.com/PavlidisLab/TGEMO/pull/20 is merged in TGEMO
            ontology.initialize( true, false );
            return ontology;
        }

        @Bean
        public OntologyService ontologyService() {
            return new OntologyServiceImpl();
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
    private ExperimentalFactorOntologyService experimentalFactorOntologyService;

    @Autowired
    private GemmaOntologyService gemmaOntologyService;

    @Autowired
    private OntologyService ontologyService;

    @Test
    public void testInferenceInGemma() throws TimeoutException {
        OntologyTerm overexpression = ontologyService.getTerm( "http://gemma.msl.ubc.ca/ont/TGEMO_00004", 5000, TimeUnit.MILLISECONDS );
        assertNotNull( overexpression );

        assertThat( gemmaOntologyService.getParents( Collections.singleton( overexpression ), false, false ) )
                .extracting( OntologyTerm::getUri )
                .containsExactlyInAnyOrder( "http://www.ebi.ac.uk/efo/EFO_0000510" );

        OntologyTerm geneticModification = experimentalFactorOntologyService.getTerm( "http://www.ebi.ac.uk/efo/EFO_0000510" );
        assertNotNull( geneticModification );

        assertThat( experimentalFactorOntologyService.getParents( Collections.singleton( geneticModification ), false, false ) )
                .extracting( OntologyTerm::getUri )
                .containsExactlyInAnyOrder( "http://www.ebi.ac.uk/efo/EFO_0000001",
                        "http://purl.obolibrary.org/obo/BFO_0000015" );

        // ensure that parents are combined when using the OS
        assertThat( ontologyService.getParents( Collections.singleton( overexpression ), false, false, 5000, TimeUnit.MILLISECONDS ) )
                .extracting( OntologyTerm::getUri )
                .containsExactlyInAnyOrder(
                        "http://www.ebi.ac.uk/efo/EFO_0000001",
                        "http://purl.obolibrary.org/obo/BFO_0000015",
                        "http://www.ebi.ac.uk/efo/EFO_0000510" );
    }
}
