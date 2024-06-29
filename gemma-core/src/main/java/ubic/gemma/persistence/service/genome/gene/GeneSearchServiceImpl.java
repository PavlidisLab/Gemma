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
package ubic.gemma.persistence.service.genome.gene;

import gemma.gsec.SecurityService;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import ubic.gemma.core.lang.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching genes (and gene sets)
 *
 * @author tvrossum
 */
@Service
public class GeneSearchServiceImpl implements GeneSearchService {

    private static final Log log = LogFactory.getLog( GeneSearchServiceImpl.class );
    private static final int MAX_GENES_PER_QUERY = 1000;
    private static final int MAX_GO_TERMS_TO_PROCESS = 20;
    private static final int MAX_GO_GROUP_SIZE = 200;

    private SearchService searchService;
    private SecurityService securityService;
    private TaxonService taxonService;
    private GeneSetSearch geneSetSearch;
    private GeneSetService geneSetService;
    private GeneService geneService;
    private GeneOntologyService geneOntologyService;
    private GeneSetValueObjectHelper geneSetValueObjectHelper;

    public GeneSearchServiceImpl() {
    }

    @Autowired
    public GeneSearchServiceImpl( SearchService searchService, SecurityService securityService,
            TaxonService taxonService, GeneSetSearch geneSetSearch, GeneSetService geneSetService,
            GeneService geneService, GeneOntologyService geneOntologyService,
            GeneSetValueObjectHelper geneSetValueObjectHelper ) {
        this.searchService = searchService;
        this.securityService = securityService;
        this.taxonService = taxonService;
        this.geneSetSearch = geneSetSearch;
        this.geneSetService = geneSetService;
        this.geneService = geneService;
        this.geneOntologyService = geneOntologyService;
        this.geneSetValueObjectHelper = geneSetValueObjectHelper;
    }

    @Override
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {
        if ( StringUtils.isBlank( goId ) || !goId.toUpperCase().startsWith( "GO" ) ) {
            return Collections.emptySet();
        }
        Taxon tax = taxonService.load( taxonId );
        if ( tax == null ) {
            return Collections.emptySet();
        }
        Collection<Gene> results = geneOntologyService.getGenes( goId, tax );
        if ( results != null ) {
            results = geneService.thawLite( results );
            return geneService.loadValueObjects( results );
        }
        return new HashSet<>();
    }

