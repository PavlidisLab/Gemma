package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

public interface PhenotypeAssociationService {

    /** Using an phenotypeAssociation id removes the evidence */
    public void removePhenotypeAssociation( Long id );

    /** find Genes link to a phenotype */
    public Collection<Gene> findCandidateGenes( String phenotypeValue );

    /** create a GenericExperiment */
    public GenericExperiment createGenericExperiment( GenericExperiment genericExperiment );

    /** find all phenotypes in Gemma */
    public Collection<CharacteristicValueObject> findAllPhenotypes();

    /** find GenericExperiments by PubMed ID */
    public Collection<GenericExperiment> findByPubmedID( String pubmed );

}
