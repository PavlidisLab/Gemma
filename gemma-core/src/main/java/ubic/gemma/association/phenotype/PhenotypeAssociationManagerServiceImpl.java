package ubic.gemma.association.phenotype;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService {

    @Autowired
    PhenotypeAssociationService associationService;

    @Autowired
    GeneService geneService;

    /**
     * Links an Evidence and phenotypes to a gene
     * 
     * @param geneNCBI The Gene id we want to add the evidence
     * @param evidence The evidence
     * @param phenotypes List of characteristics (phenotypes)
     * @return The Gene updated with the new evidence and characteristics
     */
    public GeneValueObject linkGeneToPhenotype( String geneNCBI, EvidenceValueObject evidence ) {

        // find the gene we wish to add the evidence and phenotype
        Gene gene = geneService.findByNCBIId( geneNCBI );

        // use polymorphism to create the good entity from the value object
        PhenotypeAssociation phenotypeAssociation = evidence.createEntity();

        // add the new phenotype association to the gene
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

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

        if ( gene.getPhenotypeAssociations() == null || gene.getPhenotypeAssociations().size() == 0 ) {
            return null;
        }

        return new GeneValueObject( gene );
    }

    /**
     * Given a phenotype returns the Genes that have this phenotype
     * 
     * @param value The value of the phenotype
     * @return A collection of the genes found
     */
    public Collection<GeneValueObject> findCandidateGenes( String value ) {

        Collection<Gene> genes = associationService.findCandidateGenes( value );

        return GeneValueObject.convert2ValueObjects( genes );
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
