package ubic.gemma.core.ontology;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.util.TextResourceToSetOfLinesFactoryBean;

/**
 * This class contains configuration for the ontology services used in tests
 * <p>
 * By default, no ontology are loaded in tests and no limit is applied to the number of ontologies that can be loaded
 * concurrently.
 * @author poirigui
 */
@Configuration
@Profile("test") // not needed since this is a test resource, but we need to be consistent with OntologyConfig
public class TestOntologyConfig {

    // FIXME: inject it as a Set<String>, but Spring interpret this as a set of String beans
    @Autowired
    @Qualifier("excludedWordsFromStemming")
    private TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming;

    /**
     * Executor used for loading ontologies in background.
     */
    @Bean
    public TaskExecutor ontologyTaskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    @Bean
    public TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming() {
        return new TextResourceToSetOfLinesFactoryBean( new ClassPathResource( "/ubic/gemma/core/ontology/excludedWordsFromStemming.txt" ) );
    }

    @Bean
    public FactoryBean<GemmaOntologyService> gemmaOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        OntologyServiceFactory<GemmaOntologyService> factory = createOntologyFactory( GemmaOntologyService.class, taskExecutor );
        // TODO: remove this once https://github.com/PavlidisLab/TGEMO/pull/20 is merged
        factory.setProcessImports( false );
        return factory;
    }

    @Bean
    public FactoryBean<ExperimentalFactorOntologyService> experimentalFactorOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( ExperimentalFactorOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<ObiService> obiService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( ObiService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<MondoOntologyService> mondoOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( MondoOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<MammalianPhenotypeOntologyService> mammalianPhenotypeOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( MammalianPhenotypeOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<HumanPhenotypeOntologyService> humanPhenotypeOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( HumanPhenotypeOntologyService.class, taskExecutor );
    }

    @Bean
    @Deprecated
    public FactoryBean<NIFSTDOntologyService> nisftOntologyServiceFactory( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( NIFSTDOntologyService.class, taskExecutor );
    }

    @Bean
    @Deprecated
    public FactoryBean<FMAOntologyService> fmaOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( FMAOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<UberonOntologyService> uberonOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( UberonOntologyService.class, taskExecutor );
    }

    private <T extends OntologyService> OntologyServiceFactory<T> createOntologyFactory( Class<T> ontologyClass, TaskExecutor taskExecutor ) {
        OntologyServiceFactory<T> factory = new OntologyServiceFactory<>( ontologyClass );
        // test ontologies are always loaded on-demand, so we never set autoLoad to true
        factory.setTaskExecutor( taskExecutor );
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
    }
}
