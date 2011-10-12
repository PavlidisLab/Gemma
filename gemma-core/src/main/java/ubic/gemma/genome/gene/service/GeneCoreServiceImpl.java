package ubic.gemma.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.GeneDetailsValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;

/** core service for Gene */
@Component
public class GeneCoreServiceImpl implements GeneCoreService {

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private HomologeneService homologeneService = null;

    @Autowired
    private SearchService searchService = null;

    /**
     * Returns a detailVO for a geneDd
     * 
     * @param geneId The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    public GeneDetailsValueObject loadGeneDetails( Long geneId ) {

        Gene gene = geneService.load( geneId );
        // need to thaw for aliases (at least)
        gene = geneService.thaw( gene );

        Collection<Long> ids = new HashSet<Long>();
        ids.add( gene.getId() );
        Collection<GeneValueObject> initialResults = geneService.loadValueObjects( ids );

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

        Long compositeSequenceCount = geneService.getCompositeSequenceCountById( geneId );
        details.setCompositeSequenceCount( compositeSequenceCount );

        Collection<GeneSet> genesets = geneSetSearch.findByGene( gene );
        Collection<GeneSetValueObject> gsvos = new ArrayList<GeneSetValueObject>();
        gsvos.addAll( DatabaseBackedGeneSetValueObject.convert2ValueObjects( genesets, false ) );
        details.setGeneSets( gsvos );

        Collection<Gene> geneHomologues = homologeneService.getHomologues( gene );
        Collection<GeneValueObject> homologues = GeneValueObject.convert2ValueObjects( geneHomologues );
        details.setHomologues( homologues );

        return details;

    }

    /**
     * Make a search using a Gene name, used in the interface to add new evidences
     * 
     * @param name The search name we are looking for
     * @return Collection all Gene name found for the search name entered
     */
    public Collection<GeneValueObject> searchByName( String name ) {

        /*
         * TODO for now the search is very simple depending on what we will need change the search criteria, also
         * similar code can be found on the controller and should be moved here
         */

        Collection<GeneValueObject> genesValueObject = new ArrayList<GeneValueObject>();

        SearchSettings settings = new SearchSettings();
        settings.setQuery( name );
        settings.setSearchGenes( true );
        settings.setUseDatabase( true );
        settings.setUseIndices( true );
        settings.setGeneralSearch( true );
        settings.setMaxResults( 100 );

        Map<Class<?>, List<SearchResult>> searchResults = searchService.search( settings );

        List<SearchResult> results = searchResults.get( Gene.class );

        if ( results != null ) {
            Collections.sort( results );

            for ( SearchResult searchR : results ) {
                genesValueObject.add( new GeneValueObject( ( Gene ) ( searchR.getResultObject() ) ) );
            }
        }
        return genesValueObject;
    }

}
