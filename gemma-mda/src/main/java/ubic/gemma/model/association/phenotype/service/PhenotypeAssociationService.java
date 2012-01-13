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

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.UrlEvidence;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/**
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssociationService {

    /**
     * Using an phenotypeAssociation id removes the evidence
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void remove( PhenotypeAssociation p );

    @Secured({ "GROUP_USER" })
    public PhenotypeAssociation create( PhenotypeAssociation p );

    /**
     * find Genes link to a phenotype
     * 
     * @param phenotypesValueUri The Ontology valueURI of the phenotype
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<Gene> findGeneWithPhenotypes( Set<String> phenotypesValueUri );

    /**
     * create a GenericExperiment
     * 
     * @param genericExperiment
     */
    @Secured({ "GROUP_USER" })
    public GenericExperiment create( GenericExperiment genericExperiment );

    /**
     * find all phenotypes in Gemma
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Set<PhenotypeAssociation> loadAll();

    /**
     * @return all the characteristics (phenotypes) used in the system.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Set<CharacteristicValueObject> loadAllPhenotypes();

    /**
     * find GenericExperiments by PubMed ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<GenericExperiment> findByPubmedID( String pubmed );

    /**
     * load PhenotypeAssociation given an ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public PhenotypeAssociation load( Long id );

    /**
     * load PhenotypeAssociation given an ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExperimentalEvidence loadExperimentalEvidence( Long id );

    /** load an GenericEvidence given an ID */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public GenericEvidence loadGenericEvidence( Long id );

    /** load an LiteratureEvidence given an ID */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public LiteratureEvidence loadLiteratureEvidence( Long id );

    /** load an UrlEvidence given an ID */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public UrlEvidence loadUrlEvidence( Long id );

    /**
     * update a PhenotypeAssociation
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( PhenotypeAssociation evidence );

    /**
     * count the number of Genes with a phenotype
     */
    public Long countGenesWithPhenotype( Collection<String> phenotypesURI );

    /** load all valueURI of Phenotype in the database */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Set<String> loadAllPhenotypesUri();

    /** find PhenotypeAssociations associated with a BibliographicReference */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId );

}
