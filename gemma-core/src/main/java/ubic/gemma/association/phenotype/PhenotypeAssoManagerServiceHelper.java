package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.Set;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;

public interface PhenotypeAssoManagerServiceHelper {

    /**
     * Changes all type of evidenceValueObject to their corresponding entities
     * 
     * @param evidence the value object to change in an entity
     * @return PhenotypeAssociation the entity created from the ValueObject
     */
    public abstract PhenotypeAssociation valueObject2Entity( EvidenceValueObject evidence );

    /**
     * Sets the fields that are the same for any evidence.
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public abstract void populatePhenotypeAssociation( PhenotypeAssociation phe, EvidenceValueObject evidenceValueObject );

    /**
     * Sets the fields that are the same for any evidence. Doesn't populate phenotypes
     * 
     * @param phe The phenotype association (parent class of an evidence) we are interested in populating
     * @param evidenceValueObject the value object representing a phenotype
     */
    public abstract void populatePheAssoWithoutPhenotypes( PhenotypeAssociation phe,
            EvidenceValueObject evidenceValueObject );

    /** Ontology term to CharacteristicValueObject */
    public abstract Set<CharacteristicValueObject> ontology2CharacteristicValueObject(
            Collection<OntologyTerm> ontologyTerms, String ontologyUsed );

    // load evidence from the database and populate it with the updated information
    public abstract void populateModifiedValues( EvidenceValueObject evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation );
    
    public abstract void setOntologyHelper( PhenotypeAssoOntologyHelper ontologyHelper );

}