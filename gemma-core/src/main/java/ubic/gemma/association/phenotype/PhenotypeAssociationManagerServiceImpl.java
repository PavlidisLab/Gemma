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

        pheAsso.setGene( gene ); // Important.

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

    private Set<CharacteristicValueObject> findUniquePhenotpyesForGeneId( Long geneId ) {

        Set<CharacteristicValueObject> phenotypes = new TreeSet<CharacteristicValueObject>();

        Collection<EvidenceValueObject> evidence = findEvidenceByGeneId( geneId );

        for ( EvidenceValueObject evidenceVO : evidence ) {
            phenotypes.addAll( evidenceVO.getPhenotypes() );
        }
        return phenotypes;
    }

    /**
     * Given an array of phenotypes returns the genes that have all those phenotypes
     * 
     * @param 1 to many phenotypes
     * @return A collection of the genes found
     */
    @Override
    public Collection<GeneEvidenceValueObject> findCandidateGenes( String... phenotypesValues ) {

        if ( phenotypesValues.length == 0 ) {
            return null;
        }

        Collection<GeneEvidenceValueObject> genesVO = new HashSet<GeneEvidenceValueObject>();

        // find all the Genes with the first phenotype
        Collection<Gene> genes = this.associationService.findPhenotypeAssociations( phenotypesValues[0] );

        Collection<GeneEvidenceValueObject> genesWithFirstPhenotype = GeneEvidenceValueObject
                .convert2GeneEvidenceValueObjects( genes );

        if ( phenotypesValues.length == 1 ) {
            genesVO = genesWithFirstPhenotype;
        }
        // there is more than 1 phenotype, lets filter the content
        else {
            for ( GeneEvidenceValueObject gene : genesWithFirstPhenotype ) {

                // contains all phenotypes for one gene
                HashSet<String> allPhenotypes = new HashSet<String>();

                for ( EvidenceValueObject evidence : gene.getEvidence() ) {
                    for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {
                        allPhenotypes.add( phenotype.getValue() );
                    }
                }

                boolean containAllPhenotypes = true;

                // verify if all phenotypes we are looking for are present in the gene
                for ( int i = 1; i < phenotypesValues.length; i++ ) {

                    if ( !allPhenotypes.contains( phenotypesValues[i].toLowerCase() ) ) {
                        containAllPhenotypes = false;
                    }
                }

                // if the gene had all phenotypes
                if ( containAllPhenotypes ) {
                    genesVO.add( gene );
                }
            }
        }

        // for each evidence on the gene, lets put a flag if that evidence got the chosen phenotype
        for ( GeneEvidenceValueObject gene : genesVO ) {

            for ( EvidenceValueObject evidence : gene.getEvidence() ) {

                boolean evidenceHasPhenotype = false;

                for ( CharacteristicValueObject phenotype : evidence.getPhenotypes() ) {

                    for ( int i = 0; i < phenotypesValues.length; i++ ) {

                        if ( phenotype.getValue().equalsIgnoreCase( phenotypesValues[i] ) ) {
                            evidenceHasPhenotype = true;
                        }
                    }
                }

                if ( evidenceHasPhenotype ) {
                    // score between 0 and 1
                    evidence.setRelevance( new Double( 1.0 ) );
                }
            }
        }
        return genesVO;
    }

    /**
     * Get all phenotypes linked to genes and count how many genes are link to each phenotype
     * 
     * @return A collection of the phenotypes with the gene occurence
     */
    @Override
    public Collection<CharacteristicValueObject> loadAllPhenotypes() {

        // find of all the phenotypes present in Gemma
        Collection<CharacteristicValueObject> phenotypes = this.associationService.loadAllPhenotypes();

        // for each of them, find the occurence
        for ( CharacteristicValueObject phenotype : phenotypes ) {

            phenotype.setOccurence( this.associationService.countGenesWithPhenotype( phenotype.getValue() ) );

            // TODO for now lets use lowerCase until we have a tree
            phenotype.setValue( phenotype.getValue().toLowerCase() );
        }

        return phenotypes;
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
     * Modify an existing evidence
     * 
     * @param evidenceValueObject the evidence with modified fields
     */
    @Override
    public void update( EvidenceValueObject evidenceValueObject ) {

        Long id = evidenceValueObject.getDatabaseId();

        if ( evidenceValueObject.getDatabaseId() != null ) {

            // load the phenotypeAssociation
            PhenotypeAssociation phenotypeAssociation = this.associationService.load( id );

            if ( phenotypeAssociation != null ) {

                // change field in the phenotypeAssociation using the valueObject
                this.phenotypeAssoManagerServiceHelper.populatePhenotypeAssociation( phenotypeAssociation,
                        evidenceValueObject );

                // update changes to database
                this.associationService.update( phenotypeAssociation );
            }
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

        useDiseaseMpHPOntologies();

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
     * 1- Loads all phenotypes in Gemma 2- For each phenotype construct a tree 3- Order the trees by deep 4- For each
     * tree place it in an other deeper tree if possible 5- Remove phenotypes in trees not in the database
     * 
     * @return Collection<TreeCharacteristicValueObject> list of all phenotypes in gemma represented as trees
     */
    @Override
    public Collection<TreeCharacteristicValueObject> findAllPhenotypesByTree() {

        // represents each phenotype and childs found in the Ontology, TreeSet used to order trees by deep
        TreeSet<TreeCharacteristicValueObject> treesPhenotypes = new TreeSet<TreeCharacteristicValueObject>();

        // all phenotypes in Gemma
        Collection<CharacteristicValueObject> allPhenotypes = this.associationService.loadAllPhenotypes();

        // use specific ontologies
        useDiseaseMpHPOntologies();

        // for each phenotype in Gemma construct its subtree of children
        for ( CharacteristicValueObject c : allPhenotypes ) {

            // find the ontology term using the valueURI
            OntologyTerm ontologyTerm = findPhenotypeInOntology( c.getValueUri() );

            // transform an OntologyTerm and his children to a TreeCharacteristicValueObject
            TreeCharacteristicValueObject treeCharacteristicValueObject = this.phenotypeAssoManagerServiceHelper
                    .ontology2TreeCharacteristicValueObjects( ontologyTerm );

            // set flag that this node represents a phenotype in the database
            treeCharacteristicValueObject.setDbPhenotype( true );

            treesPhenotypes.add( treeCharacteristicValueObject );
        }

        // keep track of all phenotypes found in the trees, used to find quickly the position to add subtrees
        HashMap<String, TreeCharacteristicValueObject> phenotypeFoundInTree = new HashMap<String, TreeCharacteristicValueObject>();

        // this will be the final result of combining all trees found into less trees without the terms that are not in
        // the database
        Collection<TreeCharacteristicValueObject> finalTrees = new TreeSet<TreeCharacteristicValueObject>();

        // the deepest tree is the first one, was order by deep
        TreeCharacteristicValueObject treeC = treesPhenotypes.pollFirst();

        // add tree to final result, the deepest tree cannot be a subtree of any other tree
        this.phenotypeAssoManagerServiceHelper.addToFinalResult( treeC, phenotypeFoundInTree, finalTrees );

        // for all other tree, create a new tree in not found in a deepest tree
        for ( TreeCharacteristicValueObject treeVO : treesPhenotypes ) {

            // look if the phenotype is present in a deepest tree
            TreeCharacteristicValueObject treeExist = phenotypeFoundInTree.get( treeVO.getValueUri() );

            // if a deepest tree contain the phenotype, flag the node
            if ( treeExist != null ) {
                treeExist.setDbPhenotype( true );
            }
            // if wasnt found in a deepest tree, create a new tree
            else {
                this.phenotypeAssoManagerServiceHelper.addToFinalResult( treeVO, phenotypeFoundInTree, finalTrees );
            }
        }

        // remove all nodes in the trees found in the Ontology but not in db
        for ( TreeCharacteristicValueObject tc : finalTrees ) {
            tc.removeUnusedPhenotypes();
        }

        return finalTrees;
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

    /** Use specific Ontologies from ontologyService */
    private void useDiseaseMpHPOntologies() {
        this.diseaseOntologyService = this.ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = this.ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = this.ontologyService.getHumanPhenotypeOntologyService();
    }

    private OntologyTerm findPhenotypeInOntology( String valueURI ) {

        OntologyTerm ontologyTerm = this.diseaseOntologyService.getTerm( valueURI );

        if ( ontologyTerm == null ) {
            ontologyTerm = this.mammalianPhenotypeOntologyService.getTerm( valueURI );
        }
        if ( ontologyTerm == null ) {
            ontologyTerm = this.humanPhenotypeOntologyService.getTerm( valueURI );
        }
        return ontologyTerm;
    }

}
