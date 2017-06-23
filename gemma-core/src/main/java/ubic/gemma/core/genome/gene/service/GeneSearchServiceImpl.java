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
package ubic.gemma.core.genome.gene.service;

import gemma.gsec.SecurityService;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import ubic.gemma.core.genome.gene.*;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.GeneSetSearch;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultDisplayObject;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsImpl;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.*;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

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
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( query );
        settings.noSearches();
        settings.setSearchGenes( true ); // add searching for genes
        settings.setSearchGeneSets( true ); // add searching for geneSets
        settings.setMaxResults( maxGeneralSearchResults );
        if ( taxon != null )
            settings.setTaxon( taxon ); // this doesn't work yet

        log.debug( "getting results from searchService for " + query );

        Map<Class<?>, List<SearchResult>> results = searchService.speedSearch( settings );

        List<SearchResult> geneSetSearchResults = new ArrayList<>();
        List<SearchResult> geneSearchResults = new ArrayList<>();

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

        Collection<SearchResultDisplayObject> genes = new ArrayList<>();
        Collection<SearchResultDisplayObject> geneSets;

        Map<Long, Boolean> isSetOwnedByUser = new HashMap<>();

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
                DatabaseBackedGeneSetValueObject gsVo = geneSetValueObjectHelper.convertToValueObject( g );
                sr.setResultObject( gsVo );
            }
            geneSets = SearchResultDisplayObject.convertSearchResults2SearchResultDisplayObjects( taxonCheckedSets );

            for ( SearchResultDisplayObject srDo : geneSets ) {
                // geneSets were filtered by taxon above:
                // if taxonId for geneSet != taxonId param, then gene set was already removed
                srDo.setTaxonId( taxonId );
                srDo.setTaxonName( taxonName );
            }
        } else { // set the taxon values

            for ( SearchResult sr : geneSearchResults ) {
                genes.add( new SearchResultDisplayObject( sr ) );
            }

            geneSets = new ArrayList<>();
            SearchResultDisplayObject srDo;
            for ( SearchResult sr : geneSetSearchResults ) {
                GeneSet gs = ( GeneSet ) sr.getResultObject();
                isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

                taxon = geneSetService.getTaxon( ( GeneSet ) sr.getResultObject() );
                GeneSetValueObject gsVo = geneSetValueObjectHelper.convertToValueObject( gs );
                srDo = new SearchResultDisplayObject( gsVo );
                srDo.setTaxonId( taxon.getId() );
                srDo.setTaxonName( taxon.getCommonName() );
                geneSets.add( srDo );
            }
            taxon = null;
        }

        // if a geneSet is owned by the user, mark it as such (used for giving it a special background colour in
        // search results)
        setUserOwnedForGeneSets( geneSets, isSetOwnedByUser );

        if ( exactGeneSymbolMatch ) {
            // get summary results
            log.info( "getting Summary results for " + query );

            List<SearchResultDisplayObject> summaries = addEntryForAllResults( query, genes, geneSets,
                    new ArrayList<SearchResultDisplayObject>(), new ArrayList<SearchResultDisplayObject>() );
            displayResults.addAll( summaries );
            displayResults.addAll( genes );
            displayResults.addAll( geneSets );
            return displayResults;
        }

        List<SearchResultDisplayObject> srDos;
        // get GO group results
        log.debug( "Getting GO group results for " + query );
        srDos = getGOGroupResults( query, taxon );

        List<SearchResultDisplayObject> phenotypeSrDos = new ArrayList<>();

        // only do phenotype search if there is no results at all
        // if ( ( genes.size() < 1 ) ) {

        if ( !query.toUpperCase().startsWith( "GO" ) ) {
            log.info( "getting Phenotype Association results for " + query );
            phenotypeSrDos = getPhenotypeAssociationSearchResults( query, taxon );
        }

        // }

        // get summary results
        log.debug( "Getting Summary results for " + query );
        List<SearchResultDisplayObject> summaryEntries = addEntryForAllResults( query, genes, geneSets, srDos,
                phenotypeSrDos );

        // add all results, keeping order of result types
        displayResults.addAll( summaryEntries );
        displayResults.addAll( geneSets );
        displayResults.addAll( srDos );
        displayResults.addAll( phenotypeSrDos );
        displayResults.addAll( genes );

        if ( displayResults.isEmpty() ) {
            log.info( "No results for search: " + query + " taxon=" + ( ( taxon == null ) ?
                    null :
                    taxon.getCommonName() ) );
            return new HashSet<>();
        }
        log.info( "Results for search: " + query + ", size=" + displayResults.size() );

        return displayResults;
    }

    /**
     * Get all genes that are associated with phenotypes that match the query string param. If taxon is not specified
     * (null), genes of all taxa will be returned. FIXME not used?
     *
     * @param taxon can be null
     * @deprecated not used
     */
    @Deprecated
    @Override
    public Collection<Gene> getPhenotypeAssociatedGenes( String phenotypeQuery, Taxon taxon ) {
        Collection<Taxon> taxaForPhenotypeAssoc = new ArrayList<>();
        Collection<Gene> genes = new ArrayList<>();
        // if taxon isn't set, get go groups for each possible taxon
        if ( taxon == null ) {
            taxaForPhenotypeAssoc.addAll( taxonService.loadAllTaxaWithGenes() );
        } else {
            taxaForPhenotypeAssoc.add( taxon );
        }

        // FIX THIS SO GENES ARE RETURNED DIRECTLY (or fix caller to use gene ids)
        for ( Taxon taxonForPA : taxaForPhenotypeAssoc ) {
            for ( GeneSetValueObject geneSet : geneSetSearch.findByPhenotypeName( phenotypeQuery, taxonForPA ) ) {
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
     * @param taxon can be null
     */
    @Override
    public Collection<Gene> getGOGroupGenes( String goQuery, Taxon taxon ) {
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

        // TODO Don't loop over taxa
        for ( Taxon taxonForPA : taxaForPhenotypeAssoc ) {
            for ( GeneSet geneSet : geneSetSearch
                    .findByGoTermName( goQuery, taxonForPA, MAX_GO_TERMS_TO_PROCESS, MAX_GO_GROUP_SIZE ) ) {
                // don't bother adding empty groups
                if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                    for ( GeneSetMember geneMember : geneSet.getMembers() ) {
                        genes.add( geneMember.getGene() );
                    }
                }
            }
        }
        log.info( "GO search: " + timer.getTime() + "ms" );
        return genes;
    }

    /**
     * get all genes in the given taxon that are annotated with the given go id, including its child terms in the
     * hierarchy
     *
     * @param goId    GO id that must be in the format "GO_#######"
     * @param taxonId must not be null and must correspond to a taxon
     * @return Collection<GeneSetValueObject> empty if goId was blank or taxonId didn't correspond to a taxon
     */
    @Override
    public Collection<GeneValueObject> getGenesByGOId( String goId, Long taxonId ) {

        Taxon tax = taxonService.load( taxonId );

        if ( !StringUtils.isBlank( goId ) && tax != null && goId.toUpperCase().startsWith( "GO" ) ) {

            Collection<Gene> results = geneOntologyService.getGenes( goId, tax );
            if ( results != null ) {
                results = geneService.thawLite( results );
                return geneService.loadValueObjects( results );
            }
        }

        return new HashSet<>();

    }

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param query A list of gene names (symbols), one per line.
     * @return collection of gene value objects
     */
    @Override
    public Collection<GeneValueObject> searchMultipleGenes( String query, Long taxonId ) throws IOException {

        BufferedReader reader = new BufferedReader( new StringReader( query ) );
        String line;
        Collection<String> queries = new ArrayList<>();
        while ( ( line = reader.readLine() ) != null ) {
            if ( StringUtils.isBlank( line ) )
                continue;
            if ( queries.size() > MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping" );
            }
            queries.add( line );
        }

        Map<String, GeneValueObject> geneMap = searchMultipleGenesGetMap( queries, taxonId );

        return geneMap.values();
    }

    /**
     * Search for multiple genes at once. This attempts to limit the number of genes per query to only one.
     *
     * @param query A list of gene names (symbols), one per line.
     * @return map with each gene-query as a key (toLowerCase()) and a collection of the search-results as the value
     */
    @Override
    public Map<String, GeneValueObject> searchMultipleGenesGetMap( Collection<String> query, Long taxonId )
            throws IOException {
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

            if ( queryToGenes.size() >= MAX_GENES_PER_QUERY ) {
                log.warn( "Too many genes, stopping (limit=" + MAX_GENES_PER_QUERY + ')' );
                break;
            }

            // searching one gene at a time is a bit slow; we do a quick search for symbols.
            SearchSettings settings = SearchSettingsImpl.geneSearch( line, taxon );
            List<SearchResult> geneSearchResults = searchService.speedSearch( settings ).get( Gene.class );

            if ( geneSearchResults == null || geneSearchResults.isEmpty() ) {
                // an empty set is an indication of no results.
                queryToGenes.put( queryAsKey, null );
            } else if ( geneSearchResults.size() == 1 ) { // Just one result so add it
                Gene g = ( Gene ) geneSearchResults.iterator().next().getResultObject();
                queryToGenes.put( queryAsKey, new GeneValueObject( g ) );
            } else { // Multiple results need to find best one
                // Usually if there is more than 1 results the search term was a official symbol and picked up matches
                // like grin1, grin2, grin3, grin (given the search term was grin)
                for ( SearchResult sr : geneSearchResults ) {
                    Gene srGene = ( Gene ) sr.getResultObject();
                    if ( srGene.getTaxon().equals( taxon ) && srGene.getOfficialSymbol().equalsIgnoreCase( line ) ) {
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

    private List<SearchResult> retainGeneSetsOfThisTaxon( Long taxonId, List<SearchResult> geneSetSearchResults,
            Map<Long, Boolean> isSetOwnedByUser ) {
        List<SearchResult> taxonCheckedSets = new ArrayList<>();
        for ( SearchResult sr : geneSetSearchResults ) {
            GeneSet gs = ( GeneSet ) sr.getResultObject();
            GeneSetValueObject gsVo = geneSetValueObjectHelper.convertToValueObject( gs );

            isSetOwnedByUser.put( gs.getId(), securityService.isOwnedByCurrentUser( gs ) );

            if ( Objects.equals( gsVo.getTaxonId(), taxonId ) ) {
                taxonCheckedSets.add( sr );
            }
        }
        return taxonCheckedSets;
    }

    private List<SearchResult> retainGenesOfThisTaxon( Long taxonId, List<SearchResult> geneSearchResults ) {
        List<SearchResult> taxonCheckedGenes = new ArrayList<>();
        for ( SearchResult sr : geneSearchResults ) {
            Gene gene = ( Gene ) sr.getResultObject();
            if ( gene.getTaxon() != null && gene.getTaxon().getId().equals( taxonId ) ) {
                taxonCheckedGenes.add( sr );
            }
        }
        return taxonCheckedGenes;
    }

    /**
     * updates goSets & srDos with GO results
     */
    private List<SearchResultDisplayObject> getGOGroupResults( String query, Taxon taxon ) {
        StopWatch timer = new StopWatch();
        timer.start();
        List<SearchResultDisplayObject> srDos = new ArrayList<>();

        if ( taxon != null ) {

            if ( query.toUpperCase().startsWith( "GO" ) ) {
                // FIXME this should be little more careful.
                log.debug( "Getting results from geneSetSearch.findByGoId for GO prefixed query: " + query );
                GeneSet goSet = geneSetSearch.findByGoId( query, taxon );
                if ( goSet != null && goSet.getMembers() != null && goSet.getMembers().size() > 0 ) {
                    SearchResultDisplayObject sdo = makeGoGroupSearchResult( goSet, query, query, taxon );
                    srDos.add( sdo );
                }
            } else {
                log.debug( "Getting results from geneSetSearch.findByGoTermName for " + query );
                for ( GeneSet geneSet : geneSetSearch
                        .findByGoTermName( query, taxon, GeneSearchServiceImpl.MAX_GO_TERMS_TO_PROCESS,
                                GeneSearchServiceImpl.MAX_GO_GROUP_SIZE ) ) {
                    // don't bother adding empty GO groups
                    // (should probably do this check elsewhere in case it speeds things up)
                    if ( geneSet.getMembers() != null && geneSet.getMembers().size() != 0 ) {
                        SearchResultDisplayObject sdo = makeGoGroupSearchResult( geneSet, null, query, taxon );
                        srDos.add( sdo );
                    }
                }
            }
        } else {// taxon is null, search without taxon as a constraint and bag up the results based on taxon

            log.debug( "getting results from geneSetSearch.findByGoId for GO prefixed query: " + query
                    + " with null taxon" );
            if ( query.toUpperCase().startsWith( "GO" ) ) {
                GeneSet goSet = geneSetSearch.findByGoId( query, null );
                if ( goSet == null ) {
                    return srDos;
                }

                // this gene set has genes from all the different taxons, organize them
                Collection<GeneSet> taxonSpecificSets = organizeMultiTaxaSetIntoTaxonSpecificSets( goSet );

                for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                    if ( taxonGeneSet != null && taxonGeneSet.getMembers() != null
                            && taxonGeneSet.getMembers().size() > 0 ) {
                        SearchResultDisplayObject sdo = makeGoGroupSearchResult( taxonGeneSet, query, query,
                                taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                        srDos.add( sdo );
                    }
                }
            } else {
                log.debug( "getting results from geneSetSearch.findByGoTermName for " + query + " with null taxon" );
                for ( GeneSet geneSet : geneSetSearch
                        .findByGoTermName( query, null, GeneSearchServiceImpl.MAX_GO_TERMS_TO_PROCESS,
                                GeneSearchServiceImpl.MAX_GO_GROUP_SIZE ) ) {

                    // geneSet will have genes from different taxons inside, organize them.
                    Collection<GeneSet> taxonSpecificSets = organizeMultiTaxaSetIntoTaxonSpecificSets( geneSet );

                    for ( GeneSet taxonGeneSet : taxonSpecificSets ) {

                        if ( geneSet.getMembers() != null && taxonGeneSet.getMembers().size() != 0 ) {
                            SearchResultDisplayObject sdo = makeGoGroupSearchResult( taxonGeneSet, null, query,
                                    taxonGeneSet.getMembers().iterator().next().getGene().getTaxon() );
                            srDos.add( sdo );
                        }
                    }
                }
            }

        }

        Collections.sort( srDos );
        if ( timer.getTime() > 500 )
            log.info( "GO search: " + srDos.size() + " results in " + timer.getTime() + "ms" );
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
                Collection<GeneSetMember> members = new ArrayList<>();
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
     * updates goSets & srDos with GO results
     */
    private List<SearchResultDisplayObject> getPhenotypeAssociationSearchResults( String query, Taxon taxon ) {

        List<SearchResultDisplayObject> phenotypeSrDos = new ArrayList<>();
        // if taxon==null then it grabs results for all taxons
        Collection<GeneSetValueObject> geneSets = geneSetSearch.findByPhenotypeName( query, taxon );
        for ( GeneSetValueObject geneSet : geneSets ) {
            // don't bother adding empty groups
            // (should probably do this check elsewhere in case it speeds things up)
            if ( geneSet.getGeneIds() != null && geneSet.getGeneIds().size() != 0 ) {
                SearchResultDisplayObject sdo = makePhenotypeAssociationGroupSearchResult( geneSet, query, taxon );
                phenotypeSrDos.add( sdo );
                // phenotypeSets.add( geneSet );
            }
        }

        Collections.sort( phenotypeSrDos );
        return phenotypeSrDos;
    }

    private SearchResultDisplayObject makeGoGroupSearchResult( GeneSet goSet, String goId, String query,
            Taxon taxonForGo ) {
        GOGroupValueObject ggVo = geneSetValueObjectHelper.convertToGOValueObject( goSet, goId, query );
        return getSearchResultForSessionBoundGroupValueObject( taxonForGo, ggVo );
    }

    private SearchResultDisplayObject makePhenotypeAssociationGroupSearchResult( GeneSetValueObject geneSet,
            String query, Taxon taxonForGS ) {
        PhenotypeGroupValueObject pgVo = PhenotypeGroupValueObject.convertFromGeneSetValueObject( geneSet, query );
        return getSearchResultForSessionBoundGroupValueObject( taxonForGS, pgVo );
    }

    private SearchResultDisplayObject getSearchResultForSessionBoundGroupValueObject( Taxon taxonForGS,
            SessionBoundGeneSetValueObject sbGsVo ) {

        if ( taxonForGS != null ) {
            // GO groups don't seem to have there sbGsVo's taxon info populated
            sbGsVo.setTaxonId( taxonForGS.getId() );
            sbGsVo.setTaxonName( taxonForGS.getCommonName() );

        } else {
            sbGsVo.setTaxonId( sbGsVo.getTaxonId() );
            sbGsVo.setTaxonName( sbGsVo.getTaxonName() );
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
     */
    private List<SearchResultDisplayObject> addEntryForAllResults( String query,
            Collection<SearchResultDisplayObject> genes, Collection<SearchResultDisplayObject> geneSets,
            List<SearchResultDisplayObject> srDos, List<SearchResultDisplayObject> phenotypeSrDos ) {

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
            updateGeneIdsByTaxonId( geneSets, geneIdsByTaxonId );
            updateGeneIdsByTaxonId( srDos, geneIdsByTaxonId );
            updateGeneIdsByTaxonId( phenotypeSrDos, geneIdsByTaxonId );

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
                geneIdsByTaxonId.get( srDo.getTaxonId() )
                        .addAll( ( ( GeneSetValueObject ) resultValueObject ).getGeneIds() );

            } else {
                throw new UnsupportedOperationException(
                        "Unknown search result type: " + resultValueObject.getClass().getName() );
            }

        }
    }

    /**
     * if query is blank, return list of public sets, user-owned sets (if logged in) and user's recent session-bound
     * sets called by ubic.gemma.web.controller.genome.gene.GenePickerController.searchGenesAndGeneGroups(String, Long)
     *
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

        StopWatch watch = new StopWatch();
        watch.start();

        // get all public sets (if user is admin, these were already loaded with geneSetService.loadMySets() )
        // filtered by security.
        Collection<GeneSet> sets = new ArrayList<>();
        if ( !SecurityUtil.isUserLoggedIn() ) {
            try {
                sets = geneSetService.loadAll( taxon );
                if ( watch.getTime() > 100 )
                    log.info( sets.size() + " sets loaded for taxon =" + taxon + " took: " + watch.getTime() + "ms" );
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
                log.info( "Loading the user's gene sets took: " + watch.getTime() + "ms" );
        }

        if ( sets.isEmpty() ) {
            return new ArrayList<>();
        }

        // separate these out because they go at the top of the list
        List<SearchResultDisplayObject> displayResultsPrivate = new ArrayList<>();
        List<SearchResultDisplayObject> displayResultsPublic = new ArrayList<>();
        SearchResultDisplayObject newSrDo;

        List<DatabaseBackedGeneSetValueObject> valueObjects = geneSetValueObjectHelper
                .convertToLightValueObjects( sets, false );
        if ( watch.getTime() > 500 )
            log.info( "Database stage done: " + watch.getTime() + "ms" );

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

        log.info( "Results for blank query: " + displayResults.size() + " items, " + watch.getTime() + "ms" );

        return displayResults;

    }
}
