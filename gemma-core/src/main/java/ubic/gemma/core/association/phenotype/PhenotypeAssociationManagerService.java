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
package ubic.gemma.core.association.phenotype;

import org.springframework.security.access.annotation.Secured;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 *
 * @author paul
 */
public interface PhenotypeAssociationManagerService {

    /**
     * Find all phenotypes associated to a pubmedID
     *
     * @param evidenceId optional, used if we are updating to know current annotation
     * @return BibliographicReferenceValueObject
     */
    BibliographicReferenceValueObject findBibliographicReference( String pubMedId, Long evidenceId );

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     *
     * @param phenotypesValuesUris the roots phenotype of the query
     * @param taxon                the name of the taxon (optional)
     * @return A map or uris to collections of the genes found
     */
    Collection<GeneEvidenceValueObject> findCandidateGenes( Collection<String> phenotypesValuesUris, Taxon taxon );

    /**
     * Given set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     *
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param evidenceFilter      can specify a taxon and to show modifiable evidence (optional)
     * @return A collection of the genes found
     */
    Collection<GeneEvidenceValueObject> findCandidateGenes( EvidenceFilter evidenceFilter,
            Set<String> phenotypesValuesUri );

    /**
     * @param taxon         taxon
     * @param phenotypeUris URIs
     * @return For each phenotypeUri, find the genes that are associated with it. Different from findCandidateGenes which finds
     * genes associated with <em>all</em> the phenotypes together.
     */
    Map<String, Collection<? extends GeneValueObject>> findCandidateGenesForEach( Set<String> phenotypeUris,
            Taxon taxon );

    /**
     * Return evidence satisfying the specified filters. If the current user has not logged in, empty container is
     * returned.
     *
     * @param taxonId  taxon id
     * @param limit    number of evidence value objects to return
     * @param userName user name
     * @return evidence satisfying the specified filters
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByFilters( Long taxonId, Integer limit,
            String userName );

    /**
     * Return all evidence for a specific gene id
     *
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneId( Long geneId );

    /**
     * Return all evidence for a specific gene id with evidence flagged, indicating more information
     *
     * @param geneId              The Evidence id
     * @param phenotypesValuesUri the chosen phenotypes
     * @param evidenceFilter      can specify a taxon and to show modifiable evidence (optional)
     * @return The Gene we are interested in
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneId( Long geneId,
            Set<String> phenotypesValuesUri, EvidenceFilter evidenceFilter );

    /**
     * Return all evidence for a specific gene NCBI
     *
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneNCBI( Integer geneNCBI );

    /**
     * @return the list of the owners that have evidence in the system
     */
    Collection<String> findEvidenceOwners();

    /**
     * Find category term that were used in the database, used to annotated Experiments
     *
     * @return the terms found
     */
    Collection<CharacteristicValueObject> findExperimentCategory();

