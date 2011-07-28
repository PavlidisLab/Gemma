package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import ubic.gemma.model.genome.Gene;

public interface PhenotypeAssociationService {

    /** Using an phenotypeAssociation id removes the evidence */
    public void removePhenotypeAssociation( Long id );

    /** find Genes for a specific phenotype */
    public Collection<Gene> findCandidateGenes( String value );

}
