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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;

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

    /**
     * Using an phenotypeAssociation id removes the evidence
     */
    @Override
    public void remove( PhenotypeAssociation pa ) {
        pa.getGene().getPhenotypeAssociations().remove( pa );
        this.phenotypeAssociationDao.remove( pa );
    }

    /** find Genes link to a phenotype */
    @Override
    public Collection<GeneEvidenceValueObject> findGeneWithPhenotypes( Set<String> phenotypesValueUri, Taxon taxon,
            String userName, Collection<String> groups, boolean isAdmin, boolean showOnlyEditable ) {
        return this.phenotypeAssociationDao.findGeneWithPhenotypes( phenotypesValueUri, taxon, userName, groups,
                isAdmin, showOnlyEditable );
    }

    /** find all phenotypes */
    @Override
    public Collection<PhenotypeAssociation> loadAll() {
        return ( Collection<PhenotypeAssociation> ) this.phenotypeAssociationDao.loadAll();
    }

    /** create a GenericExperiment */
    @Override
    public GenericExperiment create( GenericExperiment genericExperiment ) {
        return this.genericExperimentDao.create( genericExperiment );
    }

    /** find GenericExperiments by PubMed ID */
    @Override
    public Collection<GenericExperiment> findByPubmedID( String pubmed ) {
        return this.genericExperimentDao.findByPubmedID( pubmed );
    }

    /** load an evidence given an ID */
    @Override
    public PhenotypeAssociation load( Long id ) {
        return this.phenotypeAssociationDao.load( id );
    }

    /** load an GenericEvidence given an ID */
    @Override
    public GenericEvidence loadGenericEvidence( Long id ) {
        return this.genericEvidenceDao.load( id );
    }

    /** load an LiteratureEvidence given an ID */
    @Override
    public LiteratureEvidence loadLiteratureEvidence( Long id ) {
        return this.literatureEvidenceDao.load( id );
    }

    /** load an ExperimentalEvidence given an ID */
    @Override
    public ExperimentalEvidence loadExperimentalEvidence( Long id ) {
        return this.experimentalEvidenceDao.load( id );
    }

    /** update an evidence */
    @Override
    public void update( PhenotypeAssociation evidence ) {
        this.phenotypeAssociationDao.update( evidence );
    }

    /** load all valueURI of Phenotype in the database */
    @Override
    public Set<String> loadAllPhenotypesUri() {
        return this.phenotypeAssociationDao.loadAllPhenotypesUri();
    }

    @Override
    /** find PhenotypeAssociations associated with a BibliographicReference */
    public Collection<PhenotypeAssociation> findPhenotypesForBibliographicReference( String pubMedId ) {
        return this.phenotypeAssociationDao.findPhenotypesForBibliographicReference( pubMedId );
    }

    @Override
    public PhenotypeAssociation create( PhenotypeAssociation p ) {
        return this.phenotypeAssociationDao.create( p );
    }

    /** find PhenotypeAssociations satisfying the given filters: ids, taxonId and limit */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationWithIds( Collection<Long> ids, Long taxonId,
            Integer limit ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationWithIds( ids, taxonId, limit );
    }

    /** find all PhenotypeAssociation for a specific gene id */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneId( Long geneId ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneId( geneId );
    }

    /** find all PhenotypeAssociation for a specific NCBI id */
    @Override
    public Collection<PhenotypeAssociation> findPhenotypeAssociationForGeneNCBI( Integer geneNCBI ) {
        return this.phenotypeAssociationDao.findPhenotypeAssociationForGeneNCBI( geneNCBI );
    }

    /** find mged category term that were used in the database, used to annotated Experiments */
    @Override
    public Collection<CharacteristicValueObject> findEvidenceMgedCategoryTerms() {
        return this.phenotypeAssociationDao.findEvidenceMgedCategoryTerms();
    }

    /** find all evidences from a specific external database */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithExternalDatabaseName( String externalDatabaseName ) {
        return this.phenotypeAssociationDao.findEvidencesWithExternalDatabaseName( externalDatabaseName );
    }

    /** find all evidence that doesn't come from an external course */
    @Override
    public Collection<PhenotypeAssociation> findEvidencesWithoutExternalDatabaseName() {
        return this.phenotypeAssociationDao.findEvidencesWithoutExternalDatabaseName();
    }

    /** find all public phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPublicPhenotypesGenesAssociations( Taxon taxon, Set<String> valuesUri,
            String userName, Collection<String> groups, boolean showOnlyEditable ) {
        return this.phenotypeAssociationDao.findPublicPhenotypesGenesAssociations( taxon, valuesUri, userName, groups,
                showOnlyEditable );
    }

    /** find private evidence id that the user can modifiable or own */
    @Override
    public Set<Long> findPrivateEvidenceId( String userName, Collection<String> groups ) {
        return this.phenotypeAssociationDao.findPrivateEvidenceId( userName, groups );
    }

    /** find all private phenotypes associated with genes on a specific taxon and containing the valuesUri */
    @Override
    public HashMap<String, HashSet<Integer>> findPrivatePhenotypesGenesAssociations( Taxon taxon,
            Set<String> valuesUri, String userName, Collection<String> groups, boolean showOnlyEditable ) {
        return this.phenotypeAssociationDao.findPrivatePhenotypesGenesAssociations( taxon, valuesUri, userName, groups,
                showOnlyEditable );
    }

    /** return the list of the owners that have evidence in the system */
    @Override
    public Collection<String> findEvidenceOwners() {
        return this.phenotypeAssociationDao.findEvidenceOwners();
    }

    /** finds all external databases statistics used in neurocarta */
    @Override
    public Collection<ExternalDatabaseStatisticsValueObject> loadStatisticsOnExternalDatabases() {
        return this.phenotypeAssociationDao.loadStatisticsOnExternalDatabases();
    }

    /** find statistics for a neurocarta manual curation (numGene, numPhenotypes, etc.) */
    @Override
    public ExternalDatabaseStatisticsValueObject loadStatisticsOnManualCuration() {
        return this.phenotypeAssociationDao.loadStatisticsOnManualCuration();
    }

    /**
     * returns a Collection<DifferentialExpressionEvidence> for a geneDifferentialExpressionMetaAnalysisId if one exists
     * (can be used to find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     */
    @Override
    public Collection<DifferentialExpressionEvidence> loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, Long maxResults ) {
        return this.phenotypeAssociationDao.loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
                geneDifferentialExpressionMetaAnalysisId, maxResults );
    }

}
