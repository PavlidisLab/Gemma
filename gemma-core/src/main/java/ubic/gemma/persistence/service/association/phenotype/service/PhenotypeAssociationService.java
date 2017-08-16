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
package ubic.gemma.persistence.service.association.phenotype.service;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.annotation.Secured;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.GenericExperiment;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;

/**
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssociationService {

    /**
     * Using an phenotypeAssociation id removes the evidence
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( PhenotypeAssociation p );

    @Secured({ "GROUP_USER" })
    PhenotypeAssociation create( PhenotypeAssociation p );

    /**
     * find Genes link to a phenotype
     * 
     * @param phenotypesValueUri The Ontology valueURI of the phenotype
     */
    Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            boolean showOnlyEditable, Collection<Long> externalDatabaseIds );

    /**
     * @param phenotype
     * @param taxonId
     * @param includeIEA
     * @return map of gene value objects to ontology terms, where the term should be the most specific term relative to
     *         the phenotype that was queries (that is, it may be a child term).
     * @author paul
     */
    Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm phenotype, Long taxonId, boolean includeIEA );

    /**
     * create a GenericExperiment
     * 
     * @param genericExperiment
     */
    @Secured({ "GROUP_USER" })
    GenericExperiment create( GenericExperiment genericExperiment );

    /**
     * find all phenotypes in Gemma
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> loadAll();

    /**
     * find GenericExperiments by PubMed ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GenericExperiment> findByPubmedID( String pubmed );

    /**
     * load PhenotypeAssociation given an ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    PhenotypeAssociation load( Long id );

    /**
     * load PhenotypeAssociation given an ID
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    ExperimentalEvidence loadExperimentalEvidence( Long id );

    /** load an GenericEvidence given an ID */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    GenericEvidence loadGenericEvidence( Long id );

    /** load an LiteratureEvidence given an ID */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    LiteratureEvidence loadLiteratureEvidence( Long id );

    /**
     * update a PhenotypeAssociation
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( PhenotypeAssociation evidence );

    /** load all valueURI of Phenotype in the database FIXME cache the results of this */
    Set<String> loadAllUsedPhenotypeUris();

    /** find PhenotypeAssociations associated with a BibliographicReference */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId );

    /** find PhenotypeAssociations satisfying the given filters: paIds, taxonId and limit */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds );

    /** find all PhenotypeAssociation for a specific gene id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /** find all PhenotypeAssociation for a specific gene id and external Databases ids */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            Collection<Long> externalDatabaseIds );

    /** find all PhenotypeAssociation for a specific NCBI id */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /** find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype );

    /** find category term that were used in the database, used to annotated Experiments */
    Collection<CharacteristicValueObject> findEvidenceCategoryTerms();

    /** find all evidences from a specific external database */
    @Secured({ "GROUP_AGENT" })
    Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName, Integer limit, int start );

    /** find all evidences with no external database */
    @Secured({ "GROUP_AGENT" })
    Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation );

    /** find private evidence id that the user can modifiable or own */
    Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups, Long taxonId, Integer limit );

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation );

    /** return the list of the owners that have evidence in the system */
    Collection<String> findEvidenceOwners();

    /** finds all external databases statistics used in neurocarta */
    Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String downloadPath );

    /** find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.) */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String downloadFile );

    /**
     * returns a Collection<DifferentialExpressionEvidence> for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults );

    /** counts the evidence that from neurocarta that came from a specific MetaAnalysis */
    Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    /** find all phenotypes in Neurocarta */
    Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /**
     * Gets all External Databases that are used with evidence
     * 
     * @return Collection<ExternalDatabaseValueObject> the externalDatabases
     */
    Collection<ExternalDatabase> findExternalDatabasesWithEvidence();

    /** find statistics all evidences */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String downloadFile );

    void removePhenotypePublication( Long phenotypeAssociationPublicationId );

    Collection<String> loadAllDescription();

}
