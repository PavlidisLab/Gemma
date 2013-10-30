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
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
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
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;

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
    private GenericExperimentDao genericExperimentDao;

    /** counts the evidence that from neurocarta that came from a specific MetaAnalysis */
    @Override
    @Transactional(readOnly = true)
    public Long countEvidenceWithGeneDifferentialExpressionMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId ) {
        return this.phenotypeAssociationDao
                .countEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId );
    }

    /** create a GenericExperiment */
    @Override
    @Transactional
    public GenericExperiment create( GenericExperiment genericExperiment ) {
        return this.genericExperimentDao.create( genericExperiment );
    }

    @Override
    @Transactional
    public PhenotypeAssociation create( PhenotypeAssociation p ) {
        return this.phenotypeAssociationDao.create( p );
    }

    /** find GenericExperiments by PubMed ID */
    @Override
    @Transactional(readOnly = true)
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return this.genericExperimentDao.findByPubmedID( pubmed );
    }

    /** find mged category term that were used in the database, used to annotated Experiments */
    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> findEvidenceCategoryTerms() {
        return this.phenotypeAssociationDao.findEvidenceCategoryTerms();
    }

    /** return the list of the owners that have evidence in the system */
    @Override
    @Transactional(readOnly = true)
    public Collection<String> findEvidenceOwners() {
        return this.phenotypeAssociationDao.findEvidenceOwners();
    }

    /** find all evidences from a specific external database */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName ) {
        return this.phenotypeAssociationDao.findEvidencesWithExternalDatabaseName( externalDatabaseName );
    }

    /** find all evidence that doesn't come from an external course */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {
        return this.phenotypeAssociationDao.findEvidencesWithoutExternalDatabaseName();
    }

    /**
     * Gets all External Databases that are used with evidence
     * 
     * @return Collection<ExternalDatabaseValueObject> the externalDatabases
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabase> findExternalDatabasesWithEvidence() {
        return this.phenotypeAssociationDao.findExternalDatabasesWithEvidence();
    }

    /** find Genes link to a phenotype */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneEvidenceValueObject> findGenesWithPhenotypes( Set<String> phenotypesValueUris, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable,
            Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao.findGenesWithPhenotypes( phenotypesValueUris, taxon, userName, groups,
                isAdmin, showOnlyEditable, externalDatabaseIds );
    }

    /** find all PhenotypeAssociation for a specific gene id */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneId( geneId );
    }

    /** find all PhenotypeAssociation for a specific gene id and external Databases ids */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneIdAndDatabases( Long geneId,
            Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneIdAndDatabases( geneId, externalDatabaseIds );
    }

    /** find all PhenotypeAssociation for a specific NCBI id */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneNCBI( geneNCBI );
    }

    /** find all PhenotypeAssociation for a specific NCBI id and phenotypes valueUri */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI, Set<String> phenotype ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneNCBI( geneNCBI, phenotype );
    }

    /** find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids, Long taxonId,
            Integer limit ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationWithIds( ids, taxonId, limit );
    }

    @Override
    /** find PhenotypeAssociations associated with a BibliographicReference */
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId ) {
        return this.phenotypeAssociationDao.findPhenotypesForBibliographicReference( pubMedId );
    }

    /** find private evidence id that the user can modifiable or own */
    @Override
    @Transactional(readOnly = true)
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups, Long taxonId, Integer limit ) {
        return this.phenotypeAssociationDao.findPrivateEvidenceId( userName, groups, taxonId, limit );
    }

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao.findPrivatePhenotypesGenesAssociations( taxon, valuesUri, userName, groups,
                showOnlyEditable, externalDatabaseIds );
    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    @Transactional(readOnly = true)
    public Map<String, Set<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable, Collection<Long> externalDatabaseIds ) {
        return this.phenotypeAssociationDao.findPublicPhenotypesGenesAssociations( taxon, valuesUri, userName, groups,
                showOnlyEditable, externalDatabaseIds );
    }

    /** load an evidence given an ID */
    @Override
    @Transactional(readOnly = true)
    public PhenotypeAssociation load( Long id ) {
        return this.phenotypeAssociationDao.load( id );
    }

    /** find all phenotypes */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeAssociation> loadAll() {
        return ( Collection<PhenotypeAssociation> ) this.phenotypeAssociationDao.loadAll();
    }

    /** find all phenotypes in Neurocarta */
    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {
        return this.phenotypeAssociationDao.loadAllNeurocartaPhenotypes();
    }

    /** load all valueURI of Phenotype in the database */
    @Override
    @Transactional(readOnly = true)
    public Set<String> loadAllUsedPhenotypeUris() {
        return this.phenotypeAssociationDao.loadAllPhenotypesUri();
    }

    /**
     * returns a Collection<DifferentialExpressionEvidence> for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults ) {
        return this.phenotypeAssociationDao.loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
                geneDifferentialExpressionMetaAnalysisId, maxResults );
    }

    /** load an ExperimentalEvidence given an ID */
    @Override
    @Transactional(readOnly = true)
    public ExperimentalEvidence loadExperimentalEvidence( Long id ) {
        return this.experimentalEvidenceDao.load( id );
    }

    /** load an GenericEvidence given an ID */
    @Override
    @Transactional(readOnly = true)
    public GenericEvidence loadGenericEvidence( Long id ) {
        return this.genericEvidenceDao.load( id );
    }

    /** load an LiteratureEvidence given an ID */
    @Override
    @Transactional(readOnly = true)
    public LiteratureEvidence loadLiteratureEvidence( Long id ) {
        return this.literatureEvidenceDao.load( id );
    }

    /** find statistics all evidences */
    @Override
    @Transactional(readOnly = true)
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnAllEvidence() {
        return this.phenotypeAssociationDao.loadStatisticsOnAllEvidence();
    }

    /** finds all external databases statistics used in neurocarta */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases() {
        return this.phenotypeAssociationDao.loadStatisticsOnExternalDatabases();
    }

    /** find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.) */
    @Override
    @Transactional(readOnly = true)
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration() {
        return this.phenotypeAssociationDao.loadStatisticsOnManualCuration();
    }

    /**
     * Using an phenotypeAssociation id removes the evidence
     */
    @Override
    @Transactional
    public void remove( PhenotypeAssociation pa ) {
        pa.getGene().getPhenotypeAssociations().remove( pa );
        this.phenotypeAssociationDao.remove( pa );
    }

    /** update an evidence */
    @Override
    @Transactional
    public void update( PhenotypeAssociation evidence ) {
        this.phenotypeAssociationDao.update( evidence );
    }
    
    @Override
    @Transactional
    public void removePhenotypePublication(Long phenotypeAssociationPublicationId){
        this.phenotypeAssociationDao.removePhenotypePublication( phenotypeAssociationPublicationId );
    }

}
