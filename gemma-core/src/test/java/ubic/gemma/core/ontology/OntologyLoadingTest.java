package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.SlowTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test does not use the test profile as it aims to verify that all the ontologies we use in production are working
 * properly.
 *
 * @author poirigui
 */
@CommonsLog
@ContextConfiguration
public class OntologyLoadingTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    @Import(OntologyConfig.class)
    static class CC {

        @Bean
        public static PropertyPlaceholderConfigurer propertyConfigurer() {
            return new TestPropertyPlaceholderConfigurer(
                    "load.ontologies=false",
                    "gemma.ontology.unified.enabled=false",
                    "gemma.ontology.unified.tdb.dir=",
                    "gemma.ontology.loader.corePoolSize=4"
            );
        }

        @Bean
        public ConversionService conversionService() {
            DefaultFormattingConversionService service = new DefaultFormattingConversionService();
            service.addConverter( String.class, Path.class, source -> Paths.get( ( String ) source ) );
            return service;
        }
    }

    @Autowired
    private List<OntologyService> ontologyServices;

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private AsyncTaskExecutor taskExecutor;

    @Autowired
    private ExperimentalFactorOntologyService efo;

    @Autowired
    private ChebiOntologyService chebi;

    @Autowired
    private GemmaOntologyService tgemo;

    @Autowired
    private MammalianPhenotypeOntologyService mp;

    @Autowired
    private MondoOntologyService mondo;

    @Autowired
    private CellLineOntologyService clo;

    @Autowired
    private CellTypeOntologyService cl;

    @Autowired
    private HumanPhenotypeOntologyService hpo;

    @Autowired
    private UberonOntologyService uberon;

    @Autowired
    private ObiService obi;

    @Autowired
    private MouseDevelopmentOntologyService mdo;

    @Autowired
    @Qualifier("unifiedOntologyService")
    private OntologyService unified;

    @Test
    public void testThatConnectivityAndConnectiveAreExcludedFromStemmingInClo() {
        assertThat( clo.getExcludedWordsFromStemming() ).contains( "connectivity", "connective" );
    }

    @Test
    public void testThatChebiDoesNotHaveInferenceEnabled() {
        assertThat( chebi.getInferenceMode() ).isEqualTo( OntologyService.InferenceMode.NONE );
    }

    @Test
    public void testThatTGEMODoesNotProcessImports() {
        assertThat( tgemo.getProcessImports() ).isFalse();
    }

    @Test
    @Category(SlowTest.class)
    public void testInitializeAllOntologies() {
        // these are notoriously slow, so we skip them
        List<OntologyService> ignoredOntologies = Arrays.asList( efo, chebi, mp, mondo, clo, cl, hpo, uberon, obi, mdo, unified );
        List<OntologyService> services = new ArrayList<>();
        List<Future<?>> futures = new ArrayList<>();
        for ( OntologyService os : ontologyServices ) {
            if ( ignoredOntologies.contains( os ) ) {
                continue;
            }
            services.add( os );
            futures.add( taskExecutor.submit( () -> {
                StopWatch timer = StopWatch.createStarted();
                os.initialize( true, true );
                log.info( String.format( "Initializing %s took %d s.", os, timer.getTime( TimeUnit.SECONDS ) ) );
            } ) );
        }
        long totalTimeMs = 60 * 1000L; // 1 minute
        long initialTimeMs = System.currentTimeMillis();
        assertThat( futures ).zipSatisfy( services, ( future, os ) -> {
            long elapsedTimeMs = System.currentTimeMillis() - initialTimeMs;
            assertThat( future )
                    .describedAs( os.toString() )
                    .succeedsWithin( Duration.ofMillis( Math.max( totalTimeMs - elapsedTimeMs, 0 ) ) );
        } );
    }
}
