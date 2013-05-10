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
import ubic.gemma.genome.gene.GeneSetValueObjectHelper;
import ubic.gemma.genome.gene.PhenotypeGroupValueObject;
import ubic.gemma.genome.gene.SessionBoundGeneSetValueObject;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetMember;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.search.GeneSetSearch;
import ubic.gemma.search.SearchResult;
import ubic.gemma.search.SearchResultDisplayObject;
import ubic.gemma.search.SearchService;
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

    private static final int MAX_GO_TERMS_TO_PROCESS = 20;

    private static final int MAX_GO_GROUP_SIZE = 200;

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
    private GeneService geneService;

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

        /*
         * GET GENES AND GENESETS
         */
        // SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( query );
        settings.noSearches();
        settings.setSearchGenes( true ); // add searching for genes
        settings.setSearchGeneSets( true ); // add searching for geneSets
        settings.setMaxResults( maxGeneralSearchResults );
        if ( taxon != null ) settings.setTaxon( taxon ); // this doesn't work yet

        log.debug( "getting results from searchService for " + query );

        Map<Class<?>, List<SearchResult>> results = searchService.speedSearch( settings );

        List<SearchResult> geneSetSearchResults = new ArrayList<SearchResult>();
        List<SearchResult> geneSearchResults = new ArrayList<SearchResult>();

        if ( results.get( GeneSet.class ) != null ) {
            geneSetSearchResults.addAll( results.get( GeneSet.class ) );
        }
        if ( results.get( Gene.class ) != null ) {
            geneSearchResults.addAll( results.get( Gene.class ) );
        }

        // Check to see if we have an exact match, if so, return earlier abstaining from doing other searches
        boolean exactGeneSymbolMatch = false;
        for ( SearchResult geneResult : results.get( Gene.class ) ) {
            Gene g = ( Gene ) geneResult.getResultObject();
            // aliases too?
            if ( g != null && g.getOfficialSymbol() != null && g.getOfficialSymbol().startsWith( query.trim() ) ) {
                exactGeneSymbolMatch = true;
                break;
            }
        }

        Collection<SearchResultDisplayObject> genes = new ArrayList<SearchResultDisplayObject>();
        Collection<SearchResultDisplayObject> geneSets = new ArrayList<SearchResultDisplayObject>();

        Map<Long, Boolean> isSetOwnedByUser = new HashMap<Long, Boolean>();

        if ( taxon != null ) { // filter search results by taxon

            List<SearchResult> taxonCheckedGenes = retainGenesOfThisTaxon( taxonId, geneSearchResults );

            // convert result object to a value object to a SearchResultDisplayObject
            for ( SearchResult sr : taxonCheckedGenes ) {
                genes.add( new SearchResultDisplayObject( sr ) );
            }

            List<SearchResult> taxonCheckedSets = retainGeneSetsOfThisTaxon( taxonId, geneSetSearchResults,
                    isSetOwnedByUser );

            // convert result object to a value object
            for ( SearchResult sr : taxonCheckedSets ) {
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

            for ( SearchResult sr : geneSearchResults ) {
                genes.add( new SearchResultDisplayObject( sr ) );
            }

            geneSets = new ArrayList<SearchResultDisplayObject>();
            SearchResultDisplayObject srdo = null;
            for ( SearchResult sr : geneSetSearchResults ) {

                GeneSet gs = ( GeneSet ) sr.getResultObject();
                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

                taxon = geneSetService.getTaxon( ( GeneSet ) sr.getResultObject() );
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
        setUserOwnedForGeneSets( geneSets, isSetOwnedByUser );

        if ( exactGeneSymbolMatch ) {
            // get summary results
            log.info( "getting Summary results for " + query );

            List<SearchResultDisplayObject> summarys = addEntryForAllResults( query, genes, geneSets,
                    new ArrayList<SearchResultDisplayObject>(), new ArrayList<SearchResultDisplayObject>() );
            displayResults.addAll( summarys );
            displayResults.addAll( genes );
            displayResults.addAll( geneSets );
            return displayResults;
        }

        ArrayList<SearchResultDisplayObject> goSRDOs = new ArrayList<SearchResultDisplayObject>();
        // get GO group results
        log.debug( "Getting GO group results for " + query );
        goSRDOs = getGOGroupResults( query, taxon, MAX_GO_TERMS_TO_PROCESS, MAX_GO_GROUP_SIZE );

        ArrayList<SearchResultDisplayObject> phenotypeSRDOs = new ArrayList<SearchResultDisplayObject>();

        // only do phenotype search if there is no results at all
        if ( ( genes.size() < 1 ) ) {

            if ( !query.toUpperCase().startsWith( "GO" ) ) {
                log.info( "getting Phenotype Association results for " + query );
                phenotypeSRDOs = getPhenotypeAssociationSearchResults( query, taxon );
            }

        }
        // get summary results
        log.debug( "Getting Summary results for " + query );
        List<SearchResultDisplayObject> summaryEntries = addEntryForAllResults( query, genes, geneSets, goSRDOs,
                phenotypeSRDOs );

        // add all results, keeping order of result types
        displayResults.addAll( summaryEntries );
        displayResults.addAll( geneSets );
        displayResults.addAll( goSRDOs );
        displayResults.addAll( phenotypeSRDOs );
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
     * @param geneSets
     * @param isSetOwnedByUser
     */
    private void setUserOwnedForGeneSets( Collection<SearchResultDisplayObject> geneSets,
            Map<Long, Boolean> isSetOwnedByUser ) {
        if ( SecurityServiceImpl.isUserLoggedIn() ) {
            for ( SearchResultDisplayObject srdo : geneSets ) {
                Long id = ( srdo.getResultValueObject() instanceof DatabaseBackedGeneSetValueObject ) ? ( ( GeneSetValueObject ) srdo
                        .getResultValueObject() ).getId() : new Long( -1 );
                srdo.setUserOwned( isSetOwnedByUser.get( id ) );
            }
        }
    }

    /**
     * @param taxonId
     * @param geneSetSearchResults
     * @param isSetOwnedByUser
     * @return
     */
    private List<SearchResult> retainGeneSetsOfThisTaxon( Long taxonId, List<SearchResult> geneSetSearchResults,
            Map<Long, Boolean> isSetOwnedByUser ) {
        List<SearchResult> taxonCheckedSets = new ArrayList<SearchResult>();
        for ( SearchResult sr : geneSetSearchResults ) {
            GeneSet gs = ( GeneSet ) sr.getResultObject();
            GeneSetValueObject gsvo = geneSetValueObjectHelper.convertToValueObject( gs );

            isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

            if ( gsvo.getTaxonId() == taxonId ) {
                taxonCheckedSets.add( sr );
            }
        }
        return taxonCheckedSets;
    }

    /**
     * @param taxonId
     * @param geneSearchResults
     * @return
     */
    private List<SearchResult> retainGenesOfThisTaxon( Long taxonId, List<SearchResult> geneSearchResults ) {
        List<SearchResult> taxonCheckedGenes = new ArrayList<SearchResult>();
        for ( SearchResult sr : geneSearchResults ) {
            Gene gene = ( Gene ) sr.getResultObject();
            if ( gene.getTaxon() != null && gene.getTaxon().getId().equals( taxonId ) ) {
                taxonCheckedGenes.add( sr );
            }
        }
        return taxonCheckedGenes;
    }

    /**
     * updates goSets & goSRDOs with GO results
     * 
     * @param query
     * @param taxon
     * @param maxGoTermsProcessed
     * @param maxGeneSetSize
     * @param goSets
     * @param goSRDOs
     */
    private ArrayList<SearchResultDisplayObject> getGOGroupResults( String query, Taxon taxon,
            Integer maxGoTermsProcessed, Integer maxGeneSetSize ) {

        ArrayList<SearchResultDisplayObject> goSRDOs = new ArrayList<SearchResultDisplayObject>();

        if ( taxon != null ) {

            if ( query.toUpperCase().startsWith( "GO" ) ) {
                log.debug( "Getting results from geneSetSearch.findByGoId for GO prefixed query: " + query );
                GeneSet goSet = geneSetSearch.findByGoId( query, taxon );
                if ( goSet != null && goSet.getMembers() != null && goSet.getMembers().size() > 0 ) {
                    SearchResultDisplayObject sdo = makeGoGroupSearchResult( goSet, query, query, taxon );
                    goSRDOs.add( sdo );
                }
            } else {
                log.debug( "Getting results from geneSetSearch.findByGoTermName for " + query );
                for ( GeneSet geneSet : geneSetSearch.findByGoTermName( query, taxon, maxGoTermsProcessed,
                        maxGeneSetSize ) ) {
                    // don't bother adding empty GO groups
                    // (should probably do this check elsewhere in case it speeds things up)
                    if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                        SearchResultDisplayObject sdo = makeGoGroupSearchResult( geneSet, null, query, taxon );
                        goSRDOs.add( sdo );
                    }
                }
            }
        } else {// taxon is null, search without taxon as a constraint and bag up the results based on taxon

            log.info( "getting results from geneSetSearch.findByGoId for GO prefixed query: " + query
                    + " with null taxon" );
            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = geneSetSearch.findByGoId( query, taxon );
                if ( goSet == null ) {
                    return goSRDOs;
                }

                // this geneset has genes from all the different taxons, organize them
                Collection<GeneSet> taxonSpecificSets = organizeMultiTaxaSetIntoTaxonSpecificSets( goSet );

                for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                    if ( taxonGeneSet != null && taxonGeneSet.getMembers() != null
                            && taxonGeneSet.getMembers().size() > 0 ) {
                        SearchResultDisplayObject sdo = makeGoGroupSearchResult( taxonGeneSet, query, query,
                                taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                        goSRDOs.add( sdo );
                    }
                }
            } else {
                log.info( "getting results from geneSetSearch.findByGoTermName for " + query + " with null taxon" );
                for ( GeneSet geneSet : geneSetSearch.findByGoTermName( query, taxon, maxGoTermsProcessed,
                        maxGeneSetSize ) ) {

                    // geneSet will have genes from different taxons inside, organize them.
                    Collection<GeneSet> taxonSpecificSets = organizeMultiTaxaSetIntoTaxonSpecificSets( geneSet );

                    for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                        if ( geneSet.getMembers() != null && taxonGeneSet.getMembers().size() != 0 ) {
                            SearchResultDisplayObject sdo = makeGoGroupSearchResult( taxonGeneSet, null, query,
                                    taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                            goSRDOs.add( sdo );
                        }
                    }
                }
            }

        }

        Collections.sort( goSRDOs );
        return goSRDOs;
    }

    private Collection<GeneSet> organizeMultiTaxaSetIntoTaxonSpecificSets( GeneSet gs ) {

        HashMap<Long, GeneSet> taxonToGeneSetMap = new HashMap<Long, GeneSet>();

        for ( GeneSetMember geneMember : gs.getMembers() ) {

            Long id = geneMember.getGene().getTaxon().getId();
            if ( taxonToGeneSetMap.get( id ) == null ) {

                GeneSet newTaxonSet = GeneSet.Factory.newInstance();

                newTaxonSet.setName( gs.getName() );
                newTaxonSet.setDescription( gs.getDescription() );
                Collection<GeneSetMember> members = new ArrayList<GeneSetMember>();
                members.add( geneMember );

                newTaxonSet.setMembers( members );

                taxonToGeneSetMap.put( id, newTaxonSet );

            } else {
                GeneSet existingTaxonSet = taxonToGeneSetMap.get( id );

                existingTaxonSet.getMembers().add( geneMember );
            }

        }

        return taxonToGeneSetMap.values();
    }

    /**
     * updates goSets & goSRDOs with GO results
     * 
     * @param query
     * @param taxon
     * @param maxGoTermsProcessed
     * @param goSets
     * @param goSRDOs
     */
    private ArrayList<SearchResultDisplayObject> getPhenotypeAssociationSearchResults( String query, Taxon taxon ) {

        ArrayList<SearchResultDisplayObject> phenotypeSRDOs = new ArrayList<SearchResultDisplayObject>();
        // if taxon==null then it grabs results for all taxons
        Collection<GeneSetValueObject> geneSets = geneSetSearch.findByPhenotypeName( query, taxon );
        for ( GeneSetValueObject geneSet : geneSets ) {
            // don't bother adding empty groups
            // (should probably do this check elsewhere in case it speeds things up)
            if ( geneSet.getGeneIds() != null && geneSet.getGeneIds().size() != 0 ) {
                SearchResultDisplayObject sdo = makePhenotypeAssociationGroupSearchResult( geneSet, query, taxon );
                phenotypeSRDOs.add( sdo );
                // phenotypeSets.add( geneSet );
            }
        }

        Collections.sort( phenotypeSRDOs );
        return phenotypeSRDOs;
    }

    /**
     * Get all genes that are associated with phenotypes that match the query string param. If taxon is not specified
     * (null), genes of all taxa will be returned.
     * 
     * @param phenptypeQuery
     * @param taxon can be null
     */
    @Override
    public Collection<Gene> getPhenotypeAssociatedGenes( String phenptypeQuery, Taxon taxon ) {
        Collection<Taxon> taxaForPhenotypeAssoc = new ArrayList<Taxon>();
        Collection<Gene> genes = new ArrayList<Gene>();
        // if taxon isn't set, get go groups for each possible taxon
        if ( taxon == null ) {
            taxaForPhenotypeAssoc.addAll( taxonService.loadAllTaxaWithGenes() );
        } else {
            taxaForPhenotypeAssoc.add( taxon );
        }

        // TODO FIX THIS SO GENES ARE RETURNED DIRECTLY (or fix caller to use gene ids)
        for ( Taxon taxonForPA : taxaForPhenotypeAssoc ) {
            for ( GeneSetValueObject geneSet : geneSetSearch.findByPhenotypeName( phenptypeQuery, taxonForPA ) ) {
                // don't bother adding empty groups
                if ( geneSet.getGeneIds() != null && geneSet.getGeneIds().size() != 0 ) {
                    for ( Long id : geneSet.getGeneIds() ) {
                        genes.add( geneService.load( id ) );
                    }
                }
            }
        }

        return genes;
    }

    /**
     * Get all genes that belong to GO groups that match the query string param. If taxon is not specified (null), genes
     * of all taxa will be returned.
     * 
     * @param GO Query
     * @param taxon can be null
     */
    @Override
    public Collection<Gene> getGOGroupGenes( String goQuery, Taxon taxon ) {
        Collection<Taxon> taxaForPhenotypeAssoc = new ArrayList<Taxon>();
        Collection<Gene> genes = new ArrayList<Gene>();
        // if taxon isn't set, get go groups for each possible taxon
        if ( taxon == null ) {
            taxaForPhenotypeAssoc.addAll( taxonService.loadAllTaxaWithGenes() );
        } else {
            taxaForPhenotypeAssoc.add( taxon );
        }

        // TODO Don't loop over taxa
        for ( Taxon taxonForPA : taxaForPhenotypeAssoc ) {
            for ( GeneSet geneSet : geneSetSearch.findByGoTermName( goQuery, taxonForPA, MAX_GO_TERMS_TO_PROCESS,
                    MAX_GO_GROUP_SIZE ) ) {
                // don't bother adding empty groups
                if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                    for ( GeneSetMember geneMember : geneSet.getMembers() ) {
                        genes.add( geneMember.getGene() );
                    }
                }
            }
        }

        return genes;
    }

    /**
     * @param goSet
     * @param query
     * @param taxonForGo
     * @return
     */
    private SearchResultDisplayObject makeGoGroupSearchResult( GeneSet goSet, String goId, String query,
            Taxon taxonForGo ) {
        GOGroupValueObject ggvo = geneSetValueObjectHelper.convertToGOValueObject( goSet, goId, query );
        return getSearchResultForSessionBoundGroupValueObject( taxonForGo, ggvo );
    }

    /**
     * @param geneSet
     * @param query
     * @param taxonForGS
     * @return
     */
    private SearchResultDisplayObject makePhenotypeAssociationGroupSearchResult( GeneSetValueObject geneSet,
            String query, Taxon taxonForGS ) {
        PhenotypeGroupValueObject pgvo = PhenotypeGroupValueObject.convertFromGeneSetValueObject( geneSet, query );
        return getSearchResultForSessionBoundGroupValueObject( taxonForGS, pgvo );
    }

    /**
     * @param taxonForGS
     * @param pgvo
     * @return
     */
    private SearchResultDisplayObject getSearchResultForSessionBoundGroupValueObject( Taxon taxonForGS,
            SessionBoundGeneSetValueObject sbgsvo ) {

        if ( taxonForGS != null ) {
            // GO groups don't seem to have there sbgsvo's taxon info populated
            sbgsvo.setTaxonId( taxonForGS.getId() );
            sbgsvo.setTaxonName( taxonForGS.getCommonName() );

        } else {
            sbgsvo.setTaxonId( sbgsvo.getTaxonId() );
            sbgsvo.setTaxonName( sbgsvo.getTaxonName() );
        }
        SearchResultDisplayObject sdo = new SearchResultDisplayObject( sbgsvo );
        sdo.setUserOwned( false );
        sdo.setTaxonId( sbgsvo.getTaxonId() );
        sdo.setTaxonName( sbgsvo.getTaxonName() );
        return sdo;
    }

    /**
     * Get a list of SearchResultDisplayObjects that summarise all the results found (one per taxon)
     * 
     * @param query
     * @param genes
     * @param geneSets
     * @param goSRDOs
     * @param phenotypeSRDOs TODO
     * @param taxon1
     * @return
     */
    private List<SearchResultDisplayObject> addEntryForAllResults( String query,
            Collection<SearchResultDisplayObject> genes, Collection<SearchResultDisplayObject> geneSets,
            ArrayList<SearchResultDisplayObject> goSRDOs, ArrayList<SearchResultDisplayObject> phenotypeSRDOs ) {

        List<SearchResultDisplayObject> summaryResultEntries = new ArrayList<SearchResultDisplayObject>();

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all genes returned from search
        if ( ( genes.size() + geneSets.size() + goSRDOs.size() ) > 1 ) {

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

            updateGeneIdsByTaxonId( geneSets, geneIdsByTaxonId );

            updateGeneIdsByTaxonId( goSRDOs, geneIdsByTaxonId );

            updateGeneIdsByTaxonId( phenotypeSRDOs, geneIdsByTaxonId );

            // make an entry for each taxon

            Long taxonId = null;
            Taxon taxon;
            for ( Map.Entry<Long, HashSet<Long>> entry : geneIdsByTaxonId.entrySet() ) {
                taxonId = entry.getKey();
                taxon = taxonService.load( taxonId );

                // don't make groups for 1 gene
                if ( taxon != null && entry.getValue().size() > 1 ) {
                    FreeTextGeneResultsValueObject byTaxFTVO = new FreeTextGeneResultsValueObject( "All "
                            + taxon.getCommonName() + " results for '" + query + "'", "All " + taxon.getCommonName()
                            + " genes found for your query", taxon.getId(), taxon.getCommonName(), entry.getValue(),
                            query );
                    summaryResultEntries.add( new SearchResultDisplayObject( byTaxFTVO ) );
                }
            }
        }
        return summaryResultEntries;
    }

    /**
     * @param phenotypeSRDOs
     * @param geneIdsByTaxonId
     */
    private void updateGeneIdsByTaxonId( Collection<SearchResultDisplayObject> phenotypeSRDOs,
            HashMap<Long, HashSet<Long>> geneIdsByTaxonId ) {
        for ( SearchResultDisplayObject srdo : phenotypeSRDOs ) {
            // get the ids of the gene members
            if ( !geneIdsByTaxonId.containsKey( srdo.getTaxonId() ) ) {
                geneIdsByTaxonId.put( srdo.getTaxonId(), new HashSet<Long>() );
            }
            geneIdsByTaxonId.get( srdo.getTaxonId() ).addAll( srdo.getMemberIds() );
        }
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
            ( ( GeneSetValueObject ) newSRDO.getResultValueObject() ).setIsPublic( !isPrivate );
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
            log.info( "No results for blank query search, taxon=" + ( ( taxon == null ) ? null : taxon.getCommonName() ) );
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
    @Override
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

            line = StringUtils.strip( line );
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
            SearchSettings settings = SearchSettingsImpl.geneSearch( line, taxon );
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
