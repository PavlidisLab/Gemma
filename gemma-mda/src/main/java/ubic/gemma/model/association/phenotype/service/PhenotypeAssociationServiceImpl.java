package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/**
 * Service responsible of low level operations, used by PhenotypeAssociationManagerServiceImpl
 */
@Service
public class PhenotypeAssociationServiceImpl implements PhenotypeAssociationService {

    @Autowired
    private PhenotypeAssociationDao phenotypeAssociationDao;

    @Autowired
    private GenericExperimentDao genericExperimentDao;

    /**
     * Using an phenotypeAssociation id removes the evidence
     */
    public void remove( PhenotypeAssociation pa ) {
        pa.getGene().getPhenotypeAssociations().remove( pa );
        phenotypeAssociationDao.remove( pa );
    }

    /** find Genes link to a phenotype */
    public Collection<PhenotypeAssociation> findPhenotypeAssociations( String phenotypeValue ) {
        return phenotypeAssociationDao.findByPhenotype( phenotypeValue );
    }

    /** find all phenotypes */
    public Collection<PhenotypeAssociation> loadAll() {
        return ( Collection<PhenotypeAssociation> ) phenotypeAssociationDao.loadAll();
    }

    /** create a GenericExperiment */
    public GenericExperiment create( GenericExperiment genericExperiment ) {
        return genericExperimentDao.create( genericExperiment );
    }

    /** find GenericExperiments by PubMed ID */
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return genericExperimentDao.findByPubmedID( pubmed );
    }

    /** load an evidence given an ID */
    public PhenotypeAssociation load( Long id ) {
        return phenotypeAssociationDao.load( id );
    }

    /** update an evidence */
    public void update( PhenotypeAssociation evidence ) {
        phenotypeAssociationDao.update( evidence );
    }

    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {
        return this.phenotypeAssociationDao.loadAllPhenotypes();
    }

    @Override
    public PhenotypeAssociation create( PhenotypeAssociation p ) {
        return this.phenotypeAssociationDao.create( p );
    }

}
