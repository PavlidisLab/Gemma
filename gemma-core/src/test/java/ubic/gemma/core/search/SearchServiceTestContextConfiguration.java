package ubic.gemma.core.search;

import net.sf.ehcache.CacheManager;
import org.springframework.context.annotation.Bean;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneSearchService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityDao;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import static org.mockito.Mockito.mock;

/**
 * Base class for mocking beans for
 */
class SearchServiceTestContextConfiguration {

    @Bean
    public SearchService searchService() {
        return new SearchServiceImpl();
    }

    @Bean
    public CacheManager cacheManager() {
        return mock( CacheManager.class );
    }

    @Bean
    public SearchSource compassSearchSource() {
        return mock( SearchSource.class );
    }

    @Bean
    public SearchSource databaseSearchSource() {
        return mock( SearchSource.class );
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
    public PhenotypeAssociationManagerService phenotypeAssociationManagerService() {
        return mock( PhenotypeAssociationManagerService.class );
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
}