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
import ubic.gemma.model.genome.Taxon;
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
    public Collection<GeneEvidenceValueObject> findGeneWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable );

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
    public Set<String> loadAllPhenotypesUri();

    /** find PhenotypeAssociations associated with a BibliographicReference */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId );

    /** find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids, Long taxonId, Integer limit );
    
    /** find all PhenotypeAssociation for a specific gene id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /** find all PhenotypeAssociation for a specific NCBI id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /** find mged category term that were used in the database, used to annotated Experiments */
    public Collection<CharacteristicValueObject> findEvidenceMgedCategoryTerms();

    /** find all evidences from a specific external database */
    @Secured({ "GROUP_ADMIN" })
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName );
    
    /** find all evidences with no external database */
    @Secured({ "GROUP_ADMIN" })
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName );

    /** find private evidence id that the user can modifiable or own */
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups );

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon,
            Set<String> valuesUri, String userName, Collection<String> groups );
    
    /** return the list of the owners that have evidence in the system */
    public Collection<String> findEvidenceOwners();

}
