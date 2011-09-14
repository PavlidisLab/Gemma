package ubic.gemma.association.phenotype;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService {

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene id we want to add the evidence
     * @param evidence The evidence
     * @param phenotypes List of characteristics (phenotypes)
     * @return The Gene updated with the new evidence and characteristics
     */
    public GeneValueObject linkGeneToPhenotype( String geneNCBI, EvidenceValueObject evidence ) {

        // find the gene we wish to add the evidence and phenotype
        Gene gene = geneService.findByNCBIId( geneNCBI );

        // convert the valueObject received to the corresponding Entity
        PhenotypeAssociation pheAsso = phenotypeAssoManagerServiceHelper.valueObject2Entity( evidence );

        // add the entity to the gene
        gene.getPhenotypeAssociations().add( pheAsso );

        // save result
        geneService.update( gene );

        // return the saved gene result
        return new GeneValueObject( gene );
    };

    /**
     * Return a gene for a specific gene BNDI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    public GeneValueObject findPhenotypeAssociations( String geneNCBI ) {

        Gene gene = geneService.findByNCBIId( geneNCBI );

        if ( gene == null || gene.getPhenotypeAssociations() == null || gene.getPhenotypeAssociations().size() == 0 ) {
            return null;
        }

        return new GeneValueObject( gene );
    }

    /**
     * Given an array of phenotypes returns Genes that have all those phenotypes for an evidence
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    public Collection<GeneValueObject> findCandidateGenes( String... phenotypesValues ) {

        Collection<Long> genesID = associationService.findCandidateGenes( phenotypesValues );

        return geneService.loadValueObjects( genesID );
    }

    /**
     * Get all phenotypes linked to genes and shows how many are linked to each genes
     * 
     * @return A collection of the phenotypes with the number of genes containing them
     */
    public Collection<CharacteristicValueObject> findAllPhenotypes() {
        return associationService.findAllPhenotypes();
    }

    /**
     * Removes an evidence from a Gene
     * 
     * @param geneNCBI The Evidence id
     */
    public void removePhenotypeAssociation( Long id ) {
        associationService.removePhenotypeAssociation( id );
    };

}
