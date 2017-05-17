package ubic.gemma.core.genome.gene.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * core service for Gene
 *
 * @author Nicolas
 */
@Component
public class GeneCoreServiceImpl implements GeneCoreService {

    private static final Log log = LogFactory.getLog( GeneCoreService.class );

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private SearchService searchService = null;

    @Autowired
    private TaxonService taxonService = null;

    /**
     * Returns a detailVO for a geneDd This method may be unnecessary now that we have put all the logic into the
     * GeneService
     *
     * @param geneId The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    @Override
    public GeneValueObject loadGeneDetails( long geneId ) {

        return this.geneService.loadFullyPopulatedValueObject( geneId );

    }

    /**
     * Search for genes (by name or symbol)
     *
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    @Override
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettingsImpl.geneSearch( query, taxon );
        List<SearchResult> geneSearchResults = this.searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            log.info( "No Genes for search: " + query + " taxon=" + taxonId );
            return new HashSet<>();
        }
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + geneSearchResults.size() + " found" );

        for ( SearchResult sr : geneSearchResults ) {
            Gene g = ( Gene ) sr.getResultObject();
            g = geneService.thaw( g );
            genes.add( g );
            log.debug( "Gene search result: " + g.getOfficialSymbol() );
        }
        Collection<GeneValueObject> geneValueObjects = GeneValueObject.convert2ValueObjects( genes );
        log.debug( "Gene search: " + geneValueObjects.size() + " value objects returned." );
        return geneValueObjects;
    }

}
