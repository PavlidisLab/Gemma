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

    @Bean
    public FactoryBean<GenotypeOntologyService> genotypeOntologyService() {
        return createOntologyFactory( GenotypeOntologyService.class, "http://purl.obolibrary.org/obo/GENO_" );
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
     * MONDO, parallel to {@link #chebiOntologyService}. Uses the pre-built
     * {@link OntologyServiceFactory#OntologyServiceFactory(OntologyService)} constructor so any slim plumbing
     * lands BEFORE the factory's auto-load thread runs.
     * <p>
     * 🛑 <b>The slim is OFF for MONDO by default, and the seeding is why.</b>
     * {@link MondoSeedResolver#resolveCorpusSeeds()} seeds from MONDO terms the corpus ALREADY uses, so the slim
     * holds what we have annotated and nothing else. That is fine for looking up a term we already applied and
     * wrong for every question about a term we have not: notably, when EFO obsoletes a term and names a MONDO
     * successor, that successor is by definition a term we do not use yet, so the slim cannot contain it. Measured
     * 2026-08-19: the slim served 9,989 classes against 36,083 in the release, and 22 obsolete terms were
     * unfixable purely because their named replacement was missing.
     * <p>
     * Loading the full source costs a ~250 MB parse at startup (the slim loaded in ~15s). Set
     * {@code gemma.ontology.mondo.slim.enabled=true} to go back to the slim on a host that cannot afford it.
     */
    @Bean
    public FactoryBean<MondoOntologyService> mondoOntologyServiceOntologyService(
            @Autowired(required = false) OntologySlimExtractor slimExtractor,
            @Autowired(required = false) MondoSeedResolver seedResolver,
            @Value("${gemma.ontology.mondo.slim.enabled:false}") boolean mondoSlimEnabled ) {
        MondoOntologyService service = new MondoOntologyService();
        service.setSlimExtractor( slimExtractor );
        service.setSeedResolver( seedResolver );
        // Leaving the slim cache dir unset is what disables the slim: resolveSlimFile() then yields null and
        // loadModel falls through to the full source. An already-cached slim file is left on disk, never read.
        if ( mondoSlimEnabled && slimExtractor != null && seedResolver != null ) {
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

    /**
     * Writes the {@code ONTOLOGY} rows of {@code ANNOTATION_RELATION} — the relations CLO and CHEBI
     * already assert and nothing has ever read.
     *
     * <p>Declared here rather than component-scanned so it lives and dies with the ontologies it reads:
     * a context with no ontology services has nothing for it to do, and {@code TableMaintenanceUtil}
     * takes it optionally for exactly that reason.</p>
     *
     * <p>The ontologies arrive as the whole list and are matched by name through
     * {@link ubic.gemma.core.ontology.providers.OntologyServiceResolver}, so the producer holds no
     * bean-level dependency on any one of them and a disabled ontology is a warning rather than a
     * startup failure.</p>
     */
    @Bean
    public ubic.gemma.core.ontology.relation.OntologyRelationProducer ontologyRelationProducer(
            @Autowired(required = false) java.util.List<OntologyService> ontologies,
            ubic.gemma.persistence.service.common.description.AnnotationRelationDao annotationRelationDao,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            @Autowired(required = false) ubic.gemma.persistence.service.genome.taxon.TaxonService taxonService,
            @Autowired(required = false) ubic.gemma.core.ontology.OntologyService ontologyService ) {
        return new ubic.gemma.core.ontology.relation.OntologyRelationProducerImpl( ontologies, annotationRelationDao,
                new org.springframework.transaction.support.TransactionTemplate( transactionManager ), taxonService,
                ontologyService );
    }

    /**
     * The entailed CL cell type &rarr; anatomical structure locations, from a reviewed file.
     *
     * <p>Beside the other two file-backed producers and wired the same way. It reads no ontology
     * model at all: the rows were adjudicated offline and are shipped as a classpath resource, so
     * the only collaborators are the DAO and a transaction.</p>
     */
    @Bean
    public ubic.gemma.core.ontology.relation.ClInferredLocationProducer clInferredLocationProducer(
            ubic.gemma.persistence.service.common.description.AnnotationRelationDao annotationRelationDao,
            org.springframework.transaction.PlatformTransactionManager transactionManager ) {
        return new ubic.gemma.core.ontology.relation.ClInferredLocationProducer( annotationRelationDao,
                new org.springframework.transaction.support.TransactionTemplate( transactionManager ) );
    }

    /**
     * MGI's genotype-to-disease reports as {@code EXTERNAL} relations.
     *
     * <p>Declared here beside {@link #ontologyRelationProducer} because it shares the one thing that
     * makes either work: MONDO, which is what MGI's {@code DOID:} identifiers are translated out of.
     * It reads no other ontology — the statements themselves come off MGI's download server.</p>
     */
    @Bean
    public ubic.gemma.core.ontology.relation.MgiRelationProducer mgiRelationProducer(
            @Autowired(required = false) java.util.List<OntologyService> ontologies,
            ubic.gemma.persistence.service.common.description.AnnotationRelationDao annotationRelationDao,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            @Autowired(required = false) ubic.gemma.persistence.service.genome.taxon.TaxonService taxonService ) {
        return new ubic.gemma.core.ontology.relation.MgiRelationProducer( ontologies, annotationRelationDao,
                new org.springframework.transaction.support.TransactionTemplate( transactionManager ),
                taxonService );
    }

    /**
     * Cellosaurus as {@code EXTERNAL} relations — donor disease and derived-from site.
     *
     * <p>Beside {@link #mgiRelationProducer} and for the same reason: MONDO is what its {@code NCIt:}
     * disease identifiers are translated out of. It reads no ontology model otherwise; the statements
     * come from the cached Cellosaurus artifact the lexical service already downloads.</p>
     */
    @Bean
    public ubic.gemma.core.ontology.relation.CellosaurusRelationProducer cellosaurusRelationProducer(
            @Autowired(required = false) java.util.List<OntologyService> ontologies,
            ubic.gemma.persistence.service.common.description.AnnotationRelationDao annotationRelationDao,
            org.springframework.transaction.PlatformTransactionManager transactionManager,
            @Autowired(required = false) ubic.gemma.persistence.service.genome.taxon.TaxonService taxonService ) {
        return new ubic.gemma.core.ontology.relation.CellosaurusRelationProducer( ontologies,
                annotationRelationDao,
                new org.springframework.transaction.support.TransactionTemplate( transactionManager ),
                taxonService );
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
