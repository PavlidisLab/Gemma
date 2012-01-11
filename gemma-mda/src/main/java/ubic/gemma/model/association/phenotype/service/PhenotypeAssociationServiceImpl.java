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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidenceDao;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidenceDao;
import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.GenericExperimentDao;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidenceDao;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationDao;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.association.phenotype.UrlEvidenceDao;
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
    private ExperimentalEvidenceDao experimentalEvidenceDao;

    @Autowired
    private GenericEvidenceDao genericEvidenceDao;

    @Autowired
    private LiteratureEvidenceDao literatureEvidenceDao;

    @Autowired
    private UrlEvidenceDao urlEvidenceDao;

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
    public Collection<Gene> findPhenotypeAssociations( Set<String> phenotypesValueUri ) {
        return this.phenotypeAssociationDao.findByPhenotype( phenotypesValueUri );
    }

    /** find all phenotypes */
    @Override
    public Set<PhenotypeAssociation> loadAll() {
        return ( Set<PhenotypeAssociation> ) this.phenotypeAssociationDao.loadAll();
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

    /** load an GenericEvidence given an ID */
    @Override
    public GenericEvidence loadGenericEvidence( Long id ) {
        return this.genericEvidenceDao.load( id );
    }

    /** load an LiteratureEvidence given an ID */
    @Override
    public LiteratureEvidence loadLiteratureEvidence( Long id ) {
        return this.literatureEvidenceDao.load( id );
    }

    /** load an UrlEvidence given an ID */
    @Override
    public UrlEvidence loadUrlEvidence( Long id ) {
        return this.urlEvidenceDao.load( id );
    }

    /** load an ExperimentalEvidence given an ID */
    @Override
    public ExperimentalEvidence loadExperimentalEvidence( Long id ) {
        return this.experimentalEvidenceDao.load( id );
    }

    /** update an evidence */
    @Override
    public void update( PhenotypeAssociation evidence ) {
        this.phenotypeAssociationDao.update( evidence );
    }

    /**
     * @return all the characteristics (phenotypes) used in the system.
     */
    @Override
    public Set<CharacteristicValueObject> loadAllPhenotypes() {
        return this.phenotypeAssociationDao.loadAllPhenotypes();
    }

    /** load all valueURI of Phenotype in the database */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        return this.phenotypeAssociationDao.loadAllPhenotypesUri();
    }

    @Override
    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId ) {
        return this.phenotypeAssociationDao.findPhenotypesForBibliographicReference( pubMedId );
    }

    @Override
    public PhenotypeAssociation create( PhenotypeAssociation p ) {
        return this.phenotypeAssociationDao.create( p );
    }

    /**
     * count the number of Genes with a phenotype
     */
    @Override
    public Long countGenesWithPhenotype( Collection<String> phenotypesURI ) {
        return this.phenotypeAssociationDao.countGenesWithPhenotype( phenotypesURI );
    }

}
