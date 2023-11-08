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
package ubic.gemma.core.association.phenotype;

import gemma.gsec.SecurityService;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.util.DateUtil;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneService;
import ubic.gemma.core.loader.genome.gene.ncbi.homology.HomologeneServiceFactory;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysisSummaryValueObject;
import ubic.gemma.model.association.phenotype.*;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.EvidenceFilter;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.persistence.service.association.phenotype.PhenotypeAssociationDaoImpl;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.AsyncFactoryBeanUtils;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.Settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.io.FileUtils.forceMkdir;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 *
 * @author nicolas
 */
@Service
@Deprecated
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService, InitializingBean {

    private static final int MAX_PHENOTYPES_FROM_ONTOLOGY = 100;
    private static final String ERROR_MSG_ONTOLOGIES_NOT_LOADED = "Ontologies are not fully loaded yet, try again soon";
    private static final Log log = LogFactory.getLog( PhenotypeAssociationManagerServiceImpl.class );

    /**
     * Ontology roots or terms we do not want to propagate the parents of.
     */
    private static final String[] ROOTS = {
            "http://purl.obolibrary.org/obo/MONDO_0000001",
            "http://purl.obolibrary.org/obo/DOID_4",
            "http://purl.obolibrary.org/obo/HP_0000001",
            "http://purl.obolibrary.org/obo/MP_0000001"
    };

    @Autowired
    private PhenotypeAssociationService phenoAssocService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Autowired
    private GeneService geneService;

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

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private Future<HomologeneService> homologeneService;

    /**
     * Indicate if ontologies are loaded on startup.
     */
    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Override
    public void afterPropertiesSet() {
        this.pubMedXmlFetcher = new PubMedXMLFetcher();
        Set<ubic.basecode.ontology.providers.OntologyService> disabledOntologies = ontologyHelper.getOntologyServices().stream()
                .filter( os -> !os.isEnabled() )
                .collect( Collectors.toSet() );
        if ( autoLoadOntologies && !disabledOntologies.isEmpty() ) {
            log.warn( String.format( "The following ontologies are required by Phenocarta are not enabled:\n\t%s.",
                    disabledOntologies.stream().map( Object::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public BibliographicReferenceValueObject findBibliographicReference( String pubMedId ) {

        if ( StringUtils.isBlank( pubMedId ) )
            return null;

        // check if the given pubmedID is already in the database
        BibliographicReference bibliographicReference = this.bibliographicReferenceService.findByExternalId( pubMedId );

        // already in the database
        if ( bibliographicReference != null ) {

            BibliographicReferenceValueObject bibliographicReferenceVO = new BibliographicReferenceValueObject(
                    bibliographicReference );

            Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                    .findPhenotypesForBibliographicReference( pubMedId );

            Collection<BibliographicPhenotypesValueObject> bibliographicPhenotypesValueObjects = BibliographicPhenotypesValueObject
                    .phenotypeAssociations2BibliographicPhenotypesValueObjects( phenotypeAssociations );

            // set phenotypes associated with this bibliographic reference
            bibliographicReferenceVO.setBibliographicPhenotypes( bibliographicPhenotypesValueObjects );

            // set experiments associated with this bibliographic reference
            Collection<ExpressionExperiment> experiments = this.bibliographicReferenceService
                    .getRelatedExperiments( bibliographicReference );

            if ( experiments != null && !experiments.isEmpty() ) {
                bibliographicReferenceVO.setExperiments( expressionExperimentService.loadValueObjects( experiments ) );
            }

            return bibliographicReferenceVO;
        }

        // find the Bibliographic on PubMed
        bibliographicReference = this.pubMedXmlFetcher.retrieveByHTTP( Integer.parseInt( pubMedId ) );

        // the pudmedId doesn't exists in PudMed
        if ( bibliographicReference == null ) {
            return null;
        }

        return new BibliographicReferenceValueObject( bibliographicReference );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GeneEvidenceValueObject> findCandidateGenes( Collection<String> phenotypeValueUris,
            Taxon taxon ) {
        checkIfAllOntologiesAreLoaded();

        if ( phenotypeValueUris == null || phenotypeValueUris.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Set<String> usedPhenotypes = this.phenoAssocService.loadAllUsedPhenotypeUris();
        Map<String, Set<String>> phenotypesWithChildren = this
                .findChildrenForEachPhenotype( phenotypeValueUris, usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = this.findAllPossibleChildren( phenotypesWithChildren );

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = this.phenoAssocService
                .findGenesWithPhenotypes( possibleChildrenPhenotypes, taxon, false, null );

        return this.filterGenesWithPhenotypes( geneEvidenceValueObjects, phenotypesWithChildren );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<GeneEvidenceValueObject> findCandidateGenes( EvidenceFilter evidenceFilter,
            Set<String> phenotypesValuesUri ) {

        this.addDefaultExcludedDatabases( evidenceFilter );

        if ( phenotypesValuesUri == null || phenotypesValuesUri.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        Taxon taxon = null;
        boolean showOnlyEditable = false;
        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            taxon = this.checkAndGetTaxon( evidenceFilter );
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Set<String> usedPhenotypes = this.phenoAssocService.loadAllUsedPhenotypeUris();

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Map<String, Set<String>> phenotypesWithChildren = this
                .findChildrenForEachPhenotype( phenotypesValuesUri, usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = this.findAllPossibleChildren( phenotypesWithChildren );

        Collection<GeneEvidenceValueObject> genesPhenotypeHelperObject = this.phenoAssocService
                .findGenesWithPhenotypes( possibleChildrenPhenotypes, taxon, showOnlyEditable, externalDatabaseIds );

        return this.filterGenesWithPhenotypes( genesPhenotypeHelperObject, phenotypesWithChildren );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Collection<? extends GeneValueObject>> findCandidateGenesForEach( Set<String> phenotypeUris,
            Taxon taxon ) {
        if ( phenotypeUris == null || phenotypeUris.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );

        }

        Set<String> usedPhenotypes = this.phenoAssocService.loadAllUsedPhenotypeUris();
        Map<String, Set<String>> phenotypesWithChildren = this
                .findChildrenForEachPhenotype( phenotypeUris, usedPhenotypes );

        Map<String, Collection<? extends GeneValueObject>> results = new HashMap<>();

        for ( Entry<String, Set<String>> el : phenotypesWithChildren.entrySet() ) {
            String queryPhenotype = el.getKey();

            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = this.phenoAssocService
                    .findGenesWithPhenotypes( phenotypesWithChildren.keySet(), taxon, false, null );

            results.put( queryPhenotype, geneEvidenceValueObjects );

        }

        return results;

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByFilters( Long taxonId,
            int limit, String userName ) {
        final Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidenceValueObjects;

        if ( SecurityUtil.isUserLoggedIn() ) {
            final Set<Long> paIds;

            if ( userName == null ) {
                if ( SecurityUtil.isUserAdmin() ) {
                    paIds = this.phenoAssocService.findPrivateEvidenceId( null, null, taxonId, limit );
                } else {
                    paIds = this.phenoAssocService.findPrivateEvidenceId( this.userManager.getCurrentUsername(),
                            this.userManager.findAllGroups(), taxonId, limit );
                }
            } else {
                paIds = this.phenoAssocService
                        .findPrivateEvidenceId( userName, this.userManager.findAllGroups(), taxonId, limit );
            }

            Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                    .findPhenotypeAssociationWithIds( paIds );

            evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );
        } else {
            evidenceValueObjects = new HashSet<>();
        }

        return evidenceValueObjects;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneId( Long geneId ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findPhenotypeAssociationForGeneId( geneId );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneId( Long geneId,
            Set<String> phenotypesValuesUri, EvidenceFilter evidenceFilter ) {

        this.addDefaultExcludedDatabases( evidenceFilter );

        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Collection<? extends PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findPhenotypeAssociationForGeneIdAndDatabases( geneId, externalDatabaseIds );

        if ( evidenceFilter != null && evidenceFilter.isShowOnlyEditable() ) {
            phenotypeAssociations = this.filterPhenotypeAssociationsMyAnnotation( phenotypeAssociations );
        }

        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidenceValueObjects = this
                .convert2ValueObjects( phenotypeAssociations );

        // add all homologue evidence
        evidenceValueObjects.addAll( this.findHomologueEvidence( geneId, evidenceFilter ) );

        this.flagEvidence( evidenceValueObjects, phenotypesValuesUri,
                this.phenoAssocService.loadAllUsedPhenotypeUris() );

        return evidenceValueObjects;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findEvidenceByGeneNCBI( Integer geneNCBI ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findPhenotypeAssociationForGeneNCBI( geneNCBI );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<String> findEvidenceOwners() {
        return this.phenoAssocService.findEvidenceOwners();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> findExperimentCategory() {
        return this.phenoAssocService.findEvidenceCategoryTerms();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> findExperimentOntologyValue( String givenQueryString ) throws SearchException {
        return this.ontologyService.findExperimentsCharacteristicTags( givenQueryString, true );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabaseValueObject> findExternalDatabasesWithEvidence() {

        Collection<ExternalDatabaseValueObject> exDatabases = ExternalDatabaseValueObject
                .fromEntity( this.phenoAssocService.findExternalDatabasesWithEvidence() );

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
        List<ExternalDatabaseValueObject> exDatabasesAsList = new ArrayList<>( exDatabases );
        // add manual curation type
        ExternalDatabaseValueObject manualEvidence = new ExternalDatabaseValueObject( 1L,
                PhenotypeAssociationConstants.MANUAL_CURATION, false );
        exDatabasesAsList.add( manualEvidence );

        return exDatabasesAsList;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<GeneValueObject, OntologyTerm> findGenesForPhenotype( String phenotype, Long taxonId,
            boolean includeIEA ) {

        OntologyTerm ontologyTermFound = this.ontologyHelper.findOntologyTermByUri( phenotype );
        if ( ontologyTermFound == null )
            throw new IllegalArgumentException( "No term found for URI: " + phenotype );

        return this.phenoAssocService.findGenesForPhenotype( ontologyTermFound, taxonId, includeIEA );

    }

    @Override
    @Transactional(readOnly = true)
    public List<GeneEvidenceValueObject> findGenesWithEvidence( String query, Long taxonId ) throws SearchException {

        if ( query == null || query.length() == 0 ) {
            throw new IllegalArgumentException( "No search query provided" );
        }

        // make sure it does an inexact search
        query = prepareOntologyQuery( query );

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = this.taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult<Gene>> geneSearchResults = this.searchService.search( settings ).getByResultObjectType( Gene.class );

        Collection<Gene> genes = new HashSet<>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            return Collections.emptyList();
        }

        for ( SearchResult<Gene> sr : geneSearchResults ) {
            Gene g = geneService.load( sr.getResultId() );
            if ( g == null ) {
                log.warn( "No gene matching search result " + sr );
                continue;
            }
            genes.add( g );
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = new HashSet<>();

        for ( Gene g : genes ) {
            GeneEvidenceValueObject geneEvidenceValueObject = new GeneEvidenceValueObject( g,
                    this.convert2ValueObjects( g.getPhenotypeAssociations() ) );
            geneEvidenceValueObjects.add( geneEvidenceValueObject );
        }

        List<GeneEvidenceValueObject> geneValueObjectsFilter = new ArrayList<>();

        for ( GeneEvidenceValueObject gene : geneEvidenceValueObjects ) {
            if ( gene.getEvidence() != null && gene.getEvidence().size() != 0 ) {
                geneValueObjectsFilter.add( gene );
            }
        }

        return geneValueObjectsFilter;
    }

    @Override
    @Transactional(readOnly = true)
    public EvidenceValueObject<? extends PhenotypeAssociation> load( Long id ) {

        assert id != null;

        PhenotypeAssociation phenotypeAssociation = this.phenoAssocService.load( id );

        if ( phenotypeAssociation == null ) {
            return null;
        }

        return this.convert2ValueObjects( phenotypeAssociation );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<PhenotypeValueObject> loadAllNeurocartaPhenotypes() {
        return this.phenoAssocService.loadAllNeurocartaPhenotypes();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SimpleTreeValueObject> loadAllPhenotypesByTree( EvidenceFilter evidenceFilter ) {
        this.addDefaultExcludedDatabases( evidenceFilter );

        Collection<SimpleTreeValueObject> simpleTreeValueObjects = new TreeSet<>();

        Collection<TreeCharacteristicValueObject> phenotypes = this
                .findAllPhenotypesByTree( evidenceFilter, SecurityUtil.isUserAdmin(), false );

        Collection<TreeCharacteristicValueObject> ontologyTrees = this.customTreeFeatures( phenotypes );

        // unpack the tree in a simple structure
        for ( TreeCharacteristicValueObject t : ontologyTrees ) {
            this.convertToFlatTree( simpleTreeValueObjects, t, null /* parent of root */ );
        }

        return simpleTreeValueObjects;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<TreeCharacteristicValueObject> loadAllPhenotypesAsTree( EvidenceFilter evidenceFilter ) {
        Collection<TreeCharacteristicValueObject> phenotypes = this
                .findAllPhenotypesByTree( evidenceFilter, SecurityUtil.isUserAdmin(), false );

        return this.customTreeFeatures( phenotypes );
    }

    @Override
    @Transactional(readOnly = true)
    public Set<DumpsValueObject> helpFindAllDumps() {

        Set<DumpsValueObject> dumpsValueObjects = new HashSet<>();

        Collection<ExternalDatabaseStatisticsValueObject> externalDatabaseStatisticsValueObjects = new TreeSet<>(
                this.phenoAssocService.loadStatisticsOnExternalDatabases(
                        PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL_DATASETS ) );
        for ( ExternalDatabaseStatisticsValueObject currObj : externalDatabaseStatisticsValueObjects ) {
            DumpsValueObject currDumpsObj = new DumpsValueObject();
            if ( currObj.getName() != null && !currObj.getName().equals( "" ) )
                currDumpsObj.setName( currObj.getName() );
            if ( currObj.getPathToDownloadFile() != null && !currObj.getPathToDownloadFile().equals( "" ) )
                currDumpsObj.setUrl( currObj.getPathToDownloadFile() );
            if ( currObj.getLastUpdateDate() != null && !currObj.getLastUpdateDate().toString().equals( "" ) )
                currDumpsObj.setModified( currObj.getLastUpdateDate().toString() );
            dumpsValueObjects.add( currDumpsObj );
        }

        return dumpsValueObjects;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<EvidenceValueObject<? extends PhenotypeAssociation>> loadEvidenceWithExternalDatabaseName(
            String externalDatabaseName, int limit, int start ) {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findEvidencesWithExternalDatabaseName( externalDatabaseName, limit, start );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    @Override
    @Transactional(readOnly = true)
    public DiffExpressionEvidenceValueObject loadEvidenceWithGeneDifferentialExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId ) {

        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.phenoAssocService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, 1 );

        if ( !differentialExpressionEvidence.isEmpty() ) {
            return this.convertDifferentialExpressionEvidence2ValueObject(
                    differentialExpressionEvidence.iterator().next() );
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<EvidenceValueObject<? extends PhenotypeAssociation>> loadEvidenceWithoutExternalDatabaseName() {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findEvidencesWithoutExternalDatabaseName();

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExternalDatabaseStatisticsValueObject> loadNeurocartaStatistics() {

        // find statistics the external databases sources, each file download path depends on its name
        Collection<ExternalDatabaseStatisticsValueObject> externalDatabaseStatisticsValueObjects = new TreeSet<>(
                this.phenoAssocService.loadStatisticsOnExternalDatabases(
                        PhenotypeAssociationConstants.GEMMA_PHENOCARTA_HOST_URL_DATASETS ) );
        // manual curation and give path to download the file
        externalDatabaseStatisticsValueObjects.add( this.phenoAssocService
                .loadStatisticsOnManualCuration( PhenotypeAssociationConstants.MANUAL_CURATION_FILE_LOCATION ) );
        // total
        externalDatabaseStatisticsValueObjects.add( this.phenoAssocService.loadStatisticsOnAllEvidence(
                PhenotypeAssociationConstants.ALL_PHENOCARTA_ANNOTATIONS_FILE_LOCATION ) );

        return externalDatabaseStatisticsValueObjects;
    }

    @Override
    @Transactional
    public ValidateEvidenceValueObject makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId, SortedSet<CharacteristicValueObject> phenotypes,
            Double selectionThreshold ) {

        GeneDifferentialExpressionMetaAnalysis geneDifferentialExpressionMetaAnalysis = requireNonNull( this.geneDiffExMetaAnalysisService
                .load( geneDifferentialExpressionMetaAnalysisId ) );

        // check that no evidence already exists with that metaAnalysis
        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.phenoAssocService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, 1 );

        if ( !differentialExpressionEvidence.isEmpty() ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            return validateEvidenceValueObject;
        }

        for ( GeneDifferentialExpressionMetaAnalysisResult geneDifferentialExpressionMetaAnalysisResult : geneDifferentialExpressionMetaAnalysis
                .getResults() ) {

            if ( geneDifferentialExpressionMetaAnalysisResult.getMetaQvalue() <= selectionThreshold ) {

                DiffExpressionEvidenceValueObject diffExpressionEvidenceValueObject = new DiffExpressionEvidenceValueObject(
                        -1L, geneDifferentialExpressionMetaAnalysis, geneDifferentialExpressionMetaAnalysisResult,
                        phenotypes, "IEP", selectionThreshold );

                // set the score
                ScoreValueObject scoreValueObject = new ScoreValueObject( null,
                        geneDifferentialExpressionMetaAnalysisResult.getMetaPvalue().toString(), "P-value" );

                diffExpressionEvidenceValueObject.setScoreValueObject( scoreValueObject );

                ValidateEvidenceValueObject validateEvidenceValueObject = this
                        .makeEvidence( diffExpressionEvidenceValueObject );

                if ( validateEvidenceValueObject != null ) {
                    // since this method created multiple evidence, if a problem is detected stop the transaction
                    throw new RuntimeException(
                            "makeDifferentialExpressionEvidencesFromDiffExpressionMetaAnalysis() problem detected" );
                }
            }
        }
        return null;
    }

    @Override
    @Transactional
    public ValidateEvidenceValueObject makeEvidence( EvidenceValueObject<? extends PhenotypeAssociation> evidence ) {

        if ( evidence.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot create an Evidence with no Phenotype" );
        }
        if ( evidence.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Cannot create an Evidence not linked to a Gene" );
        }

        StopWatch sw = new StopWatch();
        sw.start();
        PhenotypeAssociationManagerServiceImpl.log
                .debug( "Create PhenotypeAssociation on geneNCBI: " + evidence.getGeneNCBI() + " to " + StringUtils
                        .join( evidence.getPhenotypes(), "," ) );

        if ( this.evidenceAlreadyInDatabase( evidence ) != null ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            PhenotypeAssociationManagerServiceImpl.log
                    .info( "The evidence is already in the database: " + evidence.getGeneNCBI() + " to " + StringUtils
                            .join( evidence.getPhenotypes(), "," ) + ", no change will be made" );
            return validateEvidenceValueObject;
        }

        if ( !StringUtil.containsValidCharacter( evidence.getDescription() ) ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setDescriptionInvalidSymbol( true );
            return validateEvidenceValueObject;
        }

        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .valueObject2Entity( evidence );
        phenotypeAssociation.setLastUpdated( new Date() );
        assert !phenotypeAssociation.getPhenotypes().isEmpty();

        phenotypeAssociation = this.phenoAssocService.create( phenotypeAssociation );

        Gene gene = phenotypeAssociation.getGene();
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

        if ( sw.getTime() > 100 )
            log.warn( "The create method took : " + sw + "  " + evidence.getGeneNCBI() );

        return null;
    }

    @Override
    @Transactional
    public ValidateEvidenceValueObject remove( Long id ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        PhenotypeAssociation evidence = this.phenoAssocService.load( id );

        if ( evidence != null ) {
            this.phenoAssocService.remove( evidence );

        } else {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
        }
        return validateEvidenceValueObject;
    }

    @Override
    @Transactional
    public ValidateEvidenceValueObject removeAllEvidenceFromMetaAnalysis(
            Long geneDifferentialExpressionMetaAnalysisId ) {

        // checking if there is something to remove
        Collection<DifferentialExpressionEvidence> differentialExpressionEvidence = this.phenoAssocService
                .loadEvidenceWithGeneDifferentialExpressionMetaAnalysis( geneDifferentialExpressionMetaAnalysisId, -1 );
        if ( differentialExpressionEvidence.isEmpty() ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
            return validateEvidenceValueObject;
        }

        for ( DifferentialExpressionEvidence diffExpressionEvidence : differentialExpressionEvidence ) {
            this.phenoAssocService.remove( diffExpressionEvidence );
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> searchInDatabaseForPhenotype( String searchQuery, int maxResults ) throws SearchException {
        String newSearchQuery = this.prepareOntologyQuery( searchQuery );

        // search the Ontology with the search query
        Collection<OntologyTerm> ontologyTermsFound = this.ontologyHelper.findValueUriInOntology( newSearchQuery );

        // add children if we did not reach max results
        if ( maxResults > 0 && ontologyTermsFound.size() < maxResults ) {
            // Set of valueUri of all Ontology Terms found + their children
            ontologyTermsFound.addAll( ontologyService.getChildren( ontologyTermsFound, false, true ) );
        }

        return ontologyTermsFound.stream()
                .map( t -> new CharacteristicValueObject( t.getLabel().toLowerCase(), t.getUri() ) )
                .limit( maxResults > 0 ? maxResults : Long.MAX_VALUE )
                .collect( Collectors.toCollection( TreeSet::new ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CharacteristicValueObject> searchOntologyForPhenotypes( String searchQuery, Long geneId ) throws SearchException {
        StopWatch timer = new StopWatch();
        timer.start();
        List<CharacteristicValueObject> orderedPhenotypesFromOntology = new ArrayList<>();

        boolean geneProvided = geneId != null;

        // prepare the searchQuery to correctly query the Ontology
        String newSearchQuery = this.prepareOntologyQuery( searchQuery );

        // search the Ontology with the new search query
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = this.ontologyHelper
                .findPhenotypesInOntology( newSearchQuery );

        // All phenotypes present on the gene (if the gene was given)
        Set<CharacteristicValueObject> phenotypesOnCurrentGene = null;

        if ( geneProvided ) {
            phenotypesOnCurrentGene = this.findUniquePhenotypesForGeneId( geneId );
        }

        // all phenotypes currently in the database
        Set<String> allPhenotypesInDatabase = this.phenoAssocService.loadAllUsedPhenotypeUris();
        if ( allPhenotypesInDatabase.isEmpty() ) {
            return orderedPhenotypesFromOntology;
        }

        // rules to order the Ontology results found
        Set<CharacteristicValueObject> phenotypesWithExactMatch = new TreeSet<>();
        Set<CharacteristicValueObject> phenotypesAlreadyPresentOnGene = new TreeSet<>();
        Set<CharacteristicValueObject> phenotypesStartWithQueryAndInDatabase = new TreeSet<>();
        Set<CharacteristicValueObject> phenotypesStartWithQuery = new TreeSet<>();
        Set<CharacteristicValueObject> phenotypesSubstringAndInDatabase = new TreeSet<>();
        Set<CharacteristicValueObject> phenotypesSubstring = new TreeSet<>();

        /*
         * for each CharacteristicVO found from the Ontology, filter them and add them to a specific list if they
         * satisfied the condition
         */
        if ( allPhenotypesFoundInOntology.isEmpty() ) {
            /*
             * This is just for the case where ontology isn't loaded; so we can still get results. But that means only
             * results are terms that are already used in a phenotype.
             */
            Collection<PhenotypeValueObject> allNeurocartaPhenotypes = this.phenoAssocService
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
                else if ( cha.getValue().toLowerCase().contains( searchQuery.toLowerCase() ) ) {
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
                else if ( cha.getValue().toLowerCase().contains( searchQuery.toLowerCase() ) ) {
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
        if ( orderedPhenotypesFromOntology.size() > PhenotypeAssociationManagerServiceImpl.MAX_PHENOTYPES_FROM_ONTOLOGY ) {
            if ( timer.getTime() > 1000 ) {
                PhenotypeAssociationManagerServiceImpl.log.warn( "Phenotype search: " + timer.getTime() + "ms" );
            }
            return orderedPhenotypesFromOntology
                    .subList( 0, PhenotypeAssociationManagerServiceImpl.MAX_PHENOTYPES_FROM_ONTOLOGY );
        }
        if ( timer.getTime() > 1000 ) {
            PhenotypeAssociationManagerServiceImpl.log.warn( "Phenotype search: " + timer.getTime() + "ms" );
        }
        return orderedPhenotypesFromOntology;
    }

    @Override
    @Transactional
    public ValidateEvidenceValueObject update(
            EvidenceValueObject<? extends PhenotypeAssociation> modifiedEvidenceValueObject ) {

        if ( modifiedEvidenceValueObject.getPhenotypes() == null || modifiedEvidenceValueObject.getPhenotypes()
                .isEmpty() ) {
            throw new IllegalArgumentException( "An evidence must have a phenotype" );
        }

        if ( modifiedEvidenceValueObject instanceof DiffExpressionEvidenceValueObject ) {
            throw new IllegalArgumentException( "DiffExpressionEvidence type cannot be updated" );
        }

        if ( modifiedEvidenceValueObject.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Evidence not linked to a Gene" );
        }

        if ( modifiedEvidenceValueObject.getId() == null ) {
            throw new IllegalArgumentException( "No database id provided" );
        }

        if ( this.evidenceAlreadyInDatabase( modifiedEvidenceValueObject ) != null ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            return validateEvidenceValueObject;
        }

        PhenotypeAssociation phenotypeAssociation = this.phenoAssocService.load( modifiedEvidenceValueObject.getId() );

        if ( phenotypeAssociation == null ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
            return validateEvidenceValueObject;
        }

        // check for the race condition
        if ( phenotypeAssociation.getLastUpdated().getTime() != modifiedEvidenceValueObject.getLastUpdated() ) {
            ValidateEvidenceValueObject validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setLastUpdateDifferent( true );
            return validateEvidenceValueObject;
        }

        EvidenceValueObject<? extends PhenotypeAssociation> evidenceValueObject = this
                .convert2ValueObjects( phenotypeAssociation );

        // evidence type changed
        if ( !evidenceValueObject.getClass().equals( modifiedEvidenceValueObject.getClass() ) ) {
            this.remove( modifiedEvidenceValueObject.getId() );
            return this.makeEvidence( modifiedEvidenceValueObject );
        }

        // modify phenotypes
        this.populateModifiedPhenotypes( modifiedEvidenceValueObject.getPhenotypes(), phenotypeAssociation );

        // modify all other values needed
        this.phenotypeAssoManagerServiceHelper
                .populateModifiedValues( modifiedEvidenceValueObject, phenotypeAssociation );

        this.phenoAssocService.update( phenotypeAssociation );

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public ValidateEvidenceValueObject validateEvidence( EvidenceValueObject<PhenotypeAssociation> evidence ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        EvidenceValueObject<?> evidenceValueObjectInDatabase = this.evidenceAlreadyInDatabase( evidence );

        if ( evidenceValueObjectInDatabase != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            validateEvidenceValueObject.getProblematicEvidenceIds().add( evidenceValueObjectInDatabase.getId() );
            return validateEvidenceValueObject;
        }

        if ( !evidence.getPhenotypeAssPubVO().isEmpty() && evidence.getEvidenceSource() == null ) {

            PhenotypeAssPubValueObject pubVo = evidence.getPhenotypeAssPubVO().iterator().next();
            CitationValueObject citationValueObject = pubVo.getCitationValueObject();
            String pubmedAccession = citationValueObject == null ? null : citationValueObject.getPubmedAccession();

            validateEvidenceValueObject = this.determineSameGeneAndPhenotypeAnnotated( evidence, pubmedAccession );

        }

        return validateEvidenceValueObject;
    }

    @Override
    @Transactional(readOnly = true)
    public void writeAllEvidenceToFile() throws IOException {

        String disclaimer = "# Generated by Gemma\n" + "# " + DateUtil.getTodayDate( true ) + "\n"
                + "# Fields are delimited by tabs\n"
                + "# If you use this file for your research, please cite PhenoCarta (previously known as Neurocarta): Portales-Casamar, E., et al., Neurocarta: aggregating and sharing disease-gene relations for the neurosciences. BMC Genomics. 2013 Feb 26;14(1):129.\n";

        int i = 0;

        // path of the folder where the dump will be created and the data put
        String mainFolderPath = PhenotypeAssociationConstants.PHENOCARTA_HOME_FOLDER_PATH
                + PhenotypeAssociationConstants.PHENOCARTA_EXPORT + "_" + DateUtil.getTodayDate( true )
                + File.separator;

        // folder where AnnotationByDatasets will be kept
        String datasetsFolderPath = mainFolderPath + PhenotypeAssociationConstants.DATASET_FOLDER_NAME + File.separator;

        // folder where ErmineJ Files are kept
        String ermineJFolderPath = mainFolderPath + PhenotypeAssociationConstants.ERMINEJ_FOLDER_NAME + File.separator;

        // folder where ErmineJ Files, without OMIM-sourced data, are kept
        String ermineJWithOmimFolderPath = mainFolderPath + "AnnotationsWithOMIM" + File.separator;

        // creates the folders if they dont exist
        File phenocartaHomeFolder = new File( PhenotypeAssociationConstants.PHENOCARTA_HOME_FOLDER_PATH );
        forceMkdir( phenocartaHomeFolder );
        File mainFolder = new File( mainFolderPath );
        forceMkdir( mainFolder );
        File datasetsFolder = new File( datasetsFolderPath );
        forceMkdir( datasetsFolder );
        File ermineJFolder = new File( ermineJFolderPath );
        forceMkdir( ermineJFolder );
        File ermineJWithOmimFolder = new File( ermineJWithOmimFolderPath );
        forceMkdir( ermineJWithOmimFolder );

        // this writer is the dump of all evidence
        try ( BufferedWriter fileWriterAllEvidence = new BufferedWriter(
                new FileWriter( mainFolderPath + PhenotypeAssociationConstants.FILE_ALL_PHENOCARTA_ANNOTATIONS ) );
                BufferedWriter fileWriterAllEvidenceWithOMIM = new BufferedWriter( new FileWriter(
                        mainFolderPath + "AnnotationsWithOMIM" + File.separator
                                + PhenotypeAssociationConstants.FILE_ALL_PHENOCARTA_ANNOTATIONS ) ) ) {

            // header of file
            String header = disclaimer
                    + "Data Source\tGene NCBI\tGene Symbol\tTaxon\tPhenotype Names\tRelationship\tPhenotype URIs\tPubmeds\tWeb Link\tIs Negative\tNote\n";
            fileWriterAllEvidence.write( header );

            // lets get all external databases linked to evidence, we will create a file for each
            Collection<ExternalDatabaseValueObject> externalDatabaseValueObjects = this
                    .findExternalDatabasesWithEvidence();

            for ( ExternalDatabaseValueObject externalDatabaseValueObject : externalDatabaseValueObjects ) {

                File thisFile = new File(
                        datasetsFolderPath + externalDatabaseValueObject.getName().replaceAll( " ", "" ) + ".tsv" );

                boolean currDBFoundinExtDBs = false;
                Iterator<ExternalDatabaseStatisticsValueObject> iter = this.loadNeurocartaStatistics().iterator();
                ExternalDatabaseStatisticsValueObject dbFromColln = null;
                while ( !currDBFoundinExtDBs && iter.hasNext() ) {
                    dbFromColln = iter.next();
                    if ( dbFromColln.getName().equals( externalDatabaseValueObject.getName() ) )
                        currDBFoundinExtDBs = true;
                }

                if ( dbFromColln != null && dbFromColln.getLastUpdateDate() != null
                        && dbFromColln.getLastUpdateDate().getTime() > thisFile.lastModified() ) {
                    // this writer will be used to write 1 file per resource
                    try ( BufferedWriter fileWriterDataSource = new BufferedWriter( new FileWriter(
                            datasetsFolderPath + externalDatabaseValueObject.getName().replaceAll( " ", "" )
                                    + ".tsv" ) ) ) {

                        // header of file
                        fileWriterDataSource.write( header );

                        // not using value object to make it faster
                        Collection<PhenotypeAssociation> phenotypeAssociations;

                        // this one is a special case, not actually linked to an external database
                        if ( externalDatabaseValueObject.getName()
                                .equalsIgnoreCase( PhenotypeAssociationConstants.MANUAL_CURATION ) ) {
                            phenotypeAssociations = this.phenoAssocService.findEvidencesWithoutExternalDatabaseName();
                        } else {
                            phenotypeAssociations = this.phenoAssocService
                                    .findEvidencesWithExternalDatabaseName( externalDatabaseValueObject.getName(), PhenotypeAssociationDaoImpl.DEFAULT_PA_LIMIT,
                                            0 );
                        }

                        for ( PhenotypeAssociation phenotypeAssociation : phenotypeAssociations ) {

                            if ( i++ % 5000 == 0 ) {
                                PhenotypeAssociationManagerServiceImpl.log
                                        .debug( "Phenocarta dump of evidence at evidence number: " + i );
                            }

                            StringBuilder pubmeds = new StringBuilder();

                            for ( PhenotypeAssociationPublication phenotypeAssociationPublication : phenotypeAssociation
                                    .getPhenotypeAssociationPublications() ) {
                                String pubId = phenotypeAssociationPublication.getCitation().getPubAccession().getAccession()
                                        + ";";
                                // primary should be order first
                                if ( phenotypeAssociationPublication.getType()
                                        .equals( PhenotypeAssPubValueObject.PRIMARY ) ) {
                                    pubmeds.insert( 0, pubId );
                                } else {
                                    pubmeds.append( pubId );
                                }
                            }

                            String relationship;
                            relationship = phenotypeAssociation.getRelationship();

                            StringBuilder phenotypes = new StringBuilder();

                            for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
                                phenotypes.append( cha.getValue() ).append( ";" );
                            }

                            StringBuilder phenotypesUri = new StringBuilder();

                            for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {
                                if ( StringUtils.isNotBlank( cha.getValueUri() ) ) {
                                    phenotypesUri.append( cha.getValueUri() ).append( ";" );
                                }
                            }

                            // this should never happen
                            if ( ( phenotypes.length() == 0 ) || ( phenotypesUri.length() == 0 ) ) {
                                PhenotypeAssociationManagerServiceImpl.log
                                        .error( "Found an evidence without phenotypes : " + phenotypeAssociation.getId() );
                            }

                            String webLink = "";

                            if ( phenotypeAssociation.getEvidenceSource() != null
                                    && phenotypeAssociation.getEvidenceSource().getExternalDatabase() != null ) {
                                webLink = phenotypeAssociation.getEvidenceSource().getExternalDatabase().getWebUri()
                                        + phenotypeAssociation.getEvidenceSource().getAccession();
                            }

                            String isNegative;

                            if ( phenotypeAssociation.getIsNegativeEvidence() ) {
                                isNegative = "Yes";
                            } else {
                                isNegative = "No";
                            }

                            String description = phenotypeAssociation.getDescription();

                            // represents 1 evidence
                            String evidenceLine = externalDatabaseValueObject.getName() + "\t" + phenotypeAssociation.getGene()
                                    .getNcbiGeneId() + "\t" + phenotypeAssociation.getGene().getOfficialSymbol()
                                    + "\t" + phenotypeAssociation.getGene().getTaxon().getCommonName() + "\t"
                                    + StringUtils.removeEnd( phenotypes.toString(), ";" ) + "\t" + relationship
                                    + "\t"
                                    // relationship
                                    // information
                                    + StringUtils.removeEnd( phenotypesUri.toString(), ";" ) + "\t" + StringUtils
                                    .removeEnd( pubmeds.toString(), ";" )
                                    + "\t" + webLink + "\t" + isNegative
                                    + "\t" + description + "\n";

                            fileWriterDataSource.write( evidenceLine );
                            if ( !externalDatabaseValueObject.getName().contains( "OMIM" ) )
                                fileWriterAllEvidence.write( evidenceLine );
                            fileWriterAllEvidenceWithOMIM.write( evidenceLine );
                        }
                    }
                } // old: finish loop of writing all ext data src files
            } // new: finish loop of writing all ext data src files, including checking modified times
        }

        // LatestEvidenceExport ---> points to the latest dump
        File symbolicLink = new File( PhenotypeAssociationConstants.PHENOCARTA_HOME_FOLDER_PATH
                + PhenotypeAssociationConstants.LATEST_EVIDENCE_EXPORT );

        if ( symbolicLink.exists() ) {
            Files.delete( symbolicLink.toPath() );
        }
        Files.createSymbolicLink( symbolicLink.toPath(), mainFolder.toPath() );

        PhenotypeAssociationManagerServiceImpl.log
                .debug( "After symlink code; symlink now exists: " + symbolicLink.exists() );
        PhenotypeAssociationManagerServiceImpl.log
                .debug( "Right before ErmineJ; latest dir exists: " + mainFolder.exists() + " and is: " + mainFolder
                        .toPath() );

        Taxon mouseTaxon = this.taxonService.findByCommonName( "mouse" );
        Taxon humanTaxon = this.taxonService.findByCommonName( "human" );
        Taxon[] taxa = new Taxon[] { mouseTaxon, humanTaxon };

        for ( Taxon taxon : taxa ) {
            this.writeErmineJFile( ermineJFolderPath, disclaimer, taxon,
                    false );
            this.writeErmineJFile( ermineJFolderPath, disclaimer, taxon, true );
            this.writeErmineJFile( ermineJWithOmimFolderPath, disclaimer, taxon,
                    false );
            this.writeErmineJFile( ermineJWithOmimFolderPath, disclaimer, taxon,
                    true );
        }
    }

    private void addDefaultExcludedDatabases( EvidenceFilter evidenceFilter ) {
        if ( evidenceFilter != null ) {
            if ( evidenceFilter.getExternalDatabaseIds() == null
                    && Settings.getString( "gemma.neurocarta.exluded_database_id" ) != null ) {
                Collection<Long> externalDatabaseIds = new HashSet<>();
                for ( String token : Settings.getString( "gemma.neurocarta.exluded_database_id" ).split( "," ) ) {
                    externalDatabaseIds.add( new Long( token ) );
                }
                evidenceFilter.setExternalDatabaseIds( externalDatabaseIds );
            }
        }
    }

    /**
     * Convert an collection of evidence entities to their corresponding value objects
     *
     * @param  phenotypeAssociations The List of entities we need to convert to value object
     * @return Collection<EvidenceValueObject> the converted results
     */
    private Set<EvidenceValueObject<? extends PhenotypeAssociation>> convert2ValueObjects(
            Collection<? extends PhenotypeAssociation> phenotypeAssociations ) {

        Set<EvidenceValueObject<? extends PhenotypeAssociation>> returnEvidenceVO = new HashSet<>();

        if ( phenotypeAssociations != null ) {

            for ( PhenotypeAssociation phe : phenotypeAssociations ) {

                EvidenceValueObject<? extends PhenotypeAssociation> evidence = this.convert2ValueObjects( phe );

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
     * @param  phe The phenotype Entity
     * @return Collection<EvidenceValueObject> its corresponding value object
     */
    private EvidenceValueObject<? extends PhenotypeAssociation> convert2ValueObjects( PhenotypeAssociation phe ) {

        EvidenceValueObject<? extends PhenotypeAssociation> evidence;

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
            evidence = this.convertDifferentialExpressionEvidence2ValueObject( ( DifferentialExpressionEvidence ) p );
        } else {
            throw new UnsupportedOperationException( "Don't know how to convert a " + userClass.getSimpleName() );
        }

        this.findEvidencePermissions( phe, evidence );

        return evidence;
    }

    private DiffExpressionEvidenceValueObject convertDifferentialExpressionEvidence2ValueObject(
            DifferentialExpressionEvidence differentialExpressionEvidence ) {

        DiffExpressionEvidenceValueObject diffExpressionEvidenceValueObject = null;
        if ( differentialExpressionEvidence != null ) {

            GeneDifferentialExpressionMetaAnalysis geneDifferentialExpressionMetaAnalysis = this.geneDiffExMetaAnalysisService
                    .loadWithResultId(
                            differentialExpressionEvidence.getGeneDifferentialExpressionMetaAnalysisResult().getId() );

            Collection<Long> ids = new HashSet<>();
            ids.add( geneDifferentialExpressionMetaAnalysis.getId() );

            GeneDifferentialExpressionMetaAnalysisSummaryValueObject geneDiffExMetaAnalysisSummaryValueObject = this.geneDiffExMetaAnalysisService
                    .findMetaAnalyses( ids ).iterator().next();

            diffExpressionEvidenceValueObject = new DiffExpressionEvidenceValueObject( differentialExpressionEvidence,
                    geneDiffExMetaAnalysisSummaryValueObject );

            // set the count, how many evidences where created from the specific meta analysis
            diffExpressionEvidenceValueObject.setNumEvidenceFromSameMetaAnalysis( this.phenoAssocService
                    .countEvidenceWithGeneDifferentialExpressionMetaAnalysis(
                            geneDifferentialExpressionMetaAnalysis.getId() ) );
        }

        return diffExpressionEvidenceValueObject;
    }

    /**
     * Recursively take the trees made and put them in the exact way the client wants them
     *
     * @param parent                        parent
     * @param simpleTreeValueObjects        simple tree value object
     * @param treeCharacteristicValueObject tree characteristic value object
     */
    private void convertToFlatTree( Collection<SimpleTreeValueObject> simpleTreeValueObjects,
            TreeCharacteristicValueObject treeCharacteristicValueObject, String parent ) {

        if ( treeCharacteristicValueObject == null ) {
            return;
        }

        SimpleTreeValueObject simpleTreeValueObject = new SimpleTreeValueObject( treeCharacteristicValueObject,
                parent );

        if ( treeCharacteristicValueObject.getChildren().isEmpty() ) {
            simpleTreeValueObject.set_is_leaf( true );
        }

        simpleTreeValueObjects.add( simpleTreeValueObject );

        for ( TreeCharacteristicValueObject tree : treeCharacteristicValueObject.getChildren() ) {
            this.convertToFlatTree( simpleTreeValueObjects, tree, simpleTreeValueObject.get_id() );
        }

    }

    /**
     * @param  ontologyTrees ontology trees
     * @return Changing the root names and the order to present them
     */
    private Collection<TreeCharacteristicValueObject> customTreeFeatures(
            Collection<TreeCharacteristicValueObject> ontologyTrees ) {

        TreeCharacteristicValueObject[] customOntologyTrees = new TreeCharacteristicValueObject[3];

        for ( TreeCharacteristicValueObject tree : ontologyTrees ) {
            if ( tree.getValueUri().contains( "MONDO" ) || tree.getValueUri().contains( "DOID" ) ) {
                tree.setValue( "Disease Ontology" );
                customOntologyTrees[0] = tree;
            } else if ( tree.getValueUri().contains( "HP" ) ) {
                tree.setValue( "Human Phenotype Ontology" );
                customOntologyTrees[1] = tree;
            } else if ( tree.getValueUri().contains( "MP" ) ) {
                tree.setValue( "Mammalian Phenotype Ontology" );
                customOntologyTrees[2] = tree;
            }
        }
        return Arrays.asList( customOntologyTrees );
    }

    /**
     * @param  evidence evidence
     * @param  pubmed   pub med
     * @return Populates the ValidateEvidenceValueObject with the correct flags if necessary
     */
    private ValidateEvidenceValueObject determineSameGeneAndPhenotypeAnnotated(
            EvidenceValueObject<PhenotypeAssociation> evidence, String pubmed ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        BibliographicReferenceValueObject bibliographicReferenceValueObject = this.findBibliographicReference( pubmed );

        if ( bibliographicReferenceValueObject == null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setPubmedIdInvalid( true );
        } else {

            // rule to determine if its an update
            if ( evidence.getId() != null ) {

                PhenotypeAssociation phenotypeAssociation = this.phenoAssocService.load( evidence.getId() );

                if ( phenotypeAssociation == null ) {
                    validateEvidenceValueObject = new ValidateEvidenceValueObject();
                    validateEvidenceValueObject.setEvidenceNotFound( true );
                    return validateEvidenceValueObject;
                }

                // check for the race condition
                if ( phenotypeAssociation.getLastUpdated().getTime() != evidence.getLastUpdated() ) {
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
                    validateEvidenceValueObject.getProblematicEvidenceIds()
                            .add( bibliographicPhenotypesValueObject.getEvidenceId() );

                    boolean containsExact = true;

                    for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                        if ( !bibliographicPhenotypesValueObject.getPhenotypesValues().contains( phenotype ) ) {
                            containsExact = false;
                            break;
                        }
                    }

                    if ( containsExact ) {
                        validateEvidenceValueObject.setSameGeneAndOnePhenotypeAnnotated( true );
                        validateEvidenceValueObject.getProblematicEvidenceIds()
                                .add( bibliographicPhenotypesValueObject.getEvidenceId() );
                    }

                    if ( evidence.getPhenotypes().size() == bibliographicPhenotypesValueObject.getPhenotypesValues()
                            .size() && evidence.getPhenotypes()
                            .containsAll( bibliographicPhenotypesValueObject.getPhenotypesValues() ) ) {
                        validateEvidenceValueObject.setSameGeneAndPhenotypesAnnotated( true );
                        validateEvidenceValueObject.getProblematicEvidenceIds()
                                .add( bibliographicPhenotypesValueObject.getEvidenceId() );
                    }

                    // for the phenotype already present we add his children and direct parents, and check that
                    // the phenotype we want to add is not in that subset
                    Set<OntologyTerm> terms = new HashSet<>();
                    for ( CharacteristicValueObject phenotypeAlreadyPresent : bibliographicPhenotypesValueObject
                            .getPhenotypesValues() ) {

                        OntologyTerm ontologyTerm = this.ontologyService
                                .getTerm( phenotypeAlreadyPresent.getValueUri() );

                        if ( ontologyTerm == null ) {
                            log.warn( String.format( "Unexpected null term for value URI %s from the ontology service.", phenotypeAlreadyPresent.getValueUri() ) );
                            continue;
                        }

                        terms.add( ontologyTerm );
                    }

                    Set<String> parentOrChildTerm = this.ontologyService.getParents( terms, false, true )
                            .stream()
                            .map( OntologyTerm::getUri )
                            .collect( Collectors.toSet() );

                    for ( CharacteristicValueObject characteristicValueObject : evidence.getPhenotypes() ) {

                        if ( parentOrChildTerm.contains( characteristicValueObject.getValueUri() ) ) {
                            validateEvidenceValueObject.setSameGeneAndPhenotypeChildOrParentAnnotated( true );
                            validateEvidenceValueObject.getProblematicEvidenceIds()
                                    .add( bibliographicPhenotypesValueObject.getEvidenceId() );
                        }
                    }
                }
            }
        }
        return validateEvidenceValueObject;
    }

    /**
     * @param  evidence the evidence
     * @return Checks to see if the evidence is already in the database
     */
    private EvidenceValueObject<?> evidenceAlreadyInDatabase(
            EvidenceValueObject<? extends PhenotypeAssociation> evidence ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.phenoAssocService
                .findPhenotypeAssociationForGeneNCBI( evidence.getGeneNCBI(), evidence.getPhenotypesValueUri() );

        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidenceValueObjects = this
                .convert2ValueObjects( phenotypeAssociations );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject<?> evidenceFound : evidenceValueObjects ) {
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

    private Set<GeneEvidenceValueObject> filterGenesWithPhenotypes(
            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects,
            Map<String, Set<String>> phenotypesWithChildren ) {

        Set<GeneEvidenceValueObject> genesVO = new HashSet<>();

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
                        break;
                    }
                }

                if ( !foundSpecificPheno ) {
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

    private Collection<PhenotypeAssociation> filterPhenotypeAssociationsMyAnnotation(
            Collection<? extends PhenotypeAssociation> phenotypeAssociations ) {

        Collection<PhenotypeAssociation> phenotypeAssociationsFiltered = new HashSet<>();

        for ( PhenotypeAssociation p : phenotypeAssociations ) {

            boolean currentUserHasWritePermission = false;
            boolean isShared = this.securityService.isShared( p );
            boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

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
     * @param  isAdmin,                can see everything
     * @param  noElectronicAnnotation, if true don't include evidence code IEA
     * @param  evidenceFilter          evidence filter
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma
     *                                 represented as
     *                                 trees
     */
    private Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree( EvidenceFilter evidenceFilter,
            boolean isAdmin, boolean noElectronicAnnotation ) {
        checkIfAllOntologiesAreLoaded();

        StopWatch sw = new StopWatch();
        sw.start();

        Taxon taxon = null;
        boolean showOnlyEditable = false;
        Collection<Long> externalDatabaseIds = null;

        if ( evidenceFilter != null ) {
            taxon = this.checkAndGetTaxon( evidenceFilter );
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
            externalDatabaseIds = evidenceFilter.getExternalDatabaseIds();
        }

        Map<String, Set<Integer>> publicPhenotypesGenesAssociations = new HashMap<>();
        Map<String, Set<Integer>> privatePhenotypesGenesAssociations = new HashMap<>();
        Set<String> allPhenotypesGenesAssociations = new HashSet<>();

        boolean isUserLoggedIn = SecurityUtil.isUserLoggedIn();
        String userName;
        Collection<String> groups;

        if ( isUserLoggedIn ) {
            userName = this.userManager.getCurrentUsername();
            // groups the user belong to
            groups = this.userManager.findGroupsForUser( userName );

            // show only my annotation was chosen
            if ( showOnlyEditable ) {
                // show public owned by the user
                publicPhenotypesGenesAssociations = this.phenoAssocService
                        .findPublicPhenotypesGenesAssociations( taxon, null, userName, groups, true,
                                externalDatabaseIds, noElectronicAnnotation );

                // show all private owned by the user or shared by a group
                privatePhenotypesGenesAssociations = this.phenoAssocService
                        .findPrivatePhenotypesGenesAssociations( taxon, null, userName, groups, true,
                                externalDatabaseIds, noElectronicAnnotation );
            } else {
                // logged in, but not filtered. all public evidences
                publicPhenotypesGenesAssociations = this.phenoAssocService
                        .findPublicPhenotypesGenesAssociations( taxon, null, null, groups, false, externalDatabaseIds,
                                noElectronicAnnotation );

                if ( isAdmin ) {
                    // show all private since admin
                    privatePhenotypesGenesAssociations = this.phenoAssocService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, null, null, false,
                                    externalDatabaseIds, noElectronicAnnotation );
                } else {
                    // show all private owned by the user or shared by a group
                    privatePhenotypesGenesAssociations = this.phenoAssocService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, userName, groups, false,
                                    externalDatabaseIds, noElectronicAnnotation );
                }
            }
        } else if ( !showOnlyEditable ) {
            // anonymous user
            publicPhenotypesGenesAssociations = this.phenoAssocService
                    .findPublicPhenotypesGenesAssociations( taxon, null, null, null, false, externalDatabaseIds,
                            noElectronicAnnotation );
        }

        allPhenotypesGenesAssociations.addAll( privatePhenotypesGenesAssociations.keySet() );
        allPhenotypesGenesAssociations.addAll( publicPhenotypesGenesAssociations.keySet() );

        // map to help the placement of elements in the tree, used to find quickly the position to add subtrees
        Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<>();

        // represents each phenotype and children found in the Ontology, TreeSet used to order trees [why do we need a
        // TreeSet? Can't we just sort at the end?]
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<>();

        Set<OntologyTerm> obsoleteTerms = new HashSet<>();

        // creates the tree structure
        for ( String valueUri : allPhenotypesGenesAssociations ) {

            // don't create the tree if it is already present in an other [another what?]
            if ( phenotypeFoundInTree.containsKey( valueUri ) ) {
                // flag the node as phenotype found in database [when does this happen? It seems useless, since we don't
                // use this]
                phenotypeFoundInTree.get( valueUri ).setDbPhenotype( true );
            } else {
                // find the ontology term using the valueURI
                OntologyTerm ontologyTerm = this.ontologyHelper.findOntologyTermByUri( valueUri );

                if ( ontologyTerm == null ) {
                    if ( PhenotypeAssociationManagerServiceImpl.log.isDebugEnabled() )
                        PhenotypeAssociationManagerServiceImpl.log.debug( String.format(
                                "A valueUri in the database was not found in the ontology; DB out of date?; valueUri: %s", valueUri ) );
                    continue;
                }

                // we don't show obsolete terms
                if ( ontologyTerm.isObsolete() ) {
                    obsoleteTerms.add( ontologyTerm );
                } else {

                    // transform an OntologyTerm and his children to a TreeCharacteristicValueObject
                    TreeCharacteristicValueObject treeCharacteristicValueObject = ontology2TreeCharacteristicValueObjects( ontologyTerm, phenotypeFoundInTree );

                    // set flag that this node represents a phenotype used in the database
                    treeCharacteristicValueObject.setDbPhenotype( true );

                    // add tree to the phenotypes found in ontology
                    phenotypeFoundInTree.put( ontologyTerm.getUri(), treeCharacteristicValueObject );

                    if ( !treesPhenotypes.add( treeCharacteristicValueObject ) ) {
                        throw new IllegalStateException( "Add failed for " + ontologyTerm );
                    }
                    if ( PhenotypeAssociationManagerServiceImpl.log.isDebugEnabled() )
                        PhenotypeAssociationManagerServiceImpl.log.debug( "Added: " + ontologyTerm );

                }

            }
        }

        if ( !obsoleteTerms.isEmpty() ) {
            PhenotypeAssociationManagerServiceImpl.log
                    .warn( String.format( "The following terms found in the database are obsolete: %s. They will not be shown in the tree.",
                            obsoleteTerms.stream().map( Object::toString ).collect( Collectors.joining( ", " ) ) ) );
        }

        TreeSet<TreeCharacteristicValueObject> finalTreesWithRoots = new TreeSet<>();

        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            this.findParentRoot( tc, finalTreesWithRoots, phenotypeFoundInTree );
        }

        // set the public count
        for ( TreeCharacteristicValueObject tc : finalTreesWithRoots ) {
            countPublicGeneForEachNode( tc, publicPhenotypesGenesAssociations, new HashSet<>() );
            countPrivateGeneForEachNode( tc, privatePhenotypesGenesAssociations, new HashSet<>() );
            removeUnusedPhenotypes( tc, new HashSet<>() );
        }

        if ( sw.getTime() > 1000 ) {
            PhenotypeAssociationManagerServiceImpl.log.warn( "Done total time=" + sw.getTime() + "ms" );
        }

        return finalTreesWithRoots;
    }

    private TreeCharacteristicValueObject ontology2TreeCharacteristicValueObjects( OntologyTerm ontologyTerm,
            Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree ) {

        Collection<OntologyTerm> directChildTerms = ontologyTerm.getChildren( true, false );

        TreeSet<TreeCharacteristicValueObject> children = new TreeSet<>();

        for ( OntologyTerm ot : directChildTerms ) {
            if ( phenotypeFoundInTree.containsKey( ot.getUri() ) ) {
                TreeCharacteristicValueObject child = phenotypeFoundInTree.get( ot.getUri() );
                children.add( child );

                // See bug 4102. Removing wreaks havoc and I cannot see why it would be necessary.
                // treesPhenotypes.remove( child );
            } else {
                TreeCharacteristicValueObject tree = ontology2TreeCharacteristicValueObjects( ot,
                        phenotypeFoundInTree );
                phenotypeFoundInTree.put( tree.getValueUri(), tree );
                children.add( tree );
            }
        }

        return new TreeCharacteristicValueObject( ontologyTerm.getLabel(), ontologyTerm.getUri(), children );
    }

    private void countPrivateGeneForEachNode( TreeCharacteristicValueObject tc, Map<String, Set<Integer>> phenotypesGenesAssociations, Set<String> visited ) {
        visited.add( tc.get_id() );

        Set<Integer> allGenes = new HashSet<>();

        for ( TreeCharacteristicValueObject child : tc.getChildren() ) {
            if ( visited.contains( child.get_id() ) ) {
                continue;
            }

            countPrivateGeneForEachNode( child, phenotypesGenesAssociations, visited );

            if ( phenotypesGenesAssociations.get( child.getValueUri() ) != null ) {
                allGenes.addAll( phenotypesGenesAssociations.get( child.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( tc.getValueUri() )
                            .addAll( phenotypesGenesAssociations.get( child.getValueUri() ) );
                } else {
                    HashSet<Integer> genesNBCI = new HashSet<>( phenotypesGenesAssociations.get( child.getValueUri() ) );
                    phenotypesGenesAssociations.put( tc.getValueUri(), genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
            allGenes.addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
        }
        tc.setPrivateGeneCount( allGenes.size() );
    }

    private void countPublicGeneForEachNode( TreeCharacteristicValueObject tc, Map<String, Set<Integer>> phenotypesGenesAssociations, Set<String> visited ) {
        visited.add( tc.get_id() );

        for ( TreeCharacteristicValueObject child : tc.getChildren() ) {
            if ( visited.contains( child.get_id() ) ) {
                continue;
            }

            countPublicGeneForEachNode( child, phenotypesGenesAssociations, visited );

            if ( phenotypesGenesAssociations.get( child.getValueUri() ) != null ) {
                tc.getPublicGenesNBCI().addAll( phenotypesGenesAssociations.get( child.getValueUri() ) );

                if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
                    phenotypesGenesAssociations.get( tc.getValueUri() )
                            .addAll( phenotypesGenesAssociations.get( child.getValueUri() ) );
                } else {
                    Set<Integer> genesNBCI = new HashSet<>( phenotypesGenesAssociations.get( child.getValueUri() ) );
                    phenotypesGenesAssociations.put( tc.getValueUri(), genesNBCI );
                }
            }
        }

        if ( phenotypesGenesAssociations.get( tc.getValueUri() ) != null ) {
            tc.getPublicGenesNBCI().addAll( phenotypesGenesAssociations.get( tc.getValueUri() ) );
        }

        tc.setPublicGeneCount( tc.getPublicGenesNBCI().size() );
    }


    private void removeUnusedPhenotypes( TreeCharacteristicValueObject tc, Set<String> visited ) {
        visited.add( tc.get_id() );

        Iterator<TreeCharacteristicValueObject> iterator = tc.getChildren().iterator();
        while ( iterator.hasNext() ) {
            TreeCharacteristicValueObject child = iterator.next();
            long count = child.getPrivateGeneCount() + child.getPublicGeneCount();
            if ( count == 0 ) {
                iterator.remove();
            }
        }

        for ( TreeCharacteristicValueObject child : tc.getChildren() ) {
            if ( visited.contains( child.get_id() ) ) {
                continue;
            }
            removeUnusedPhenotypes( child, visited );
        }
    }

    private Taxon checkAndGetTaxon( EvidenceFilter evidenceFilter ) {
        Taxon taxon = null;
        if ( evidenceFilter.getTaxonId() != null && evidenceFilter.getTaxonId() > 0 ) {
            taxon = this.taxonService.load( evidenceFilter.getTaxonId() );
        }
        return taxon;
    }

    /**
     * @param  phenotypesWithChildren phenotypes
     * @return add all the keySet together and return a set representing all children for all
     *                                valueUri given (collapse the map
     *                                down to a single set)
     */
    private Set<String> findAllPossibleChildren( Map<String, Set<String>> phenotypesWithChildren ) {

        Set<String> possibleChildrenPhenotypes = new HashSet<>();

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
     * @param  usedPhenotypes       the URIs of all phenotypes actually used in the database.
     * @param  phenotypesValuesUris URIs
     * @return map of terms to their children. The term itself is included.
     */
    private Map<String, Set<String>> findChildrenForEachPhenotype( Collection<String> phenotypesValuesUris,
            Collection<String> usedPhenotypes ) {
        checkIfAllOntologiesAreLoaded();

        // root corresponds to one value found in phenotypesValuesUri
        // root ---> root+children phenotypes
        Map<String, Set<String>> parentPheno = new HashMap<>();

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUris ) {

            if ( phenoRoot.isEmpty() ) {
                continue;
            }
            OntologyTerm ontologyTermFound = this.ontologyHelper.findOntologyTermByUri( phenoRoot );
            if ( ontologyTermFound == null )
                continue;
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
     * Check if all the necessary ontologies are loaded or else raise an exception.
     */
    private void checkIfAllOntologiesAreLoaded() throws RuntimeException {
        // if these ontologies are not configured, we will never be ready. Check for valid configuration.
        for ( ubic.basecode.ontology.providers.OntologyService ontology : ontologyHelper.getOntologyServices() ) {
            if ( !ontology.isOntologyLoaded() ) {
                throw new RuntimeException( PhenotypeAssociationManagerServiceImpl.ERROR_MSG_ONTOLOGIES_NOT_LOADED );
            }
        }
    }

    private void findEvidencePermissions( PhenotypeAssociation p, EvidenceValueObject<?> evidenceValueObject ) {

        boolean currentUserHasWritePermission = false;
        String owner = null;
        boolean isPublic = this.securityService.isPublic( p );
        boolean isShared = this.securityService.isShared( p );
        boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

        if ( currentUserIsOwner || isPublic || isShared || SecurityUtil.isUserAdmin() ) {

            /*
             * FIXME WARNING it is not guaranteed that owner is a PrincipalSid.
             */
            currentUserHasWritePermission = this.securityService.isEditable( p );
            owner = ( ( AclPrincipalSid ) this.securityService.getOwner( p ) ).getPrincipal();
        }

        evidenceValueObject.setEvidenceSecurityValueObject(
                new EvidenceSecurityValueObject( currentUserHasWritePermission, currentUserIsOwner, isPublic, isShared,
                        owner ) );
    }

    /**
     * @param  evidenceFilter evidence filter
     * @param  geneId         Gemma's identifier
     * @return find Homologue-linked Evidence for a gene
     */
    private Collection<EvidenceValueObject<? extends PhenotypeAssociation>> findHomologueEvidence( Long geneId,
            EvidenceFilter evidenceFilter ) {

        Collection<Long> externalDatabaseIDs = null;

        if ( evidenceFilter != null ) {
            externalDatabaseIDs = evidenceFilter.getExternalDatabaseIds();
        }

        // Get the Gene object for finding homologues' evidence.
        Gene gene = this.geneService.load( geneId );

        Collection<Gene> homologues = AsyncFactoryBeanUtils.getSilently( homologeneService, HomologeneServiceFactory.class ).getHomologues( gene );

        Collection<PhenotypeAssociation> homologuePhenotypeAssociations = new HashSet<>();

        for ( Gene homologue : homologues ) {
            Collection<PhenotypeAssociation> currHomologuePhenotypeAssociations = this.phenoAssocService
                    .findPhenotypeAssociationForGeneIdAndDatabases( homologue.getId(), externalDatabaseIDs );
            homologuePhenotypeAssociations.addAll( currHomologuePhenotypeAssociations );
        }

        if ( evidenceFilter != null && evidenceFilter.isShowOnlyEditable() ) {
            homologuePhenotypeAssociations = this
                    .filterPhenotypeAssociationsMyAnnotation( homologuePhenotypeAssociations );
        }

        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> homologueEvidenceValueObjects = this
                .convert2ValueObjects( homologuePhenotypeAssociations );

        for ( EvidenceValueObject<?> evidenceValueObject : homologueEvidenceValueObjects ) {
            evidenceValueObject.setHomologueEvidence( true );
        }

        return homologueEvidenceValueObjects;
    }

    /**
     * Recursively build the full trees of the Ontology with the given branches
     *
     * @param finalTreesWithRoots  final trees with roots
     * @param phenotypeFoundInTree phenotypes found in tree
     * @param tc                   tree characteristic value object
     */
    private void findParentRoot( TreeCharacteristicValueObject tc,
            TreeSet<TreeCharacteristicValueObject> finalTreesWithRoots,
            Map<String, TreeCharacteristicValueObject> phenotypeFoundInTree ) {

        if ( tc == null || tc.getValueUri() == null ) {
            // ???? there was a null check at the end of this method, which doesn't do any good since we access TC in
            // the next line here.
            return;
        }

        OntologyTerm ontologyTerm = this.ontologyHelper.findOntologyTermByUri( tc.getValueUri() );

        if ( ontologyTerm == null ) {
            return;
        }

        // MONDO has BFO as parent, but it's not loaded
        Collection<OntologyTerm> ontologyParents;
        if ( ArrayUtils.contains( ROOTS, ontologyTerm.getUri() ) || ontologyTerm.isRoot() ) {
            ontologyParents = Collections.emptySet();
        } else {
            ontologyParents = ontologyTerm.getParents( true );
        }

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

                    // put node in the hashmap for fast access
                    phenotypeFoundInTree.put( tree.getValueUri(), tree );

                    this.findParentRoot( tree, finalTreesWithRoots, phenotypeFoundInTree );
                }
            }
        } else {
            // found a root, no more parents
            if ( tc.getValue() != null && !tc.getValue().equals( "" ) ) {
                finalTreesWithRoots.add( tc );
            }
        }
    }

    /**
     * @param  geneId gene id
     * @return Given a geneId finds all phenotypes for that gene
     */
    private Set<CharacteristicValueObject> findUniquePhenotypesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<>();

        Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidence = this.findEvidenceByGeneId( geneId );

        for ( EvidenceValueObject<? extends PhenotypeAssociation> evidenceVO : evidence ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /**
     * Add flags to Evidence and CharacteristicValueObjects
     *
     * @param evidencesVO         evidence value objects
     * @param phenotypesValuesUri phenotype value URIs
     * @param usedPhenotypes      used phenotypes
     */
    private void flagEvidence( Collection<EvidenceValueObject<? extends PhenotypeAssociation>> evidencesVO,
            Set<String> phenotypesValuesUri, Collection<String> usedPhenotypes ) {

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        Map<String, Set<String>> phenotypesWithChildren = this
                .findChildrenForEachPhenotype( phenotypesValuesUri, usedPhenotypes );

        Set<String> possibleChildrenPhenotypes = new HashSet<>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }

        // flag relevant evidence, root phenotypes and children phenotypes

        for ( EvidenceValueObject<? extends PhenotypeAssociation> evidenceVO : evidencesVO ) {

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

    /**
     * Take care of populating new values for the phenotypes in an update
     *
     * @param phenotypeAssociation phenotype associations
     * @param updatedPhenotypes    updated phenotypes
     */
    private void populateModifiedPhenotypes( Set<CharacteristicValueObject> updatedPhenotypes,
            PhenotypeAssociation phenotypeAssociation ) {

        // the modified final phenotype to update
        Collection<Characteristic> finalPhenotypes = new HashSet<>();

        Map<Long, CharacteristicValueObject> updatedPhenotypesMap = new HashMap<>();

        for ( CharacteristicValueObject updatedPhenotype : updatedPhenotypes ) {

            // updated
            if ( updatedPhenotype.getId() != null ) {
                updatedPhenotypesMap.put( updatedPhenotype.getId(), updatedPhenotype );
            }
            // new one
            else {
                Characteristic c = this.ontologyHelper.valueUri2Characteristic( updatedPhenotype.getValueUri() );
                if ( c == null ) {
                    throw new IllegalStateException(
                            updatedPhenotype.getValueUri() + " could not be converted to a characteristic" );
                }
                finalPhenotypes.add( c );
            }
        }

        for ( Characteristic phenotype : phenotypeAssociation.getPhenotypes() ) {

            CharacteristicValueObject updatedPhenotype = updatedPhenotypesMap.get( phenotype.getId() );

            // found an update, same database id
            if ( updatedPhenotype != null ) {
                phenotype.setValueUri( StringUtils.stripToNull( updatedPhenotype.getValueUri() ) );
                phenotype.setValue( updatedPhenotype.getValue() );
                finalPhenotypes.add( phenotype );
            }
            // this phenotype was deleted
            else {
                this.characteristicService.remove( phenotype );
            }
        }
        phenotypeAssociation.getPhenotypes().clear();
        phenotypeAssociation.getPhenotypes().addAll( finalPhenotypes );
    }

    /**
     * used when we add an evidence and search for phenotype to add, other places too adds a wildcard to the search
     *
     * @param  query query
     * @return the string with an added wildcard to it.
     */
    private String prepareOntologyQuery( String query ) {
        if ( query == null )
            return null;
        String newSearchQuery = query.trim();
        if ( !query.endsWith( "\"" ) && !query.endsWith( "*" ) ) {
            newSearchQuery = newSearchQuery + "*";
        }
        return newSearchQuery;
    }

    private void writeErmineJFile( String writeFolder, String disclaimer, Taxon taxon, boolean noElectronicAnnotation )
            throws IOException {

        String noElectronicA = "";

        if ( noElectronicAnnotation ) {
            noElectronicA = "NoIEA";
        }

        try ( BufferedWriter phenoCartageneSets = new BufferedWriter( new FileWriter(
                writeFolder + "Phenocarta_ErmineJ_" + taxon.getCommonName() + "Genesets" + noElectronicA + ".tsv" ) ) ) {

            phenoCartageneSets.write( disclaimer );

            // gets all : DOID id ---> gene NBCI
            Collection<ExternalDatabaseValueObject> externalDatabaseValueObjects = this
                    .findExternalDatabasesWithEvidence();
            Collection<Long> extIDs = new HashSet<>();

            for ( ExternalDatabaseValueObject externalDatabaseValueObject : externalDatabaseValueObjects ) {
                if ( !externalDatabaseValueObject.getName().contains( "OMIM" ) )
                    extIDs.add( externalDatabaseValueObject.getId() );
            }
            EvidenceFilter ef = new EvidenceFilter( taxon.getId(), false, extIDs );
            Collection<TreeCharacteristicValueObject> ontologyTrees;
            if ( writeFolder.contains( "OMIM" ) )
                ontologyTrees = this
                        .customTreeFeatures( this.findAllPhenotypesByTree( null, false, noElectronicAnnotation ) );
            else
                ontologyTrees = this
                        .customTreeFeatures( this.findAllPhenotypesByTree( ef, false, noElectronicAnnotation ) );

            // cache the results, for a gene found
            HashMap<Integer, String> cacheMap = new HashMap<>();

            if ( writeFolder.contains( "OMIM" ) )
                PhenotypeAssociationManagerServiceImpl.log
                        .debug( "ErmineJ file dump, incl OMIM; ontologyTrees: " + ontologyTrees.size() );
            else
                PhenotypeAssociationManagerServiceImpl.log
                        .debug( "ErmineJ file dump; ontologyTrees: " + ontologyTrees.size() );

            // ontologyTrees.iterator().next() is the disease Ontology, always at first position
            this.writeForErmineJ( ontologyTrees.iterator().next(), taxon, cacheMap, phenoCartageneSets );

        }
    }

    private void writeForErmineJ( TreeCharacteristicValueObject t, Taxon taxon, HashMap<Integer, String> cacheMap,
            BufferedWriter phenoCartageneSets ) throws IOException {

        HomologeneService hs = hs = AsyncFactoryBeanUtils.getSilently( this.homologeneService, HomologeneServiceFactory.class );

        Set<String> geneSymbols = new HashSet<>();

        if ( t != null ) {
            PhenotypeAssociationManagerServiceImpl.log.debug( "Writing ErmineJ: tree is not null" );
            if ( t.getPublicGenesNBCI() != null ) {
                PhenotypeAssociationManagerServiceImpl.log.debug( "Writing ErmineJ: tree genes are not null" );
                for ( Integer geneNCBI : t.getPublicGenesNBCI() ) {

                    if ( cacheMap.get( geneNCBI ) != null ) {
                        geneSymbols.add( cacheMap.get( geneNCBI ) );
                    } else {
                        Gene gene = this.geneService.findByNCBIId( geneNCBI );
                        if ( gene.getTaxon().equals( taxon ) ) {
                            geneSymbols.add( gene.getOfficialSymbol() );
                            cacheMap.put( geneNCBI, gene.getOfficialSymbol() );
                        } else {
                            Gene homoGene = hs.getHomologue( gene, taxon );
                            if ( homoGene != null ) {
                                geneSymbols.add( homoGene.getOfficialSymbol() );
                                cacheMap.put( geneNCBI, homoGene.getOfficialSymbol() );
                            }
                        }
                    }
                }
            }
        }

        // do all children
        if ( t != null ) {

            if ( geneSymbols.size() > 1 && !t.get_id()
                    .equalsIgnoreCase( PhenotypeAssociationConstants.DISEASE_ONTOLOGY_ROOT ) ) {
                phenoCartageneSets.write( t.get_id() + "\t" + t.getValue() + "\t" );
                phenoCartageneSets.write( StringUtils.join( geneSymbols, "\t" ) );
                phenoCartageneSets.write( "\n" );
            }

            if ( t.getChildren() != null ) {
                for ( TreeCharacteristicValueObject children : t.getChildren() ) {
                    this.writeForErmineJ( children, taxon, cacheMap, phenoCartageneSets );
                }
            }
        }

    }

}