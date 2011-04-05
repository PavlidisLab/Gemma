/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.genome.gene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;

import ubic.gemma.model.Reference;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetService;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.security.SecurityService;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.web.session.SessionListManager;
import ubic.gemma.ontology.providers.GeneOntologyService;

/**
 * For 'live searches' from the web interface.
 * 
 * @author luke
 * @version $Id$
 */
@Controller
public class GenePickerController {

    private static Log log = LogFactory.getLog( GenePickerController.class );

    @Autowired
    private GeneService geneService = null;

    @Autowired
    private GeneSetService geneSetService = null;

    @Autowired
    private TaxonService taxonService = null;

    @Autowired
    private GeneOntologyService geneOntologyService = null;

    @Autowired
    private SecurityService securityService = null;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SearchService searchService = null;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private SessionListManager sessionListManager;

    private static final int MAX_GENES_PER_QUERY = 1000;

    private static Comparator<Taxon> TAXON_COMPARATOR = new Comparator<Taxon>() {
        public int compare( Taxon o1, Taxon o2 ) {
            return ( o1 ).getScientificName().compareTo( ( o2 ).getScientificName() );
        }
    };

    /**
     * AJAX
     * 
     * @param collection of <long> geneIds
     * @return collection of gene entity objects
     */
    public Collection<GeneValueObject> getGenes( Collection<Long> geneIds ) {
        if ( geneIds == null || geneIds.isEmpty() ) {
            return new HashSet<GeneValueObject>();
        }
        return GeneValueObject.convert2ValueObjects( geneService.thawLite( geneService.loadMultiple( geneIds ) ) );
    }

    /**
     * for AJAX get all genes in the given taxon that are annotated with the given go id, including its child terms in
     * the hierarchy
     * 
     * @param goId GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
     * @return Collection<GeneSet> empty if goId was blank or taxonId didn't correspond to a taxon
     */
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {

        Taxon tax = taxonService.load( taxonId );

        if ( !StringUtils.isBlank( goId ) && tax != null && goId.toUpperCase().startsWith( "GO" ) ) {

            Collection<Gene> results = this.geneOntologyService.getGenes( goId, tax );
            if ( results != null ) {
                return GeneValueObject.convert2ValueObjects( results );
            }
        }

        return new HashSet<GeneValueObject>();

    }

