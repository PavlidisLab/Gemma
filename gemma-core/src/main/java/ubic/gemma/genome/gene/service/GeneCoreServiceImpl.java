package ubic.gemma.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.GeneDetailsValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

/** core service for Gene */
@Service
public class GeneCoreServiceImpl implements GeneCoreService {

    private static Log log = LogFactory.getLog( GeneCoreService.class );
    
    @Autowired
    private GeneService geneService = null;
    
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private HomologeneService homologeneService = null;

    @Autowired
    private SearchService searchService = null;

    @Autowired
    private TaxonService taxonService = null;

    /**
     * Returns a detailVO for a geneDd
     * 
     * @param geneId The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    @Override
    public GeneDetailsValueObject loadGeneDetails( long geneId ) {

        Gene gene = this.geneService.load( geneId );
        // need to thaw for aliases (at least)
        gene = this.geneService.thawAliases( gene );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( gene.getId() );
        Collection<GeneValueObject> initialResults = this.geneService.loadValueObjects( ids );

        if ( initialResults.size() == 0 ) {
            return null;
        }

        GeneValueObject initialResult = initialResults.iterator().next();
        GeneDetailsValueObject details = new GeneDetailsValueObject( initialResult );

        Collection<GeneAlias> aliasObjs = gene.getAliases();
        Collection<String> aliasStrs = new ArrayList<String>();
        for ( GeneAlias ga : aliasObjs ) {
            aliasStrs.add( ga.getAlias() );
        }
        details.setAliases( aliasStrs );

        Long compositeSequenceCount = this.geneService.getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount );

        Collection<GeneSet> genesets = this.geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( geneSetValueObjectHelper.convertToLightValueObjects( genesets, false ) );
        details.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = this.homologeneService.getHomologues( gene );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;

    }

    /**
     * Search for genes (by name or symbol)
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    @Override
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult> geneSearchResults = this.searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            log.info( "No Genes for search: " + query + " taxon=" + taxonId );
            return new HashSet<GeneValueObject>();
        }
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + geneSearchResults.size() + " found" );

        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
            log.debug( "Gene search result: " + ( ( Gene ) sr.getResultObject() ).getOfficialSymbol() );
        }

        Collection<GeneValueObject> geneValueObjects = GeneValueObject.convert2ValueObjects( genes );
        log.debug( "Gene search: " + geneValueObjects.size() + " value objects returned." );
        return geneValueObjects;
    }

}
