package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public interface PhenotypeAssociationService {

    /** Using an phenotypeAssociation id removes the evidence */
    public void removePhenotypeAssociation( Long id );

    /** find Genes for specific phenotypes */
    public Collection<Long> findCandidateGenes( String... phenotypesValues );

    /** create a GenericExperiment */
    public GenericExperiment createGenericExperiment( GenericExperiment genericExperiment );

    /** find all phenotypes in Gemma */
    public Collection<CharacteristicValueObject> findAllPhenotypes();

}
