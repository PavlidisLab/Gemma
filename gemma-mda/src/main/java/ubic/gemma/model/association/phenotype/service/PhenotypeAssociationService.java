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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;

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
    public Collection<GeneEvidenceValueObject> findGeneWithPhenotypes( Set<String> phenotypesValueUri, String taxon,
            String userName, boolean isAdmin );

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
    public Collection<PhenotypeAssociation> loadAll();

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

    /**
     * update a PhenotypeAssociation
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( PhenotypeAssociation evidence );

    /** load all valueURI of Phenotype in the database */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    public Set<String> loadAllPhenotypesUri();

    /** find PhenotypeAssociations associated with a BibliographicReference */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId );

    /** find all PhenotypeAssociation for a specific gene id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /** find all PhenotypeAssociation for a specific NCBI id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /** find mged category term that were used in the database, used to annotated Experiments */
    public Collection<CharacteristicValueObject> findEvidenceMgedCategoryTerms();

    /** delete all evidences from a specific external database */
    @Secured({ "GROUP_ADMIN" })
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName );

    /** find all public phenotypes associated with genes */
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations();

    /** find all phenotypes associated with genes for a user */
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( String userName );

    /** find all phenotypes associated with genes */
    public HashMap<String, HashSet<Integer>> findAllPhenotypesGenesAssociations();

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( String taxon, Set<String> valuesUri );

}
