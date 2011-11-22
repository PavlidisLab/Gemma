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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.description.VocabCharacteristicImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.TreeCharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/** High Level Service used to add Candidate Gene Management System capabilities */
@Component
public class PhenotypeAssociationManagerServiceImpl implements PhenotypeAssociationManagerService {

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
    private CacheManager cacheManager;

    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;

    /**
     * Links an Evidence to a Gene
     * 
     * @param geneNCBI The Gene NCBI we want to add the evidence
     * @param evidence The evidence
     * @return The Gene updated with the new evidence and phenotypes
     */
    @Override
    public GeneEvidenceValueObject create( String geneNCBI, EvidenceValueObject evidence ) {

        // find the gene we wish to add the evidence and phenotype
        Gene gene = this.geneService.findByNCBIId( new Integer( geneNCBI ) );

        // convert all evidence for this gene to valueObject
        Collection<EvidenceValueObject> evidenceValueObjects = EvidenceValueObject.convert2ValueObjects( gene
                .getPhenotypeAssociations() );

        // verify that the evidence is not a duplicate
        for ( EvidenceValueObject evidenceFound : evidenceValueObjects ) {
            if ( evidenceFound.equals( evidence ) ) {
                // the evidence already exists, no need to create it again
                return new GeneEvidenceValueObject( gene );
            }
        }

        // convert the valueObject received to the corresponding entity
        PhenotypeAssociation pheAsso = this.phenotypeAssoManagerServiceHelper.valueObject2Entity( evidence );

        // Important.
        pheAsso.setGene( gene );

        pheAsso = this.associationService.create( pheAsso );

        // add the entity to the gene
        gene.getPhenotypeAssociations().add( pheAsso );

        // return the saved gene result
        return new GeneEvidenceValueObject( gene );
    }

    /**
     * Return all evidence for a specific gene NCBI
     * 
     * @param geneNCBI The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneNCBI( String geneNCBI ) {

        Gene gene = this.geneService.findByNCBIId( new Integer( geneNCBI ) );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Return all evidence for a specific gene id
     * 
     * @param geneId The Evidence id
     * @return The Gene we are interested in
     */
    @Override
    public Collection<EvidenceValueObject> findEvidenceByGeneId( Long geneId ) {

        Gene gene = this.geneService.load( ( geneId.longValue() ) );

        if ( gene == null ) {
            return null;
        }
        return EvidenceValueObject.convert2ValueObjects( gene.getPhenotypeAssociations() );
    }

