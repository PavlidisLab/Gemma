package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
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

    /** find Genes for specific phenotypes */
    public Collection<Long> findCandidateGenes( String... phenotypesValues ) {
        return phenotypeAssociationDao.findByPhenotype( phenotypesValues );
    }

    /** find all phenotypes in Gemma */
    public Collection<CharacteristicValueObject> findAllPhenotypes() {
        return phenotypeAssociationDao.findAllPhenotypes();
    }

    /** create a GenericExperiment */
    public GenericExperiment createGenericExperiment( GenericExperiment genericExperiment ) {
        return genericExperimentDao.create( genericExperiment );
    }

}
