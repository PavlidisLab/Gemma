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
package ubic.gemma.persistence.service.association.phenotype;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.service.BaseDao;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author nicolas
 */
public interface PhenotypeAssociationDao extends BaseDao<PhenotypeAssociation> {

    /**
     * @param geneDifferentialExpressionMetaAnalysisId id
     * @return count of the evidence that from neurocarta that came from a specific MetaAnalysis
     */
    Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId );

    /**
     * @return category terms currently used in the database by evidence
     */
    Collection<CharacteristicValueObject> findEvidenceCategoryTerms();

    /**
     * @return the list of the owners that have evidence in the system
     */
    Collection<String> findEvidenceOwners();

    /**
     * @param limit limit
     * @param externalDatabaseName external database name
     * @param start start
     * @return all evidences from a specific external database
     */
    Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName, int limit,
            int start );

    /**
     * @return all evidence that doesn't come from an external course
     */
    Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName();

    /**
     * @return all External Databases that are used with evidence
     */
    Collection<ExternalDatabase> findExternalDatabasesWithEvidence();

    /**
     * @param taxon      taxon
     * @param term       term
     * @param includeIEA if false, electronic annotations will be omitted
     * @return map of gene value objects to the exact phenotype the gene was annotated to. (gives no indication of 'bag
     * of terms')
     */
    Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm term, Long taxon, boolean includeIEA );

    /**
     * @param taxon               taxon
     * @param externalDatabaseIds external db ids
     * @param phenotypesValueUri  phenotype value uri
     * @param showOnlyEditable    show only editable
     * @return Genes link to a phenotype
     */
    Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUri, @Nullable Taxon taxon,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds );

    /**
     * @param geneId gene id
     * @return all PhenotypeAssociation for a specific gene id
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId );

    /**
     * @param externalDatabaseIds external db ids
     * @param geneId              gene id
     * @return all PhenotypeAssociation for a specific gene id and external Databases ids
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            @Nullable Collection<Long> externalDatabaseIds );

    /**
     * @param geneNCBI gene ncbi id
     * @return all PhenotypeAssociation for a specific NCBI id
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI );

    /**
     * @param geneNCBI  gene ncbi id
     * @param phenotype phenotype
     * @return all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype );

    /**
     * @param ids ids
     * @return PhenotypeAssociations satisfying the given filters: ids, taxonId and limit
     */
    Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids );

    /**
     * @param pubMedID pumbed id
     * @return PhenotypeAssociations associated with a BibliographicReference
     */
    Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedID );

    /**
     * @param limit   limit
     * @param taxonId taxon id
     * @return private evidence id that the user can modify or owns
     */
    Set<Long> findPrivateEvidenceId( @Nullable Long taxonId, int limit );

    /**
     * @param externalDatabaseIds    external db ids
     * @param taxon                  taxon
     * @param noElectronicAnnotation no electronic annotation
     * @param showOnlyEditable       show only editable
     * @param valuesUri              value uris
     * @return ll private phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation );

    /**
     * @param showOnlyEditable       show only editable
     * @param noElectronicAnnotation no electronic annotation
     * @param taxon                  taxon
     * @param externalDatabaseIds    external database ids
     * @param valuesUri              values uri
     * @return all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds, boolean noElectronicAnnotation );

    /**
     * @return all phenotypes in Neurocarta
     */
    Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes();

    /**
     * @return all valueURI of Phenotype in the database
     */
    Set<String> loadAllPhenotypesUri();

    /**
     * @param maxResults                               max results
     * @param geneDifferentialExpressionMetaAnalysisId ids
     * @return a Collection of DifferentialExpressionEvidence for a geneDifferentialExpressionMetaAnalysisId if one
     * exists (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, int maxResults );

    /**
     * @param filePath path
     * @return statistics all evidences
     */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String filePath );

    /**
     * @param folderPath path
     * @return all external databases statistics used in neurocarta
     */
    Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String folderPath );

    /**
     * @param filePath path
     * @return statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.)
     */
    ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String filePath );

    /**
     * remove a PhenotypeAssociationPublication
     *
     * @param phenotypeAssociationPublicationId id
     */
    void removePhenotypePublication( PhenotypeAssociationPublication phenotypeAssociationPublicationId );

    int removeAll();
}