    @Override
    public Collection<Gene> getGOGroupGenes( String goQuery, @Nullable Taxon taxon ) throws SearchException {
        StopWatch timer = new StopWatch();
        timer.start();
        Collection<Taxon> taxaForPhenotypeAssoc = new ArrayList<>();
        Collection<Gene> genes = new ArrayList<>();
        // if taxon isn't set, get go groups for each possible taxon
        if ( taxon == null ) {
            taxaForPhenotypeAssoc.addAll( taxonService.loadAllTaxaWithGenes() );
        } else {
            taxaForPhenotypeAssoc.add( taxon );
        }

        for ( Taxon taxonForPA : taxaForPhenotypeAssoc ) {
            for ( GeneSet geneSet : geneSetSearch
                    .findByGoTermName( goQuery, taxonForPA, GeneSearchServiceImpl.MAX_GO_TERMS_TO_PROCESS,
                            GeneSearchServiceImpl.MAX_GO_GROUP_SIZE ) ) {
                // don't bother adding empty groups
                if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                    for ( GeneSetMember geneMember : geneSet.getMembers() ) {
                        genes.add( geneMember.getGene() );
                    }
                }
            }
        }
        if ( timer.getTime() > 50 ) {
            GeneSearchServiceImpl.log.warn( "GO search: " + timer.getTime() + "ms" );
        }
        return genes;
    }

    @Override
    public Collection<SearchResultDisplayObject> searchGenesAndGeneGroups( String query, Long taxonId ) throws SearchException {
        Taxon taxon = null;
        String taxonName = "";
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                GeneSearchServiceImpl.log.warn( "No such taxon with id=" + taxonId );
            } else {
                taxonName = taxon.getCommonName();
            }
        }

        List<SearchResultDisplayObject> displayResults = new ArrayList<>();

        // if query is blank, return list of auto generated sets, user-owned sets (if logged in) and user's recent
        // session-bound sets
        if ( StringUtils.isBlank( query ) ) {

            return this.searchGenesAndGeneGroupsBlankQuery( taxonId );

        }

        Integer maxGeneralSearchResults = 500;

        /*
         * GET GENES AND GENE SETS
         */
        // SearchSettings settings = SearchSettings.geneSearch( query, taxon );
        SearchSettings settings = SearchSettings.builder()
                .query( query )
                .resultType( Gene.class )
                .resultType( GeneSet.class )
                .maxResults( maxGeneralSearchResults )
                .taxonConstraint( taxon ) // FIXME: this doesn't work yet
                .build();

        GeneSearchServiceImpl.log.debug( "getting results from searchService for " + query );

        SearchService.SearchResultMap results = searchService.search( settings.withMode( SearchSettings.SearchMode.FAST ) );

        List<SearchResult<GeneSet>> geneSetSearchResults = new ArrayList<>();
        List<SearchResult<Gene>> geneSearchResults = new ArrayList<>();

        boolean exactGeneSymbolMatch = false;
        if ( !results.isEmpty() ) {
            if ( settings.hasResultType( GeneSet.class ) ) {
                geneSetSearchResults.addAll( results.getByResultObjectType( GeneSet.class ) );
            }
            if ( settings.hasResultType( Gene.class ) ) {
                geneSearchResults.addAll( results.getByResultObjectType( Gene.class ) );
            }

            // Check to see if we have an exact match, if so, return earlier abstaining from doing other searches
            for ( SearchResult<Gene> geneResult : results.getByResultObjectType( Gene.class ) ) {
                Gene g = geneResult.getResultObject();
                // aliases too?
                if ( g != null && g.getOfficialSymbol() != null && g.getOfficialSymbol().startsWith( query.trim() ) ) {
                    exactGeneSymbolMatch = true;
                    break;
                }
            }
        }

        Collection<SearchResultDisplayObject> genes = new ArrayList<>();
        Collection<SearchResultDisplayObject> geneSets;

        Map<Long, Boolean> isSetOwnedByUser = new HashMap<>();

        if ( taxon != null ) { // filter search results by taxon

            List<SearchResult<Gene>> taxonCheckedGenes = this.retainGenesOfThisTaxon( taxonId, geneSearchResults );

            // convert result object to a value object to a SearchResultDisplayObject
            for ( SearchResult<Gene> sr : taxonCheckedGenes ) {
                genes.add( new SearchResultDisplayObject( sr ) );
            }

            List<SearchResult<GeneSet>> taxonCheckedSets = this
                    .retainGeneSetsOfThisTaxon( taxonId, geneSetSearchResults, isSetOwnedByUser );

            // convert result object to a value object
            List<SearchResult<DatabaseBackedGeneSetValueObject>> dbsgvo = taxonCheckedSets.stream()
                    .filter( Objects::nonNull )
                    .map( sr -> {
                        try {
                            return sr.withResultObject( geneSetValueObjectHelper.convertToValueObject( sr.getResultObject() ) );
                        } catch ( AccessDeniedException e ) {
                            // ignore gene sets current user is not allowed to see
                            return null;
                        }
                    } )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toList() );
            geneSets = SearchResultDisplayObject.convertSearchResults2SearchResultDisplayObjects( dbsgvo );

            for ( SearchResultDisplayObject srDo : geneSets ) {
                // geneSets were filtered by taxon above:
                // if taxonId for geneSet != taxonId param, then gene set was already removed
                srDo.setTaxonId( taxonId );
                srDo.setTaxonName( taxonName );
            }
        } else { // set the taxon values

            for ( SearchResult<Gene> sr : geneSearchResults ) {
                genes.add( new SearchResultDisplayObject( sr ) );
            }

            geneSets = new ArrayList<>();
            SearchResultDisplayObject srDo;
            for ( SearchResult<GeneSet> sr : geneSetSearchResults ) {
                GeneSet gs = sr.getResultObject();
                if ( gs == null ) {
                    continue;
                }
                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

                taxon = geneSetService.getTaxon( gs );
                GeneSetValueObject gsVo;
                try {
                    gsVo = geneSetValueObjectHelper.convertToValueObject( gs );
                } catch ( AccessDeniedException e ) {
                    // ignore gene sets current user is not allowed to see
                    continue;
                }
                srDo = new SearchResultDisplayObject( gsVo );
                if ( taxon != null ) {
                    srDo.setTaxonId( taxon.getId() );
                    srDo.setTaxonName( taxon.getCommonName() );
                }
                geneSets.add( srDo );
            }
            taxon = null;
        }

        // if a geneSet is owned by the user, mark it as such (used for giving it a special background colour in
        // search results)
        this.setUserOwnedForGeneSets( geneSets, isSetOwnedByUser );

        if ( exactGeneSymbolMatch ) {
            // get summary results
            GeneSearchServiceImpl.log.info( "getting Summary results for " + query );

            List<SearchResultDisplayObject> summaries = this
                    .addEntryForAllResults( query, genes, geneSets, new ArrayList<SearchResultDisplayObject>() );
            displayResults.addAll( summaries );
            displayResults.addAll( genes );
            displayResults.addAll( geneSets );
            return displayResults;
        }

        // get GO group results
        GeneSearchServiceImpl.log.debug( "Getting GO group results for " + query );
        List<SearchResultDisplayObject> srDos = this.getGOGroupResults( query, taxon );


        // get summary results
        GeneSearchServiceImpl.log.debug( "Getting Summary results for " + query );
        List<SearchResultDisplayObject> summaryEntries = this
                .addEntryForAllResults( query, genes, geneSets, srDos );

        // add all results, keeping order of result types
        displayResults.addAll( summaryEntries );
        displayResults.addAll( geneSets );
        displayResults.addAll( srDos );
        displayResults.addAll( genes );

        if ( displayResults.isEmpty() ) {
            GeneSearchServiceImpl.log.info( "No results for search: " + query + " taxon=" + ( ( taxon == null ) ?
                    null :
                    taxon.getCommonName() ) );
            return new HashSet<>();
        }
        GeneSearchServiceImpl.log.info( "Results for search: " + query + ", size=" + displayResults.size() );

        return displayResults;
    }

    @Override
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException, SearchException {

        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        String line;
        Collection<String> queries = new ArrayList<>();
        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) )
                continue;
            if ( queries.size() > GeneSearchServiceImpl.MAX_GENES_PER_QUERY ) {
                GeneSearchServiceImpl.log.warn( "Too many genes, stopping" );
            }
            queries.add( line );
        }

        Map<String, GeneValueObject> geneMap = this.searchMultipleGenesGetMap( queries, taxonId );

        return geneMap.values();
    }

    @Override
    public Map<String, GeneValueObject> searchMultipleGenesGetMap( Collection<String> query, Long taxonId ) throws SearchException {
        Taxon taxon = taxonService.load( taxonId );

        if ( taxon == null )
            throw new IllegalArgumentException( "No such taxon with id=" + taxonId );

        // this deals with the simple cases. For remainder we look a little harder
        Map<String, GeneValueObject> queryToGenes = geneService.findByOfficialSymbols( query, taxonId );

        for ( String line : query ) {
            line = StringUtils.strip( line );

            if ( StringUtils.isBlank( line ) ) {
                continue;
            }

            String queryAsKey = line.toLowerCase();
            if ( queryToGenes.containsKey( queryAsKey ) ) {
                // already found.
                continue;
            }

            if ( queryToGenes.size() >= GeneSearchServiceImpl.MAX_GENES_PER_QUERY ) {
                GeneSearchServiceImpl.log
                        .warn( "Too many genes, stopping (limit=" + GeneSearchServiceImpl.MAX_GENES_PER_QUERY + ')' );
                break;
            }

            // searching one gene at a time is a bit slow; we do a quick search for symbols.
            SearchSettings settings = SearchSettings.geneSearch( line, taxon );
            List<SearchResult<Gene>> geneSearchResults = searchService.search( settings.withMode( SearchSettings.SearchMode.FAST ) ).getByResultObjectType( Gene.class );

            if ( geneSearchResults.isEmpty() ) {
                // an empty set is an indication of no results.
                queryToGenes.put( queryAsKey, null );
            } else if ( geneSearchResults.size() == 1 ) { // Just one result so add it
                Gene g = geneSearchResults.iterator().next().getResultObject();
                if ( g != null ) {
                    queryToGenes.put( queryAsKey, new GeneValueObject( g ) );
                }
            } else { // Multiple results need to find best one
                // Usually if there is more than 1 results the search term was a official symbol and picked up matches
                // like grin1, grin2, grin3, grin (given the search term was grin)
                for ( SearchResult<Gene> sr : geneSearchResults ) {
                    Gene srGene = ( Gene ) sr.getResultObject();
                    if ( srGene != null && srGene.getTaxon().equals( taxon ) && srGene.getOfficialSymbol().equalsIgnoreCase( line ) ) {
                        queryToGenes.put( queryAsKey, new GeneValueObject( srGene ) );
                        break; // found so done
                    }
                }

            }
        }

        return queryToGenes;
    }

    private void setUserOwnedForGeneSets( Collection<SearchResultDisplayObject> geneSets,
            Map<Long, Boolean> isSetOwnedByUser ) {
        if ( SecurityUtil.isUserLoggedIn() ) {
            for ( SearchResultDisplayObject srDo : geneSets ) {
                Long id = ( srDo.getResultValueObject() instanceof DatabaseBackedGeneSetValueObject ) ?
                        ( ( GeneSetValueObject ) srDo.getResultValueObject() ).getId() :
                        new Long( -1 );
                srDo.setUserOwned( isSetOwnedByUser.get( id ) );
            }
        }
    }

    private List<SearchResult<GeneSet>> retainGeneSetsOfThisTaxon( Long taxonId, List<SearchResult<GeneSet>> geneSetSearchResults,
            Map<Long, Boolean> isSetOwnedByUser ) {
        List<SearchResult<GeneSet>> taxonCheckedSets = new ArrayList<>();
        for ( SearchResult<GeneSet> sr : geneSetSearchResults ) {
            GeneSet gs = sr.getResultObject();
            if ( gs != null ) {
                Set<Long> geneSetTaxaIds = geneSetService.getTaxa( gs ).stream()
                        .map( Taxon::getId )
                        .collect( Collectors.toSet() );
                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );
                if ( geneSetTaxaIds.contains( taxonId ) ) {
                    taxonCheckedSets.add( sr );
                }
            }
        }
        return taxonCheckedSets;
    }

    private List<SearchResult<Gene>> retainGenesOfThisTaxon( Long taxonId, List<SearchResult<Gene>> geneSearchResults ) {
        List<SearchResult<Gene>> taxonCheckedGenes = new ArrayList<>();
        for ( SearchResult<Gene> sr : geneSearchResults ) {
            Gene gene = sr.getResultObject();
            if ( gene != null && gene.getTaxon() != null && gene.getTaxon().getId().equals( taxonId ) ) {
                taxonCheckedGenes.add( sr );
            }
        }
        return taxonCheckedGenes;
    }

    /**
     * updates goSets & srDos with GO results
     *
     * @param query query
     * @param taxon taxon
     * @return list of search result display objects
     */
    private List<SearchResultDisplayObject> getGOGroupResults( String query, Taxon taxon ) throws SearchException {
        StopWatch timer = new StopWatch();
        timer.start();
        List<SearchResultDisplayObject> srDos = new ArrayList<>();

        if ( taxon != null ) {

            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSearchServiceImpl.log
                        .debug( "Getting results from geneSetSearch.findByGoId for GO prefixed query: " + query );
                GeneSet goSet = geneSetSearch.findByGoId( query, taxon );
                if ( goSet != null && goSet.getMembers() != null && goSet.getMembers().size() > 0 ) {
                    SearchResultDisplayObject sdo = this.makeGoGroupSearchResult( goSet, query, query, taxon );
                    srDos.add( sdo );
                }
            } else {
                GeneSearchServiceImpl.log.debug( "Getting results from geneSetSearch.findByGoTermName for " + query );
                for ( GeneSet geneSet : geneSetSearch
                        .findByGoTermName( query, taxon, GeneSearchServiceImpl.MAX_GO_TERMS_TO_PROCESS,
                                GeneSearchServiceImpl.MAX_GO_GROUP_SIZE ) ) {
                    // don't bother adding empty GO groups
                    // (should probably do this check elsewhere in case it speeds things up)
                    if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                        SearchResultDisplayObject sdo = this.makeGoGroupSearchResult( geneSet, null, query, taxon );
                        srDos.add( sdo );
                    }
                }
            }
        } else {// taxon is null, search without taxon as a constraint and bag up the results based on taxon

            GeneSearchServiceImpl.log
                    .debug( "getting results from geneSetSearch.findByGoId for GO prefixed query: " + query
                            + " with null taxon" );
            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = geneSetSearch.findByGoId( query, null );
                if ( goSet == null ) {
                    return srDos;
                }

                // this gene set has genes from all the different taxons, organize them
                Collection<GeneSet> taxonSpecificSets = this.organizeMultiTaxaSetIntoTaxonSpecificSets( goSet );

                for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                    if ( taxonGeneSet != null && taxonGeneSet.getMembers() != null
                            && taxonGeneSet.getMembers().size() > 0 ) {
                        SearchResultDisplayObject sdo = this.makeGoGroupSearchResult( taxonGeneSet, query, query,
                                taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                        srDos.add( sdo );
                    }
                }
            } else {
                GeneSearchServiceImpl.log.debug( "getting results from geneSetSearch.findByGoTermName for " + query
                        + " with null taxon" );
                for ( GeneSet geneSet : geneSetSearch
                        .findByGoTermName( query, null, GeneSearchServiceImpl.MAX_GO_TERMS_TO_PROCESS,
                                GeneSearchServiceImpl.MAX_GO_GROUP_SIZE ) ) {

                    // geneSet will have genes from different taxons inside, organize them.
                    Collection<GeneSet> taxonSpecificSets = this.organizeMultiTaxaSetIntoTaxonSpecificSets( geneSet );

                    for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                        if ( geneSet.getMembers() != null && taxonGeneSet.getMembers().size() != 0 ) {
                            SearchResultDisplayObject sdo = this.makeGoGroupSearchResult( taxonGeneSet, null, query,
                                    taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                            srDos.add( sdo );
                        }
                    }
                }
            }

        }

        Collections.sort( srDos );
        if ( timer.getTime() > 500 )
            GeneSearchServiceImpl.log.info( "GO search: " + srDos.size() + " results in " + timer.getTime() + "ms" );
        return srDos;
    }

    private Collection<GeneSet> organizeMultiTaxaSetIntoTaxonSpecificSets( GeneSet gs ) {

        HashMap<Long, GeneSet> taxonToGeneSetMap = new HashMap<>();

        for ( GeneSetMember geneMember : gs.getMembers() ) {

            Long id = geneMember.getGene().getTaxon().getId();
            if ( taxonToGeneSetMap.get( id ) == null ) {

                GeneSet newTaxonSet = GeneSet.Factory.newInstance();

                newTaxonSet.setName( gs.getName() );
                newTaxonSet.setDescription( gs.getDescription() );
                Set<GeneSetMember> members = new HashSet<>();
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

    private SearchResultDisplayObject makeGoGroupSearchResult( GeneSet goSet, String goId, String query,
            Taxon taxonForGo ) {
        GOGroupValueObject ggVo = geneSetValueObjectHelper.convertToGOValueObject( goSet, goId, query );
        return this.getSearchResultForSessionBoundGroupValueObject( taxonForGo, ggVo );
    }

    private SearchResultDisplayObject getSearchResultForSessionBoundGroupValueObject( Taxon taxonForGS,
            SessionBoundGeneSetValueObject sbGsVo ) {
        if ( taxonForGS != null ) {
            // GO groups don't seem to have there sbGsVo's taxon info populated
            sbGsVo.setTaxon( new TaxonValueObject( taxonForGS ) );
        } else {
            sbGsVo.setTaxon( sbGsVo.getTaxon() );
        }
        SearchResultDisplayObject sdo = new SearchResultDisplayObject( sbGsVo );
        sdo.setUserOwned( false );
        sdo.setTaxonId( sbGsVo.getTaxonId() );
        sdo.setTaxonName( sbGsVo.getTaxonName() );
        return sdo;
    }

    /**
     * Get a list of SearchResultDisplayObjects that summarise all the results found (one per taxon), so at front end
     * user can "select all"
     *
     * @param query          query
     * @param genes          genes
     * @param geneSets       gene sets
     * @param srDos          display objects
     * @return list of search result display objects
     */
    private List<SearchResultDisplayObject> addEntryForAllResults( String query,
            Collection<SearchResultDisplayObject> genes, Collection<SearchResultDisplayObject> geneSets,
            List<SearchResultDisplayObject> srDos ) {

        List<SearchResultDisplayObject> summaryResultEntries = new ArrayList<>();

        /*
         * ALL RESULTS BY TAXON GROUPS
         */

        // if >1 result, add a group whose members are all genes returned from search
        if ( ( genes.size() + geneSets.size() + srDos.size() ) > 1 ) {

            Map<Long, Set<Long>> geneIdsByTaxonId = new HashMap<>();

            // add every individual gene to the set
            for ( SearchResultDisplayObject srDo : genes ) {
                if ( !geneIdsByTaxonId.containsKey( srDo.getTaxonId() ) ) {
                    geneIdsByTaxonId.put( srDo.getTaxonId(), new HashSet<Long>() );
                }
                Long id = ( srDo.getResultValueObject() instanceof GeneValueObject ) ?
                        ( ( GeneValueObject ) srDo.getResultValueObject() ).getId() :
                        new Long( -1 );
                if ( id != -1 ) {
                    geneIdsByTaxonId.get( srDo.getTaxonId() ).add( id );
                }

            }

            // if there's a group, get the number of members
            this.updateGeneIdsByTaxonId( geneSets, geneIdsByTaxonId );
            this.updateGeneIdsByTaxonId( srDos, geneIdsByTaxonId );

            // make an entry for each taxon
            Long taxonId;
            Taxon taxon;
            for ( Map.Entry<Long, Set<Long>> entry : geneIdsByTaxonId.entrySet() ) {
                taxonId = entry.getKey();
                assert taxonId != null;
                taxon = taxonService.load( taxonId );

                // don't make groups for 1 gene
                if ( taxon != null && entry.getValue().size() > 1 ) {
                    FreeTextGeneResultsValueObject byTaxFtVo = new FreeTextGeneResultsValueObject(
                            "All " + taxon.getCommonName() + " results for '" + query + "'",
                            "All " + taxon.getCommonName() + " genes found for your query", taxon.getId(),
                            taxon.getCommonName(), entry.getValue(), query );
                    summaryResultEntries.add( new SearchResultDisplayObject( byTaxFtVo ) );
                }
            }
        }
        return summaryResultEntries;
    }

    private void updateGeneIdsByTaxonId( Collection<SearchResultDisplayObject> searchResultDisplayObject,
            Map<Long, Set<Long>> geneIdsByTaxonId ) {
        for ( SearchResultDisplayObject srDo : searchResultDisplayObject ) {
            // get the ids of the gene members
            if ( !geneIdsByTaxonId.containsKey( srDo.getTaxonId() ) ) {
                geneIdsByTaxonId.put( srDo.getTaxonId(), new HashSet<Long>() );
            }

            Object resultValueObject = srDo.getResultValueObject();

            if ( resultValueObject instanceof GeneValueObject ) {
                geneIdsByTaxonId.get( srDo.getTaxonId() ).add( ( ( GeneValueObject ) resultValueObject ).getId() );

            } else if ( resultValueObject instanceof GeneSetValueObject ) {
                GeneSetValueObject gsvo = ( GeneSetValueObject ) resultValueObject;
                if ( gsvo.getGeneIds() != null ) {
                    geneIdsByTaxonId.get( srDo.getTaxonId() ).addAll( gsvo.getGeneIds() );
                }

            } else {
                throw new UnsupportedOperationException(
                        "Unknown search result type: " + resultValueObject.getClass().getName() );
            }

        }
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller.genome.gene.GenePickerController.searchGenesAndGeneGroups(String, Long)
     */
    private Collection<SearchResultDisplayObject> searchGenesAndGeneGroupsBlankQuery( Long taxonId ) {

        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
            if ( taxon == null ) {
                GeneSearchServiceImpl.log.warn( "No such taxon with id=" + taxonId );
            }
        }

        // if query is blank, return list of auto generated sets, user-owned sets (if logged in) and user's recent
        // session-bound sets

        // right now, no public gene sets are useful so we don't want to prompt them

        StopWatch watch = new StopWatch();
        watch.start();

        // get all public sets
        // filtered by security.
        Collection<GeneSet> sets = new ArrayList<>();
        if ( !SecurityUtil.isUserLoggedIn() ) {
            try {
                sets = geneSetService.loadAll( taxon );
                if ( watch.getTime() > 100 )
                    GeneSearchServiceImpl.log
                            .info( sets.size() + " sets loaded for taxon =" + taxon + " took: " + watch.getTime()
                                    + "ms" );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else if ( SecurityUtil.isUserLoggedIn() ) {
            /*
             * actually, loadMyGeneSets and loadAll point to the same method (they just use different spring security
             * filters)
             */
            sets = ( taxon != null ) ? geneSetService.loadMyGeneSets( taxon ) : geneSetService.loadMyGeneSets();
            if ( watch.getTime() > 100 )
                GeneSearchServiceImpl.log.info( "Loading the user's gene sets took: " + watch.getTime() + "ms" );
        }

        if ( sets.isEmpty() ) {
            return new ArrayList<>();
        }

        // separate these out because they go at the top of the list
        List<SearchResultDisplayObject> displayResultsPrivate = new ArrayList<>();
        List<SearchResultDisplayObject> displayResultsPublic = new ArrayList<>();
        SearchResultDisplayObject newSrDo;

        List<DatabaseBackedGeneSetValueObject> valueObjects = geneSetValueObjectHelper
                .convertToValueObjects( sets, false );
        if ( watch.getTime() > 500 )
            GeneSearchServiceImpl.log.info( "Database stage done: " + watch.getTime() + "ms" );

        for ( DatabaseBackedGeneSetValueObject set : valueObjects ) {
            newSrDo = new SearchResultDisplayObject( set );
            newSrDo.setTaxonId( ( ( GeneSetValueObject ) newSrDo.getResultValueObject() ).getTaxonId() );
            newSrDo.setTaxonName( ( ( GeneSetValueObject ) newSrDo.getResultValueObject() ).getTaxonName() );
            newSrDo.setUserOwned( !set.getIsPublic() );
            ( ( GeneSetValueObject ) newSrDo.getResultValueObject() ).setIsPublic( !newSrDo.isUserOwned() );
            if ( newSrDo.isUserOwned() ) {
                displayResultsPrivate.add( newSrDo );
            } else {
                displayResultsPublic.add( newSrDo );
            }
        }

        // keep sets in proper order (user's groups first, then public ones)
        Collections.sort( displayResultsPrivate );
        Collections.sort( displayResultsPublic );

        List<SearchResultDisplayObject> displayResults = new ArrayList<>();

        displayResults.addAll( displayResultsPrivate );
        displayResults.addAll( displayResultsPublic );

        GeneSearchServiceImpl.log
                .info( "Results for blank query: " + displayResults.size() + " items, " + watch.getTime() + "ms" );

        return displayResults;

    }
}
