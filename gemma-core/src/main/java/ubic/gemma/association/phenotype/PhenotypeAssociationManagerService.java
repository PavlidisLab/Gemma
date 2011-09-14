package ubic.gemma.association.phenotype;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

@Service
public interface PhenotypeAssociationManagerService {

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene id we want to add the evidence
     * @param evidence The evidence
     * @param phenotypes List of characteristics (phenotypes)
     * @return The Gene updated with the new evidence and characteristics
     */
    public GeneValueObject linkGeneToPhenotype( String geneNCBI, EvidenceValueObject evidence );

    /**
     * Removes an evidence from a Gene
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
     * Given an array of phenotypes returns Genes that have all those phenotypes for an evidence
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    public Collection<GeneValueObject> findCandidateGenes( String... phenotypesValues );

    /**
     * Get all phenotypes linked to genes and shows how many are linked to each genes
     * 
     * @return A collection of the phenotypes with the number of genes containing them
     */
    public Collection<CharacteristicValueObject> findAllPhenotypes();
}