    /**
     * Given an set of phenotypes returns the genes that have all those phenotypes or children phenotypes
     * 
     * @param phenotypesValuesUri the roots phenotype of the query
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneEvidenceValueObject> findCandidateGenes( Set<String> phenotypesValuesUri ) {

        if ( phenotypesValuesUri.size() == 0 ) {
            return null;
        }

        // use specific ontologies
        useDiseaseMpHpOntologies();

        // load all current phenotypes in the database
        Set<String> phenotypesUriInDatabase = this.associationService.loadAllPhenotypesURI();

        // set of all possible children for all given root phenotypes
        Set<String> possibleChildrenPhenotypes = new HashSet<String>();

        // map a root phenotype given to the set of possible children phenotypes in the database + root
        // root ---> root+children phenotypes
        HashMap<String, Set<String>> parentPheno = new HashMap<String, Set<String>>();

        // lets keep track of one root phenotype to make the first sql query, then we will filter it
        String aRootPhenotype = "";

        // determine all children terms for each other phenotypes
        for ( String phenoRoot : phenotypesValuesUri ) {

            OntologyTerm ontologyTermFound = findPhenotypeInOntology( phenoRoot );
            Collection<OntologyTerm> ontologyChildrenFound = ontologyTermFound.getChildren( false );

            Set<String> parentChilren = new HashSet<String>();
            parentChilren.add( phenoRoot );

            for ( OntologyTerm ot : ontologyChildrenFound ) {

                if ( phenotypesUriInDatabase.contains( ot.getUri() ) ) {
                    possibleChildrenPhenotypes.add( ot.getUri() );
                    parentChilren.add( ot.getUri() );
                }
            }
            parentPheno.put( phenoRoot, parentChilren );
            aRootPhenotype = phenoRoot;
        }

        // take one of the root phenotype + its children and find all Gene that has evidence with root or children
        Collection<Gene> genes = this.associationService.findPhenotypeAssociations( parentPheno.get( aRootPhenotype ) );
        parentPheno.remove( aRootPhenotype );

        Collection<GeneEvidenceValueObject> genesWithFirstPhenotype = GeneEvidenceValueObject
                .convert2GeneEvidenceValueObjects( genes );

        Collection<GeneEvidenceValueObject> genesVO = new HashSet<GeneEvidenceValueObject>();

        // simple case, we only had one root phenotype
        if ( phenotypesValuesUri.size() == 1 ) {
            genesVO = genesWithFirstPhenotype;
        }
        // we received a set of Gene with the first root phenotype, we need to filter this set and keep only Gene
        // that have all root phenotypes or their children
        else {

            for ( GeneEvidenceValueObject geneVO : genesWithFirstPhenotype ) {

                // all phenotypeUri for a gene
                Set<String> allPhenotypesOnGene = findUniquePhenotpyesForGeneId( geneVO );

                // if the Gene has all the phenotypes
                boolean keepGene = true;

                for ( String phe : parentPheno.keySet() ) {

                    // at least 1 value must be found
                    Set<String> possiblePheno = parentPheno.get( phe );

                    boolean foundSpecificPheno = false;

                    for ( String pheno : possiblePheno ) {

                        if ( allPhenotypesOnGene.contains( pheno ) ) {
                            foundSpecificPheno = true;
                        }
                    }

                    if ( foundSpecificPheno == false ) {
                        // dont keep gene since a root phenotype + children was not found for all evidence of that gene
                        keepGene = false;
                        break;
                    }
                }
                if ( keepGene ) {
                    genesVO.add( geneVO );
                }
            }
        }

        // flag relevant evidence, root phenotypes and children phenotypes
        for ( GeneEvidenceValueObject geneVO : genesVO ) {
            for ( EvidenceValueObject evidenceVO : geneVO.getEvidence() ) {

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

        return genesVO;
    }

    /**
     * This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future Get all
     * phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        Collection<CharacteristicValueObject> characteristcsVO = new TreeSet<CharacteristicValueObject>();

        // load the tree
        Collection<TreeCharacteristicValueObject> tree = findAllPhenotypesByTree();

        // undo the tree in a simple structure
        for ( TreeCharacteristicValueObject t : tree ) {

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
    public void remove( Long id ) {
        PhenotypeAssociation loaded = this.associationService.load( id );
        if ( loaded != null ) this.associationService.remove( loaded );
    }

    /**
     * Load an evidence
     * 
     * @param id The Evidence database id
     */
    @Override
    public EvidenceValueObject load( Long id ) {
        return EvidenceValueObject.convert2ValueObjects( this.associationService.load( id ) );
    }

