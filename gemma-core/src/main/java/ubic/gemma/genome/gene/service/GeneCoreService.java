package ubic.gemma.genome.gene.service;

import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.GeneDetailsValueObject;

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

}
