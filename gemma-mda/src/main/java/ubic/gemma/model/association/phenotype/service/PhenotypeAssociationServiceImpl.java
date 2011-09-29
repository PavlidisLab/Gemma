package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/**
 * Service responsible of low level operations, used by PhenotypeAssociationManagerServiceImpl
 */
@Component
public class PhenotypeAssociationServiceImpl implements PhenotypeAssociationService {

    @Autowired
    private PhenotypeAssociationDao phenotypeAssociationDao;

    @Autowired
    private GenericExperimentDao genericExperimentDao;

    /** Using an phenotypeAssociation id removes the evidence */
    public void removePhenotypeAssociation( Long id ) {
        phenotypeAssociationDao.remove( id );
    }

    /** find Genes link to a phenotype */
    public Collection<Gene> findCandidateGenes( String phenotypeValue ) {
        return phenotypeAssociationDao.findByPhenotype( phenotypeValue );
    }

    /** find all phenotypes in Gemma */
    public Collection<CharacteristicValueObject> findAllPhenotypes() {
        return phenotypeAssociationDao.findAllPhenotypes();
    }

    /** create a GenericExperiment */
    public GenericExperiment createGenericExperiment( GenericExperiment genericExperiment ) {
        return genericExperimentDao.create( genericExperiment );
    }

    /** find GenericExperiments by PubMed ID */
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return genericExperimentDao.findByPubmedID( pubmed );
    }
    
    /** load an evidence given an ID */
    public PhenotypeAssociation loadEvidence( Long id ) {
        return phenotypeAssociationDao.load( id );
    }
    
    /** update an evidence */
    public void updateEvidence( PhenotypeAssociation evidence ) {
        phenotypeAssociationDao.update( evidence );
    }

}
