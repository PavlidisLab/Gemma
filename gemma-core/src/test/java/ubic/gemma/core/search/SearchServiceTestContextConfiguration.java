package ubic.gemma.core.search;

import org.hibernate.SessionFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.source.DatabaseSearchSource;
import ubic.gemma.core.search.source.HibernateSearchSource;
import ubic.gemma.core.search.source.OntologySearchSource;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.gene.GeneSearchService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.ValueObjectConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for mocking beans for
 */
@Import(ValueObjectConfig.class)
class SearchServiceTestContextConfiguration {

    @Bean
    public SearchService searchService() {
        return new SearchServiceImpl();
    }

    @Bean
    public CacheManager cacheManager() {
        CacheManager cacheManager = mock( CacheManager.class );
        when( cacheManager.getCache( any() ) ).thenAnswer( a -> new ConcurrentMapCache( a.getArgument( 0 ) ) );
        return cacheManager;
    }

    @Bean
    public SearchSource databaseSearchSource() {
        return mock( DatabaseSearchSource.class );
    }

    @Bean
    public SearchSource ontologySearchSource() {
        return mock( OntologySearchSource.class );
    }

    @Bean
    public SearchSource hibernateSearchSource() {
        return mock( HibernateSearchSource.class );
    }

    @Bean
    public GeneProductService geneProductService() {
        return mock( GeneProductService.class );
    }

    @Bean
    public ArrayDesignService arrayDesignService() {
        return mock( ArrayDesignService.class );
    }

    @Bean
    public CharacteristicService characteristicService() {
        return mock( CharacteristicService.class );
    }

    @Bean
    public ExpressionExperimentService expressionExperimentService() {
        return mock( ExpressionExperimentService.class );
    }

    @Bean
    public ExpressionExperimentSetService experimentSetService() {
        return mock( ExpressionExperimentSetService.class );
    }

    @Bean
    public GeneSearchService geneSearchService() {
        return mock( GeneSearchService.class );
    }

    @Bean
    public GeneService geneService() {
        return mock( GeneService.class );
    }

    @Bean
    public GeneSetService geneSetService() {
        return mock( GeneSetService.class );
    }

    @Bean
    public OntologyService ontologyService() {
        return mock( OntologyService.class );
    }

    @Bean
    public BioSequenceService bioSequenceService() {
        return mock( BioSequenceService.class );
    }

    @Bean
    public CompositeSequenceService compositeSequenceService() {
        return mock( CompositeSequenceService.class );
    }

    @Bean
    public BlacklistedEntityService blacklistedEntityService() {
        return mock( BlacklistedEntityService.class );
    }

    @Bean
    public TaxonService taxonService() {
        return mock( TaxonService.class );
    }

    @Bean
    public BibliographicReferenceService bibliographicReferenceService() {
        return mock( BibliographicReferenceService.class );
    }

    @Bean
    public SessionFactory sessionFactory() {
        return mock( SessionFactory.class );
    }
}