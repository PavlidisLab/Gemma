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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.phenotype.*;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.association.phenotype.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Service responsible for low level operations, used by PhenotypeAssociationManagerServiceImpl
 */
@Service
public class PhenotypeAssociationServiceImpl extends AbstractService<PhenotypeAssociation> implements PhenotypeAssociationService {

    @Autowired
    private GenericExperimentDao genericExperimentDao;

    @Autowired
    private PhenotypeAssociationDao phenotypeAssociationDao;

    @Autowired
    public PhenotypeAssociationServiceImpl( PhenotypeAssociationDao phenotypeAssociationDao ) {
        super( phenotypeAssociationDao );
    }

    /**
     * @param taxon               taxon
     * @param externalDatabaseIds external db ids
     * @param phenotypesValueUris phenotype value uris
     * @param showOnlyEditable    show only editable
     * @return find Genes link to a phenotype
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUris, @Nullable Taxon taxon,
            boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao
                .findGenesWithPhenotypes( phenotypesValueUris, taxon, showOnlyEditable, externalDatabaseIds );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( OntologyTerm phenotype, Long taxonId,
            boolean includeIEA ) {
        return this.phenotypeAssociationDao.findGenesForPhenotype( phenotype, taxonId, includeIEA );
    }

    @Override
    @Transactional
    public GenericExperiment create( GenericExperiment genericExperiment ) {
        return this.genericExperimentDao.create( genericExperiment );
    }

    /**
     * @param pubmed pubmed
     * @return find GenericExperiments by PubMed ID
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return this.genericExperimentDao.findByPubmedID( pubmed );
    }

    /**
     * @return load all valueURI of Phenotype in the database
     */
    @Override
    @Transactional(readOnly = true)
    public Set<String> loadAllUsedPhenotypeUris() {
        return this.phenotypeAssociationDao.loadAllPhenotypesUri();
    }

    /**
     * @param pubMedId pub med id
     * @return find PhenotypeAssociations associated with a BibliographicReference
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId ) {
        return this.phenotypeAssociationDao.findPhenotypesForBibliographicReference( pubMedId );
    }

    /**
     * @param ids ids
     * @return find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationWithIds( ids );
    }

    /**
     * @param geneId gene id
     * @return find all PhenotypeAssociation for a specific gene id
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneId( geneId );
    }

    /**
     * @param externalDatabaseIds external db ids
     * @param geneId              gene id
     * @return find all PhenotypeAssociation for a specific gene id and external Databases ids
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            @Nullable Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao
                .findPhenotypeAssociationForGeneIdAndDatabases( geneId, externalDatabaseIds );
    }

    /**
     * @param geneNCBI gene ncbi id
     * @return find all PhenotypeAssociation for a specific NCBI id
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneNCBI( geneNCBI );
    }

    /**
     * @param geneNCBI  gene necbi id
     * @param phenotype phenotype
     * @return find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI,
            Set<String> phenotype ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneNCBI( geneNCBI, phenotype );
    }

    /**
     * @return find mged category term that were used in the database, used to annotated Experiments
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> findEvidenceCategoryTerms() {
        return this.phenotypeAssociationDao.findEvidenceCategoryTerms();
    }

    /**
     * @param externalDatabaseName external db name
     * @param limit                limit
     * @param start                start
     * @return find all evidences from a specific external database
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName,
            int limit, int start ) {
        return this.phenotypeAssociationDao.findEvidencesWithExternalDatabaseName( externalDatabaseName, limit, start );
    }

    /**
     * @return find all evidence that doesn't come from an external course
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {
        return this.phenotypeAssociationDao.findEvidencesWithoutExternalDatabaseName();
    }

    /**
     * @param groups                 groups
     * @param externalDatabaseIds    external db ids
     * @param taxon                  taxon
     * @param noElectronicAnnotation no electronic annotation
     * @param showOnlyEditable       show only editable
     * @param userName               user name
     * @param valuesUri              values uri
     * @return find all public phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            @Nullable String userName, @Nullable Collection<String> groups, boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation ) {
        // FIXME bug 4349 - userName is not used!
        return this.phenotypeAssociationDao
                .findPublicPhenotypesGenesAssociations( taxon, valuesUri, showOnlyEditable, externalDatabaseIds,
                        noElectronicAnnotation );
    }

    /**
     * @param limit    limit
     * @param groups   groups
     * @param taxonId  taxon id
     * @param userName user name
     * @return find private evidence id that the user can modifiable or own
     */
    @Override
    @Transactional(readOnly = true)
    public Set<Long> findPrivateEvidenceId( @Nullable String userName, @Nullable Collection<String> groups, Long taxonId, int limit ) {
        return this.phenotypeAssociationDao.findPrivateEvidenceId( taxonId, limit );
    }