    /**
     * AJAX
     * 
     * @return a collection of the taxa in gemma (whether usable or not)
     */
    public Collection<Taxon> getTaxa() {
        SortedSet<Taxon> taxa = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            taxonService.thaw( taxon );
            taxa.add( taxon );
        }
        return taxa;
    }

    /**
     * AJAX
     * 
     * @return Taxon that are species. (only returns usable taxa)
     */
    public Collection<Taxon> getTaxaSpecies() {
        SortedSet<Taxon> taxaSpecies = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsSpecies() ) {
                taxonService.thaw( taxon );
                taxaSpecies.add( taxon );
            }
        }
        return taxaSpecies;
    }

    /**
     * AJAX
     * 
     * @return Taxon that have genes loaded into Gemma and that should be used
     */
    public Collection<Taxon> getTaxaWithGenes() {
        SortedSet<Taxon> taxaWithGenes = new TreeSet<Taxon>( TAXON_COMPARATOR );
        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( taxon.getIsGenesUsable() ) {
                taxonService.thaw( taxon );
                taxaWithGenes.add( taxon );
            }
        }
        return taxaWithGenes;
    }

    /**
     * AJAX
     * 
     * @return collection of taxa that have expression experiments available.
     */
    public Collection<Taxon> getTaxaWithDatasets() {
        Set<Taxon> taxaWithDatasets = new TreeSet<Taxon>( TAXON_COMPARATOR );

        Map<Taxon, Long> perTaxonCount = expressionExperimentService.getPerTaxonCount();

        for ( Taxon taxon : taxonService.loadAll() ) {
            if ( perTaxonCount.containsKey( taxon ) && perTaxonCount.get( taxon ) > 0 ) {
                taxonService.thaw( taxon );
                taxaWithDatasets.add( taxon );
            }
        }
        return taxaWithDatasets;
    }

    /**
     * AJAX
     * 
     * @return List of taxa with array designs in gemma
     */
    public Collection<Taxon> getTaxaWithArrays() {
        Set<Taxon> taxaWithArrays = new TreeSet<Taxon>( TAXON_COMPARATOR );

        for ( Taxon taxon : arrayDesignService.getPerTaxonCount().keySet() ) {
            taxonService.thaw( taxon );
            taxaWithArrays.add( taxon );
        }

        log.debug( "GenePicker::getTaxaWithArrays returned " + taxaWithArrays.size() + " results" );
        return taxaWithArrays;
    }

    /**
     * AJAX (used by GeneCombo.js)
     * 
     * @param query
     * @param taxonId
     * @return Collection of Gene entity objects
     */
    public Collection<GeneValueObject> searchGenes( String query, Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class );

        // Collection<Gene> genes = new HashSet<Gene>();
        if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
            log.info( "No Genes for search: " + query + " taxon=" + taxonId );
            return new HashSet<GeneValueObject>();
        }
        /*
         * for ( SearchResult sr : geneSearchResults ) { genes.add( ( Gene ) sr.getResultObject() ); log.debug(
         * "Gene search result: " + ((Gene)sr.getResultObject()).getOfficialSymbol() ); }
         */
        log.info( "Gene search: " + query + " taxon=" + taxonId + ", " + geneSearchResults.size() + " found" );
        Collection<GeneValueObject> geneValueObjects = GeneValueObject.convert2ValueObjects( geneService
                .loadMultiple( EntityUtils.getIds( geneSearchResults ) ) );
        log.debug( "Gene search: " + geneValueObjects.size() + " value objects returned." );
        return geneValueObjects;
    }

    /**
     * AJAX (used by GeneAndGeneGroupCombo.js)
     * 
     * @param query
     * @param taxonId
     * @return Collection of SearchResultDisplayObject
     */
    public Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) {
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                throw new IllegalArgumentException( "No such taxon with id=" + taxonId );
            }
        }else{
            throw new IllegalArgumentException( "No taxon supplied");
        }
        String taxonName = taxon.getCommonName();
        boolean privateOnly = true;

        /*
         * Arbitrary id values for temporary groups (GO groups and 'all results' groups) (only needed so when displayed
         * in a combo box, the selected entry's name will appear in the field after being selected)
         */
        Long tempId = new Long( -3 );

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();

        // if query is blank, return list of auto generated sets, user-owned sets (if logged in) and user's recent
        // session-bound sets
        if ( query.equals( "" ) ) {

            // get authenticated user's sets
            Collection<GeneSet> userGeneSets = new ArrayList<GeneSet>();
            Collection<GeneSetValueObject> result = new ArrayList<GeneSetValueObject>();
            if ( SecurityService.isUserLoggedIn() ) {
                // get DB groups
                if ( privateOnly ) {
                    try {
                        userGeneSets = ( taxon != null ) ? geneSetService.loadMyGeneSets( taxon ) : geneSetService
                                .loadMyGeneSets();
                        userGeneSets.retainAll( securityService.choosePrivate( userGeneSets ) );
                    } catch ( AccessDeniedException e ) {
                        // okay, they just aren't allowed to see those.
                    }
                } else {
                    userGeneSets = ( taxon != null ) ? geneSetService.loadMyGeneSets( taxon ) : geneSetService
                            .loadMyGeneSets();
                }
                result = GeneSetValueObject.convert2ValueObjects( userGeneSets, false );
            }
            // get any session-bound groups

            Collection<GeneSetValueObject> sessionResult = sessionListManager.getRecentGeneSets( taxonId );

            result.addAll( sessionResult );

            SearchResultDisplayObject newSRDO = null;
            for ( GeneSetValueObject registeredUserSet : result ) {
                newSRDO = new SearchResultDisplayObject( registeredUserSet );
                newSRDO.setType( "users" + newSRDO.getType() ); // will either end up usergeneSet or usergeneSetSession
                // newSRDO.setId(new Long(registeredUserSet.getSessionId()));
                displayResults.add( newSRDO );
            }
            Collections.sort( displayResults );

        } else {

            /*
             * GET GENES AND GENESETS
             */
            SearchSettings settings = SearchSettings.geneSearch( query, taxon );
            settings.setGeneralSearch( true ); // add a general search
            settings.setSearchGeneSets( true ); // add searching for geneSets
            Map<Class<?>, List<SearchResult>> results = searchService.search( settings );
            List<SearchResult> geneSetSearchResults = results.get( GeneSet.class );

            // filter results by taxon
            List<SearchResult> taxonCheckedSets = new ArrayList<SearchResult>();
            for ( SearchResult sr : geneSetSearchResults ) {
                GeneSet gs = ( GeneSet ) sr.getResultObject();
                GeneSetValueObject gsvo = new GeneSetValueObject( gs );
                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonCheckedSets.add( sr );
                }
            }

            Collection<SearchResultDisplayObject> genes = SearchResultDisplayObject
                    .convertSearchResults2SearchResultDisplayObjects( results.get( Gene.class ) );
            Collection<SearchResultDisplayObject> geneSets = SearchResultDisplayObject
                    .convertSearchResults2SearchResultDisplayObjects( taxonCheckedSets );

            // set the taxon values for geneSets
            for ( SearchResultDisplayObject srdo : geneSets ) {
                // geneSets were filtered by taxon above:
                // if taxonId for geneSet != taxonId param, then geneset was already removed
                srdo.setTaxonId( taxonId );
                srdo.setTaxonName( taxonName );
            }

            // if a geneSet is owned by the user, mark it as such (used for giving it a special background colour in
            // search results)
            // TODO make a db call so you can just test each gene set by ID to see if the owner is the current user
            // (avoids loading all user's sets' genes)
            // probably not high priority fix b/c users won't tend to have many sets
            ArrayList<Long> userSetsIds = new ArrayList<Long>();
            // get ids of user's sets
            if ( SecurityService.isUserLoggedIn() ) {
                Collection<GeneSet> myGeneSets = ( taxon != null ) ? geneSetService.loadMyGeneSets( taxon )
                        : geneSetService.loadMyGeneSets();
                for ( GeneSet myGeneSet : myGeneSets ) {
                    userSetsIds.add( myGeneSet.getId() );
                }
                // tag search result display objects appropriately
                for ( SearchResultDisplayObject srdo : geneSets ) {
                    if ( userSetsIds.contains( srdo.getReference().getId() ) ) {
                        srdo.setType( "usersgeneSet" );
                    }
                }
            }

            displayResults.addAll( genes );
            displayResults.addAll( geneSets );

            
            /*
             * GET GO GROUPS
             */

            List<GeneSet> goSets = new ArrayList<GeneSet>();
            ArrayList<SearchResultDisplayObject> goSRDOs = new ArrayList<SearchResultDisplayObject>();

            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = this.geneSetSearch.findByGoId( query, taxon );
                if ( goSet != null ) {
                    SearchResultDisplayObject sdo = new SearchResultDisplayObject( goSet );
                    sdo.setTaxonId( taxonId );
                    sdo.setTaxonName( taxonName );
                    tempId--;
                    goSRDOs.add( sdo );
                    goSets.add( goSet );
                }
            } else {
                for ( GeneSet geneSet : geneSetSearch.findByGoTermName( query, taxon ) ) {
                    // don't bother adding empty GO groups
                    // (should probably do this check elsewhere in case it speeds things up)
                    if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                        SearchResultDisplayObject sdo = new SearchResultDisplayObject( geneSet );
                        sdo.setType( "GOgroup" );
                        sdo.setTaxonId( taxonId );
                        sdo.setTaxonName( taxonName );
                        tempId--;
                        goSRDOs.add( sdo );
                        goSets.add( geneSet );
                    }
                }
            }
            
            Collections.sort( goSRDOs );
            displayResults.addAll( goSRDOs );
            

            /*
             * GET 'ALL RESULTS' GROUPS
             */

            // if >1 result, add a group whose members are all genes returned from search
            if ( ( genes.size() + geneSets.size() + goSets.size() ) > 1 ) {

                // if a gene was returned by both gene and gene set search, don't count it twice (managed by set)
                HashSet<Long> geneIds = new HashSet<Long>();

                // add every individual experiment to the set
                for ( SearchResultDisplayObject srdo : genes ) {
                    geneIds.add( srdo.getReference().getId() );
                }

                // if there's a group, get the number of members by taxon
                if ( geneSets.size() > 0 ) {
                    // for each group
                    for ( SearchResult geneSetSRO : results.get( GeneSet.class ) ) {
                        // get the ids of the gene members
                        Iterator<GeneSetMember> iter = ( ( GeneSet ) geneSetSRO.getResultObject() ).getMembers()
                                .iterator();
                        Long id = null;
                        Gene gene = null;
                        while ( iter.hasNext() ) {
                            gene = iter.next().getGene();
                            id = gene.getId();
                            geneIds.add( id );
                        }
                    }
                }

                // if there are any go group results, get the members
                if ( goSets.size() > 0 ) {
                    // for each group
                    for ( GeneSet geneSet : goSets ) {
                        // get the ids of the gene members
                        Iterator<GeneSetMember> iter = geneSet.getMembers().iterator();
                        Long id = null;
                        Gene gene = null;
                        while ( iter.hasNext() ) {
                            gene = iter.next().getGene();
                            id = gene.getId();
                            geneIds.add( id );
                        }
                    }
                }

                Reference ref = new Reference( tempId, Reference.SESSION_UNBOUND_GROUP );
                SearchResultDisplayObject allResultsGroup = new SearchResultDisplayObject(
                        ExpressionExperimentSet.class, ref, "All '" + query + "' results",
                        "All genes found for your query", true, geneIds.size(), taxonId, taxonName, 
                        "freeText", geneIds );
                tempId--;
                displayResults.add( allResultsGroup );
            }

        }

        if ( displayResults == null || displayResults.isEmpty() ) {
            log.info( "No results for search: " + query + " taxon="
                    + ( ( taxon == null ) ? null : taxon.getCommonName() ) );
            return new HashSet<SearchResultDisplayObject>();
        }
        log.info( "Results for search: " + query + ", size=" + displayResults.size() );

        return displayResults;

    }

    /**
     * Similar to method of same name in GeneSetController.java but here: - no taxon needed - GO groups always searched
     * - GeneSet objects returned instead of GeneSetValueObjects
     * 
     * @param query string to match to a gene set.
     * @param taxonId
     * @return collection of GeneSet
     */
    public Collection<GeneSet> findGeneSetsByName( String query, Long taxonId ) {

        if ( StringUtils.isBlank( query ) ) {
            return new HashSet<GeneSet>();
        }
        Collection<GeneSet> foundGeneSets = null;
        Taxon tax = null;
        tax = taxonService.load( taxonId );

        if ( tax == null ) {
            // throw new IllegalArgumentException( "Can't locate taxon with id=" + taxonId );
            foundGeneSets = this.geneSetSearch.findByName( query );
        } else {
            foundGeneSets = this.geneSetSearch.findByName( query, tax );
        }

        foundGeneSets.clear(); // for testing general search

        /*
         * SEARCH GENE ONTOLOGY
         */

        if ( query.toUpperCase().startsWith( "GO" ) ) {
            GeneSet goSet = this.geneSetSearch.findByGoId( query, tax );
            if ( goSet != null ) foundGeneSets.add( goSet );
        } else {
            foundGeneSets.addAll( geneSetSearch.findByGoTermName( query, tax ) );
        }

        return foundGeneSets;
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return colleciton of gene entity objects
     * @throws IOException
     */
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        Taxon taxon = taxonService.load( taxonId );

        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        Collection<Gene> genes = new HashSet<Gene>();
        String line = null;

        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) ) continue;
            if ( genes.size() >= MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping" );
                break;
            }
            line = StringUtils.strip( line );
            SearchSettings settings = SearchSettings.geneSearch( line, taxon );
            List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class ); // drops
            // predicted gene
            // results....

            // FIXME inform the user (on the client!) if there are some that don't have results.
            if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
                log.warn( "No gene results for gene with id: " + line );
            } else if ( geneSearchResults.size() == 1 ) { // Just one result so add it
                genes.add( ( Gene ) geneSearchResults.iterator().next().getResultObject() );

            } else { // Many results need to find best if possible
                Collection<Gene> notExactMatch = new HashSet<Gene>();
                Collection<Gene> sameTaxonMatch = new HashSet<Gene>();

                Boolean foundMatch = false;

                // Usually if there is more than 1 results the search term was a official symbol and picked up matches
                // like grin1, grin2, grin3, grin (given the search term was grin)
                for ( SearchResult sr : geneSearchResults ) {
                    Gene srGene = ( Gene ) sr.getResultObject();
                    if ( srGene.getOfficialSymbol().equalsIgnoreCase( line ) ) {
                        genes.add( srGene );
                        foundMatch = true;
                        break; // found so return
                    } else if ( srGene.getTaxon().equals( taxon ) ) {
                        sameTaxonMatch.add( srGene );
                    } else
                        notExactMatch.add( srGene );
                }

                // if no exact match found add all of them of the same taxon and toss a warning
                if ( !foundMatch ) {

                    if ( !sameTaxonMatch.isEmpty() ) {
                        genes.addAll( sameTaxonMatch );
                        log.warn( sameTaxonMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + sameTaxonMatch + ". Adding All" );
                    } else {
                        log.warn( notExactMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + notExactMatch + ". Adding None" );
                    }
                }
            }

        }
        return GeneValueObject.convert2ValueObjects( geneService.thawLite( genes ) );
    }

    /**
     * AJAX Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return map with each gene-query as a key and a collection of the search-results as the value
     * @throws IOException
     */
    public Map<String, Collection<GeneValueObject>> searchMultipleGenesGetMap( String query, Long taxonId )
            throws IOException {
        Taxon taxon = taxonService.load( taxonId );
        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        String line = null;
        int genesAdded = 0;

        Map<String, Collection<GeneValueObject>> queryToGenes = new HashMap<String, Collection<GeneValueObject>>();
        while ( ( line = reader.readLine() ) != null ) {
            queryToGenes.put( line, new HashSet<GeneValueObject>() );
        }

        reader = new BufferedReader( new StringReader( query ) );
        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) ) continue;
            if ( genesAdded >= MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping" );
                break;
            }
            line = StringUtils.strip( line );
            SearchSettings settings = SearchSettings.geneSearch( line, taxon );
            List<SearchResult> geneSearchResults = searchService.search( settings ).get( Gene.class ); // drops
                                                                                                       // predicted gene
                                                                                                       // results

            // FIXME inform the user (on the client!) if there are some that don't have results.
            if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
                log.warn( "No gene results for gene with id: " + line );
            } else if ( geneSearchResults.size() == 1 ) { // Just one result so add it
                Gene g = ( Gene ) geneSearchResults.iterator().next().getResultObject();
                queryToGenes.get( line ).add( new GeneValueObject( g ) );
                genesAdded++;
            } else { // Many results need to find best if possible
                Collection<Gene> notExactMatch = new HashSet<Gene>();
                Collection<GeneValueObject> sameTaxonMatch = new HashSet<GeneValueObject>();

                Boolean foundMatch = false;

                // Usually if there is more than 1 results the search term was a official symbol and picked up matches
                // like grin1, grin2, grin3, grin (given the search term was grin)
                for ( SearchResult sr : geneSearchResults ) {
                    Gene srGene = ( Gene ) sr.getResultObject();
                    if ( srGene.getOfficialSymbol().equalsIgnoreCase( line ) ) {
                        queryToGenes.get( line ).add( new GeneValueObject( srGene ) );
                        genesAdded++;
                        foundMatch = true;
                        break; // found so return
                    } else if ( srGene.getTaxon().equals( taxon ) ) {
                        sameTaxonMatch.add( new GeneValueObject( srGene ) );
                    } else
                        notExactMatch.add( srGene );
                }

                // if no exact match found add all of them of the same taxon and toss a warning
                if ( !foundMatch ) {

                    if ( !sameTaxonMatch.isEmpty() ) {

                        queryToGenes.get( line ).addAll( sameTaxonMatch );

                        log.warn( sameTaxonMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + sameTaxonMatch + ". Adding All" );
                    } else {
                        log.warn( notExactMatch.size() + " genes found for query id = " + line + ". Genes found are: "
                                + notExactMatch + ". Adding None" );
                    }
                }
            }
        }

        return queryToGenes;
    }

}