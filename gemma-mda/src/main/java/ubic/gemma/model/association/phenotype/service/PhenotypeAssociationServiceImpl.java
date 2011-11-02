/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.association.phenotype.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
import ubic.gemma.model.genome.Gene;
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
    @Override
    public void remove( PhenotypeAssociation pa ) {
        pa.getGene().getPhenotypeAssociations().remove( pa );
        this.phenotypeAssociationDao.remove( pa );
    }

    /** find Genes link to a phenotype */
    @Override
    public Collection<Gene> findPhenotypeAssociations( String phenotypeValue ) {
        return this.phenotypeAssociationDao.findByPhenotype( phenotypeValue );
    }

    /** find all phenotypes */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<PhenotypeAssociation> loadAll() {
        return ( Collection<PhenotypeAssociation> ) this.phenotypeAssociationDao.loadAll();
    }

    /** create a GenericExperiment */
    @Override
    public GenericExperiment create( GenericExperiment genericExperiment ) {
        return this.genericExperimentDao.create( genericExperiment );
    }

    /** find GenericExperiments by PubMed ID */
    @Override
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return this.genericExperimentDao.findByPubmedID( pubmed );
    }

    /** load an evidence given an ID */
    @Override
    public PhenotypeAssociation load( Long id ) {
        return this.phenotypeAssociationDao.load( id );
    }

    /** update an evidence */
    @Override
    public void update( PhenotypeAssociation evidence ) {
        this.phenotypeAssociationDao.update( evidence );
    }

    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {
        return this.phenotypeAssociationDao.loadAllPhenotypes();
    }

    @Override
    public PhenotypeAssociation create( PhenotypeAssociation p ) {
        return this.phenotypeAssociationDao.create( p );
    }

    /** find the number of Genes with a phenotype */
    @Override
    public Long countGenesWithPhenotype( String phenotypeValue ) {
        return this.phenotypeAssociationDao.countGenesWithPhenotype( phenotypeValue );
    }

}
