/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */
package ubic.gemma.genome.gene.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import ubic.gemma.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.genome.gene.FreeTextGeneResultsValueObject;
import ubic.gemma.genome.gene.GOGroupValueObject;
import ubic.gemma.genome.gene.GeneSetValueObject;
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
import ubic.gemma.search.SearchSettings;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.SecurityServiceImpl;

/**
 * Service for searching genes (and gene sets)
 * 
 * @author tvrossum
 * @version $Id: GeneSearchServiceImpl.java,
 */
@Service
public class GeneSearchServiceImpl implements GeneSearchService {

    private static Log log = LogFactory.getLog( GeneSearchServiceImpl.class );

    private static final int MAX_GENES_PER_QUERY = 1000;

    @Autowired
    private SearchService searchService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneSetSearch geneSetSearch;

    @Autowired
    private GeneSetService geneSetService;

    @Autowired
    private GeneOntologyService geneOntologyService;
    
    @Autowired
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    // TODO REFACTOR method is much too long -Thea
    @Override
    public Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) {
        Taxon taxon = null;
        String taxonName = "";
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                log.warn( "No such taxon with id=" + taxonId );
            } else {
                taxonName = taxon.getCommonName();
            }
        }

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();

        // if query is blank, return list of auto generated sets, user-owned sets (if logged in) and user's recent
        // session-bound sets
        if ( StringUtils.isBlank( query ) ) {

            return this.searchGenesAndGeneGroupsBlankQuery( taxonId );

        }

        Integer maxGeneralSearchResults = 500;

        Integer maxGoTermsProcessed = 50;

        /*
         * GET GENES AND GENESETS
         */
        // SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        SearchSettings settings = new SearchSettings( query );
        settings.noSearches();
        settings.setGeneralSearch( true ); // add a general search, needed for finding GO groups
        settings.setSearchGenes( true ); // add searching for genes
        settings.setSearchGeneSets( true ); // add searching for geneSets
        settings.setMaxResults( maxGeneralSearchResults );
        if ( taxon != null ) settings.setTaxon( taxon ); // this doesn't work yet
        Map<Class<?>, List<SearchResult>> results = searchService.search( settings );
        List<SearchResult> geneSetSearchResults = new ArrayList<SearchResult>();
        List<SearchResult> geneSearchResults = new ArrayList<SearchResult>();

        if ( results.get( GeneSet.class ) != null ) {
            geneSetSearchResults.addAll( results.get( GeneSet.class ) );
        }
        if ( results.get( Gene.class ) != null ) {
            geneSearchResults.addAll( results.get( Gene.class ) );
        }

        Collection<SearchResultDisplayObject> genes = null;
        Collection<SearchResultDisplayObject> geneSets = null;

        Map<Long, Boolean> isSetOwnedByUser = new HashMap<Long, Boolean>();

        if ( taxon != null ) { // filter search results by taxon

            List<SearchResult> taxonCheckedGenes = new ArrayList<SearchResult>();
            for ( SearchResult sr : geneSearchResults ) {
                Gene gene = ( Gene ) sr.getResultObject();
                if ( gene.getTaxon() != null && gene.getTaxon().getId().equals( taxonId ) ) {
                    taxonCheckedGenes.add( sr );
                }
            }


            // convert result object to a value object
            for(SearchResult sr : taxonCheckedGenes ){
                Gene g = ( Gene ) sr.getResultObject();
                GeneValueObject gvo = new GeneValueObject( g );
                sr.setResultObject( gvo );
            }
            
            genes = SearchResultDisplayObject.convertSearchResults2SearchResultDisplayObjects( taxonCheckedGenes );

            List<SearchResult> taxonCheckedSets = new ArrayList<SearchResult>();
            for ( SearchResult sr : geneSetSearchResults ) {
                GeneSet gs = ( GeneSet ) sr.getResultObject();
                GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( gs );

                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

                if ( gsvo.getTaxonId() == taxonId ) {
                    taxonCheckedSets.add( sr );
                }
            }

            // convert result object to a value object
            for(SearchResult sr : taxonCheckedSets ){
                GeneSet g = ( GeneSet ) sr.getResultObject();
                DatabaseBackedGeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( g );
                sr.setResultObject( gsvo );
            }
            
            geneSets = SearchResultDisplayObject.convertSearchResults2SearchResultDisplayObjects( taxonCheckedSets );
            for ( SearchResultDisplayObject srdo : geneSets ) {
                // geneSets were filtered by taxon above:
                // if taxonId for geneSet != taxonId param, then geneset was already removed
                srdo.setTaxonId( taxonId );
                srdo.setTaxonName( taxonName );
            }
        } else { // set the taxon values


            // convert result object to a value object
            for(SearchResult sr : geneSearchResults ){
                Gene g = ( Gene ) sr.getResultObject();
                GeneValueObject gvo = new GeneValueObject( g );
                sr.setResultObject( gvo );
            }
            genes = SearchResultDisplayObject.convertSearchResults2SearchResultDisplayObjects( geneSearchResults );

            geneSets = new ArrayList<SearchResultDisplayObject>();
            SearchResultDisplayObject srdo = null;
            for ( SearchResult sr : geneSetSearchResults ) {

                GeneSet gs = ( GeneSet ) sr.getResultObject();
                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

                taxon = geneSetService.getTaxonForGeneSet( ( GeneSet ) sr.getResultObject() );
                GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( gs );
                srdo = new SearchResultDisplayObject( gsvo );
                srdo.setTaxonId( taxon.getId() );
                srdo.setTaxonName( taxon.getCommonName() );
                geneSets.add( srdo );
            }
            taxon = null;
        }

        // if a geneSet is owned by the user, mark it as such (used for giving it a special background colour in
        // search results)
        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            for ( SearchResultDisplayObject srdo : geneSets ) {
                Long id = ( srdo.getResultValueObject() instanceof DatabaseBackedGeneSetValueObject ) ? ( ( GeneSetValueObject ) srdo
                        .getResultValueObject() ).getId()
                        : new Long( -1 );
                srdo.setUserOwned( isSetOwnedByUser.get( id ) );
            }
        }

        boolean skipGetGoGroups = false;

        // if there is an exact match for a gene, then don't do GO search
        for ( SearchResultDisplayObject srdo : genes ) {

            if ( srdo.getName().trim().equalsIgnoreCase( query.trim() ) ) {
                skipGetGoGroups = true;
                break;
            }

        }

        /*
         * GET GO GROUPS
         */

        List<GeneSet> goSets = new ArrayList<GeneSet>();
        ArrayList<SearchResultDisplayObject> goSRDOs = new ArrayList<SearchResultDisplayObject>();

        if ( !skipGetGoGroups ) {
            Collection<Taxon> taxonsForGo = new ArrayList<Taxon>();
            // if taxon isn't set, get go groups for each possible taxon
            if ( taxon == null ) {
                taxonsForGo.addAll( taxonService.loadAll() );
            } else {
                taxonsForGo.add( taxon );
            }

            for ( Taxon taxonForGo : taxonsForGo ) {
                if ( query.toUpperCase().startsWith( "GO" ) ) {
                    GeneSet goSet = geneSetSearch.findByGoId( query, taxonForGo );
                    if ( goSet != null && goSet.getMembers() != null && goSet.getMembers().size() > 0 ) {
                        GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( goSet, query, query );
                        ggvo.setTaxonId( taxonForGo.getId() );
                        ggvo.setTaxonName( taxonForGo.getCommonName() );
                        SearchResultDisplayObject sdo = new SearchResultDisplayObject( ggvo );
                        sdo.setUserOwned( false );
                        sdo.setTaxonId( taxonForGo.getId() );
                        sdo.setTaxonName( taxonForGo.getCommonName() );
                        goSRDOs.add( sdo );
                        goSets.add( goSet );
                    }
                } else {
                    for ( GeneSet geneSet : geneSetSearch.findByGoTermName( query, taxonForGo, maxGoTermsProcessed ) ) {
                        // don't bother adding empty GO groups
                        // (should probably do this check elsewhere in case it speeds things up)
                        if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                            GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( geneSet, null, query );
                            ggvo.setTaxonId( taxonForGo.getId() );
                            ggvo.setTaxonName( taxonForGo.getCommonName() );
                            SearchResultDisplayObject sdo = new SearchResultDisplayObject( ggvo );
                            sdo.setUserOwned( false );
                            sdo.setTaxonId( taxonForGo.getId() );
                            sdo.setTaxonName( taxonForGo.getCommonName() );
                            goSRDOs.add( sdo );
                            goSets.add( geneSet );
                        }
                    }
                }
            }

            Collections.sort( goSRDOs );
        }
        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all genes returned from search
        if ( ( genes.size() + geneSets.size() + goSets.size() ) > 1 ) {

            // if an experiment was returned by both experiment and experiment set search, don't count it twice
            // (managed by set)
            HashSet<Long> geneIds = new HashSet<Long>();
            HashMap<Long, HashSet<Long>> geneIdsByTaxonId = new HashMap<Long, HashSet<Long>>();

            // add every individual gene to the set
            for ( SearchResultDisplayObject srdo : genes ) {
                if ( !geneIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                    geneIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
                }
                Long id = ( srdo.getResultValueObject() instanceof GeneValueObject ) ? ( ( GeneValueObject ) srdo
                        .getResultValueObject() ).getId() : new Long( -1 );
                if ( id != -1 ) {
                    geneIdsByTaxonId.get( srdo.getTaxonId() ).add( id );
                    geneIds.add( id );
                }

            }

            // if there's a group, get the number of members

            // for each group
            for ( SearchResultDisplayObject srdo : geneSets ) {
                // get the ids of the gene members
                if ( !geneIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                    geneIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
                }
                geneIdsByTaxonId.get( srdo.getTaxonId() ).addAll( srdo.getMemberIds() );
            }

            // for each go group
            for ( SearchResultDisplayObject srdo : goSRDOs ) {
                // get the ids of the gene members
                if ( !geneIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                    geneIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
                }
                geneIdsByTaxonId.get( srdo.getTaxonId() ).addAll( srdo.getMemberIds() );
            }

            // make an entry for each taxon

            Long taxonId2 = null;
            for ( Map.Entry<Long, HashSet<Long>> entry : geneIdsByTaxonId.entrySet() ) {
                taxonId2 = entry.getKey();
                taxon = taxonService.load( taxonId2 );

                // don't make groups for 1 gene
                if ( taxon != null && entry.getValue().size() > 1 ) {
                    FreeTextGeneResultsValueObject byTaxFTVO = new FreeTextGeneResultsValueObject( "All "
                            + taxon.getCommonName() + " results for '" + query + "'", "All " + taxon.getCommonName()
                            + " genes found for your query", taxon.getId(), taxon.getCommonName(), entry.getValue(),
                            query );
                    displayResults.add( new SearchResultDisplayObject( byTaxFTVO ) );
                }
            }
        }

        displayResults.addAll( geneSets );
        displayResults.addAll( goSRDOs );
        displayResults.addAll( genes );

        if ( displayResults.isEmpty() ) {
            log.info( "No results for search: " + query + " taxon="
                    + ( ( taxon == null ) ? null : taxon.getCommonName() ) );
            return new HashSet<SearchResultDisplayObject>();
        }
        log.info( "Results for search: " + query + ", size=" + displayResults.size() );

        return displayResults;
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller.genome.gene.GenePickerController.searchGenesAndGeneGroups(String, Long)
     * 
     * @param taxonId
     * @return Collection<SearchResultDisplayObject>
     */
    private Collection<SearchResultDisplayObject> searchGenesAndGeneGroupsBlankQuery( Long taxonId ) {
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                log.warn( "No such taxon with id=" + taxonId );
            }
        }

        // if query is blank, return list of auto generated sets, user-owned sets (if logged in) and user's recent
        // session-bound sets

        // right now, no public gene sets are useful so we don't want to prompt them
        boolean promptPublicSets = false;

        StopWatch watch = new StopWatch();
        watch.start();

        // get all public sets (if user is admin, these were already loaded with geneSetService.loadMySets() )
        // filtered by security.
        Collection<GeneSet> sets = new ArrayList<GeneSet>();
        if ( promptPublicSets && !SecurityServiceImpl.isUserLoggedIn() ) {
            try {
                sets = geneSetService.loadAll( taxon );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else if ( SecurityServiceImpl.isUserLoggedIn() ) {
            /*
             * actually, loadMyGeneSets and loadAll point to the same method (they just use different spring security
             * filters)
             */
            sets = ( taxon != null ) ? geneSetService.loadMyGeneSets( taxon ) : geneSetService.loadMyGeneSets();
            log.info( "Loading the user's gene sets took: " + watch.getTime() );
        }

        // separate these out because they go at the top of the list
        List<SearchResultDisplayObject> displayResultsPrivate = new LinkedList<SearchResultDisplayObject>();
        List<SearchResultDisplayObject> displayResultsPublic = new LinkedList<SearchResultDisplayObject>();
        SearchResultDisplayObject newSRDO = null;
        for ( GeneSet set : sets ) {
            GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( set );
            newSRDO = new SearchResultDisplayObject( gsvo );
            newSRDO.setTaxonId( ( ( GeneSetValueObject ) newSRDO.getResultValueObject() ).getTaxonId() );
            newSRDO.setTaxonName( ( ( GeneSetValueObject ) newSRDO.getResultValueObject() ).getTaxonName() );
            boolean isPrivate = securityService.isPrivate( set );
            newSRDO.setUserOwned( isPrivate );
            ( ( GeneSetValueObject ) newSRDO.getResultValueObject() ).setPublik( !isPrivate );
            if ( isPrivate ) {
                displayResultsPrivate.add( newSRDO );
            } else {
                displayResultsPublic.add( newSRDO );
            }
        }

        // keep sets in proper order (user's groups first, then public ones)
        Collections.sort( displayResultsPrivate );
        Collections.sort( displayResultsPublic );

        List<SearchResultDisplayObject> displayResults = new LinkedList<SearchResultDisplayObject>();

        displayResults.addAll( displayResultsPrivate );
        displayResults.addAll( displayResultsPublic );

        if ( displayResults.isEmpty() ) {
            log
                    .info( "No results for blank query search, taxon="
                            + ( ( taxon == null ) ? null : taxon.getCommonName() ) );
            return new HashSet<SearchResultDisplayObject>();
        }
        log.info( "Results for blank query search, size=" + displayResults.size() );

        return displayResults;

    }

    /**
     * get all genes in the given taxon that are annotated with the given go id, including its child terms in the
     * hierarchy
     * 
     * @param goId GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
     * @return Collection<GeneSetValueObject> empty if goId was blank or taxonId didn't correspond to a taxon
     */
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {

        Taxon tax = taxonService.load( taxonId );

        if ( !StringUtils.isBlank( goId ) && tax != null && goId.toUpperCase().startsWith( "GO" ) ) {

            Collection<Gene> results = geneOntologyService.getGenes( goId, tax );
            if ( results != null ) {
                return GeneValueObject.convert2ValueObjects( results );
            }
        }

        return new HashSet<GeneValueObject>();

    }

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return collection of gene value objects
     * @throws IOException
     */
    @Override
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {
        Map<String, Collection<GeneValueObject>> geneMap = searchMultipleGenesGetMap( query, taxonId );
        Collection<GeneValueObject> allGenes = new ArrayList<GeneValueObject>();
        for ( Collection<GeneValueObject> genes : geneMap.values() ) {
            allGenes.addAll( genes );
        }

        return allGenes;
    }

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     * 
     * @param query A list of gene names (symbols), one per line.
     * @param taxonId
     * @return map with each gene-query as a key and a collection of the search-results as the value
     * @throws IOException
     */
    @Override
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
