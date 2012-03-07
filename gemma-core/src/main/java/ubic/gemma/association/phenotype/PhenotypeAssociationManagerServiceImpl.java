/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.BibliographicPhenotypesValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.SecurityInfoValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ValidateEvidenceValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;
import ubic.gemma.security.authentication.UserManager;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Service
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService, InitializingBean {

    @Autowired
    private PhenotypeAssociationService associationService;

    @Autowired
    private GeneService geneService;

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

    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    private PubMedXMLFetcher pubMedXmlFetcher = null;

    private static Log log = LogFactory.getLog( PhenotypeAssociationManagerServiceImpl.class.getName() );

    @Override
    public void afterPropertiesSet() throws Exception {
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
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
    public ValidateEvidenceValueObject create( EvidenceValueObject evidence ) {

        if ( evidence.getPhenotypes().isEmpty() ) {
            throw new IllegalArgumentException( "Cannot create an Evidence with no Phenotype" );
        }
        if ( evidence.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Cannot create an Evidence not linked to a Gene" );
        }

        ValidateEvidenceValueObject validateEvidenceValueObject = null;

        Gene gene = this.geneService.findByNCBIId( evidence.getGeneNCBI() );

        if ( gene == null ) {
            throw new IllegalArgumentException( "Cannot find the geneNCBI id in Gemma: " + evidence.getGeneNCBI() );
        }

        Collection<EvidenceValueObject> evidenceValueObjects = EvidenceValueObject.convert2ValueObjects( gene
                .getPhenotypeAssociations() );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {
                // the evidence already exists, no need to create it again
                log.warn( "Trying to create an Evidence already present in the database" );

                validateEvidenceValueObject = new ValidateEvidenceValueObject();
                validateEvidenceValueObject.setSameGeneAndPhenotypesAnnotated( true );
                return validateEvidenceValueObject;
            }
        }

        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .valueObject2Entity( evidence );
        phenotypeAssociation.setGene( gene );
        gene.getPhenotypeAssociations().add( phenotypeAssociation );

        phenotypeAssociation = this.associationService.create( phenotypeAssociation );

        return validateEvidenceValueObject;
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

        return this.convert2ValueObjects( phenotypeAssociations );
    }

    /**
     * Return all evidence for a specific gene id with evidence flagged, indicating more information
     * 
     * @param geneId The Evidence id
     * @param phenotypesValuesUri the chosen phenotypes
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId, Set<String> phenotypesValuesUri ) {

        Collection<EvidenceValueObject> evidenceValueObjects = findEvidenceByGeneId( geneId );

        flagEvidence( evidenceValueObjects, phenotypesValuesUri );

        return evidenceValueObjects;
    }

    /**
     * Given a set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneValueObject> findCandidateGenes( Set<String> phenotypesValuesUri ) {

        if ( phenotypesValuesUri == null || phenotypesValuesUri.isEmpty() ) {
            throw new IllegalArgumentException( "No phenotypes values uri provided" );
        }

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotypes( phenotypesValuesUri );

        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        for ( String key : phenotypesWithChildren.keySet() ) {
            possibleChildrenPhenotypes.addAll( phenotypesWithChildren.get( key ) );
        }

        // find all Genes containing the first phenotypeValueUri
        Collection<Gene> genes = this.associationService.findGeneWithPhenotypes( possibleChildrenPhenotypes );

        // dont keep genes with evidence that the user doesnt have the permissions to see
        Collection<Gene> genesAfterAcl = filterGeneAfterAcl( genes );

        Collection<GeneValueObject> genesVO = null;

        if ( phenotypesValuesUri.size() == 1 ) {
            genesVO = GeneValueObject.convert2ValueObjects( genesAfterAcl );
        }
        // we received a set of Gene with the first phenotype, we need to filter this set and keep only genes that have
        // all root phenotypes or their children
        else {
            genesVO = filterGenesWithPhenotypes( genesAfterAcl, phenotypesWithChildren );
        }

        return genesVO;
    }

    /**
     * This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        Collection<CharacteristicValueObject> characteristcsVO = new TreeSet<CharacteristicValueObject>();

        // load the tree
        Collection<TreeCharacteristicValueObject> treeCharacteristicValueObject = findAllPhenotypesByTree();

        // undo the tree in a simple structure
        for ( TreeCharacteristicValueObject t : treeCharacteristicValueObject ) {
            addChildren( characteristcsVO, t );
        }

        return characteristcsVO;
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
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public EvidenceValueObject load( Long id ) {

        PhenotypeAssociation phenotypeAssociation = this.associationService.load( id );
        EvidenceValueObject evidenceValueObject = EvidenceValueObject.convert2ValueObjects( phenotypeAssociation );
        findEvidencePermissions( phenotypeAssociation, evidenceValueObject );

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

        if ( modifedEvidenceValueObject.getGeneNCBI() == null ) {
            throw new IllegalArgumentException( "Evidence not linked to a Gene" );
        }

        if ( modifedEvidenceValueObject.getId() == null ) {
            throw new IllegalArgumentException( "No database id provided" );
        }

        PhenotypeAssociation phenotypeAssociation = this.associationService.load( modifedEvidenceValueObject.getId() );

        if ( phenotypeAssociation == null ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setEvidenceNotFound( true );
            return validateEvidenceValueObject;
        }

        EvidenceValueObject evidenceValueObject = EvidenceValueObject.convert2ValueObjects( phenotypeAssociation );

        // check for the race condition
        if ( phenotypeAssociation.getStatus().getLastUpdateDate().getTime() != modifedEvidenceValueObject
                .getLastUpdated() ) {
            validateEvidenceValueObject = new ValidateEvidenceValueObject();
            validateEvidenceValueObject.setLastUpdateDifferent( true );
            return validateEvidenceValueObject;
        }

        // evidence type changed
        if ( !evidenceValueObject.getClass().equals( modifedEvidenceValueObject.getClass() ) ) {
            remove( modifedEvidenceValueObject.getId() );
            return create( modifedEvidenceValueObject );
        }

        // modify phenotypes
        populateModifiedPhenotypes( modifedEvidenceValueObject, phenotypeAssociation );

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

        ArrayList<CharacteristicValueObject> orderedPhenotypesFromOntology = new ArrayList<CharacteristicValueObject>();

        boolean geneProvided = true;

        if ( geneId == null ) {
            geneProvided = false;
        }

        // prepare the searchQuery to correctly query the Ontology
        String newSearchQuery = prepareOntologyQuery( searchQuery );

        // search the Ontology with the new search query
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = findPhenotypesInOntology( newSearchQuery );

        // All phenotypes present on the gene (if the gene was given)
        Set<CharacteristicValueObject> phenotypesOnCurrentGene = null;

        if ( geneProvided ) {
            phenotypesOnCurrentGene = findUniquePhenotypesForGeneId( geneId );
        }

        // all phenotypes currently in the database
        Set<String> allPhenotypesInDatabase = this.associationService.loadAllPhenotypesUri();

        // rules to order the Ontology results found
        Collection<CharacteristicValueObject> phenotypesWithExactMatch = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesAlreadyPresentOnGene = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesStartWithQueryAndInDatabase = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesStartWithQuery = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesSubstringAndInDatabase = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesSubstring = new ArrayList<CharacteristicValueObject>();
        Collection<CharacteristicValueObject> phenotypesNoRuleFound = new ArrayList<CharacteristicValueObject>();

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
        if ( orderedPhenotypesFromOntology.size() > 100 ) {
            return orderedPhenotypesFromOntology.subList( 0, 100 );
        }

        return orderedPhenotypesFromOntology;
    }

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology
     * 
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    @Override
    public Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree() {

        Collection<TreeCharacteristicValueObject> treesPhenotypes = buildTree();

        Collection<TreeCharacteristicValueObject> finalTree = new TreeSet<TreeCharacteristicValueObject>();

        String username = null;

        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            // find user
            username = this.userManager.getCurrentUsername();
            // TODO find also groups
        }

        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {

            // count occurrence recursively for each phenotype in the branch
            tc.countGeneOccurence( this.associationService, SecurityServiceImpl.isUserAdmin(), username );
            if ( tc.getPublicGeneCount() + tc.getPrivateGeneCount() != 0 ) {
                finalTree.add( tc );
            }
        }

        return finalTree;
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
        SearchSettings settings = SearchSettings.geneSearch( newQuery, taxon );
        List<SearchResult> geneSearchResults = this.searchService.search( settings ).get( Gene.class );

        Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            return new HashSet<GeneEvidenceValueObject>();
        }

        for ( SearchResult sr : geneSearchResults ) {
            genes.add( ( Gene ) sr.getResultObject() );
        }

        Collection<GeneEvidenceValueObject> geneEvidenceValueObjects = GeneEvidenceValueObject
                .convert2GeneEvidenceValueObjects( genes );

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
     * @return BibliographicReferenceValueObject
     */
    @Override
    public BibliographicReferenceValueObject findBibliographicReference( String pubMedId ) {

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

        return new BibliographicReferenceValueObject( bibliographicReference );
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

        if ( evidence instanceof LiteratureEvidenceValueObject ) {

            String pubmedId = ( ( LiteratureEvidenceValueObject ) evidence ).getCitationValueObject()
                    .getPubmedAccession();

            BibliographicReferenceValueObject bibliographicReferenceValueObject = findBibliographicReference( pubmedId );

            if ( bibliographicReferenceValueObject == null ) {
                validateEvidenceValueObject = new ValidateEvidenceValueObject();
                validateEvidenceValueObject.setPubmedIdInvalid( true );
            } else {

                for ( BibliographicPhenotypesValueObject bibliographicPhenotypesValueObject : bibliographicReferenceValueObject
                        .getBibliographicPhenotypes() ) {

                    // we are using validate in update
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

                        boolean containsExact = true;

                        for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                            if ( !bibliographicPhenotypesValueObject.getPhenotypesValues().contains( phenotype ) ) {
                                containsExact = false;
                            }
                        }

                        if ( containsExact ) {
                            validateEvidenceValueObject.setSameGeneAndOnePhenotypeAnnotated( true );
                        }

                        if ( evidence.getPhenotypes().size() == bibliographicPhenotypesValueObject
                                .getPhenotypesValues().size()
                                && evidence.getPhenotypes().containsAll(
                                        bibliographicPhenotypesValueObject.getPhenotypesValues() ) ) {
                            validateEvidenceValueObject.setSameGeneAndPhenotypesAnnotated( true );
                        }

                        Set<String> parentOrChildTerm = new HashSet<String>();

                        // for the phenotype already present we add his children and direct parents, and check that the
                        // phenotype we want to add is not in that subset
                        for ( CharacteristicValueObject phenotypeAlreadyPresent : bibliographicPhenotypesValueObject
                                .getPhenotypesValues() ) {

                            OntologyTerm ontologyTerm = this.ontologyService.getTerm( phenotypeAlreadyPresent
                                    .getValueUri() );

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
                            }
                        }
                    }
                }
            }
        } else if ( evidence instanceof ExperimentalEvidenceValueObject ) {
            // TODO
        }
        return validateEvidenceValueObject;
    }

    /** For a valueUri return the Characteristic (represents a phenotype) */
    private Characteristic valueUri2Characteristic( String valueUri ) {

        OntologyTerm o = findPhenotypeInOntology( valueUri );

        VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
        myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );

        return myPhenotype;
    }

    /** For a valueUri return the OntologyTerm found */
    private OntologyTerm findPhenotypeInOntology( String valueUri ) {

        OntologyTerm ontologyTerm = this.diseaseOntologyService.getTerm( valueUri );

        if ( ontologyTerm == null ) {
            ontologyTerm = this.mammalianPhenotypeOntologyService.getTerm( valueUri );
        }
        if ( ontologyTerm == null ) {
            ontologyTerm = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }
        return ontologyTerm;
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

    /** This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future */
    private void addChildren( Collection<CharacteristicValueObject> characteristcsVO, TreeCharacteristicValueObject t ) {

        CharacteristicValueObject cha = new CharacteristicValueObject( t.getValue().toLowerCase(), t.getCategory(),
                t.getValueUri(), t.getCategoryUri() );

        cha.setPublicGeneCount( t.getPublicGeneCount() );
        cha.setPrivateGeneCount( t.getPrivateGeneCount() );

        characteristcsVO.add( cha );

        for ( TreeCharacteristicValueObject tree : t.getChildren() ) {
            addChildren( characteristcsVO, tree );
        }
    }

    private Collection<TreeCharacteristicValueObject> buildTree() {

        // represents each phenotype and childs found in the Ontology, TreeSet used to order trees
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<TreeCharacteristicValueObject>();

        // all phenotypes in Gemma
        Set<CharacteristicValueObject> allPhenotypes = this.associationService.loadAllPhenotypes();

        // keep track of all phenotypes found in the trees, used to find quickly the position to add subtrees
        HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<String, TreeCharacteristicValueObject>();

        // for each phenotype in Gemma construct its subtree of children if necessary
        for ( CharacteristicValueObject c : allPhenotypes ) {

            // dont create the tree if it is already present in an other
            if ( phenotypeFoundInTree.get( c.getValueUri() ) != null ) {
                // flag the node as phenotype found in database
                phenotypeFoundInTree.get( c.getValueUri() ).setDbPhenotype( true );

            } else {

                // find the ontology term using the valueURI
                OntologyTerm ontologyTerm = findPhenotypeInOntology( c.getValueUri() );

                if ( ontologyTerm != null ) {

                    // transform an OntologyTerm and his children to a TreeCharacteristicValueObject
                    TreeCharacteristicValueObject treeCharacteristicValueObject = TreeCharacteristicValueObject
                            .ontology2TreeCharacteristicValueObjects( ontologyTerm, phenotypeFoundInTree,
                                    treesPhenotypes );

                    // set flag that this node represents a phenotype in the database
                    treeCharacteristicValueObject.setDbPhenotype( true );

                    // add tree to the phenotypes found in ontology
                    phenotypeFoundInTree.put( ontologyTerm.getUri(), treeCharacteristicValueObject );

                    treesPhenotypes.add( treeCharacteristicValueObject );
                }
            }
        }

        // remove all nodes in the trees found in the Ontology but not in the database
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {
            tc.removeUnusedPhenotypes( tc.getValueUri() );
        }

        return treesPhenotypes;
    }

    /** map query phenotypes given to the set of possible children phenotypes in the database */
    private HashMap<String, Set<String>> findChildrenForEachPhenotypes( Set<String> phenotypesValuesUri ) {

        // root corresponds to one value found in phenotypesValuesUri
        // root ---> root+children phenotypes
        HashMap<String, Set<String>> parentPheno = new HashMap<String, Set<String>>();

        Set<String> phenotypesUriInDatabase = this.associationService.loadAllPhenotypesUri();

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUri ) {

            OntologyTerm ontologyTermFound = findPhenotypeInOntology( phenoRoot );
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
    private Collection<GeneValueObject> filterGenesWithPhenotypes( Collection<Gene> genes,
            HashMap<String, Set<String>> phenotypesWithChildren ) {

        Collection<GeneValueObject> genesVO = new HashSet<GeneValueObject>();

        for ( Gene gene : genes ) {

            // all phenotypeUri for a gene
            Set<String> allPhenotypesOnGene = findAllPhenotpyesOnGene( gene );

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
                GeneValueObject geneValueObject = new GeneValueObject( gene );
                genesVO.add( geneValueObject );
            }
        }

        return genesVO;
    }

    /** add flags to Evidence and CharacteristicvalueObjects */
    private void flagEvidence( Collection<EvidenceValueObject> evidencesVO, Set<String> phenotypesValuesUri ) {

        // map query phenotypes given to the set of possible children phenotypes in the database + query phenotype
        HashMap<String, Set<String>> phenotypesWithChildren = findChildrenForEachPhenotypes( phenotypesValuesUri );

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
                evidenceVO.setRelevance( new Double( 1.0 ) );
            }
        }

    }

    /** change a searchQuery to make it search in the Ontology using * and AND */
    private String prepareOntologyQuery( String searchQuery ) {
        String newSearchQuery = searchQuery.trim().replaceAll( "\\s+", "* " ) + "*";
        return StringUtils.join( newSearchQuery.split( " " ), " AND " );
    }

    /** search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO */
    private Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) {
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = new TreeSet<CharacteristicValueObject>();

        // search disease ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.diseaseOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.DISEASE ) );

        // search mp ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.mammalianPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.MAMMALIAN_PHENOTYPE ) );

        // search hp ontology
        allPhenotypesFoundInOntology.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.humanPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.HUMAN_PHENOTYPE ) );

        return allPhenotypesFoundInOntology;
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

                EvidenceValueObject evidence = EvidenceValueObject.convert2ValueObjects( phe );
                findEvidencePermissions( phe, evidence );

                if ( evidence != null ) {
                    returnEvidenceVO.add( evidence );
                }
            }
        }
        return returnEvidenceVO;
    }

    /** determine permissions for an PhenotypeAssociation */
    private void findEvidencePermissions( PhenotypeAssociation p, EvidenceValueObject evidenceValueObject ) {

        Boolean currentUserHasWritePermission = false;
        String owner = null;
        Boolean isPublic = this.securityService.isPublic( p );
        Boolean isShared = this.securityService.isShared( p );
        Boolean currentUserIsOwner = this.securityService.isOwnedByCurrentUser( p );

        if ( currentUserIsOwner || isPublic || isShared ) {

            currentUserHasWritePermission = this.securityService.isEditable( p );
            owner = ( ( PrincipalSid ) this.securityService.getOwner( p ) ).getPrincipal();
        }

        evidenceValueObject.setSecurityInfoValueObject( new SecurityInfoValueObject( currentUserHasWritePermission,
                currentUserIsOwner, isPublic, isShared, owner ) );
    }

    private void populateModifiedPhenotypes( EvidenceValueObject evidenceValueObject,
            PhenotypeAssociation phenotypeAssociation ) {

        // new phenotypes found on the evidence that will be compared to current phenotypes in the database
        Set<String> updatedPhenotypesValuesUri = evidenceValueObject.getPhenotypesValueUri();

        // the final phenotypes to update the evidence with
        Collection<Characteristic> updatedPhenotypes = new HashSet<Characteristic>();

        // for each phenotypes determine if it is new or delete
        for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {

            if ( cha instanceof VocabCharacteristicImpl ) {
                String valueUri = ( ( VocabCharacteristicImpl ) cha ).getValueUri();

                // this phenotype been deleted
                if ( !updatedPhenotypesValuesUri.contains( valueUri ) ) {
                    // delete phenotype from the database
                    this.characteristicService.delete( cha.getId() );
                }
                // this phenotype is already on the evidence
                else {
                    updatedPhenotypes.add( cha );
                    updatedPhenotypesValuesUri.remove( valueUri );
                }
            }
        }

        // all phenotypes left in newPhenotypesValuesUri represent new phenotypes that were not there before
        for ( String valueUri : updatedPhenotypesValuesUri ) {
            Characteristic cha = valueUri2Characteristic( valueUri );
            updatedPhenotypes.add( cha );
        }

        // set the correct new phenotypes
        phenotypeAssociation.getPhenotypes().clear();
        phenotypeAssociation.getPhenotypes().addAll( updatedPhenotypes );
    }

    /** return a collection of Gene that the user is allowed to see */
    private Collection<Gene> filterGeneAfterAcl( Collection<Gene> genes ) {

        Collection<Gene> genesAfterAcl = new HashSet<Gene>();

        for ( Gene gene : genes ) {
            for ( PhenotypeAssociation phenotypeAssociation : gene.getPhenotypeAssociations() ) {

                if ( this.securityService.isPublic( phenotypeAssociation ) ) {
                    genesAfterAcl.add( gene );
                    break;
                } else if ( this.securityService.isShared( phenotypeAssociation ) ) {
                    genesAfterAcl.add( gene );
                    break;
                } else if ( this.securityService.isOwnedByCurrentUser( phenotypeAssociation ) ) {
                    genesAfterAcl.add( gene );
                    break;
                }
            }
        }
        return genesAfterAcl;
    }

    /** get a Set of all phenotypes present ona gene */
    private Set<String> findAllPhenotpyesOnGene( Gene gene ) {

        Set<String> allPhenotypesOnGene = new HashSet<String>();

        for ( PhenotypeAssociation p : gene.getPhenotypeAssociations() ) {
            for ( Characteristic cha : p.getPhenotypes() ) {
                allPhenotypesOnGene.add( ( ( VocabCharacteristic ) ( cha ) ).getValueUri() );
            }
        }
        return allPhenotypesOnGene;
    }

}
