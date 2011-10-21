package ubic.gemma.genome.gene.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.GeneDetailsValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/** core service for Gene */
@Service
public interface GeneCoreService {

    /**
     * Returns a detailVO for a geneId
     * 
     * @param geneId The gene id
     * @return GeneDetailsValueObject a representation of that gene
     */
    public GeneDetailsValueObject loadGeneDetails( Long geneId );
    

    /**
     * Search for genes (by name or symbol)
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId );

}
