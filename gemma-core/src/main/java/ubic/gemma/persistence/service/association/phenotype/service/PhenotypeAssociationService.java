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

import org.springframework.security.access.annotation.Secured;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.*;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.service.BaseImmutableService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author nicolas
 */
@SuppressWarnings({ "UnusedReturnValue", "unused" }) // Possible external use
public interface PhenotypeAssociationService extends BaseImmutableService<PhenotypeAssociation> {

    /**
     * @param p Using a phenotypeAssociation id removes the evidence
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( PhenotypeAssociation p );

    @Secured({ "GROUP_USER" })
    PhenotypeAssociation create( PhenotypeAssociation p );

    Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUri, @Nullable Taxon taxon,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds );

    /**
     * @param phenotype  phenotype
     * @param taxonId    taxon id
     * @param includeIEA include IEA
     * @return map of gene value objects to ontology terms, where the term should be the most specific term relative to
     * the phenotype that was queries (that is, it may be a child term).
     * @author paul
     */
    Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm phenotype, Long taxonId,
            boolean includeIEA );

    @Secured({ "GROUP_USER" })
    GenericExperiment create( GenericExperiment genericExperiment );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> loadAll();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<GenericExperiment> findByPubmedID( String pubmed );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    PhenotypeAssociation load( Long id );

    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void update( PhenotypeAssociation evidence );

    /**
     * @return load all valueURI of Phenotype in the database
     */
    Set<String> loadAllUsedPhenotypeUris();

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> paIds );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            @Nullable Collection<Long> externalDatabaseIds );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype );

    Collection<CharacteristicValueObject> findEvidenceCategoryTerms();

    Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName, int limit,
            int start );

    @Secured({ "GROUP_AGENT" })
    Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            @Nullable String userName, @Nullable Collection<String> groups, boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation );

    Set<Long> findPrivateEvidenceId( @Nullable String userName, @Nullable Collection<String> groups, Long taxonId, int limit );

    Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            @Nullable String userName, @Nullable Collection<String> groups, boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation );

    Collection<String> findEvidenceOwners();

    Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String downloadPath );

    ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String downloadFile );

    /**
     * @param geneDifferentialExpressionMetaAnalysisId id
     * @param maxResults                               max results
     * @return a Collection for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, int maxResults );

    Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /**
     * Gets all External Databases that are used with evidence
     *
     * @return Collection the externalDatabases
     */
    Collection<ExternalDatabase> findExternalDatabasesWithEvidence();

    ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String downloadFile );

    void removePhenotypePublication( PhenotypeAssociationPublication phenotypeAssociationPublicationId );

    @Secured({ "GROUP_ADMIN" })
    int removeAll();
}
