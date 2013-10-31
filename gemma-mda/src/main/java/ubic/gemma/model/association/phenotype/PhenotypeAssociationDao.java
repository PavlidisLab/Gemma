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
package ubic.gemma.model.association.phenotype;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.BaseDao;

/**
 * @author nicolas
 * @version $Id$
 */
public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /** find Genes link to a phenotype */
    public Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable,
            Collection<Long> externalDatabaseIds );

    /**
     * load all valueURI of Phenotype in the database
     */
    public Set<String> loadAllPhenotypesUri();

    /**
     * find PhenotypeAssociations associated with a BibliographicReference
     */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID );

    /**
     * find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit
     * 
     * @param ids
     * @param taxonId
     * @param limit
     * @return
     */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids);

    /** find all PhenotypeAssociation for a specific gene id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /** find all PhenotypeAssociation for a specific gene id and external Databases ids */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            Collection<Long> externalDatabaseIds );

    /** find all PhenotypeAssociation for a specific NCBI id */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /**
     * find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri
     * 
     * @param geneNCBI
     * @param phenotype
     * @return
     */
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype );

    /** find category terms currently used in the database by evidence */
    public Collection<CharacteristicValueObject> findEvidenceCategoryTerms();

    /** find all evidences from a specific external database */
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName );

    /** find all evidence that doesn't come from an external course */
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    /**
     * find all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     * 
     * @param taxon
     * @param valuesUri
     * @param userName
     * @param groups
     * @param showOnlyEditable
     * @param externalDatabaseIds
     * @return
     */
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds );

    /** finds all external databases statistics used in neurocarta */
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases();

    /** find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.) */
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration();

    /**
     * find all private phenotypes associated with genes on a specific taxon and containing the valuesUri
     * 
     * @param taxon
     * @param valuesUri
     * @param userName
     * @param groups
     * @param showOnlyEditable
     * @param externalDatabaseIds
     * @return
     */
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds );

    /** find private evidence id that the user can modifiable or own */
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups, Long taxonId, Integer limit );

    /** return the list of the owners that have evidence in the system */
    public Collection<String> findEvidenceOwners();

    /**
     * returns a Collection of DifferentialExpressionEvidence for a geneDifferentialExpressionMetaAnalysisId if one
     * exists (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults );

    /** counts the evidence that from neurocarta that came from a specific MetaAnalysis */
    public Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    /** find all phenotypes in Neurocarta */
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /** Gets all External Databases that are used with evidence */
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence();

    /** find statistics all evidences */
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence();

    /** remove a PhenotypeAssociationPublication **/
    public void removePhenotypePublication( Long phenotypeAssociationPublicationId );

}