    /**
     * @param groups                 groups
     * @param externalDatabaseIds    external db ids
     * @param taxon                  taxon
     * @param noElectronicAnnotation no electronic annotation
     * @param showOnlyEditable       show only editable
     * @param userName               user name
     * @param valuesUri              values uri
     * @return find all private phenotypes associated with genes on a specific taxon and containing the valuesUri
     */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( @Nullable Taxon taxon, @Nullable Set<String> valuesUri,
            @Nullable String userName, @Nullable Collection<String> groups, boolean showOnlyEditable, @Nullable Collection<Long> externalDatabaseIds,
            boolean noElectronicAnnotation ) {
        // FIXME bug 4349 - userName is not used!
        return this.phenotypeAssociationDao
                .findPrivatePhenotypesGenesAssociations( taxon, valuesUri, showOnlyEditable, externalDatabaseIds,
                        noElectronicAnnotation );
    }

    /**
     * @return the list of the owners that have evidence in the system
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<String> findEvidenceOwners() {
        return this.phenotypeAssociationDao.findEvidenceOwners();
    }

    /**
     * @param downloadPath path
     * @return finds all external databases statistics used in neurocarta
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases( String downloadPath ) {
        return this.phenotypeAssociationDao.loadStatisticsOnExternalDatabases( downloadPath );
    }

    /**
     * @param downloadFile file
     * @return find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.)
     */
    @Override
    @Transactional(readOnly = true)
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration( String downloadFile ) {
        return this.phenotypeAssociationDao.loadStatisticsOnManualCuration( downloadFile );
    }

    /**
     * @param maxResults                               max results
     * @param geneDifferentialExpressionMetaAnalysisId id
     * @return a Collection for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, int maxResults ) {
        return this.phenotypeAssociationDao
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId,
                        maxResults );
    }

    /**
     * @param geneDifferentialExpressionMetaAnalysisId analysis id
     * @return counts the evidence that from neurocarta that came from a specific MetaAnalysis
     */
    @Override
    @Transactional(readOnly = true)
    public Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId ) {
        return this.phenotypeAssociationDao
                .countEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId );
    }

    /**
     * @return find all phenotypes in Neurocarta
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {
        return this.phenotypeAssociationDao.loadAllNeurocartaPhenotypes();
    }

    /**
     * Gets all External Databases that are used with evidence
     *
     * @return Collection the externalDatabases
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence() {
        return this.phenotypeAssociationDao.findExternalDatabasesWithEvidence();
    }

    /**
     * @param downloadFile file
     * @return find statistics all evidences
     */
    @Override
    @Transactional(readOnly = true)
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence( String downloadFile ) {
        return this.phenotypeAssociationDao.loadStatisticsOnAllEvidence( downloadFile );
    }

    @Override
    @Transactional
    public void removePhenotypePublication( PhenotypeAssociationPublication phenotypeAssociationPublication ) {
        this.phenotypeAssociationDao.removePhenotypePublication( phenotypeAssociationPublication );
    }

    @Override
    @Transactional
    public int removeAll() {
        return this.phenotypeAssociationDao.removeAll();
    }
}
