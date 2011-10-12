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
     * Make a search using a Gene name, used in the interface to add new evidences
     * 
     * @param name The search name we are looking for
     * @return Collection all Gene name found for the search name entered
     */
    public Collection<GeneValueObject> searchByName(String name);

}
