package ubic.gemma.core.ontology;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.jena.AbstractOntologyMemoryBackedService;
import ubic.basecode.ontology.providers.ChebiOntologyService;
import ubic.basecode.ontology.providers.ExperimentalFactorOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.util.test.category.SlowTest;

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
@ContextConfiguration(locations = { "classpath*:ubic/gemma/applicationContext-ontology.xml", "classpath:ubic/gemma/core/ontology/OntologyLoadingTest-context.xml" })
public class OntologyLoadingTest extends AbstractJUnit4SpringContextTests {

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
        List<AbstractOntologyMemoryBackedService> ignoredOntologies = Arrays.asList( efo, chebi, mp, mondo );
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
                log.info( String.format( "Initializing " + os + " took %d s.", timer.getTime( TimeUnit.SECONDS ) ) );
            } ) );
        }
        assertThat( futures ).zipSatisfy( services, ( future, os ) -> {
            assertThat( future )
                    .describedAs( os.toString() )
                    .succeedsWithin( Duration.ofSeconds( 60 ) );
        } );
    }
}
