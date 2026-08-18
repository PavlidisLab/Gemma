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
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import ubic.gemma.core.metrics.binder.VirtualThreadExecutorMetrics;
import ubic.gemma.core.ontology.jena.OntologyLoader;
import ubic.gemma.core.ontology.jena.TdbOntologyService;
import ubic.gemma.core.ontology.providers.*;
import ubic.gemma.core.ontology.providers.OntologyService;
import ubic.gemma.core.ontology.providers.OntologySlimExtractor;
import ubic.gemma.core.ontology.providers.chebi.ChebiSeedResolver;
import ubic.gemma.core.context.EnvironmentProfiles;
import ubic.gemma.core.ontology.providers.GemmaOntologyService;
import ubic.gemma.core.ontology.providers.MondoOntologyService;
import ubic.gemma.core.ontology.providers.mondo.MondoSeedResolver;
import ubic.gemma.core.ontology.providers.OntologyServiceFactory;
import ubic.gemma.core.ontology.providers.PatoOntologyService;
import ubic.gemma.core.ontology.search.JenaTextOntologySearchService;
import ubic.gemma.core.ontology.search.OntologySearchService;
import ubic.gemma.core.util.TextResourceToSetOfLinesFactoryBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Configuration
@Profile({ "!" + EnvironmentProfiles.TEST }) // we use a different set of ontologies in tests
public class OntologyConfig {

    @Value("${load.ontologies}")
    private boolean loadOntologies;

    // FIXME: inject it as a Set<String>, but Spring interpret this as a set of String beans
    @Autowired
    @Qualifier("excludedWordsFromStemming")
    private TextResourceToSetOfLinesFactoryBean excludedWordsFromStemming;

    @Autowired
    @Qualifier("ontologyTaskExecutorMetrics")
    private VirtualThreadExecutorMetrics ontologyTaskExecutorMetrics;

    /**
     * Executor used for loading ontologies in background.
     * <p>
     * JDK 21 migration: backed by a virtual-thread-per-task executor wrapped through Spring's
     * {@link ConcurrentTaskExecutor}. The {@code gemma.ontology.loader.corePoolSize} property no
     * longer constrains this executor.
     * <p>
     * Risk note: ontology loaders delegate into baseCode's {@code OntologyService}, whose locking
     * posture is not controlled by Gemma. The first production boot on JDK 21 should run with
     * {@code -Djdk.tracePinnedThreads=full} so any carrier-thread pinning in the baseCode loader
     * is caught early.
     */
    @Bean
    public TaskExecutor ontologyTaskExecutor() {
        return new ConcurrentTaskExecutor( ontologyTaskExecutorMetrics.wrap(
                ubic.gemma.core.util.concurrent.Executors.newVirtualThreadPerTaskExecutorIfAvailable() ) );
    }

