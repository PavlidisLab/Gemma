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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyTerm;
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
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.DatabaseEntryDao;
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
import ubic.gemma.model.genome.gene.phenotype.valueObject.GroupEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ScoreValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SimpleTreeValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.authentication.UserManager;

/**
 * High Level Service used to add Candidate Gene Management System capabilities
 * 
 * @author nicolas
 * @version $Id$
 */
@Service
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService, InitializingBean {

    private static final int MAX_PHENOTYPES_FROM_ONTOLOGY = 100;

    private static Log log = LogFactory.getLog( PhenotypeAssociationManagerServiceImpl.class );

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private HomologeneService homologeneService;

    @Autowired
    private PhenotypeAssoManagerServiceHelper phenotypeAssoManagerServiceHelper;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    @Autowired
    private DatabaseEntryDao databaseEntryDao;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private UserManager userManager;

    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;

    @Autowired
    private PhenotypeAssoOntologyHelper ontologyHelper = null;

    private PubMedXMLFetcher pubMedXmlFetcher = null;

    @Override
    public void afterPropertiesSet() {
        this.pubMedXmlFetcher = new PubMedXMLFetcher();
    }

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return Status of the operation
     */
    @Override
    public ValidateEvidenceValueObject makeEvidence( EvidenceValueObject evidence ) {

        if ( evidence.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot create an Evidence with no Phenotype" );
        }
        if ( evidence.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Cannot create an Evidence not linked to a Gene" );
        }

        StopWatch sw = new StopWatch();
        sw.start();
        log.info( "The create is being called for PhenotypeAssociation on geneNCBI: " + evidence.getGeneNCBI() );

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        if ( evidenceAlreadyInDatabase( evidence ) != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            log.info( "The evidence is already in the database: " + evidence.getGeneNCBI() );
            return validateEvidenceValueObject;
        }

        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .valueObject2Entity( evidence );

        phenotypeAssociation = this.associationService.create( phenotypeAssociation );

        // updates the gene in the cache
        Gene gene = phenotypeAssociation.getGene();
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

        log.info( "The create method took : " + sw + "  " + evidence.getGeneNCBI() );

        return validateEvidenceValueObject;
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
    public Collection<EvidenceValueObject> findEvidenceByFilters( Long taxonId, Integer limit, String userName ) {
        final Collection<EvidenceValueObject> evidenceValueObjects;

        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            final Set<Long> paIds;

            if ( userName == null ) {
                if ( SecurityServiceImpl.isUserAdmin() ) {
                    paIds = null;
                } else {
                    paIds = this.associationService.findPrivateEvidenceId( this.userManager.getCurrentUsername(),
                            this.userManager.findAllGroups() );
                }
            } else {
                paIds = this.associationService.findPrivateEvidenceId( userName, this.userManager.findAllGroups() );
            }

            Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                    .findPhenotypeAssociationWithIds( paIds, taxonId, limit );
            evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );
        } else {
            evidenceValueObjects = new HashSet<EvidenceValueObject>();
        }

        return evidenceValueObjects;
    }

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneNCBI( Integer geneNCBI ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneNCBI( geneNCBI );

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    @Override
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
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId, Set<String> phenotypesValuesUri,
            EvidenceFilter evidenceFilter ) {

        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findPhenotypeAssociationForGeneId( geneId );

        if ( evidenceFilter.isShowOnlyEditable() ) {
            phenotypeAssociations = filterPhenotypeAssociationsMyAnnotation( phenotypeAssociations );
        }

        Collection<EvidenceValueObject> evidenceValueObjects = this.convert2ValueObjects( phenotypeAssociations );

        // add all homologue evidence
        evidenceValueObjects.addAll( findHomologueEvidence( geneId, evidenceFilter ) );

        flagEvidence( evidenceValueObjects, phenotypesValuesUri );

        Collection<EvidenceValueObject> evidenceValueObjectsRegrouped = groupCommonEvidences( evidenceValueObjects );

        return evidenceValueObjectsRegrouped;
    }

    /**
     * Given a set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param evidenceFilter can specify a taxon and to show modifiable evidence (optional)
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneValueObject> findCandidateGenes( EvidenceFilter evidenceFilter,
            Set<String> phenotypesValuesUri ) {

        if ( phenotypesValuesUri == null || phenotypesValuesUri.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        Taxon taxon = null;
        boolean showOnlyEditable = false;

        if ( evidenceFilter != null ) {
            if ( evidenceFilter.getTaxonId() != null && evidenceFilter.getTaxonId() > 0 ) {
                taxon = this.taxonService.load( evidenceFilter.getTaxonId() );
            }
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypesValuesUri );

        Set<String> possibleChildrenPhenotypes = findAllPossibleChildren( phenotypesWithChildren );

        String userName = "";
        Collection<String> groups = new HashSet<String>();

        if ( SecurityServiceImpl.isUserLoggedIn() ) {

            userName = this.userManager.getCurrentUsername();
            groups = this.userManager.findAllGroups();
        }

        Collection<GeneEvidenceValueObject> genesPhenotypeHelperObject = this.associationService
                .findGeneWithPhenotypes( possibleChildrenPhenotypes, taxon, userName, groups,
                        SecurityServiceImpl.isUserAdmin(), showOnlyEditable );

        return filterGenesWithPhenotypes( genesPhenotypeHelperObject, phenotypesWithChildren );
    }

    /**
     * Set<String> phenotypesValuesUri ) Given a set of phenotypes returns the genes that have all those phenotypes or
     * children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @param taxon the name of the taxon (optinal)
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneValueObject> findCandidateGenes( Collection<String> phenotypesValuesUri, Taxon taxon ) {

        if ( phenotypesValuesUri == null || phenotypesValuesUri.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypesValuesUri );

        Set<String> possibleChildrenPhenotypes = findAllPossibleChildren( phenotypesWithChildren );

        String userName = "";
        Collection<String> groups = new HashSet<String>();

        if ( SecurityServiceImpl.isUserLoggedIn() ) {

            userName = this.userManager.getCurrentUsername();
            groups = this.userManager.findAllGroups();
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = this.associationService.findGeneWithPhenotypes(
                possibleChildrenPhenotypes, taxon, userName, groups, SecurityServiceImpl.isUserAdmin(), false );

        return filterGenesWithPhenotypes( geneEvidenceValueObjects, phenotypesWithChildren );
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
     * Removes an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
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
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public EvidenceValueObject load( Long id ) {

        PhenotypeAssociation phenotypeAssociation = this.associationService.load( id );

        EvidenceValueObject evidenceValueObject = convert2ValueObjects( phenotypeAssociation );

        return evidenceValueObject;
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     * @return Status of the operation
     */
    @Override
    public ValidateEvidenceValueObject update( EvidenceValueObject modifedEvidenceValueObject ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        if ( modifedEvidenceValueObject.getPhenotypes() == null || modifedEvidenceValueObject.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "An evidence cannot have no phenotype" );
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
        ArrayList<CharacteristicValueObject> orderedPhenotypesFromOntology = new ArrayList<CharacteristicValueObject>();

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
        Set<String> allPhenotypesInDatabase = this.associationService.loadAllPhenotypesUri();

        // rules to order the Ontology results found
        Set<CharacteristicValueObject> phenotypesWithExactMatch = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesAlreadyPresentOnGene = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesStartWithQueryAndInDatabase = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesStartWithQuery = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesSubstringAndInDatabase = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesSubstring = new TreeSet<CharacteristicValueObject>();
        Set<CharacteristicValueObject> phenotypesNoRuleFound = new TreeSet<CharacteristicValueObject>();

        /*
         * for each CharacteristicVO found from the Ontology, filter them and add them to a specific list if they
         * satisfied the condition
         */
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
            } else {
                phenotypesNoRuleFound.add( cha );
            }
        }

        // place them in the correct order to display
        orderedPhenotypesFromOntology.addAll( phenotypesWithExactMatch );
        orderedPhenotypesFromOntology.addAll( phenotypesAlreadyPresentOnGene );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQueryAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstringAndInDatabase );
        orderedPhenotypesFromOntology.addAll( phenotypesStartWithQuery );
        orderedPhenotypesFromOntology.addAll( phenotypesSubstring );
        orderedPhenotypesFromOntology.addAll( phenotypesNoRuleFound );

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
     * Does a Gene search (by name or symbol) for a query and return only Genes with evidence
     * 
     * @param query
     * @param taxonId, can be null to not constrain by taxon
     * @return Collection<GeneEvidenceValueObject> list of Genes
     */
    @Override
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
     * Find all phenotypes associated to a pubmedID
     * 
     * @param pubMedId
     * @param evidenceId optional, used if we are updating to know current annotation
     * @return BibliographicReferenceValueObject
     */
    @Override
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
     * Validate an Evidence before we create it
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return ValidateEvidenceValueObject flags of information to show user messages
     */
    @Override
    public ValidateEvidenceValueObject validateEvidence( EvidenceValueObject evidence ) {

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        EvidenceValueObject evidenceValueObjectInDatabase = evidenceAlreadyInDatabase( evidence );

        if ( evidenceValueObjectInDatabase != null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setSameEvidenceFound( true );
            validateEvidenceValueObject.getProblematicEvidenceIds().add( evidenceValueObjectInDatabase.getId() );
            return validateEvidenceValueObject;
        }

        if ( evidence instanceof LiteratureEvidenceValueObject ) {

            String pubmedId = ( ( LiteratureEvidenceValueObject ) evidence ).getCitationValueObject()
                    .getPubmedAccession();

            validateEvidenceValueObject = determineSameGeneAndPhenotypeAnnotated( evidence, pubmedId );

        } else if ( evidence instanceof ExperimentalEvidenceValueObject ) {

            ExperimentalEvidenceValueObject experimentalEvidenceValueObject = ( ExperimentalEvidenceValueObject ) evidence;

            if ( experimentalEvidenceValueObject.getPrimaryPublicationCitationValueObject() != null ) {

                String pubmedId = experimentalEvidenceValueObject.getPrimaryPublicationCitationValueObject()
                        .getPubmedAccession();
                validateEvidenceValueObject = determineSameGeneAndPhenotypeAnnotated( evidence, pubmedId );
            }
        }
        return validateEvidenceValueObject;
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
     * this method can be used if we want to reimport data from a specific external Database
     * 
     * @param externalDatabaseName
     */
    @Override
    public Collection<EvidenceValueObject> loadEvidenceWithExternalDatabaseName( String externalDatabaseName ) {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findEvidencesWithExternalDatabaseName( externalDatabaseName );

        return this.convert2ValueObjects( phenotypeAssociations );
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

        return externalDatabaseStatisticsValueObjects;
    }

    /**
     * find all evidence that doesn't come from an external source
     */
    @Override
    public Collection<EvidenceValueObject> loadEvidenceWithoutExternalDatabaseName() {
        Collection<PhenotypeAssociation> phenotypeAssociations = this.associationService
                .findEvidencesWithoutExternalDatabaseName();

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /** return the list of the owners that have evidence in the system */
    @Override
    public Collection<String> findEvidenceOwners() {
        return this.associationService.findEvidenceOwners();
    }

    /**
     * For a given search string find all Ontology terms related, and then count their gene occurrence by taxon,
     * including ontology children terms
     * 
     * @param searchQuery the query search that was type by the user
     * @return Collection<CharacteristicValueObject> the terms found in the database with taxon and gene occurrence
     */
    @Override
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

    /** For a given Ontology Term, count the occurence of the term + children in the database */
    private Collection<CharacteristicValueObject> findPhenotypeCount( Collection<OntologyTerm> ontologyTermsFound,
            Taxon taxon, Set<String> phenotypesFoundAndChildren ) {

        Collection<CharacteristicValueObject> phenotypesFound = new HashSet<CharacteristicValueObject>();

        // Phenotype ---> Genes
        HashMap<String, HashSet<Integer>> publicPhenotypesGenesAssociations = this.associationService
                .findPublicPhenotypesGenesAssociations( taxon, phenotypesFoundAndChildren, null, null, false );

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

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology
     * 
     * @param withParentTerms if we want to include the parents terms from the Ontology
     * @param taxonCommonName if we only want a certain taxon
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    private Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree( boolean withParentTerms,
            EvidenceFilter evidenceFilter ) {

        Taxon taxon = null;
        boolean showOnlyEditable = false;

        if ( evidenceFilter != null ) {
            if ( evidenceFilter.getTaxonId() != null && evidenceFilter.getTaxonId() > 0 ) {
                taxon = this.taxonService.load( evidenceFilter.getTaxonId() );
            }
            showOnlyEditable = evidenceFilter.isShowOnlyEditable();
        }

        HashMap<String, HashSet<Integer>> publicPhenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();
        HashMap<String, HashSet<Integer>> privatePhenotypesGenesAssociations = new HashMap<String, HashSet<Integer>>();
        // public phenotypes + private phenotypes (what the user can see)
        Set<String> allPhenotypesGenesAssociations = new HashSet<String>();

        boolean isUserLoggedIn = SecurityServiceImpl.isUserLoggedIn();
        String userName = "";
        Collection<String> groups = new HashSet<String>();
        boolean isAdmin = false;

        if ( isUserLoggedIn ) {
            userName = this.userManager.getCurrentUsername();
            // groups the user belong to
            groups = this.userManager.findGroupsForUser( userName );

            isAdmin = SecurityServiceImpl.isUserAdmin();

            // show only my annotation was chosen
            if ( showOnlyEditable ) {

                // show public owned by the user
                publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations(
                        taxon, null, userName, groups, showOnlyEditable );

                // show all private owned by the user or shared by a group
                privatePhenotypesGenesAssociations = this.associationService.findPrivatePhenotypesGenesAssociations(
                        taxon, null, userName, groups, showOnlyEditable );
            }
            // default case to build the tree
            else {

                // all public evidences
                publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations(
                        taxon, null, null, groups, false );

                if ( isAdmin ) {
                    // show all private since admin
                    privatePhenotypesGenesAssociations = this.associationService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, null, null, false );
                } else {
                    // show all private owned by the user or shared by a group
                    privatePhenotypesGenesAssociations = this.associationService
                            .findPrivatePhenotypesGenesAssociations( taxon, null, userName, groups, false );
                }
            }
        }
        // anonymous user
        else if ( !showOnlyEditable ) {

            publicPhenotypesGenesAssociations = this.associationService.findPublicPhenotypesGenesAssociations( taxon,
                    null, null, null, false );
        }

        for ( String phenotype : privatePhenotypesGenesAssociations.keySet() ) {
            allPhenotypesGenesAssociations.add( phenotype );
        }
        for ( String phenotype : publicPhenotypesGenesAssociations.keySet() ) {
            allPhenotypesGenesAssociations.add( phenotype );
        }

        // map to help the placement of elements in the tree, used to find quickly the position to add subtrees
        HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<String, TreeCharacteristicValueObject>();

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
                        throw new RuntimeException( "Ontologies are not fully loaded yet, try again soon" );
                        /*
                         * FIXME there has to be a mechanism to try again? Or is it going to happen anyway.
                         */
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

        return treesPhenotypes;
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

    /** Build the full trees of the Ontology with the given branches */
    private void findParentRoot( TreeCharacteristicValueObject tc,
            TreeSet<TreeCharacteristicValueObject> finalTreesWithRoots,
            HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree ) {

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

    /** Map query phenotypes given to the set of possible children phenotypes in the database */
    private HashMap<String, Set<String>> findChildrenForEachPhenotype( Collection<String> phenotypesValuesUri ) {

        // root corresponds to one value found in phenotypesValuesUri
        // root ---> root+children phenotypes
        HashMap<String, Set<String>> parentPheno = new HashMap<String, Set<String>>();

        Set<String> phenotypesUriInDatabase = this.associationService.loadAllPhenotypesUri();

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUri ) {

            if ( phenoRoot.isEmpty() ) {
                continue;
            }

            OntologyTerm ontologyTermFound = this.ontologyHelper.findOntologyTermByUri( phenoRoot );
            Collection<OntologyTerm> ontologyChildrenFound = ontologyTermFound.getChildren( false );

            Set<String> parentChildren = new HashSet<String>();
            parentChildren.add( phenoRoot );

            for ( OntologyTerm ot : ontologyChildrenFound ) {

                if ( phenotypesUriInDatabase.contains( ot.getUri() ) ) {
                    parentChildren.add( ot.getUri() );
                }
            }
            parentPheno.put( phenoRoot, parentChildren );
        }
        return parentPheno;
    }

    /** Filter a set of genes if who have the root phenotype or a children of a root phenotype */
    private Collection<GeneValueObject> filterGenesWithPhenotypes(
            Collection<GeneEvidenceValueObject> geneEvidenceValueObjects,
            HashMap<String, Set<String>> phenotypesWithChildren ) {

        Collection<GeneValueObject> genesVO = new HashSet<GeneValueObject>();

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

    /** Add flags to Evidence and CharacteristicvalueObjects */
    private void flagEvidence( Collection<EvidenceValueObject> evidencesVO, Set<String> phenotypesValuesUri ) {

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotype( phenotypesValuesUri );

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

    /**
     * Change a searchQuery to make it search in the Ontology using * and AND (?)
     * 
     * @param query
     * @return fixed-up query. Undocumented special case. Add a wildcard to it.
     */
    private String prepareOntologyQuery( String query ) {
        // what was the purpose of this? It breaks queries like "parkinson's disease" (with quotes)
        // if ( query.startsWith( "\\\"" ) && query.endsWith( "\\\"" ) ) {
        // return query;
        // }

        String newSearchQuery = query;

        // special case when we have character '-' replace it with a blank
        if ( query.length() > 2 ) {
            /*
             * FIXME: I don't really know what this is trying to do. What are the first 2 characters?
             */
            String part1 = query.substring( 0, 2 );
            String part2 = query.substring( 2, query.length() ).replaceAll( "-", " " );
            newSearchQuery = part1 + part2;
        }

        newSearchQuery = newSearchQuery + "*";

        return newSearchQuery;
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

        if ( phe instanceof ExperimentalEvidence ) {
            evidence = new ExperimentalEvidenceValueObject( ( ExperimentalEvidence ) phe );
        } else if ( phe instanceof GenericEvidence ) {
            evidence = new GenericEvidenceValueObject( ( GenericEvidence ) phe );
        } else if ( phe instanceof LiteratureEvidence ) {
            evidence = new LiteratureEvidenceValueObject( ( LiteratureEvidence ) phe );
        } else if ( phe instanceof DifferentialExpressionEvidence ) {
            evidence = convertDifferentialExpressionEvidence2ValueObject( ( DifferentialExpressionEvidence ) phe );
        }

        if ( evidence != null ) {
            findEvidencePermissions( phe, evidence );
        }

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

    /** Determine permissions for an PhenotypeAssociation */
    private void findEvidencePermissions( PhenotypeAssociation p, EvidenceValueObject evidenceValueObject ) {

        Boolean currentUserHasWritePermission = false;
        String owner = null;
        Boolean isPublic = this.securityService.isPublic( p );
        Boolean isShared = this.securityService.isShared( p );
        Boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

        if ( currentUserIsOwner || isPublic || isShared || SecurityServiceImpl.isUserAdmin() ) {

            currentUserHasWritePermission = this.securityService.isEditable( p );
            owner = ( ( PrincipalSid ) this.securityService.getOwner( p ) ).getPrincipal();
        }

        evidenceValueObject.setEvidenceSecurityValueObject( new EvidenceSecurityValueObject(
                currentUserHasWritePermission, currentUserIsOwner, isPublic, isShared, owner ) );
    }

    /** Take care of populating new values for the phenotypes in an update */
    private void populateModifiedPhenotypes( Set<CharacteristicValueObject> updatedPhenotypes,
            PhenotypeAssociation phenotypeAssociation ) {

        // the modified final phenotype to update
        Collection<Characteristic> finalPhenotypes = new HashSet<Characteristic>();

        HashMap<Long, CharacteristicValueObject> updatedPhenotypesMap = new HashMap<Long, CharacteristicValueObject>();

        for ( CharacteristicValueObject updatedPhenotype : updatedPhenotypes ) {

            // updated
            if ( updatedPhenotype.getId() != null ) {
                updatedPhenotypesMap.put( updatedPhenotype.getId(), updatedPhenotype );
            }
            // new one
            else {
                Characteristic c = this.ontologyHelper.valueUri2Characteristic( updatedPhenotype.getValueUri() );
                if ( c != null ) finalPhenotypes.add( c );
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
                .findPhenotypeAssociationForGeneNCBI( evidence.getGeneNCBI() );

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

    /** Literature Evidence that are very similar are grouped together into a new type called GroupEvidenceValueObject */
    private Collection<EvidenceValueObject> groupCommonEvidences( Collection<EvidenceValueObject> evidenceValueObjects ) {

        Collection<EvidenceValueObject> evidenceValueObjectsRegrouped = new TreeSet<EvidenceValueObject>();

        HashMap<String, Collection<LiteratureEvidenceValueObject>> commonEvidences = new HashMap<String, Collection<LiteratureEvidenceValueObject>>();

        for ( EvidenceValueObject evidence : evidenceValueObjects ) {

            if ( evidence.getEvidenceSource() != null && evidence instanceof LiteratureEvidenceValueObject ) {

                LiteratureEvidenceValueObject litEvidenceValueObject = ( LiteratureEvidenceValueObject ) evidence;

                // we want to regroup evidence with the same key, (key representing what makes 2 evidences very similar)
                String key = makeUniqueKey( litEvidenceValueObject );

                if ( commonEvidences.get( key ) == null ) {
                    Collection<LiteratureEvidenceValueObject> setCommonEvidences = new HashSet<LiteratureEvidenceValueObject>();
                    setCommonEvidences.add( litEvidenceValueObject );
                    commonEvidences.put( key, setCommonEvidences );
                } else {
                    commonEvidences.get( key ).add( litEvidenceValueObject );
                }
            } else {
                evidenceValueObjectsRegrouped.add( evidence );
            }
        }

        for ( Collection<LiteratureEvidenceValueObject> groupedLiteratureEvidences : commonEvidences.values() ) {

            if ( groupedLiteratureEvidences.size() == 1 ) {
                evidenceValueObjectsRegrouped.addAll( groupedLiteratureEvidences );
            } else {
                // create the new type of evidence that regroup common evidences
                GroupEvidenceValueObject groupEvidenceValueObject = new GroupEvidenceValueObject(
                        groupedLiteratureEvidences );
                evidenceValueObjectsRegrouped.add( groupEvidenceValueObject );
            }
        }

        return evidenceValueObjectsRegrouped;
    }

    /**
     * To be regrouped an evidence must have the same phenotypes + type + evidenceCode + isNegative
     */
    private String makeUniqueKey( LiteratureEvidenceValueObject evidence ) {

        String key = "";

        for ( CharacteristicValueObject cha : evidence.getPhenotypes() ) {
            key = key + cha.getValueUri();
        }

        key = key + evidence.getDescription();
        key = key + evidence.getGeneNCBI();
        key = key + evidence.getIsNegativeEvidence();

        // When we display homologue's evidence, we don't show evidence code. So,
        // we don't use evidence code as part of the key for homologue's evidence.
        if ( !evidence.isHomologueEvidence() ) {
            key = key + evidence.getEvidenceCode();
        }

        key = key + evidence.getEvidenceSource().getAccession();

        return key;
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

    /** add all the keySet together and return a set representing all children for all valueUri given */
    private Set<String> findAllPossibleChildren( HashMap<String, Set<String>> phenotypesWithChildren ) {

        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }
        return possibleChildrenPhenotypes;
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
     * find Homologue-linked Evidence for a gene
     * 
     * @param geneId Gemma's identifier
     * @param evidenceFilter
     * @return
     */
    private Collection<EvidenceValueObject> findHomologueEvidence( Long geneId, EvidenceFilter evidenceFilter ) {

        // Get the Gene object for finding homologues' evidence.
        Gene gene = this.geneService.load( geneId );

        Collection<Gene> homologues = this.homologeneService.getHomologues( gene );

        Collection<PhenotypeAssociation> homologuePhenotypeAssociations = new HashSet<PhenotypeAssociation>();

        for ( Gene homologue : homologues ) {
            Collection<PhenotypeAssociation> currHomologuePhenotypeAssociations = this.associationService
                    .findPhenotypeAssociationForGeneId( homologue.getId() );
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
}
