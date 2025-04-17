package ubic.gemma.core.ontology;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ubic.basecode.ontology.jena.TdbOntologyService;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.ontology.providers.PatoOntologyService;
import ubic.gemma.core.util.TextResourceToSetOfLinesFactoryBean;

import java.nio.file.Path;

@Configuration
@Profile({ "!test" }) // we use a different set of ontologies in tests
public class OntologyConfig {

    @Value("${load.ontologies}")
    private boolean loadOntologies;

    // FIXME: inject it as a Set<String>, but Spring interpret this as a set of String beans
    @Autowired
    @Qualifier("excludedWordsFromStemming")
    private TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming;

    /**
     * Executor used for loading ontologies in background.
     */
    @Bean
    public TaskExecutor ontologyTaskExecutor( @Value("${gemma.ontology.loader.corePoolSize}") int corePoolSize ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize( corePoolSize );
        executor.setThreadNamePrefix( "gemma-ontology-loader-thread-" );
        return executor;
    }

    @Bean
    public TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming() {
        return new TextResourceToSetOfLinesFactoryBean( new ClassPathResource( "/ubic/gemma/core/ontology/excludedWordsFromStemming.txt" ) );
    }

    /**
     * This ontology encompasses all the ontologies declared below.
     */
    @Bean
    public OntologyServiceFactory<?> unifiedOntologyService(
            @Value("${gemma.ontology.unified.enabled}") boolean enabled,
            @Value("${gemma.ontology.unified.tdb.dir}") Path tdbDir,
            @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        TdbOntologyService ontology = new TdbOntologyService( "Gemma Unified Ontology", tdbDir, null, enabled, "unified" );
        OntologyServiceFactory<TdbOntologyService> factory = new OntologyServiceFactory<>( ontology );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( taskExecutor );
        factory.setProcessImports( false );
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
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
    public FactoryBean<CellTypeOntologyService> cellTypeOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( CellTypeOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<ChebiOntologyService> chebiOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        OntologyServiceFactory<ChebiOntologyService> factory = createOntologyFactory( ChebiOntologyService.class, taskExecutor );
        factory.setInferenceMode( OntologyService.InferenceMode.NONE );
        return factory;
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
    public FactoryBean<MouseDevelopmentOntologyService> mouseDevelopmentOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( MouseDevelopmentOntologyService.class, taskExecutor );
    }

    @Bean
    @Deprecated
    public FactoryBean<HumanDevelopmentOntologyService> humanDevelopmentOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( HumanDevelopmentOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<SequenceOntologyService> sequenceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( SequenceOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<CellLineOntologyService> cellLineOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( CellLineOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<UberonOntologyService> uberonOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( UberonOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<PatoOntologyService> patoOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( PatoOntologyService.class, taskExecutor );
    }

    @Bean
    public FactoryBean<MondoOntologyService> mondoOntologyServiceOntologyService( @Qualifier("ontologyTaskExecutor") TaskExecutor taskExecutor ) {
        return createOntologyFactory( MondoOntologyService.class, taskExecutor );
    }

    private <T extends OntologyService> OntologyServiceFactory<T> createOntologyFactory( Class<T> ontologyClass, TaskExecutor taskExecutor ) {
        OntologyServiceFactory<T> factory = new OntologyServiceFactory<>( ontologyClass );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( taskExecutor );
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
    }
}