    /**
     * for a given search string look in the database and Ontology for matches
     *
     * @param givenQueryString the search query
     * @param categoryUri      the mged category (can be null)
     * @param taxonId          the taxon id (can be null)
     * @return the terms found
     */
    Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString, String categoryUri,
            Long taxonId );

    /**
     * Gets all External Databases that are used with evidence
     *
     * @return the externalDatabases
     */
    Collection<ExternalDatabaseValueObject> findExternalDatabasesWithEvidence();

    Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( String phenotype, Long taxonId, boolean includeIEA );

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     *
     * @param taxonId can be null to not constrain by taxon
     * @param query   query
     * @return list of Genes
     */
    Collection<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId );

    /**
     * Load an evidence
     *
     * @param id The Evidence database id
     */
    EvidenceValueObject<? extends PhenotypeAssociation> load( Long id );

    /**
     * load all the valueUri and value of phenotype present in Neurocarta
     *
     * @return the valueUri of the phenotypes
     */
    Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /**
     * Loads all phenotypes in the database and counts their occurrence using the database It builds the tree using
     * parents of terms, and will return 3 trees representing Disease, HP and MP
     *
     * @return A collection of the phenotypes with the gene occurrence
     */
    Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( EvidenceFilter evidenceFilter );

    /**
     * Same as {@link this#loadAllPhenotypesByTree(EvidenceFilter)}, but does not flatten out the tree.
     *
     * @param evidenceFilter evidence filter
     * @return a tree set of phenotypes
     */
    Collection<TreeCharacteristicValueObject> loadAllPhenotypesAsTree( EvidenceFilter evidenceFilter );

    /**
     * Get information about external data sources from Phenocarta, including URLs and timestamps of the most recent
     * update dates/times.
     *
     * @return A collection of objects with information about external data sources in Phenocarta
     */
    Collection<DumpsValueObject> helpFindAllDumps();

    /**
     * use if we want to reimport data from a specific external Database
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> loadEvidenceWithExternalDatabaseName(
            String externalDatabaseName, Integer limit, int start );

    /**
     * returns an DifferentialExpressionEvidence for a geneDifferentialExpressionMetaAnalysisId if one exists (used to
     * find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     *
     * @param geneDifferentialExpressionMetaAnalysisId id of the GeneDifferentialExpressionMetaAnalysis
     * @return DifferentialExpressionEvidence if an differentialExpressionEvidence exists for that id returns it
     */
    DiffExpressionEvidenceValueObject loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId );

    /**
     * @return find all evidence that doesn't come from an external source
     */
    Collection<EvidenceValueObject<? extends PhenotypeAssociation>> loadEvidenceWithoutExternalDatabaseName();

    /**
     * find statistics on evidence used in neurocarta
     *
     * @return statistics for each external database
     */
    Collection<ExternalDatabaseStatisticsValueObject> loadNeurocartaStatistics();

    /**
     * creates the DifferentialExpressionEvidences using an DiffExpressionMetaAnalysis
     *
     * @param geneDifferentialExpressionMetaAnalysisId id of the DiffExpressionMetaAnalysis
     * @param phenotypes                               phenotypes chosen
     * @param thresholdChosen                          threshold chosen to keep certain results
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    ValidateEvidenceValueObject makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, SortedSet<CharacteristicValueObject> phenotypes,
            Double thresholdChosen );

    /**
     * Links an Evidence to a Gene
     *
     * @param evidence The evidence
     * @return Status of the operation
     */
    ValidateEvidenceValueObject makeEvidence( EvidenceValueObject<? extends PhenotypeAssociation> evidence );

    /**
     * Removes an evidence
     *
     * @param id The Evidence database id
     */
    ValidateEvidenceValueObject remove( Long id );

    /**
     * Removes all the evidence that came from a specific metaAnalysis
     *
     * @param geneDifferentialExpressionMetaAnalysisId the geneDifferentialExpressionMetaAnalysis Id
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    ValidateEvidenceValueObject removeAllEvidenceFromMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    /**
     * For a given search string find all Ontology terms related, and then count their gene occurrence by taxon,
     * including ontology children terms
     *
     * @param searchQuery the query search that was type by the user
     * @return the terms found in the database with taxon and gene occurrence
     */
    Collection<CharacteristicValueObject> searchInDatabaseForPhenotype( String searchQuery );

    /**
     * Giving a phenotype searchQuery, returns a selection choice to the user
     *
     * @param searchQuery query typed by the user
     * @param geneId      the id of the chosen gene
     * @return list of choices returned
     */
    Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId );

    /**
     * Modify an existing evidence
     *
     * @param evidenceValueObject the evidence with modified fields
     * @return Status of the operation
     */
    ValidateEvidenceValueObject update( EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject );

    /**
     * Validate an Evidence before we create it
     *
     * @param evidence The evidence
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    ValidateEvidenceValueObject validateEvidence( EvidenceValueObject<PhenotypeAssociation> evidence );

    /**
     * Creates a dump of all evidence in the database that can be downloaded on the client, this is run once per month
     * by Quartz
     */
    @Secured({ "GROUP_AGENT" })
    void writeAllEvidenceToFile() throws IOException;
}