    /**
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    @Override
    public void update( EvidenceValueObject evidenceValueObject ) {

        // new phenotypes found on the evidence (the difference will be what is added or removed)
        Set<String> newPhenotypesValuesUri = new HashSet<String>();

        for ( CharacteristicValueObject cha : evidenceValueObject.getPhenotypes() ) {
            newPhenotypesValuesUri.add( cha.getValueUri() );
        }

        // an evidenceValueObject always has at least 1 phenotype
        if ( newPhenotypesValuesUri.size() == 0 ) {
            return;
        }

        useDiseaseMpHpOntologies();

        // replace specific values for this type type of evidence
        PhenotypeAssociation phenotypeAssociation = this.phenotypeAssoManagerServiceHelper
                .populateTypePheAsso( evidenceValueObject );

        // the final characteristics to update the evidence with
        Collection<Characteristic> characteristicsUpdated = new HashSet<Characteristic>();

        if ( evidenceValueObject.getDatabaseId() != null ) {

            // for each phenotypes determine if there is new or delete ones
            for ( Characteristic cha : phenotypeAssociation.getPhenotypes() ) {

                if ( cha instanceof VocabCharacteristicImpl ) {
                    String valueUri = ( ( VocabCharacteristicImpl ) cha ).getValueUri();

                    // this phenotype been deleted
                    if ( !newPhenotypesValuesUri.contains( valueUri ) ) {
                        // delete phenotype from the database
                        this.characteristicService.delete( cha.getId() );
                    }
                    // this phenotype is already on the evidence
                    else {
                        characteristicsUpdated.add( cha );
                        newPhenotypesValuesUri.remove( valueUri );
                    }
                }
            }

            // all phenotypes left in newPhenotypesValuesUri represent new phenotypes that were not there before
            for ( String valueUri : newPhenotypesValuesUri ) {
                Characteristic cha = valueUri2Characteristic( valueUri );
                characteristicsUpdated.add( cha );
            }

            // set the correct new phenotypes
            phenotypeAssociation.getPhenotypes().clear();
            phenotypeAssociation.getPhenotypes().addAll( characteristicsUpdated );

            // replace simple values common to all evidences
            this.phenotypeAssoManagerServiceHelper.populatePheAssoWithoutPhenotypes( phenotypeAssociation,
                    evidenceValueObject );

            // update changes to database
            this.associationService.update( phenotypeAssociation );
        }
    }

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery ) {
        return searchOntologyForPhenotype( searchQuery, null );
    }

    /**
     * Giving a phenotype searchQuery, return a selection choice to the user
     * 
     * @param termUsed is what the user typed
     * @param geneId the id of the gene chosen
     * @return Collection<CharacteristicValueObject> list of choices returned
     */
    // TO DO test and discuss more
    @Override
    public Collection<CharacteristicValueObject> searchOntologyForPhenotype( String searchQuery, Long geneId ) {

        Collection<CharacteristicValueObject> phenotypesFound = new ArrayList<CharacteristicValueObject>();

        boolean geneProvided = true;

        if ( geneId == null ) {
            geneProvided = false;
        }

        String[] tokens = searchQuery.split( " " );

        String newSearchQuery = "";

        for ( int i = 0; i < tokens.length; i++ ) {

            newSearchQuery = newSearchQuery + tokens[i] + "* ";

            // last one
            if ( i != tokens.length - 1 ) {
                newSearchQuery = newSearchQuery + "AND ";
            }
        }

        useDiseaseMpHpOntologies();

        Set<CharacteristicValueObject> phenotypes = new HashSet<CharacteristicValueObject>();

        // search disease ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.diseaseOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.DISEASE ) );

        // search mp ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.mammalianPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.MAMMALIAN_PHENOTYPE ) );

        // search hp ontology
        phenotypes.addAll( this.phenotypeAssoManagerServiceHelper.ontology2CharacteristicValueObject(
                this.humanPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.HUMAN_PHENOTYPE ) );

        // This list will contain exact match found in the Ontology search result
        Collection<CharacteristicValueObject> phenotypesFound1 = new ArrayList<CharacteristicValueObject>();
        // This list will contain phenotypes that are already present for that gene
        Collection<CharacteristicValueObject> phenotypesFound2 = new ArrayList<CharacteristicValueObject>();
        // This list will contain phenotypes that are a substring of the searchQuery
        Collection<CharacteristicValueObject> phenotypesFound3 = new ArrayList<CharacteristicValueObject>();
        // Phenotypes present in the database already and found in the ontology search
        Collection<CharacteristicValueObject> phenotypesFound4 = new ArrayList<CharacteristicValueObject>();
        // others
        Collection<CharacteristicValueObject> phenotypesFound5 = new ArrayList<CharacteristicValueObject>();

        // Set of all the phenotypes present on the gene
        Set<CharacteristicValueObject> phenotypesOnGene = null;

        if ( geneProvided ) {
            phenotypesOnGene = findUniquePhenotpyesForGeneId( geneId );
        }

        // lets loas all phenotypes presents in the database
        Collection<CharacteristicValueObject> allPhenotypes = loadAllPhenotypes();

        /*
         * for each CharacteristicVO made from the Ontology search lets filter them and add them to a specific list if
         * they satisfied the condition
         */
        for ( CharacteristicValueObject cha : phenotypes ) {

            // Case 1, exact match
            if ( cha.getValue().equalsIgnoreCase( searchQuery ) ) {

                // if also already present on that gene
                if ( phenotypesOnGene != null && phenotypesOnGene.contains( cha ) ) {
                    cha.setAlreadyPresentOnGene( true );
                }
                phenotypesFound1.add( cha );
            }
            // Case 2, phenotpye already present on Gene
            else if ( phenotypesOnGene != null && phenotypesOnGene.contains( cha ) ) {
                cha.setAlreadyPresentOnGene( true );
                phenotypesFound2.add( cha );
            }
            // Case 3, contains a substring of the word
            else if ( searchQuery.toLowerCase().indexOf( cha.getValue().toLowerCase() ) != -1 ) {
                phenotypesFound3.add( cha );
            }
            // Case 4, phenotypes already in Gemma database
            else if ( allPhenotypes.contains( cha ) ) {
                phenotypesFound4.add( cha );
            } else {
                phenotypesFound5.add( cha );
            }
        }

        // place them in the correct order to display
        phenotypesFound.addAll( phenotypesFound1 );
        phenotypesFound.addAll( phenotypesFound2 );
        phenotypesFound.addAll( phenotypesFound3 );
        phenotypesFound.addAll( phenotypesFound4 );
        phenotypesFound.addAll( phenotypesFound5 );

        return phenotypesFound;
    }

