/*
 * The Gemma project
 * 
 * Copyright (c) 2007-2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.association.phenotype;

import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.util.SecurityUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.DateUtil;
import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.model.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.association.phenotype.DifferentialExpressionEvidence;
import ubic.gemma.model.association.phenotype.ExperimentalEvidence;
import ubic.gemma.model.association.phenotype.GenericEvidence;
import ubic.gemma.model.association.phenotype.LiteratureEvidence;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationPublication;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.ExternalDatabaseValueObject;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.DiffExpressionEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceSecurityValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExternalDatabaseStatisticsValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ScoreValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SimpleTreeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.Settings;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 * 
 * @author nicolas
 * @version $Id$
 */
@Service
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService, InitializingBean {

    private static Log log = LogFactory.getLog( PhenotypeAssociationManagerServiceImpl.class );

    private static final int MAX_PHENOTYPES_FROM_ONTOLOGY = 100;

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private HomologeneService homologeneService;

    @Autowired
    private PhenotypeAssoOntologyHelper ontologyHelper = null;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    private PubMedXMLFetcher pubMedXmlFetcher = null;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private UserManager userManager;

    @Override
    public void afterPropertiesSet() {
        this.pubMedXmlFetcher = new PubMedXMLFetcher();
    }

    /**
     * Find all phenotypes associated to a pubmedID
     * 
     * @param pubMedId
     * @param evidenceId optional, used if we are updating to know current annotation
     * @return BibliographicReferenceValueObject
     */
    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findBibliographicReference( String pubMedId, Long evidenceId ) {

        // check if the given pubmedID is already in the database
        BibliographicReference bibliographicReference = this.bibliographicReferenceService.findByExternalId( pubMedId );

        // already in the database
        if ( bibliographicReference != null ) {

            BibliographicReferenceValueObject bibliographicReferenceVO = new BibliographicReferenceValueObject(
                    bibliographicReference );

            Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                    .findPhenotypesForBibliographicReference( pubMedId );

            Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                    .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );

            // set phenotypes associated with this bibliographic reference
            bibliographicReferenceVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );

            // set experiments associated with this bibliographic reference
            Collection<ExpressionExperiment> experiments = this.bibliographicReferenceService
                    .getRelatedExperiments( bibliographicReference );

            if ( experiments != null && !experiments.isEmpty() ) {
                bibliographicReferenceVO.setExperiments( ExpressionExperimentValueObject
                        .convert2ValueObjects( experiments ) );
            }

            return bibliographicReferenceVO;
        }

        // find the Bibliographic on PubMed
        bibliographicReference = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        // the pudmedId doesn't exists in PudMed
        if ( bibliographicReference == null ) {
            return null;
        }

        BibliographicReferenceValueObject bibliographicReferenceValueObject = new BibliographicReferenceValueObject(
                bibliographicReference );

        return bibliographicReferenceValueObject;
    }

    /**
     * Set<String> phenotypesValuesUri ) Given a set of phenotypes returns the genes that have <em>all</em> those
     * phenotypes or children phenotypes
     * 
     * @param phenotypeValueUris the roots phenotype of the query
     * @param taxon the name of the taxon (optinal)
     * @return A collection of the genes found
     */

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> findCandidateGenes( Collection<String> phenotypeValueUris, Taxon taxon ) {

        if ( phenotypeValueUris == null || phenotypeValueUris.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Set<String> usedPhenotypes = this.associationService.loadAllUsedPhenotypeUris();
        Map<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypeValueUris,
                usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = findAllPossibleChildren( phenotypesWithChildren );

        String userName = "";
        Collection<String> groups = new HashSet<String>();

        if ( SecurityUtil.isUserLoggedIn() ) {

            userName = this.userManager.getCurrentUsername();
            groups = this.userManager.findAllGroups();
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = this.associationService.findGenesWithPhenotypes(
                possibleChildrenPhenotypes, taxon, userName, groups, SecurityUtil.isUserAdmin(), false, null );

        return filterGenesWithPhenotypes( geneEvidenceValueObjects, phenotypesWithChildren );
    }

    @Override
    public Map<String, Collection<? extends GeneValueObject>> findCandidateGenesForEach( Set<String> phenotypeUris,
            Taxon taxon ) {
        if ( phenotypeUris == null || phenotypeUris.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );

        }

        Set<String> usedPhenotypes = this.associationService.loadAllUsedPhenotypeUris();
        Map<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypeUris, usedPhenotypes );

        Map<String, Collection<? extends GeneValueObject>> results = new HashMap<>();

        String userName = "";
        Collection<String> groups = new HashSet<String>();
        if ( SecurityUtil.isUserLoggedIn() ) {
            userName = this.userManager.getCurrentUsername();
            groups = this.userManager.findAllGroups();
        }
        boolean userAdmin = SecurityUtil.isUserAdmin();

        /*
         * FIXME if this can be done 'in bulk' it would be faster ...
         */
        for ( Entry<String, Set<String>> el : phenotypesWithChildren.entrySet() ) {
            String queryPhenotype = el.getKey();

            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = this.associationService
                    .findGenesWithPhenotypes( el.getValue(), taxon, userName, groups, userAdmin, false, null );

            results.put( queryPhenotype, geneEvidenceValueObjects );

        }

        return results;

    }

    /**
     * Given a set of phenotypes returns the genes that have <em>all</em> those phenotypes (children are okay)
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param evidenceFilter can specify a taxon and to show modifiable evidence (optional)
     * @return A collection of the genes found
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneValueObject> findCandidateGenes( EvidenceFilter evidenceFilter,
            Set<String> phenotypesValuesUri ) {

        if ( phenotypesValuesUri == null || phenotypesValuesUri.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        Taxon taxon = null;
        boolean showOnlyEditable = false;
        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            if ( evidenceFilter.getTaxonId() != null && evidenceFilter.getTaxonId() > 0 ) {
                taxon = this.taxonService.load( evidenceFilter.getTaxonId() );
            }
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Set<String> usedPhenotypes = this.associationService.loadAllUsedPhenotypeUris();

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Map<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypesValuesUri,
                usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = findAllPossibleChildren( phenotypesWithChildren );

        String userName = "";
        Collection<String> groups = new HashSet<String>();

        if ( SecurityUtil.isUserLoggedIn() ) {
            userName = this.userManager.getCurrentUsername();
            groups = this.userManager.findAllGroups();
        }

        Collection<GeneEvidenceValueObject> genesPhenotypeHelperObject = this.associationService
                .findGenesWithPhenotypes( possibleChildrenPhenotypes, taxon, userName, groups,
                        SecurityUtil.isUserAdmin(), showOnlyEditable, externalDatabaseIds );

        return filterGenesWithPhenotypes( genesPhenotypeHelperObject, phenotypesWithChildren );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.association.phenotype.PhenotypeAssociationManagerService#findCandidateGenes(java.lang.String,
     * ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<GeneValueObject> findCandidateGenes( String phenotypeValueUri, Taxon taxon ) {
        Set<String> uris = new HashSet<>();
        uris.add( phenotypeValueUri );
        return this.findCandidateGenes( uris, taxon );
    }

    /**
     * Return evidence satisfying the specified filters. If the current user has not logged in, empty container is
     * returned.
     * 
     * @param taxonId taxon id
     * @param limit number of evidence value objects to return
     * @param userName user name
     * @return evidence satisfying the specified filters
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> findEvidenceByFilters( Long taxonId, Integer limit, String userName ) {
        final Collection<EvidenceValueObject> evidenceValueObjects;

        if ( SecurityUtil.isUserLoggedIn() ) {
            final Set<Long> paIds;

            if ( userName == null ) {
                if ( SecurityUtil.isUserAdmin() ) {
                    paIds = this.associationService.findPrivateEvidenceId( null, null, taxonId, limit );
                } else {
                    paIds = this.associationService.findPrivateEvidenceId( this.userManager.getCurrentUsername(),
                            this.userManager.findAllGroups(), taxonId, limit );
                }
            } else {
                paIds = this.associationService.findPrivateEvidenceId( userName, this.userManager.findAllGroups(),
                        taxonId, limit );
            }

            Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                    .findPhenotypeAssociationWithIds( paIds );

            evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );
        } else {
            evidenceValueObjects = new HashSet<EvidenceValueObject>();
        }

        return evidenceValueObjects;
    }

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneId( geneId );

        Collection<EvidenceValueObject> evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );

        return evidenceValueObjects;
    }

    /**
     * Return all evidence for a specific gene id with evidence flagged, indicating more information
     * 
     * @param geneId The Evidence id
     * @param phenotypesValuesUri the chosen phenotypes
     * @param evidenceFilter can specify a taxon and to show modifiable evidence (optional)
     * @return The Gene we are interested in
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId, Set<String> phenotypesValuesUri,
            EvidenceFilter evidenceFilter ) {

        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneIdAndDatabases( geneId, externalDatabaseIds );

        if ( evidenceFilter != null && evidenceFilter.isShowOnlyEditable() ) {
            phenotypeAssociations = filterPhenotypeAssociationsMyAnnotation( phenotypeAssociations );
        }

        Collection<EvidenceValueObject> evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );

        // add all homologue evidence
        evidenceValueObjects.addAll( findHomologueEvidence( geneId, evidenceFilter ) );

        flagEvidence( evidenceValueObjects, phenotypesValuesUri, this.associationService.loadAllUsedPhenotypeUris() );

        return evidenceValueObjects;
    }

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> findEvidenceByGeneNCBI( Integer geneNCBI ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneNCBI( geneNCBI );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /** return the list of the owners that have evidence in the system */
    @Override
    public Collection<String> findEvidenceOwners() {
        return this.associationService.findEvidenceOwners();
    }

    /**
     * Find mged category term that were used in the database, used to annotated Experiments
     * 
     * @return Collection<CharacteristicValueObject> the terms found
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentCategory() {
        return this.associationService.findEvidenceCategoryTerms();
    }

    /**
     * For a given search string look in the database and Ontology for matches
     * 
     * @param givenQueryString the search query
     * @param categoryUri the mged category (can be null)
     * @param taxonId the taxon id (can be null)
     * @return Collection<CharacteristicValueObject> the terms found
     */
    @Override
    public Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString,
            String categoryUri, Long taxonId ) {

        // TODO new method created, will we use categoryUri and taxon ???
        return this.ontologyService.findExperimentsCharacteristicTags( givenQueryString, true );
    }

    /**
     * Gets all External Databases that are used with evidence
     * 
     * @return Collection<ExternalDatabaseValueObject> the externalDatabases
     */
    @Override
    public Collection<ExternalDatabaseValueObject> findExternalDatabasesWithEvidence() {

        Collection<ExternalDatabaseValueObject> exDatabases = ExternalDatabaseValueObject
                .fromEntity( this.associationService.findExternalDatabasesWithEvidence() );

        // id of databases we want to exclude in the filter
        String excludedDefaultDatabases = Settings.getString( "gemma.neurocarta.exluded_database_id" );

        if ( excludedDefaultDatabases != null ) {
            Collection<String> excludedDatabaseId = Arrays.asList( excludedDefaultDatabases.split( "," ) );

            for ( ExternalDatabaseValueObject databaseVO : exDatabases ) {
                if ( excludedDatabaseId.contains( databaseVO.getId().toString() ) ) {
                    databaseVO.setChecked( true );
                }
            }
        }

        // so manual curation will be put at the end
        ArrayList<ExternalDatabaseValueObject> exDatabasesAsList = new ArrayList<ExternalDatabaseValueObject>(
                exDatabases );
        // add manual curation type
        ExternalDatabaseValueObject manualEvidence = new ExternalDatabaseValueObject( 1L, "Manual Curation", false );
        exDatabasesAsList.add( manualEvidence );

        return exDatabasesAsList;
    }

    /**
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidenceValueObject> list of Genes
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId ) {

        if ( query == null || query.length() == 0 ) {
            throw new IllegalArgumentException( "No search query provided" );
        }

        // make sure it does an inexact search
        String newQuery = query + "%";

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettingsImpl.geneSearch( newQuery, taxon );
        List<SearchResult> geneSearchResults = this.searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            return new HashSet<GeneEvidenceValueObject>();
        }

        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = new HashSet<GeneEvidenceValueObject>();

        for ( Gene g : genes ) {
            GeneEvidenceValueObject geneEvidenceValueObject = new GeneEvidenceValueObject( g,
                    convert2ValueObjects( g.getPhenotypeAssociations() ) );
            geneEvidenceValueObjects.add( geneEvidenceValueObject );
        }

        Collection<GeneEvidenceValueObject> geneValueObjectsFilter = new ArrayList<GeneEvidenceValueObject>();

        for ( GeneEvidenceValueObject gene : geneEvidenceValueObjects ) {
            if ( gene.getEvidence() != null && gene.getEvidence().size() != 0 ) {
                geneValueObjectsFilter.add( gene );
            }
        }

        return geneValueObjectsFilter;
    }

    /**
     * Load an evidence
     * 
     * @param id The Evidence database id
     * @return evidence, or null if not found (why not throw an exception?)
     */
    @Override
    @Transactional(readOnly = true)
    public EvidenceValueObject load( Long id ) {

        assert id != null;

        PhenotypeAssociation phenotypeAssociation = this.associationService.load( id );

        if ( phenotypeAssociation == null ) {
            return null;
        }

        EvidenceValueObject evidenceValueObject = convert2ValueObjects( phenotypeAssociation );

        return evidenceValueObject;
    }

    /**
     * load all the valueUri and value of phenotype present in Neurocarta
     * 
     * @return Collection<String> the valueUri of the phenotypes
     */
    @Override
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {
        return this.associationService.loadAllNeurocartaPhenotypes();
    }

    /**
     * This method loads all phenotypes in the database and counts their occurence using the database It builts the tree
     * using parents of terms, and will return 3 trees representing Disease, HP and MP
     * 
     * @param taxonCommonName specify a taxon (optional)
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( EvidenceFilter evidenceFilter ) {

        Collection<SimpleTreeValueObject> simpleTreeValueObjects = new TreeSet<SimpleTreeValueObject>();

        Collection<TreeCharacteristicValueObject> ontologyTrees = customTreeFeatures( findAllPhenotypesByTree( true,
                evidenceFilter ) );

        // undo the tree in a simple structure
        for ( TreeCharacteristicValueObject t : ontologyTrees ) {
            convertToFlatTree( simpleTreeValueObjects, t, null /* parent of root */);
        }

        return simpleTreeValueObjects;
    }

    /**
     * this method can be used if we want to reimport data from a specific external Database
     * 
     * @param externalDatabaseName
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> loadEvidenceWithExternalDatabaseName( String externalDatabaseName ) {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findEvidencesWithExternalDatabaseName( externalDatabaseName );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /**
     * returns an DifferentialExpressionEvidence for a geneDifferentialExpressionMetaAnalysisId if one exists (used to
     * find the threshold and phenotypes for a GeneDifferentialExpressionMetaAnalysis)
     * 
     * @param geneDifferentialExpressionMetaAnalysisId id of the GeneDifferentialExpressionMetaAnalysis
     * @return DifferentialExpressionEvidence if an differentialExpressionEvidence exists for that id returns it
     */
    @Override
    public DiffExpressionEvidenceValueObject loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId ) {

        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.associationService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, 1L );

        if ( !differentialExpressionEvidence.isEmpty() ) {
            return this.convertDifferentialExpressionEvidence2ValueObject( differentialExpressionEvidence.iterator()
                    .next() );
        }

        return null;
    }

    /**
     * find all evidence that doesn't come from an external source
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject> loadEvidenceWithoutExternalDatabaseName() {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findEvidencesWithoutExternalDatabaseName();

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /**
     * find statistics on evidence used in neurocarta
     * 
     * @return Collection<ExternalDatabaseStatisticsValueObject> statistics for each external database
     */
    @Override
    public Collection<ExternalDatabaseStatisticsValueObject> loadNeurocartaStatistics() {

        Collection<ExternalDatabaseStatisticsValueObject> externalDatabaseStatisticsValueObjects = new TreeSet<ExternalDatabaseStatisticsValueObject>();

        // find statistics the external databases sources
        externalDatabaseStatisticsValueObjects.addAll( this.associationService.loadStatisticsOnExternalDatabases() );
        // manual curation
        externalDatabaseStatisticsValueObjects.add( this.associationService.loadStatisticsOnManualCuration() );
        // total
        externalDatabaseStatisticsValueObjects.add( this.associationService.loadStatisticsOnAllEvidence() );

        return externalDatabaseStatisticsValueObjects;
    }

    /**
     * creates the DifferentialExpressionEvidences using an DiffExpressionMetaAnalysis
     * 
     * @param geneDifferentialExpressionMetaAnalysisId id of the DiffExpressionMetaAnalysis
     * @param phenotypes phenotypes chosen
     * @param thresholdChosen threshold chosen to keep certain results
     * @return ValidateEvidenceValueObject flags of information to show user messages
     * @throws Exception
     */
    @Override
    public ValidateEvidenceValueObject makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, SortedSet<CharacteristicValueObject> phenotypes,
            Double selectionThreshold ) {

        GeneDifferentialExpressionMetaAnalysis geneDifferentialExpressionMetaAnalysis = this.geneDiffExMetaAnalysisService
                .load( geneDifferentialExpressionMetaAnalysisId );

        // check that no evidence already exists with that metaAnalysis
        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.associationService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, 1L );

        if ( !differentialExpressionEvidence.isEmpty() ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            return validateEvidenceValueObject;
        }

        for ( GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult : geneDifferentialExpressionMetaAnalysis
                .getResults() ) {

            if ( geneDifferentialExpressionMetaAnalysisResult.getMetaQvalue() <= selectionThreshold ) {

                DiffExpressionEvidenceValueObject diffExpressionEvidenceValueObject = new DiffExpressionEvidenceValueObject(
                        geneDifferentialExpressionMetaAnalysis, geneDifferentialExpressionMetaAnalysisResult,
                        phenotypes, "IEP", selectionThreshold );

                // set the score
                ScoreValueObject scoreValueObject = new ScoreValueObject( null,
                        geneDifferentialExpressionMetaAnalysisResult.getMetaPvalue().toString(), "P-value" );

                diffExpressionEvidenceValueObject.setScoreValueObject( scoreValueObject );

                ValidateEvidenceValueObject validateEvidenceValueObject = makeEvidence( diffExpressionEvidenceValueObject );

                if ( validateEvidenceValueObject != null ) {
                    // since this method created multiple evidence, if a problem is detected stop the transaction
                    throw new RuntimeException(
                            "makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis() problem detected" );
                }
            }
        }
        return null;
    }

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return Status of the operation
     */
    @Override
    @Transactional
    public ValidateEvidenceValueObject makeEvidence( EvidenceValueObject evidence ) {

        if ( evidence.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot create an Evidence with no Phenotype" );
        }
        if ( evidence.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Cannot create an Evidence not linked to a Gene" );
        }

        StopWatch sw = new StopWatch();
        sw.start();
        log.info( "Create PhenotypeAssociation on geneNCBI: " + evidence.getGeneNCBI() + " to "
                + StringUtils.join( evidence.getPhenotypes(), "," ) );

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        if ( evidenceAlreadyInDatabase( evidence ) != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            log.info( "The evidence is already in the database: " + evidence.getGeneNCBI() + " to "
                    + StringUtils.join( evidence.getPhenotypes(), "," ) + ", no change will be made" );
            return validateEvidenceValueObject;
        }

        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .valueObject2Entity( evidence );

        assert !phenotypeAssociation.getPhenotypes().isEmpty();

        phenotypeAssociation = this.associationService.create( phenotypeAssociation );

        // updates the gene in the cache FIXME is this the right way to enforce this?
        /*
         * NOTE : me and Anton used this solution, if not would cause problems in other services calls,would return the
         * cached unchanged version of it, we found documentation on this, might be a better way to do it
         */
        Gene gene = phenotypeAssociation.getGene();
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

        if ( sw.getTime() > 100 ) log.info( "The create method took : " + sw + "  " + evidence.getGeneNCBI() );

        return validateEvidenceValueObject;
    }

    /**
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    @Transactional
    public ValidateEvidenceValueObject remove( Long id ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        PhenotypeAssociation evidence = this.associationService.load( id );

        if ( evidence != null ) {

            if ( evidence.getEvidenceSource() != null ) {
                this.databaseEntryDao.remove( evidence.getEvidenceSource().getId() );
            }

            this.associationService.remove( evidence );

        } else {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
        }
        return validateEvidenceValueObject;
    }

    /**
     * Removes all the evidence that came from a specific metaAnalysis
     * 
     * @param geneDifferentialExpressionMetaAnalysisId the geneDifferentialExpressionMetaAnalysis Id
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    @Override
    public ValidateEvidenceValueObject removeAllEvidenceFromMetaAnalysis( Long geneDifferentialExpressionMetaAnalysisId ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        // checking if there is something to delete
        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.associationService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, null );
        if ( differentialExpressionEvidence.isEmpty() ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
            return validateEvidenceValueObject;
        }

        for ( DifferentialExpressionEvidence diffExpressionEvidence : differentialExpressionEvidence ) {
            this.associationService.remove( diffExpressionEvidence );
        }

        return validateEvidenceValueObject;
    }

    /**
     * For a given search string find all Ontology terms related, and then count their gene occurrence by taxon,
     * including ontology children terms
     * 
     * @param searchQuery the query search that was type by the user
     * @return Collection<CharacteristicValueObject> the terms found in the database with taxon and gene occurrence
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> searchInDatabaseForPhenotype( String searchQuery ) {

        Collection<CharacteristicValueObject> results = new TreeSet<CharacteristicValueObject>();

        String newSearchQuery = prepareOntologyQuery( searchQuery );

        // search the Ontology with the search query
        Collection<OntologyTerm> ontologyTermsFound = this.ontologyHelper.findValueUriInOntology( newSearchQuery );

        // Set of valueUri of all Ontology Terms found + their children
        Set<String> phenotypesFoundAndChildren = this.ontologyHelper.findAllChildrenAndParent( ontologyTermsFound );

        if ( !phenotypesFoundAndChildren.isEmpty() ) {

            // gene counts for all phenotypes used
            for ( int i = 0; i < PhenotypeAssociationConstants.TAXA_IN_USE.length; i++ ) {
                results.addAll( this.findPhenotypeCount( ontologyTermsFound,
                        this.taxonService.findByCommonName( PhenotypeAssociationConstants.TAXA_IN_USE[i] ),
                        phenotypesFoundAndChildren ) );
            }
        }

        return results;
    }

    /**
     * Giving a phenotype searchQuery, returns a selection choice to the user
     * 
     * @param searchQuery query typed by the user
     * @param geneId the id of the chosen gene
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId ) {
        StopWatch timer = new StopWatch();
        timer.start();
        List<CharacteristicValueObject> orderedPhenotypesFromOntology = new ArrayList<CharacteristicValueObject>();

        boolean geneProvided = true;

        if ( geneId == null ) {
            geneProvided = false;
        }

        // prepare the searchQuery to correctly query the Ontology
        String newSearchQuery = prepareOntologyQuery( searchQuery );

        // search the Ontology with the new search query
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = this.ontologyHelper
                .findPhenotypesInOntology( newSearchQuery );

        // All phenotypes present on the gene (if the gene was given)
        Set<CharacteristicValueObject> phenotypesOnCurrentGene = null;

        if ( geneProvided ) {
            phenotypesOnCurrentGene = findUniquePhenotypesForGeneId( geneId );
        }

        // all phenotypes currently in the database
        Set<String> allPhenotypesInDatabase = this.associationService.loadAllUsedPhenotypeUris();
        if ( allPhenotypesInDatabase.isEmpty() ) {
            return orderedPhenotypesFromOntology;
        }

        // rules to order the Ontology results found
        Set<CharacteristicValueObject> phenotypesWithExactMatch = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesAlreadyPresentOnGene = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesStartWithQueryAndInDatabase = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesStartWithQuery = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesSubstringAndInDatabase = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesSubstring = new TreeSet<CharacteristicValueObject>();

        /*
         * for each CharacteristicVO found from the Ontology, filter them and add them to a specific list if they
         * satisfied the condition
         */
        if ( allPhenotypesFoundInOntology.isEmpty() ) {
            /*
             * This is just for the case where ontology isn't loaded; so we can still get results. But that means only
             * results are terms that are already used in a phenotype.
             */
            Collection<PhenotypeValueObject> allNeurocartaPhenotypes = this.associationService
                    .loadAllNeurocartaPhenotypes();
            for ( PhenotypeValueObject pvo : allNeurocartaPhenotypes ) {

                CharacteristicValueObject cha = new CharacteristicValueObject( pvo.getValue(), pvo.getValueUri() );
                // set flag for UI, flag if the phenotype is on the Gene or if in the database
                cha.setAlreadyPresentOnGene( true );
                cha.setAlreadyPresentInDatabase( true );

                // order the results by specific rules

                // Case 1, exact match
                if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {
                    phenotypesWithExactMatch.add( cha );
                }
                // Case 2, phenotype already present on Gene
                else if ( phenotypesOnCurrentGene != null && phenotypesOnCurrentGene.contains( cha ) ) {
                    phenotypesAlreadyPresentOnGene.add( cha );
                }
                // Case 3, starts with a substring of the word
                else if ( cha.getValue().toLowerCase().startsWith( searchQuery.toLowerCase() ) ) {
                    phenotypesStartWithQueryAndInDatabase.add( cha );
                    phenotypesStartWithQuery.add( cha );

                }
                // Case 4, contains a substring of the word
                else if ( cha.getValue().toLowerCase().indexOf( searchQuery.toLowerCase() ) != -1 ) {
                    phenotypesSubstringAndInDatabase.add( cha );
                    phenotypesSubstring.add( cha );
                }
            }

        } else {
            for ( CharacteristicValueObject cha : allPhenotypesFoundInOntology ) {

                // set flag for UI, flag if the phenotype is on the Gene or if in the database
                if ( phenotypesOnCurrentGene != null && phenotypesOnCurrentGene.contains( cha ) ) {
                    cha.setAlreadyPresentOnGene( true );
                } else if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                    cha.setAlreadyPresentInDatabase( true );
                }

                // order the results by specific rules

                // Case 1, exact match
                if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {
                    phenotypesWithExactMatch.add( cha );
                }
                // Case 2, phenotype already present on Gene
                else if ( phenotypesOnCurrentGene != null && phenotypesOnCurrentGene.contains( cha ) ) {
                    phenotypesAlreadyPresentOnGene.add( cha );
                }
                // Case 3, starts with a substring of the word
                else if ( cha.getValue().toLowerCase().startsWith( searchQuery.toLowerCase() ) ) {
                    if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                        phenotypesStartWithQueryAndInDatabase.add( cha );
                    } else {
                        phenotypesStartWithQuery.add( cha );
                    }
                }
                // Case 4, contains a substring of the word
                else if ( cha.getValue().toLowerCase().indexOf( searchQuery.toLowerCase() ) != -1 ) {
                    if ( allPhenotypesInDatabase.contains( cha.getValueUri() ) ) {
                        phenotypesSubstringAndInDatabase.add( cha );
                    } else {
                        phenotypesSubstring.add( cha );
                    }
                }
            }
        }

        // place them in the correct order to display
        orderedPhenotypesFromOntology.addAll( phenotypesWithExactMatch );
        orderedPhenotypesFromOntology.addAll( phenotypesAlreadyPresentOnGene );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQueryAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstringAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQuery );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstring );

        // limit the size of the returned phenotypes to 100 terms
        if ( orderedPhenotypesFromOntology.size() > MAX_PHENOTYPES_FROM_ONTOLOGY ) {
            if ( timer.getTime() > 1000 ) {
                log.info( "Phenotype search: " + timer.getTime() + "ms" );
            }
            return orderedPhenotypesFromOntology.subList( 0, MAX_PHENOTYPES_FROM_ONTOLOGY );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Phenotype search: " + timer.getTime() + "ms" );
        }
        return orderedPhenotypesFromOntology;
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     * @return Status of the operation
     */
    @Override
    @Transactional
    public ValidateEvidenceValueObject update( EvidenceValueObject modifedEvidenceValueObject ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        if ( modifedEvidenceValueObject.getPhenotypes() == null || modifedEvidenceValueObject.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "An evidence must have a phenotype" );
        }

        if ( modifedEvidenceValueObject instanceof DiffExpressionEvidenceValueObject ) {
            throw new IllegalArgumentException( "DiffExpressionEvidence type cannot be updated" );
        }

        if ( modifedEvidenceValueObject.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Evidence not linked to a Gene" );
        }

        if ( modifedEvidenceValueObject.getId() == null ) {
            throw new IllegalArgumentException( "No database id provided" );
        }

        if ( evidenceAlreadyInDatabase( modifedEvidenceValueObject ) != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            return validateEvidenceValueObject;
        }

        PhenotypeAssociation phenotypeAssociation = this.associationService.load( modifedEvidenceValueObject.getId() );

        if ( phenotypeAssociation == null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
            return validateEvidenceValueObject;
        }

        // check for the race condition
        if ( phenotypeAssociation.getStatus().getLastUpdateDate().getTime() != modifedEvidenceValueObject
                .getLastUpdated() ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setLastUpdateDifferent( true );
            return validateEvidenceValueObject;
        }

        EvidenceValueObject evidenceValueObject = convert2ValueObjects( phenotypeAssociation );

        // evidence type changed
        if ( !evidenceValueObject.getClass().equals( modifedEvidenceValueObject.getClass() ) ) {
            remove( modifedEvidenceValueObject.getId() );
            return makeEvidence( modifedEvidenceValueObject );
        }

        // modify phenotypes
        populateModifiedPhenotypes( modifedEvidenceValueObject.getPhenotypes(), phenotypeAssociation );

        // modify all other values needed
        this.phenotypeAssoManagerServiceHelper
                .populateModifiedValues( modifedEvidenceValueObject, phenotypeAssociation );

        this.associationService.update( phenotypeAssociation );

        return validateEvidenceValueObject;
    }

    /**
     * Validate an Evidence before we create it
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    @Override
    @Transactional(readOnly = true)
    public ValidateEvidenceValueObject validateEvidence( EvidenceValueObject evidence ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        EvidenceValueObject evidenceValueObjectInDatabase = evidenceAlreadyInDatabase( evidence );

        if ( evidenceValueObjectInDatabase != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            validateEvidenceValueObject.getProblematicEvidenceIds().add( evidenceValueObjectInDatabase.getId() );
            return validateEvidenceValueObject;
        }

        if ( !evidence.getPhenotypeAssPubVO().isEmpty() && evidence.getEvidenceSource() == null ) {
            validateEvidenceValueObject = determineSameGeneAndPhenotypeAnnotated( evidence, evidence
                    .getPhenotypeAssPubVO().iterator().next().getCitationValueObject().getPubmedAccession() );
        }

        return validateEvidenceValueObject;
    }

    // TODO this method right now, is not accessible to the client
    /**
     * Creates a dump of all evidence in the database that can be downloaded on the client, this is run once per month
     * by Quartz
     */
    @Override
    @Transactional(readOnly = true)
    public void writeAllEvidenceToFile() {

        int i = 0;

        try {
            // the root path of the folder named NeurocartaExport
            String neurocartaDataHome = Settings.getString( "gemma.appdata.home" ) + File.separator
                    + "NeurocartaExport" + File.separator;

            // path of the folder where the dump will be created and the data put
            String writeFolder = neurocartaDataHome + "EvidenceExport_" + DateUtil.getTodayDate( true )
                    + File.separator;

            // creates the folders if they dont exist
            File mainFolder = new File( neurocartaDataHome );
            mainFolder.mkdir();
            File dataFolder = new File( writeFolder );
            dataFolder.mkdir();

            // this writer will be used to write 1 file per resource
            BufferedWriter fileWriterDataSource = null;
            // this writer is the dump of all evidence
            BufferedWriter fileWriterAllEvidence = new BufferedWriter(
                    new FileWriter( writeFolder + "ALL_EVIDENCE.tsv" ) );

            // TODO change this to correct one
            String disclaimer = "# Generated by Gemma " + "\n" + "# " + DateUtil.getTodayDate( true ) + "\n"
                    + "# Fields are delimited by tabs " + "\n"
                    + "#If you use this file for your research, please cite Neurocarta\n";

            // header of file
            String header = disclaimer
                    + "Data Source\tGene NCBI\tGene Symbol\tPhenotypes\tPubmeds\tWeb Link\tIs Negative\n";
            fileWriterAllEvidence.write( header );

            // lets get all external databases linked to evidence, we will create a file for each
            Collection<ExternalDatabaseValueObject> externalDatabaseValueObjects = findExternalDatabasesWithEvidence();

            for ( ExternalDatabaseValueObject externalDatabaseValueObject : externalDatabaseValueObjects ) {

                fileWriterDataSource = new BufferedWriter( new FileWriter( writeFolder
                        + externalDatabaseValueObject.getName().replaceAll( " ", "" ) + ".tsv" ) );

                // header of file
                fileWriterDataSource.write( header );

                // not using value object to make it faster
                Collection<PhenotypeAssociation> phenotypeAssociations = null;

                // this one is a special case, not actually linked to an external database
                if ( externalDatabaseValueObject.getName().equalsIgnoreCase( "Manual Curation" ) ) {
                    phenotypeAssociations = this.associationService.findEvidencesWithoutExternalDatabaseName();
                } else {
                    phenotypeAssociations = this.associationService
                            .findEvidencesWithExternalDatabaseName( externalDatabaseValueObject.getName() );
                }

                for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

                    if ( i++ % 5000 == 0 ) {
                        log.info( "Neurocarta dump of evidence at evidence number: " + i );
                    }

                    String pubmeds = "";

                    for ( PhenotypeAssociationPublication phenotypeAssociationPublication : phenotypeAssociation
                            .getPhenotypeAssociationPublications() ) {

                        pubmeds = phenotypeAssociationPublication.getCitation().getPubAccession().getAccession() + ";";
                    }

                    String phenotypes = "";

                    for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
                        phenotypes = cha.getValue() + ";";
                    }

                    String webLink = "";

                    if ( phenotypeAssociation.getEvidenceSource() != null ) {
                        webLink = phenotypeAssociation.getEvidenceSource().getExternalDatabase().getWebUri()
                                + phenotypeAssociation.getEvidenceSource().getAccession();
                    }

                    String isNegative = "";

                    if ( phenotypeAssociation.getIsNegativeEvidence() ) {
                        isNegative = "Yes";
                    }

                    // represents 1 evidence
                    String evidenceLine = externalDatabaseValueObject.getName() + "\t"
                            + phenotypeAssociation.getGene().getNcbiGeneId() + "\t"
                            + phenotypeAssociation.getGene().getOfficialSymbol() + "\t"
                            + StringUtils.removeEnd( phenotypes, ";" ) + "\t" + pubmeds + "\t" + webLink + "\t"
                            + isNegative + "\n";

                    fileWriterDataSource.write( evidenceLine );
                    fileWriterAllEvidence.write( evidenceLine );
                }
                fileWriterDataSource.close();
            }
            fileWriterAllEvidence.close();

            // LatestEvidenceExport ---> points to the latest dump
            File symbolicLink = new File( neurocartaDataHome + "LatestEvidenceExport" );

            if ( symbolicLink.exists() ) {
                Files.delete( symbolicLink.toPath() );
            }
            Files.createSymbolicLink( symbolicLink.toPath(), dataFolder.toPath() );

        } catch ( IOException e ) {
            log.error( ExceptionUtils.getStackTrace( e ) );
        }

    }

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     * 
     * @param phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    private Collection<EvidenceValueObject> convert2ValueObjects( Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<EvidenceValueObject> returnEvidenceVO = new HashSet<EvidenceValueObject>();

        if ( phenotypeAssociations != null ) {

            for ( PhenotypeAssociation phe : phenotypeAssociations ) {

                EvidenceValueObject evidence = convert2ValueObjects( phe );

                if ( evidence != null ) {
                    returnEvidenceVO.add( evidence );
                }
            }
        }
        return returnEvidenceVO;
    }

    /**
     * Convert an evidence entity to its corresponding value object
     * 
     * @param phe The phenotype Entity
     * @return Collection<EvidenceValueObject> its corresponding value object
     */
    private EvidenceValueObject convert2ValueObjects( PhenotypeAssociation phe ) {

        EvidenceValueObject evidence = null;

        Class<?> userClass = phe.getClass();
        Object p = phe;
        if ( EntityUtils.isProxy( phe ) ) {
            p = EntityUtils.getImplementationForProxy( phe );
            userClass = p.getClass();
        }

        if ( ExperimentalEvidence.class.isAssignableFrom( userClass ) ) {
            evidence = new ExperimentalEvidenceValueObject( ( ExperimentalEvidence ) p );
        } else if ( GenericEvidence.class.isAssignableFrom( userClass ) ) {
            evidence = new GenericEvidenceValueObject( ( GenericEvidence ) p );
        } else if ( LiteratureEvidence.class.isAssignableFrom( userClass ) ) {
            evidence = new LiteratureEvidenceValueObject( ( LiteratureEvidence ) p );
        } else if ( DifferentialExpressionEvidence.class.isAssignableFrom( userClass ) ) {
            evidence = convertDifferentialExpressionEvidence2ValueObject( ( DifferentialExpressionEvidence ) p );
        } else {
            throw new UnsupportedOperationException( "Don't know how to convert a " + userClass.getSimpleName() );
        }

        findEvidencePermissions( phe, evidence );

        return evidence;
    }

    private DiffExpressionEvidenceValueObject convertDifferentialExpressionEvidence2ValueObject(
            DifferentialExpressionEvidence differentialExpressionEvidence ) {

        DiffExpressionEvidenceValueObject diffExpressionEvidenceValueObject = null;
        if ( differentialExpressionEvidence != null ) {

            GeneDifferentialExpressionMetaAnalysis geneDifferentialExpressionMetaAnalysis = this.geneDiffExMetaAnalysisService
                    .loadWithResultId( differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult()
                            .getId() );

            Collection<Long> ids = new HashSet<Long>();
            ids.add( geneDifferentialExpressionMetaAnalysis.getId() );

            GeneDifferentialExpressionMetaAnalysisSummaryValueObject geneDiffExMetaAnalysisSummaryValueObject = this.geneDiffExMetaAnalysisService
                    .findMetaAnalyses( ids ).iterator().next();

            diffExpressionEvidenceValueObject = new DiffExpressionEvidenceValueObject( differentialExpressionEvidence,
                    geneDiffExMetaAnalysisSummaryValueObject );

            // set the count, how many evidences where created from the specific meta analysis
            diffExpressionEvidenceValueObject.setNumEvidenceFromSameMetaAnalysis( this.associationService
                    .countEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysis
                            .getId() ) );
        }

        return diffExpressionEvidenceValueObject;
    }

    /**
     * take the trees made and put them in the exact way the client wants them
     */
    private void convertToFlatTree( Collection<SimpleTreeValueObject> simpleTreeValueObjects,
            TreeCharacteristicValueObject treeCharacteristicValueObject, String parent ) {

        if ( treeCharacteristicValueObject == null ) {
            return;
        }

        SimpleTreeValueObject simpleTreeValueObject = new SimpleTreeValueObject( treeCharacteristicValueObject, parent );

        if ( treeCharacteristicValueObject.getChildren().isEmpty() ) {
            simpleTreeValueObject.set_is_leaf( true );
        }

        simpleTreeValueObjects.add( simpleTreeValueObject );

        for ( TreeCharacteristicValueObject tree : treeCharacteristicValueObject.getChildren() ) {
            convertToFlatTree( simpleTreeValueObjects, tree, simpleTreeValueObject.get_id() );
        }

    }

    /** Changing the root names and the order to present them */
    private Collection<TreeCharacteristicValueObject> customTreeFeatures(
            Collection<TreeCharacteristicValueObject> ontologyTrees ) {

        TreeCharacteristicValueObject[] customOntologyTrees = new TreeCharacteristicValueObject[3];

        for ( TreeCharacteristicValueObject tree : ontologyTrees ) {
            if ( tree.getValueUri().indexOf( "DOID" ) != -1 ) {
                tree.setValue( "Disease Ontology" );
                customOntologyTrees[0] = tree;
            } else if ( tree.getValueUri().indexOf( "HP" ) != -1 ) {
                tree.setValue( "Human Phenotype Ontology" );
                customOntologyTrees[1] = tree;
            } else if ( tree.getValueUri().indexOf( "MP" ) != -1 ) {
                tree.setValue( "Mammalian Phenotype Ontology" );
                customOntologyTrees[2] = tree;
            }
        }
        return Arrays.asList( customOntologyTrees );
    }

    /** Populates the ValidateEvidenceValueObject with the correct flags if necessary */
    private ValidateEvidenceValueObject determineSameGeneAndPhenotypeAnnotated( EvidenceValueObject evidence,
            String pubmed ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        BibliographicReferenceValueObject bibliographicReferenceValueObject = findBibliographicReference( pubmed,
                evidence.getId() );

        if ( bibliographicReferenceValueObject == null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setPubmedIdInvalid( true );
        } else {

            // rule to determine if its an update
            if ( evidence.getId() != null ) {

                PhenotypeAssociation phenotypeAssociation = this.associationService.load( evidence.getId() );

                if ( phenotypeAssociation == null ) {
                    validateEvidenceValueObject = new ValidateEvidenceValueObject();
                    validateEvidenceValueObject.setEvidenceNotFound( true );
                    return validateEvidenceValueObject;
                }

                // check for the race condition
                if ( phenotypeAssociation.getStatus().getLastUpdateDate().getTime() != evidence.getLastUpdated() ) {
                    validateEvidenceValueObject = new ValidateEvidenceValueObject();
                    validateEvidenceValueObject.setLastUpdateDifferent( true );
                    return validateEvidenceValueObject;
                }
            }

            for ( BibliographicPhenotypesValueObject bibliographicPhenotypesValueObject : bibliographicReferenceValueObject
                    .getBibliographicPhenotypes() ) {

                if ( evidence.getId() != null ) {
                    // dont compare evidence to itself since it already exists
                    if ( evidence.getId().equals( bibliographicPhenotypesValueObject.getEvidenceId() ) ) {
                        continue;
                    }
                }

                // look if the gene have already been annotated
                if ( evidence.getGeneNCBI().equals( bibliographicPhenotypesValueObject.getGeneNCBI() ) ) {

                    if ( validateEvidenceValueObject == null ) {
                        validateEvidenceValueObject = new ValidateEvidenceValueObject();
                    }

                    validateEvidenceValueObject.setSameGeneAnnotated( true );
                    validateEvidenceValueObject.getProblematicEvidenceIds().add(
                            bibliographicPhenotypesValueObject.getEvidenceId() );

                    boolean containsExact = true;

                    for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                        if ( !bibliographicPhenotypesValueObject.getPhenotypesValues().contains( phenotype ) ) {
                            containsExact = false;
                        }
                    }

                    if ( containsExact ) {
                        validateEvidenceValueObject.setSameGeneAndOnePhenotypeAnnotated( true );
                        validateEvidenceValueObject.getProblematicEvidenceIds().add(
                                bibliographicPhenotypesValueObject.getEvidenceId() );
                    }

                    if ( evidence.getPhenotypes().size() == bibliographicPhenotypesValueObject.getPhenotypesValues()
                            .size()
                            && evidence.getPhenotypes().containsAll(
                                    bibliographicPhenotypesValueObject.getPhenotypesValues() ) ) {
                        validateEvidenceValueObject.setSameGeneAndPhenotypesAnnotated( true );
                        validateEvidenceValueObject.getProblematicEvidenceIds().add(
                                bibliographicPhenotypesValueObject.getEvidenceId() );
                    }

                    Set<String> parentOrChildTerm = new HashSet<String>();

                    // for the phenotype already present we add his children and direct parents, and check that
                    // the phenotype we want to add is not in that subset
                    for ( CharacteristicValueObject phenotypeAlreadyPresent : bibliographicPhenotypesValueObject
                            .getPhenotypesValues() ) {

                        OntologyTerm ontologyTerm = this.ontologyService
                                .getTerm( phenotypeAlreadyPresent.getValueUri() );

                        for ( OntologyTerm ot : ontologyTerm.getParents( true ) ) {
                            parentOrChildTerm.add( ot.getUri() );
                        }

                        for ( OntologyTerm ot : ontologyTerm.getChildren( false ) ) {
                            parentOrChildTerm.add( ot.getUri() );
                        }
                    }

                    for ( CharacteristicValueObject characteristicValueObject : evidence.getPhenotypes() ) {

                        if ( parentOrChildTerm.contains( characteristicValueObject.getValueUri() ) ) {
                            validateEvidenceValueObject.setSameGeneAndPhenotypeChildOrParentAnnotated( true );
                            validateEvidenceValueObject.getProblematicEvidenceIds().add(
                                    bibliographicPhenotypesValueObject.getEvidenceId() );
                        }
                    }
                }
            }
        }
        return validateEvidenceValueObject;
    }

    /** Checks to see if the evidence is already in the database */
    private EvidenceValueObject evidenceAlreadyInDatabase( EvidenceValueObject evidence ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneNCBI( evidence.getGeneNCBI(), evidence.getPhenotypesValueUri() );

        Collection<EvidenceValueObject> evidenceValueObjects = convert2ValueObjects( phenotypeAssociations );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {

                // if doing an update dont take into account the current evidence
                if ( evidence.getId() != null ) {
                    if ( evidenceFound.getId().equals( evidence.getId() ) ) {
                        continue;
                    }
                }
                return evidenceFound;
            }
        }
        return null;
    }

    /**
     * Filter a set of genes if who have the root phenotype or a children of a root phenotype
     * 
     * @param geneEvidenceValueObjects
     * @param phenotypesWithChildren
     * @return
     */
    private Collection<GeneValueObject> filterGenesWithPhenotypes(
            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects,
            Map<String, Set<String>> phenotypesWithChildren ) {

        Collection<GeneValueObject> genesVO = new HashSet<>();

        for ( GeneEvidenceValueObject geneEvidenceValueObject : geneEvidenceValueObjects ) {

            // all phenotypeUri for a gene
            Set<String> allPhenotypesOnGene = geneEvidenceValueObject.getPhenotypesValueUri();

            // if the Gene has all the phenotypes
            boolean keepGene = true;

            for ( String phe : phenotypesWithChildren.keySet() ) {
                // at least 1 value must be found
                Set<String> possiblePheno = phenotypesWithChildren.get( phe );

                boolean foundSpecificPheno = false;

                for ( String pheno : possiblePheno ) {
                    if ( allPhenotypesOnGene.contains( pheno ) ) {
                        foundSpecificPheno = true;
                    }
                }

                if ( foundSpecificPheno == false ) {
                    // dont keep the gene since a root phenotype + children was not found for all evidence of that gene
                    keepGene = false;
                    break;
                }
            }
            if ( keepGene ) {
                genesVO.add( geneEvidenceValueObject );
            }
        }

        return genesVO;
    }

    /** filter evidence by owned by user or shared write access */
    private Collection<PhenotypeAssociation> filterPhenotypeAssociationsMyAnnotation(
            Collection<PhenotypeAssociation> phenotypeAssociations ) {

        Collection<PhenotypeAssociation> phenotypeAssociationsFiltered = new HashSet<PhenotypeAssociation>();

        for ( PhenotypeAssociation p : phenotypeAssociations ) {

            Boolean currentUserHasWritePermission = false;
            Boolean isShared = this.securityService.isShared( p );
            Boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

            if ( isShared ) {
                currentUserHasWritePermission = this.securityService.isEditable( p );
            }

            if ( currentUserHasWritePermission || currentUserIsOwner ) {
                phenotypeAssociationsFiltered.add( p );
            }
        }
        return phenotypeAssociationsFiltered;
    }

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology
     * 
     * @param withParentTerms if we want to include the parents terms from the Ontology
     * @param taxonCommonName if we only want a certain taxon
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    private Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree( boolean withParentTerms,
            EvidenceFilter evidenceFilter ) {

        StopWatch sw = new StopWatch();
        sw.start();
        Taxon taxon = null;
        boolean showOnlyEditable = false;
        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            if ( evidenceFilter.getTaxonId() != null && evidenceFilter.getTaxonId() > 0 ) {
                taxon = this.taxonService.load( evidenceFilter.getTaxonId() );
            }
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Map<String, Set<Integer>> publicPhenotypesGenesAssociations = new HashMap<>();
        Map<String, Set<Integer>> privatePhenotypesGenesAssociations = new HashMap<>();
        // public phenotypes + private phenotypes (what the user can see)
        Set<String> allPhenotypesGenesAssociations = new HashSet<String>();

        boolean isUserLoggedIn = SecurityUtil.isUserLoggedIn();
        String userName = "";
        Collection<String> groups = new HashSet<String>();
        boolean isAdmin = false;

        log.info( "Starting loading phenotype tree" );
        if ( isUserLoggedIn ) {
            userName = this.userManager.getCurrentUsername();
            // groups the user belong to
            groups = this.userManager.findGroupsForUser( userName );

            isAdmin = SecurityUtil.isUserAdmin();

            // show only my annotation was chosen
            if ( showOnlyEditable ) {
                // log.info( "Loading editable" );
                // show public owned by the user
                publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations(
                        taxon, null, userName, groups, showOnlyEditable, externalDatabaseIds );

                // show all private owned by the user or shared by a group
                privatePhenotypesGenesAssociations = this.associationService.findPrivatePhenotypesGenesAssociations(
                        taxon, null, userName, groups, showOnlyEditable, externalDatabaseIds );
                // log.info( "Loaded editable: " + sw.getTime() + "ms" );
            }
            // default case to build the tree
            else {
                // log.info( "Loading all public" );
                // all public evidences
                publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations(
                        taxon, null, null, groups, false, externalDatabaseIds );

                // log.info( "Loaded public: " + sw.getTime() + "ms" );
                if ( isAdmin ) {
                    // log.info( "Loading private" );
                    // show all private since admin
                    privatePhenotypesGenesAssociations = this.associationService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, null, null, false,
                                    externalDatabaseIds );
                    // log.info( "Loaded private: total time=" + sw.getTime() + "ms" );
                } else {
                    // show all private owned by the user or shared by a group
                    // log.info( "Loading owned" );
                    // show all private since admin
                    privatePhenotypesGenesAssociations = this.associationService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, userName, groups, false,
                                    externalDatabaseIds );
                    // log.info( "Loaded owned: total time=" + sw.getTime() + "ms" );
                }
            }
        }

        // anonymous user
        else if ( !showOnlyEditable ) {
            // log.info( "Loading editable" );
            publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations( taxon,
                    null, null, null, false, externalDatabaseIds );
            // log.info( "Loaded editable: total time=" + sw.getTime() + "ms" );
        }

        // log.info( "Done loading associations" );

        for ( String phenotype : privatePhenotypesGenesAssociations.keySet() ) {
            allPhenotypesGenesAssociations.add( phenotype );
        }
        for ( String phenotype : publicPhenotypesGenesAssociations.keySet() ) {
            allPhenotypesGenesAssociations.add( phenotype );
        }

        // map to help the placement of elements in the tree, used to find quickly the position to add subtrees
        Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<String, TreeCharacteristicValueObject>();

        // represents each phenotype and children found in the Ontology, TreeSet used to order trees
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<TreeCharacteristicValueObject>();

        // creates the tree structure
        for ( String valueUri : allPhenotypesGenesAssociations ) {

            // don't create the tree if it is already present in an other
            if ( phenotypeFoundInTree.get( valueUri ) != null ) {
                // flag the node as phenotype found in database
                phenotypeFoundInTree.get( valueUri ).setDbPhenotype( true );
            } else {
                try {
                    // find the ontology term using the valueURI
                    OntologyTerm ontologyTerm = this.ontologyHelper.findOntologyTermByUri( valueUri );

                    // we don't show obsolete terms
                    if ( ontologyTerm.isTermObsolete() ) {

                        log.error( "A valueUri found in the database is obsolete: " + valueUri );

                    } else {

                        // transform an OntologyTerm and his children to a TreeCharacteristicValueObject
                        TreeCharacteristicValueObject treeCharacteristicValueObject = TreeCharacteristicValueObject
                                .ontology2TreeCharacteristicValueObjects( ontologyTerm, phenotypeFoundInTree,
                                        treesPhenotypes );

                        // set flag that this node represents a phenotype in the database
                        treeCharacteristicValueObject.setDbPhenotype( true );

                        // add tree to the phenotypes found in ontology
                        phenotypeFoundInTree.put( ontologyTerm.getUri(), treeCharacteristicValueObject );

                        treesPhenotypes.add( treeCharacteristicValueObject );
                        if ( log.isDebugEnabled() ) log.debug( "Added: " + ontologyTerm );

                    }

                } catch ( EntityNotFoundException entityNotFoundException ) {
                    if ( this.ontologyHelper.areOntologiesAllLoaded() ) {
                        log.error( "A valueUri found in the database was not found in the ontology; This can happen when a valueUri is updated in the ontology; valueUri: "
                                + valueUri );
                    } else {
                        throw new RuntimeException( "Ontologies are not fully loaded yet, try again soon ("
                                + entityNotFoundException.getMessage() + ")" );
                    }
                }
            }
        }

        if ( withParentTerms ) {
            TreeSet<TreeCharacteristicValueObject> finalTreesWithRoots = new TreeSet<TreeCharacteristicValueObject>();

            for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
                findParentRoot( tc, finalTreesWithRoots, phenotypeFoundInTree );
            }
            treesPhenotypes = finalTreesWithRoots;
        }

        // set the public count
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            tc.countPublicGeneForEachNode( publicPhenotypesGenesAssociations );
        }

        // set the private count
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            tc.countPrivateGeneForEachNode( privatePhenotypesGenesAssociations );
        }

        // remove all nodes in the trees found in the Ontology but not in the database
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            tc.removeUnusedPhenotypes();
        }
        log.info( "Done total time=" + sw.getTime() + "ms" );
        return treesPhenotypes;
    }

    /**
     * add all the keySet together and return a set representing all children for all valueUri given (collapse the map
     * down to a single set)
     * 
     * @param phenotypesWithChildren
     * @return
     */
    private Set<String> findAllPossibleChildren( Map<String, Set<String>> phenotypesWithChildren ) {

        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }
        return possibleChildrenPhenotypes;
    }

    /**
     * Map query phenotypes given to the set of possible children phenotypes in the database so for example if the user
     * search for : cerebral palsy (parent phenotype), this will return all children associated with it that are also
     * present in the database(all children of phenotype) : Map<String(parent phenotype), Set<String>(all children of
     * phenotype)>
     * 
     * @param phenotypesValuesUris
     * @param usedPhenotypes the URIs of all phenotypes actually used in the database.
     * @return map of terms to their children. The term itself is included.
     */
    private Map<String, Set<String>> findChildrenForEachPhenotype( Collection<String> phenotypesValuesUris,
            Collection<String> usedPhenotypes ) {

        // root corresponds to one value found in phenotypesValuesUri
        // root ---> root+children phenotypes
        Map<String, Set<String>> parentPheno = new HashMap<String, Set<String>>();

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUris ) {

            if ( phenoRoot.isEmpty() ) {
                continue;
            }
            OntologyTerm ontologyTermFound = null;
            try {
                ontologyTermFound = this.ontologyHelper.findOntologyTermByUri( phenoRoot );
                if ( ontologyTermFound == null ) continue;
            } catch ( EntityNotFoundException e ) {
                // that's okay keep it. Ontologies might not be loaded.
                parentPheno.put( phenoRoot, new HashSet<String>() );
                parentPheno.get( phenoRoot ).add( phenoRoot );
                continue;
            }
            Collection<OntologyTerm> ontologyChildrenFound = ontologyTermFound.getChildren( false );

            Set<String> parentChildren = new HashSet<>();
            parentChildren.add( phenoRoot );

            for ( OntologyTerm ot : ontologyChildrenFound ) {

                if ( usedPhenotypes.contains( ot.getUri() ) ) {
                    parentChildren.add( ot.getUri() );
                }
            }
            parentPheno.put( phenoRoot, parentChildren );
        }
        return parentPheno;
    }

    /**
     * Determine permissions for an PhenotypeAssociation
     */
    private void findEvidencePermissions( PhenotypeAssociation p, EvidenceValueObject evidenceValueObject ) {

        Boolean currentUserHasWritePermission = false;
        String owner = null;
        Boolean isPublic = this.securityService.isPublic( p );
        Boolean isShared = this.securityService.isShared( p );
        Boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

        if ( currentUserIsOwner || isPublic || isShared || SecurityUtil.isUserAdmin() ) {

            /*
             * FIXME WARNING it is not guaranteed that owner is a PrincipalSid.
             */
            currentUserHasWritePermission = this.securityService.isEditable( p );
            owner = ( ( AclPrincipalSid ) this.securityService.getOwner( p ) ).getPrincipal();
        }

        evidenceValueObject.setEvidenceSecurityValueObject( new EvidenceSecurityValueObject(
                currentUserHasWritePermission, currentUserIsOwner, isPublic, isShared, owner ) );
    }

    /**
     * find Homologue-linked Evidence for a gene
     * 
     * @param geneId Gemma's identifier
     * @param evidenceFilter
     * @return
     */
    private Collection<EvidenceValueObject> findHomologueEvidence( Long geneId, EvidenceFilter evidenceFilter ) {

        Collection<Long> externalDatabaseIDs = null;

        if ( evidenceFilter != null ) {
            externalDatabaseIDs = evidenceFilter.getExternalDatabaseIds();
        }

        // Get the Gene object for finding homologues' evidence.
        Gene gene = this.geneService.load( geneId );

        Collection<Gene> homologues = this.homologeneService.getHomologues( gene );

        Collection<PhenotypeAssociation> homologuePhenotypeAssociations = new HashSet<PhenotypeAssociation>();

        for ( Gene homologue : homologues ) {
            Collection<PhenotypeAssociation> currHomologuePhenotypeAssociations = this.associationService
                    .findPhenotypeAssociationForGeneIdAndDatabases( homologue.getId(), externalDatabaseIDs );
            homologuePhenotypeAssociations.addAll( currHomologuePhenotypeAssociations );
        }

        if ( evidenceFilter != null && evidenceFilter.isShowOnlyEditable() ) {
            homologuePhenotypeAssociations = filterPhenotypeAssociationsMyAnnotation( homologuePhenotypeAssociations );
        }

        Collection<EvidenceValueObject> homologueEvidenceValueObjects = this
                .convert2ValueObjects( homologuePhenotypeAssociations );

        for ( EvidenceValueObject evidenceValueObject : homologueEvidenceValueObjects ) {
            evidenceValueObject.setHomologueEvidence( true );
        }

        return homologueEvidenceValueObjects;
    }

    /**
     * Build the full trees of the Ontology with the given branches
     */
    private void findParentRoot( TreeCharacteristicValueObject tc,
            TreeSet<TreeCharacteristicValueObject> finalTreesWithRoots,
            Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree ) {

        OntologyTerm ontologyTerm = this.ontologyHelper.findOntologyTermByUri( tc.getValueUri() );

        Collection<OntologyTerm> ontologyParents = ontologyTerm.getParents( true );

        if ( !ontologyParents.isEmpty() ) {

            for ( OntologyTerm onTerm : ontologyParents ) {

                // see if the node is already in the tree
                TreeCharacteristicValueObject alreadyOnTree = phenotypeFoundInTree.get( onTerm.getUri() );

                if ( alreadyOnTree != null ) {
                    alreadyOnTree.getChildren().add( tc );
                } else {
                    TreeCharacteristicValueObject tree = new TreeCharacteristicValueObject( onTerm.getLabel(),
                            onTerm.getUri() );

                    // add children to the parent
                    tree.getChildren().add( tc );

                    // put node in the hashmap for fast acces
                    phenotypeFoundInTree.put( tree.getValueUri(), tree );

                    findParentRoot( tree, finalTreesWithRoots, phenotypeFoundInTree );
                }
            }
        } else {
            // found a root, no more parents
            finalTreesWithRoots.add( tc );
        }
    }

    /** For a given Ontology Term, count the occurence of the term + children in the database */
    private Collection<CharacteristicValueObject> findPhenotypeCount( Collection<OntologyTerm> ontologyTermsFound,
            Taxon taxon, Set<String> phenotypesFoundAndChildren ) {

        Collection<CharacteristicValueObject> phenotypesFound = new HashSet<CharacteristicValueObject>();

        // Phenotype ---> Genes
        Map<String, Set<Integer>> publicPhenotypesGenesAssociations = this.associationService
                .findPublicPhenotypesGenesAssociations( taxon, phenotypesFoundAndChildren, null, null, false, null );

        // for each Ontoly Term find in the search
        for ( OntologyTerm ontologyTerm : ontologyTermsFound ) {

            Set<Integer> geneFoundForOntologyTerm = new HashSet<Integer>();

            if ( publicPhenotypesGenesAssociations.get( ontologyTerm.getUri() ) != null ) {
                geneFoundForOntologyTerm.addAll( publicPhenotypesGenesAssociations.get( ontologyTerm.getUri() ) );
            }

            // for all his children
            for ( OntologyTerm ontologyTermChildren : ontologyTerm.getChildren( false ) ) {

                if ( publicPhenotypesGenesAssociations.get( ontologyTermChildren.getUri() ) != null ) {
                    geneFoundForOntologyTerm.addAll( publicPhenotypesGenesAssociations.get( ontologyTermChildren
                            .getUri() ) );
                }
            }
            // count the number of distinct gene linked to this ontologyTerm ( or children) in the database
            if ( !geneFoundForOntologyTerm.isEmpty() ) {
                CharacteristicValueObject characteristicValueObject = new CharacteristicValueObject( ontologyTerm
                        .getLabel().toLowerCase(), ontologyTerm.getUri() );
                characteristicValueObject.setPublicGeneCount( geneFoundForOntologyTerm.size() );
                characteristicValueObject.setTaxon( taxon.getCommonName() );
                phenotypesFound.add( characteristicValueObject );
            }
        }
        return phenotypesFound;
    }

    /** Given a geneId finds all phenotypes for that gene */
    private Set<CharacteristicValueObject> findUniquePhenotypesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        Collection<EvidenceValueObject> evidence = findEvidenceByGeneId( geneId );

        for ( EvidenceValueObject evidenceVO : evidence ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /** Add flags to Evidence and CharacteristicvalueObjects */
    private void flagEvidence( Collection<EvidenceValueObject> evidencesVO, Set<String> phenotypesValuesUri,
            Collection<String> usedPhenotypes ) {

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Map<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypesValuesUri,
                usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }

        // flag relevant evidence, root phenotypes and children phenotypes

        for ( EvidenceValueObject evidenceVO : evidencesVO ) {

            boolean relevantEvidence = false;

            for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {

                // if the phenotype is a root
                if ( phenotypesValuesUri.contains( chaVO.getValueUri() ) ) {
                    relevantEvidence = true;
                    chaVO.setRoot( true );
                }
                // if the phenotype is a children of the root
                else if ( possibleChildrenPhenotypes.contains( chaVO.getValueUri() ) ) {
                    chaVO.setChild( true );
                    relevantEvidence = true;
                }
            }
            if ( relevantEvidence ) {
                evidenceVO.setContainQueryPhenotype( true );
            }
        }
    }

    /** Take care of populating new values for the phenotypes in an update */
    private void populateModifiedPhenotypes( Set<CharacteristicValueObject> updatedPhenotypes,
            PhenotypeAssociation phenotypeAssociation ) {

        // the modified final phenotype to update
        Collection<Characteristic> finalPhenotypes = new HashSet<Characteristic>();

        Map<Long, CharacteristicValueObject> updatedPhenotypesMap = new HashMap<Long, CharacteristicValueObject>();

        for ( CharacteristicValueObject updatedPhenotype : updatedPhenotypes ) {

            // updated
            if ( updatedPhenotype.getId() != null ) {
                updatedPhenotypesMap.put( updatedPhenotype.getId(), updatedPhenotype );
            }
            // new one
            else {
                Characteristic c = this.ontologyHelper.valueUri2Characteristic( updatedPhenotype.getValueUri() );
                if ( c == null ) {
                    throw new IllegalStateException( updatedPhenotype.getValueUri()
                            + " could not be converted to a characteristic" );
                }
                finalPhenotypes.add( c );
            }
        }

        for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {

            VocabCharacteristic phenotype = ( VocabCharacteristic ) cha;

            CharacteristicValueObject updatedPhenotype = updatedPhenotypesMap.get( phenotype.getId() );

            // found an update, same database id
            if ( updatedPhenotype != null ) {

                // same values as before
                if ( updatedPhenotype.equals( phenotype ) ) {
                    finalPhenotypes.add( phenotype );
                } else {
                    // different values found
                    phenotype.setValueUri( updatedPhenotype.getValueUri() );
                    phenotype.setValue( updatedPhenotype.getValue() );
                    finalPhenotypes.add( phenotype );
                }
            }
            // this phenotype was deleted
            else {
                this.characteristicService.delete( cha.getId() );
            }
        }
        phenotypeAssociation.getPhenotypes().clear();
        phenotypeAssociation.getPhenotypes().addAll( finalPhenotypes );
    }

    /**
     * used when we add an evidence and search for phenotype to add, other places too adds a wildcard to the search
     * 
     * @param query
     * @return the string with an added wildcard to it.
     */
    private String prepareOntologyQuery( String query ) {

        String newSearchQuery = query;

        if ( query != null ) {
            newSearchQuery = newSearchQuery + "*";
        }
        return newSearchQuery;
    }

}