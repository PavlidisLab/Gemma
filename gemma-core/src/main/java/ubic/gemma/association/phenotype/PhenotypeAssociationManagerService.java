package ubic.gemma.association.phenotype;

import java.util.Collection;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidencesValueObject;

public interface PhenotypeAssociationManagerService {

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return The Gene updated with the new evidence and phenotypes
     */
    public GeneEvidencesValueObject create( String geneNCBI, EvidenceValueObject evidence );

    /**
     * Return all evidences for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public Collection<EvidenceValueObject> findEvidencesByGeneNCBI( String geneNCBI );

    /**
     * Return all evidences for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    public Collection<EvidenceValueObject> findEvidencesByGeneId( Long geneId );

    /**
     * Given an array of phenotypes returns the genes that have all those phenotypes
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    public Collection<GeneEvidencesValueObject> findCandidateGenes( String... phenotypesValues );

    /**
     * Get all phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    public Collection<CharacteristicValueObject> findAllPhenotypes();

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    public void removeEvidence( Long id );

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    public void modifyEvidence( EvidenceValueObject evidenceValueObject );

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery );

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @param geneId the id of the gene chosen
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery, Long geneId );
    

}