    /**
     * Using all the phenotypes in the database, builds a tree structure using the Ontology, uses cache for fast access
     * 
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    @Override
    public Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree() {

        // init the cache if it is not
        if ( !this.cacheManager.cacheExists( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE ) ) {
            this.cacheManager.addCache( new Cache( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE, 1500, false,
                    false, 24 * 3600, 24 * 3600 ) );
        }

        // represents each phenotype and childs found in the Ontology, TreeSet used to order trees
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<TreeCharacteristicValueObject>();

        // all phenotypes in Gemma
        Set<CharacteristicValueObject> allPhenotypes = this.associationService.loadAllPhenotypes();

        // use specific ontologies
        useDiseaseMpHpOntologies();

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
                    TreeCharacteristicValueObject treeCharacteristicValueObject = this.phenotypeAssoManagerServiceHelper
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
            tc.removeUnusedPhenotypes();
        }

        // look in cache we if have this tree
        Cache phenoCountCache = this.cacheManager.getCache( PhenotypeAssociationConstants.PHENOTYPES_COUNT_CACHE );

        Collection<TreeCharacteristicValueObject> finalTree = new HashSet<TreeCharacteristicValueObject>();

        // last step is to count how many unique Genes we have for each phenotype + children count
        for ( TreeCharacteristicValueObject tc : treesPhenotypes ) {

            // not in the cache
            if ( phenoCountCache.get( tc.getValueUri() ) == null ) {
                countGeneOccurence( tc );
                phenoCountCache.put( new Element( tc.getValueUri(), tc ) );
                finalTree.add( tc );
            } else {
                tc = ( TreeCharacteristicValueObject ) phenoCountCache.get( tc.getValueUri() ).getObjectValue();
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
            return null;
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

    /** counts gene on a TreeCharacteristicValueObject */
    private void countGeneOccurence( TreeCharacteristicValueObject tc ) {

        tc.setOccurence( this.associationService.countGenesWithPhenotype( tc.getAllChildrenUri() ) );

        // count for each node of the tree
        for ( TreeCharacteristicValueObject tree : tc.getChildren() ) {
            countGeneOccurence( tree );
        }
    }

    /** Use specific Ontologies from ontologyService */
    private void useDiseaseMpHpOntologies() {
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
    }

    /** For a valueUri return the Characteristic (represents a phenotype) */
    private Characteristic valueUri2Characteristic( String valueUri ) {

        OntologyTerm o = findPhenotypeInOntology( valueUri );

        VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategoryUri( "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Phenotype" );
        myPhenotype.setCategory( "Phenotype" );

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
    private Set<CharacteristicValueObject> findUniquePhenotpyesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        Collection<EvidenceValueObject> evidence = findEvidenceByGeneId( geneId );

        for ( EvidenceValueObject evidenceVO : evidence ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /** Given a geneVO finds all valueRI of phenotypes for that gene */
    private Set<String> findUniquePhenotpyesForGeneId( GeneEvidenceValueObject geneVO ) {

        Set<String> allPhenotypesOnGene = new HashSet<String>();

        for ( EvidenceValueObject evidenceVO : geneVO.getEvidence() ) {
            for ( CharacteristicValueObject chaVO : evidenceVO.getPhenotypes() ) {
                allPhenotypesOnGene.add( chaVO.getValueUri() );
            }
        }

        return allPhenotypesOnGene;
    }

    /** This method is a temporary solution, we will be using findAllPhenotypesByTree() directly in the future */
    private void addChildren( Collection<CharacteristicValueObject> characteristcsVO, TreeCharacteristicValueObject t ) {

        CharacteristicValueObject cha = new CharacteristicValueObject( t.getValue().toLowerCase(), t.getCategory(),
                t.getValueUri(), t.getCategoryUri() );

        cha.setOccurence( t.getOccurence() );

        characteristcsVO.add( cha );

        for ( TreeCharacteristicValueObject tree : t.getChildren() ) {
            addChildren( characteristcsVO, tree );
        }
    }

}