    /**
     * Micrometer binder for the {@link #ontologyTaskExecutor} VT executor. Picked up by
     * {@code MetricsConfig#genericMeterRegistryConfigurer} when the {@code metrics} profile is active.
     */
    @Bean
    public VirtualThreadExecutorMetrics ontologyTaskExecutorMetrics() {
        return new VirtualThreadExecutorMetrics( "ontologyTaskExecutor" );
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
            @Value("${gemma.ontology.unified.tdb.dir}") Path tdbDir,
            @Value("${gemma.ontology.unified.tdb.tempDir}") Path tdbTempDir
    ) {
        TdbOntologyService ontology = new TdbOntologyService( "Gemma Unified Ontology", tdbDir,
                null, enabled, "unified", true );
        ontology.setTempDir( tdbTempDir );
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

    /**
     * Bean for the CHEBI ontology with slim-cache wiring.
     * <p>
     * Uses the pre-built {@link OntologyServiceFactory#OntologyServiceFactory(OntologyService)}
     * constructor rather than the reflective class-instantiation path so the slim extractor
     * and seed resolver land on the service instance BEFORE the factory's auto-load thread
     * calls {@code initialize()}. Spring's autowiring doesn't run on FactoryBean products by
     * default, so explicit setter wiring here is the safer pattern.
     */
    @Bean
    public FactoryBean<ChebiOntologyService> chebiOntologyService(
            @Autowired(required = false) OntologySlimExtractor slimExtractor,
            @Autowired(required = false) ChebiSeedResolver seedResolver ) {
        ChebiOntologyService service = new ChebiOntologyService();
        // When slimExtractor or seedResolver are absent (test contexts that don't import
        // the chebi/ stereotypes), the service falls back to UrlOntologyService's full-load
        // behaviour. Both null + the slim-cache-dir below results in the legacy load path.
        service.setSlimExtractor( slimExtractor );
        service.setSeedResolver( seedResolver );
        if ( slimExtractor != null && seedResolver != null ) {
            // Slim file lands alongside the cached source: ${ontology.cache.dir}/ontology/.
            // Derive from OntologyLoader so any future change to the path convention stays in
            // one place.
            File cacheDir = OntologyLoader.getDiskCachePath( "chebiOntology" ).getParentFile();
            service.setSlimCacheDir( cacheDir );
        }

        OntologyServiceFactory<ChebiOntologyService> factory = new OntologyServiceFactory<>( service );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( ontologyTaskExecutor() );
        factory.setInferenceMode( OntologyService.InferenceMode.NONE );
        factory.setAllowedUriPrefixes( new String[]{ "http://purl.obolibrary.org/obo/CHEBI_" } );
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
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
    public FactoryBean<SequenceOntologyService> sequenceOntologyService() {
        return createOntologyFactory( SequenceOntologyService.class, "http://purl.obolibrary.org/obo/SO_" );
    }

    @Bean
    public FactoryBean<CellLineOntologyService> cellLineOntologyService() {
        return createOntologyFactory( CellLineOntologyService.class, "http://purl.obolibrary.org/obo/CLO_" );
    }

    @Bean
    public FactoryBean<CellosaurusOntologyService> cellosaurusOntologyService() {
        return createOntologyFactory( CellosaurusOntologyService.class, CellosaurusOntologyService.URI_PREFIX + "CVCL_" );
    }

    @Bean
    public FactoryBean<MgiStrainOntologyService> mgiStrainOntologyService() {
        return createOntologyFactory( MgiStrainOntologyService.class, MgiStrainOntologyService.URI_PREFIX + "MGI:" );
    }

    @Bean
    public FactoryBean<NeuroBehaviorOntologyService> neuroBehaviorOntologyService() {
        return createOntologyFactory( NeuroBehaviorOntologyService.class, "http://purl.obolibrary.org/obo/NBO_" );
    }

    /**
     * Extra search strings for MONDO disease terms, not a vocabulary of its own — the URI prefix is
     * MONDO's, which also fences the table to MONDO URIs should the builder ever emit anything else.
     */
    @Bean
    public FactoryBean<MeshDiseaseSynonymOntologyService> meshDiseaseSynonymOntologyService() {
        return createOntologyFactory( MeshDiseaseSynonymOntologyService.class,
                MeshDiseaseSynonymOntologyService.URI_PREFIX );
    }

    @Bean
    public FactoryBean<UberonOntologyService> uberonOntologyServiceOntologyService() {
        return createOntologyFactory( UberonOntologyService.class, "http://purl.obolibrary.org/obo/UBERON_" );
    }

    @Bean
    public FactoryBean<PatoOntologyService> patoOntologyService() {
        return createOntologyFactory( PatoOntologyService.class, "http://purl.obolibrary.org/obo/PATO_" );
    }

    /**
     * MONDO with slim-cache wiring, parallel to {@link #chebiOntologyService}. Uses the
     * pre-built {@link OntologyServiceFactory#OntologyServiceFactory(OntologyService)}
     * constructor so the slim plumbing lands BEFORE the factory's auto-load thread runs.
     */
    @Bean
    public FactoryBean<MondoOntologyService> mondoOntologyServiceOntologyService(
            @Autowired(required = false) OntologySlimExtractor slimExtractor,
            @Autowired(required = false) MondoSeedResolver seedResolver ) {
        MondoOntologyService service = new MondoOntologyService();
        service.setSlimExtractor( slimExtractor );
        service.setSeedResolver( seedResolver );
        if ( slimExtractor != null && seedResolver != null ) {
            File cacheDir = OntologyLoader.getDiskCachePath( "mondoOntology" ).getParentFile();
            service.setSlimCacheDir( cacheDir );
        }
        OntologyServiceFactory<MondoOntologyService> factory = new OntologyServiceFactory<>( service );
        factory.setAutoLoad( loadOntologies );
        factory.setTaskExecutor( ontologyTaskExecutor() );
        factory.setAllowedUriPrefixes( new String[]{ "http://purl.obolibrary.org/obo/MONDO_" } );
        try {
            factory.setExcludedWordsFromStemming( excludedWordsFromStemming.getObject() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
        return factory;
    }

    /**
     * In-Gemma jena-text / Lucene-9 ontology full-text search service
     * (Phase 3 search restoration; see {@code SEARCH_RECCE.md} Section 6).
     * Wraps the unified-ontology TDB as a {@code TextDataset}. Disabled when
     * {@code gemma.ontology.unified.enabled=false}.
     */
    @Bean(destroyMethod = "close")
    public OntologySearchService ontologySearchService(
            @Value("${gemma.ontology.unified.enabled}") boolean enabled,
            @Value("${gemma.ontology.unified.tdb.dir}") Path tdbDir
    ) {
        return new JenaTextOntologySearchService( tdbDir, enabled );
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
