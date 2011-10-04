package ubic.gemma.genome.gene.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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

/** core service for Gene */
@Component
public class GeneCoreServiceImpl implements GeneCoreService {

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private HomologeneService homologeneService = null;

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

}
