package ubic.gemma.association.phenotype;

import java.util.Collection;

import org.springframework.stereotype.Service;


import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

@Service
public interface PhenotypeAssociationManagerService {

    /**
     * Links an Evidence and phenotypes to a gene
     * 
     * @param geneNCBI The Gene id we want to add the evidence
     * @param evidence The evidence
     * @param phenotypes List of characteristics (phenotypes)
     * @return The Gene updated with the new evidence and characteristics
     */
    public GeneValueObject linkGeneToPhenotype( String geneNCBI, EvidenceValueObject evidence );

    /**
     * Removes an evidence froma Gene
     * 
     * @param geneNCBI The Evidence id
     */
    public void removePhenotypeAssociation( Long id );

    /**
     * Return a gene for a specific gene BNDI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public GeneValueObject findPhenotypeAssociations( String geneNCBI );

    /**
     * Given a phenotype returns the Genes that have this phenotype
     * 
     * @param value The value of the phenotype
     * @return A collection of the genes found
     */
    public Collection<GeneValueObject> findCandidateGenes( String value );
}
