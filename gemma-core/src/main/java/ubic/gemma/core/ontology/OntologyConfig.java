package ubic.gemma.core.ontology;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import ubic.basecode.ontology.jena.TdbOntologyService;
import ubic.basecode.ontology.providers.*;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.ontology.providers.PatoOntologyService;
import ubic.gemma.core.util.TextResourceToSetOfLinesFactoryBean;

import java.io.IOException;
import java.nio.file.Path;

@Configuration
@Profile({ "!test" }) // we use a different set of ontologies in tests
public class OntologyConfig {

    @Value("${load.ontologies}")
    private boolean loadOntologies;

    @Value("${gemma.ontology.loader.corePoolSize}")
    private int corePoolSize;

    // FIXME: inject it as a Set<String>, but Spring interpret this as a set of String beans
    @Autowired
    @Qualifier("excludedWordsFromStemming")
    private TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming;

    /**
     * Executor used for loading ontologies in background.
     */
    @Bean
    public TaskExecutor ontologyTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize( corePoolSize );
        executor.setThreadNamePrefix( "gemma-ontology-loader-thread-" );
        return executor;
    }

    @Bean
    public TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming() {
        return new TextResourceToSetOfLinesFactoryBean( new ClassPathResource( "/ubic/gemma/core/ontology/excludedWordsFromStemming.txt" ) );
    }

    @Bean
    public OntologyExternalLinks ontologyExternalLinks( Environment environment ) throws IOException {
        return new OntologyExternalLinks( environment.acceptsProfiles( EnvironmentProfiles.DEV ) );
    }

    /**
     * This ontology encompasses all the ontologies declared below.
     */
    @Bean
    public OntologyServiceFactory<?> unifiedOntologyService(
            @Value("${gemma.ontology.unified.enabled}") boolean enabled,
            @Value("${gemma.ontology.unified.tdb.dir}") Path tdbDir
    ) {
        TdbOntologyService ontology = new TdbOntologyService( "Gemma Unified Ontology", tdbDir, null, enabled, "unified" );
        OntologyServiceFactory<TdbOntologyService> factory = new OntologyServiceFactory<>( ontology );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( ontologyTaskExecutor() );
        factory.setProcessImports( false );
        // TODO: find a mechanism to tell TDB which ontologies should have precedence for specific URI prefixes
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
    }

    @Bean
    public FactoryBean<GemmaOntologyService> gemmaOntologyService() {
        OntologyServiceFactory<GemmaOntologyService> factory = createOntologyFactory( GemmaOntologyService.class );
        // TODO: remove this once https://github.com/PavlidisLab/TGEMO/pull/20 is merged
        factory.setProcessImports( false );
        return factory;
    }

    @Bean
    public FactoryBean<ExperimentalFactorOntologyService> experimentalFactorOntologyService() {
        return createOntologyFactory( ExperimentalFactorOntologyService.class, "http://www.ebi.ac.uk/efo/EFO_" );
    }

    @Bean
    public FactoryBean<ObiService> obiService() {
        return createOntologyFactory( ObiService.class, "http://purl.obolibrary.org/obo/OBI_" );
    }

    @Bean
    public FactoryBean<CellTypeOntologyService> cellTypeOntologyService() {
        return createOntologyFactory( CellTypeOntologyService.class, "http://purl.obolibrary.org/obo/CL_" );
    }

    @Bean
    public FactoryBean<ChebiOntologyService> chebiOntologyService() {
        OntologyServiceFactory<ChebiOntologyService> factory = createOntologyFactory( ChebiOntologyService.class, "http://purl.obolibrary.org/obo/CHEBI_" );
        factory.setInferenceMode( OntologyService.InferenceMode.NONE );
        return factory;
    }

    @Bean
    public FactoryBean<MammalianPhenotypeOntologyService> mammalianPhenotypeOntologyServiceOntologyService() {
        return createOntologyFactory( MammalianPhenotypeOntologyService.class, "http://purl.obolibrary.org/obo/MP_" );
    }

    @Bean
    public FactoryBean<HumanPhenotypeOntologyService> humanPhenotypeOntologyServiceOntologyService() {
        return createOntologyFactory( HumanPhenotypeOntologyService.class, "http://purl.obolibrary.org/obo/HP_" );
    }

    @Bean
    public FactoryBean<MouseDevelopmentOntologyService> mouseDevelopmentOntologyServiceOntologyService() {
        return createOntologyFactory( MouseDevelopmentOntologyService.class, "http://purl.obolibrary.org/obo/EMAPA_" );
    }

    @Bean
    @Deprecated
    public FactoryBean<HumanDevelopmentOntologyService> humanDevelopmentOntologyServiceOntologyService() {
        return createOntologyFactory( HumanDevelopmentOntologyService.class, "http://purl.obolibrary.org/obo/EHDAA2_" );
    }

    @Bean
    public FactoryBean<SequenceOntologyService> sequenceOntologyService() {
        return createOntologyFactory( SequenceOntologyService.class, "http://purl.obolibrary.org/obo/SO_" );
    }

    @Bean
    public FactoryBean<CellLineOntologyService> cellLineOntologyService() {
        return createOntologyFactory( CellLineOntologyService.class, "http://purl.obolibrary.org/obo/CLO_" );
    }

    @Bean
    public FactoryBean<UberonOntologyService> uberonOntologyServiceOntologyService() {
        return createOntologyFactory( UberonOntologyService.class, "http://purl.obolibrary.org/obo/UBERON_" );
    }

    @Bean
    public FactoryBean<PatoOntologyService> patoOntologyService() {
        return createOntologyFactory( PatoOntologyService.class, "http://purl.obolibrary.org/obo/PATO_" );
    }

    @Bean
    public FactoryBean<MondoOntologyService> mondoOntologyServiceOntologyService() {
        return createOntologyFactory( MondoOntologyService.class, "http://purl.obolibrary.org/obo/MONDO_" );
    }

    private <T extends OntologyService> OntologyServiceFactory<T> createOntologyFactory( Class<T> ontologyClass, String... allowedUriPrefixes ) {
        OntologyServiceFactory<T> factory = new OntologyServiceFactory<>( ontologyClass );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( ontologyTaskExecutor() );
        if ( allowedUriPrefixes.length > 0 ) {
            factory.setAllowedUriPrefixes( allowedUriPrefixes );
        }
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
    }
}
