package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
import ubic.gemma.model.genome.Gene;

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

    /** find Genes for a specific phenotype */
    public Collection<Gene> findCandidateGenes( String value ) {

        return phenotypeAssociationDao.findByPhenotype( value );
    }

    /** create a GenericExperiment */
    public GenericExperiment createGenericExperiment( GenericExperiment genericExperiment ) {
        return genericExperimentDao.create( genericExperiment );
    }

}
