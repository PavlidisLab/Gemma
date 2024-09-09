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

package ubic.gemma.core.search;

import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import ubic.gemma.core.search.source.CompositeSearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.lucene.LuceneQueryUtils.extractTerms;
import static ubic.gemma.core.search.source.DatabaseSearchSource.NCBI_GENE_ID_URI_PREFIX;

/**
 * This service is used for performing searches using free text or exact match to items in the database.
 * <h2>Implementation notes</h2>
 * Internally, there are generally two kinds of searches performed, precise database searches looking for exact match
 * in the database and compass/lucene searches which look for match in the stored index.
 * To add more dependencies to this Service edit the applicationContext-search.xml
 *
 * @author klc
 * @author paul
 * @author keshav
 */
@Service
@CommonsLog
public class SearchServiceImpl implements SearchService, InitializingBean {


    private static class SearchResultMapImpl extends LinkedMultiValueMap<Class<? extends Identifiable>, SearchResult<?>> implements SearchResultMap {

        @Override
        public List<SearchResult<?>> getByResultType( Class<? extends Identifiable> searchResultType ) {
            return getOrDefault( searchResultType, Collections.emptyList() );
        }

        @Override
        public <T extends Identifiable> List<SearchResult<T>> getByResultObjectType( Class<T> clazz ) {
            //noinspection unchecked
            return values().stream().flatMap( List::stream )
                    .filter( e -> ( clazz.isAssignableFrom( e.getResultType() ) && e.getResultObject() == null ) || clazz.isInstance( e.getResultObject() ) )
                    .map( e -> ( SearchResult<T> ) e )
                    .collect( Collectors.toList() );
        }

        @Override
        public Set<Class<? extends Identifiable>> getResultTypes() {
            return keySet();
        }

        @Override
        public List<SearchResult<?>> toList() {
            return values().stream()
                    .flatMap( List::stream )
                    .collect( Collectors.toList() );
        }

        private <T extends Identifiable> void add( SearchResult<T> searchResult ) {
            super.add( searchResult.getResultType(), searchResult );
        }

        private <T extends Identifiable> void addAll( Collection<SearchResult<T>> searchResult ) {
            for ( SearchResult<T> sr : searchResult ) {
                this.add( sr );
            }
        }
    }

    private final Map<String, Taxon> nameToTaxonMap = new LinkedHashMap<>();

    /* sources */
    @Autowired
    private List<SearchSource> searchSources;

    @Autowired
    @Qualifier("ontologySearchSource")
    private SearchSource ontologySearchSource;

    // TODO: move all this under DatabaseSearchSource
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private TaxonService taxonService;

    @Autowired
    @Qualifier("valueObjectConversionService")
    private ConversionService valueObjectConversionService;

    /**
     * Mapping of supported result types to their corresponding VO type.
     */
    private final Map<Class<? extends Identifiable>, Class<? extends IdentifiableValueObject<?>>> supportedResultTypes = new HashMap<>();

    /**
     * A composite search source that combines all the search sources.
     */
    private CompositeSearchSource searchSource;

    @Override
    public Set<String> getFields( Class<? extends Identifiable> resultType ) {
        return searchSources.stream()
                .filter( s -> s instanceof FieldAwareSearchSource )
                .map( s -> ( ( FieldAwareSearchSource ) s ).getFields( resultType ) )
                .flatMap( Set::stream )
                .collect( Collectors.toSet() );
    }

    /*
     * This is the method used by the main search page.
     */
    @Override
    @Transactional(readOnly = true)
    public SearchResultMap search( SearchSettings settings ) throws SearchException {
        if ( !supportedResultTypes.keySet().containsAll( settings.getResultTypes() ) ) {
            throw new IllegalArgumentException( "The search settings contains unsupported result types:" + SetUtils.difference( settings.getResultTypes(), supportedResultTypes.keySet() ) + "." );
        }

        StopWatch timer = StopWatch.createStarted();

        // attempt to infer a taxon from the query if missing
        if ( settings.getTaxon() == null ) {
            settings = settings.withTaxon( inferTaxon( settings ) );
        }

        // If nothing to search return nothing.
        if ( StringUtils.isBlank( settings.getQuery() ) ) {
            return new SearchResultMapImpl();
        }

        // Get the top N results for each class.
        SearchResultMapImpl results = new SearchResultMapImpl();
        // do gene first before we munge the query too much.
        if ( settings.hasResultType( Gene.class ) ) {
            results.addAll( this.geneSearch( settings ) );
        }
        if ( settings.hasResultType( ExpressionExperiment.class ) ) {
            results.addAll( this.expressionExperimentSearch( settings ) );
        }
        if ( settings.hasResultType( CompositeSequence.class ) ) {
            results.addAll( this.compositeSequenceSearch( settings ) );
        }
        if ( settings.hasResultType( ArrayDesign.class ) ) {
            results.addAll( searchSource.searchArrayDesign( settings ) );
        }
        if ( settings.hasResultType( BioSequence.class ) ) {
            results.addAll( searchSource.searchBioSequence( settings ) );
        }
        if ( settings.hasResultType( BibliographicReference.class ) ) {
            results.addAll( searchSource.searchBibliographicReference( settings ) );
        }
        if ( settings.hasResultType( GeneSet.class ) ) {
            results.addAll( searchSource.searchGeneSet( settings ) );
        }
        if ( settings.hasResultType( ExpressionExperimentSet.class ) ) {
            results.addAll( searchSource.searchExperimentSet( settings ) );
        }
        if ( settings.hasResultType( BlacklistedEntity.class ) ) {
            results.addAll( searchSource.searchBlacklistedEntities( settings ) );
        }

        if ( !settings.isFillResults() ) {
            results.forEach( ( k, v ) -> v.forEach( SearchResult::clearResultObject ) );
        }

        if ( !results.isEmpty() ) {
            log.debug( String.format( "Search for %s yielded %d results in %d ms.", settings, results.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;
    }

    /*
     * NOTE used via the DataSetSearchAndGrabToolbar -> DatasetGroupEditor
     */

    @Override
    public Set<Class<? extends Identifiable>> getSupportedResultTypes() {
        return supportedResultTypes.keySet();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        searchSource = new CompositeSearchSource( searchSources );
        initializeSupportedResultTypes();
        this.initializeNameToTaxonMap();
    }

    private void initializeSupportedResultTypes() {
        supportedResultTypes.put( ArrayDesign.class, ArrayDesignValueObject.class );
        supportedResultTypes.put( BibliographicReference.class, BibliographicReferenceValueObject.class );
        supportedResultTypes.put( BioSequence.class, BioSequenceValueObject.class );
        supportedResultTypes.put( CompositeSequence.class, CompositeSequenceValueObject.class );
        supportedResultTypes.put( ExpressionExperiment.class, ExpressionExperimentValueObject.class );
        supportedResultTypes.put( ExpressionExperimentSet.class, ExpressionExperimentSetValueObject.class );
        supportedResultTypes.put( Gene.class, GeneValueObject.class );
        supportedResultTypes.put( GeneSet.class, GeneSetValueObject.class );
        supportedResultTypes.put( BlacklistedEntity.class, BlacklistedValueObject.class );
        for ( Map.Entry<Class<? extends Identifiable>, Class<? extends IdentifiableValueObject<?>>> e : supportedResultTypes.entrySet() ) {
            canConvertFromEntity( e.getKey(), e.getValue() );
            canConvertFromId( e.getValue() );
        }
    }

    private void canConvertFromEntity( Class<? extends Identifiable> from, Class<? extends IdentifiableValueObject<?>> to ) {
        Assert.isTrue( valueObjectConversionService.canConvert( from, to ),
                String.format( "Must be able to convert from %s to %s.", from.getName(), to.getName() ) );
        Assert.isTrue( valueObjectConversionService.canConvert( TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( from ) ),
                        TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( to ) ) ),
                String.format( "Must be able to convert from collection of %s to list of %s.", from.getName(), to.getName() ) );
    }

    public void canConvertFromId( Class<? extends IdentifiableValueObject<?>> to ) {
        Assert.isTrue( valueObjectConversionService.canConvert( Long.class, to ),
                String.format( "Must be able to convert from %s to %s.", Long.class.getName(), to.getName() ) );
        Assert.isTrue( valueObjectConversionService.canConvert( TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( Long.class ) ),
                        TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( to ) ) ),
                String.format( "Must be able to convert from collection of %s to list of %s.", Long.class.getName(), to.getName() ) );
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Identifiable, U extends IdentifiableValueObject<T>> SearchResult<U> loadValueObject( SearchResult<T> searchResult ) throws IllegalArgumentException {
        try {
            // null sf a valid state if the original result is provisional, the converter is capable of retrieving the VO by ID
            T resultObject = searchResult.getResultObject();
            //noinspection unchecked
            return searchResult.withResultObject( ( U ) valueObjectConversionService.convert(
                    resultObject != null ? resultObject : searchResult.getResultId(),
                    supportedResultTypes.get( searchResult.getResultType() ) ) );
        } catch ( ConverterNotFoundException e ) {
            throw new IllegalArgumentException( "Result type " + searchResult.getResultType() + " is not supported for VO conversion.", e );
        }
    }

    @Transactional(readOnly = true)
    public List<SearchResult<? extends IdentifiableValueObject<?>>> loadValueObjects( Collection<SearchResult<?>> searchResults ) throws IllegalArgumentException {
        // regroup by type so that we can apply efficient array conversion
        Map<Class<? extends Identifiable>, List<SearchResult<?>>> searchResultsByType = searchResults.stream()
                .collect( Collectors.groupingBy( SearchResult::getResultType,
                        Collectors.toList() ) );
        // TODO: implement retain missing VOs
        return searchResultsByType.entrySet().stream()
                .map( l -> this.loadValueObjectsOfSameResultType( l.getValue(), l.getKey() ) )
                .flatMap( List::stream )
                .collect( Collectors.toList() );
    }

    /**
     * Perform optimized VO conversion on collections of the same type.
     */
    private List<SearchResult<? extends IdentifiableValueObject<?>>> loadValueObjectsOfSameResultType( List<SearchResult<?>> results, Class<? extends Identifiable> resultType ) {
        List<Identifiable> entities = new ArrayList<>();
        List<Long> entitiesIds = new ArrayList<>();
        List<IdentifiableValueObject<?>> entitiesVos = new ArrayList<>();
        for ( SearchResult<?> result : results ) {
            if ( resultType.isInstance( result.getResultObject() ) ) {
                entities.add( result.getResultObject() );
            } else {
                entitiesIds.add( result.getResultId() );
            }
        }

        // convert entities to VOs
        if ( !entities.isEmpty() ) {
            //noinspection unchecked
            entitiesVos.addAll( ( List<IdentifiableValueObject<?>> )
                    valueObjectConversionService.convert( entities,
                            TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( resultType ) ),
                            TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( supportedResultTypes.get( resultType ) ) ) ) );
        }

        // convert IDs to VOs
        if ( !entitiesIds.isEmpty() ) {
            //noinspection unchecked
            entitiesVos.addAll( ( List<IdentifiableValueObject<?>> )
                    valueObjectConversionService.convert( entitiesIds,
                            TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( Long.class ) ),
                            TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( supportedResultTypes.get( resultType ) ) ) ) );
        }

        Map<Long, IdentifiableValueObject<?>> entityVosById = EntityUtils.getIdMap( entitiesVos );

        Set<SearchResult<?>> excludedResults = new HashSet<>();

        // reassemble everything
        List<SearchResult<? extends IdentifiableValueObject<?>>> resultsVo = new ArrayList<>( results.size() );
        for ( SearchResult<?> sr : results ) {
            if ( entityVosById.containsKey( sr.getResultId() ) ) {
                IdentifiableValueObject<?> newResultObject = entityVosById.get( sr.getResultId() );
                resultsVo.add( sr.withResultObject( newResultObject ) );
            } else if ( sr.getResultObject() == null ) {
                // result was originally unfilled and nothing was found, so it's somewhat safe to restore it
                if ( sr.getHighlights() != null ) {
                    resultsVo.add( SearchResult.from( sr.getResultType(), sr.getResultId(), sr.getScore(), sr.getHighlights(), sr.getSource() ) );
                } else {
                    long entityId = sr.getResultId();
                    resultsVo.add( SearchResult.from( sr.getResultType(), entityId, sr.getScore(), null, sr.getSource() ) );
                }
            } else {
                // this happens if the VO was filtered out after VO conversion (i.e. via ACL) or uninitialized
                // I think it's a bit risky to add it given the possibility that the result might be missing because of
                // an ACL rule, but I think it's fine since we only expose the ID.
                excludedResults.add( sr );
            }
        }

        if ( !excludedResults.isEmpty() ) {
            log.warn( String.format( "%d %s results were excluded while performing bulk VO conversion.",
                    excludedResults.size(), resultType.getSimpleName() ) );
        }

        return resultsVo;
    }

    /**
     * Search by name of the composite sequence as well as gene.
     */
    private SearchResultSet<CompositeSequence> compositeSequenceSearch( SearchSettings settings ) throws SearchException {

        StopWatch watch = StopWatch.createStarted();

        /*
         * FIXME: this at least partly ignores any array design that was set as a restriction, especially in a gene
         * search.
         */

        // Skip compass searching of composite sequences because it only bloats the results.
        Collection<SearchResult<?>> compositeSequenceResults = this.searchSource.searchCompositeSequenceAndGene( settings );

        /*
         * This last step is needed because the compassSearch for compositeSequences returns bioSequences too.
         */
        SearchResultSet<CompositeSequence> finalResults = new SearchResultSet<>( settings );
        for ( SearchResult<?> sr : compositeSequenceResults ) {
            if ( CompositeSequence.class.equals( sr.getResultType() ) ) {
                //noinspection unchecked
                finalResults.add( ( SearchResult<CompositeSequence> ) sr );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            SearchServiceImpl.log.warn( String.format( "Composite sequence search for %s took %d ms, %d results.",
                    settings, watch.getTime(), finalResults.size() ) );
        }
        return finalResults;
    }

    /**
     * A key method for experiment search. This search does both an database search and a compass search, and looks at
     * several different associations. To allow maximum flexibility, we try not to limit the number of results here (it
     * can be done via the settings object)
     * <p>
     * If the search match a GEO ID, short name or full name of an experiment, the search ends. Otherwise, we search
     * free-text indices and ontology annotations.
     *
     * @param settings object; the maximum results can be set here but also has a default value defined by
     *                 SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE
     * @return {@link Collection} of SearchResults
     */
    private SearchResultSet<ExpressionExperiment> expressionExperimentSearch( final SearchSettings settings ) throws SearchException {

        StopWatch totalTime = StopWatch.createStarted();
        StopWatch watch = StopWatch.createStarted();

        SearchServiceImpl.log.debug( ">>>>> Starting search for " + settings );

        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        // searches for GEO names, etc - "exact" match.
        results.addAll( searchSource.searchExpressionExperiment( settings ) );
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log.warn( String.format( "Expression Experiment database search for %s took %d ms, %d hits.",
                    settings, watch.getTime(), results.size() ) );

        // in fast mode, stop now
        if ( settings.getMode().equals( SearchSettings.SearchMode.FAST ) ) {
            return results;
        }

        /*
         * If we get results here, probably we want to just stop immediately, because the user is searching for
         * something exact. In response to https://github.com/PavlidisLab/Gemma/issues/140 we continue if the user
         * has admin status.
         */

        // special case: search for experiments associated with genes
        // this is achieved by crafting a URI with the NCBI gene id
        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) || SecurityUtil.isUserAdmin() ) {
            SearchResultSet<Gene> geneHits = this.geneSearch( settings.withMode( SearchSettings.SearchMode.FAST ) );
            for ( SearchResult<Gene> gh : geneHits ) {
                Gene g = gh.getResultObject();
                if ( g == null || g.getNcbiGeneId() == null ) {
                    continue;
                }
                results.addAll( ontologySearchSource.searchExpressionExperiment( settings.withQuery( NCBI_GENE_ID_URI_PREFIX + g.getNcbiGeneId() ) ) );
            }
        }

        /*
         * this should be unnecessary we we hit bibrefs in our regular lucene-index search. Also as written, this is
         * very slow
         */
        //        // possibly keep looking
        //        if ( results.size() == 0 ) { //
        //            watch.reset();
        //            watch.start();
        //            log.info( "Searching for experiments via publications..." );
        //            List<BibliographicReferenceValueObject> bibrefs = bibliographicReferenceService
        //                    .search( settings.getQuery() );
        //
        //            if ( !bibrefs.isEmpty() ) {
        //                log.info( "... found " + bibrefs.size() + " papers matching " + settings.getQuery() );
        ////                Collection<BibliographicReference> refs = new HashSet<>();
        ////                // this seems like an extra
        ////                Collection<SearchResult> r = this.compassBibliographicReferenceSearch( settings );
        ////                for ( SearchResult searchResult : r ) {
        ////                    refs.add( ( BibliographicReference ) searchResult.getResultObject() );
        ////                }
        //
        //                Map<BibliographicReference, Collection<ExpressionExperiment>> relatedExperiments = this.bibliographicReferenceService
        //                        .getRelatedExperiments( bibrefs );
        //                for ( Entry<BibliographicReference, Collection<ExpressionExperiment>> e : relatedExperiments
        //                        .entrySet() ) {
        //                    results.addAll( this.dbHitsToSearchResult( e.getValue(), null ) );
        //                }
        //                if ( watch.getTime() > 500 )
        //                    SearchServiceImpl.log
        //                            .info( "... Publication search for took " + watch
        //                                    .getTime() + " ms, " + results.size() + " hits" );
        //
        //            }
        //        }

        /*
         * Find data sets that match a platform. This will probably only be trigged if the search is for a GPL id. NOTE:
         * we may want to move this sooner, but we don't want to slow down the process if they are not searching by
         * array design
         */
        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) || SecurityUtil.isUserAdmin() ) {
            watch.reset();
            watch.start();
            Collection<SearchResult<ArrayDesign>> matchingPlatforms = searchSource.searchArrayDesign( settings );
            for ( SearchResult<ArrayDesign> adRes : matchingPlatforms ) {
                ArrayDesign ad = adRes.getResultObject();
                if ( ad != null ) {
                    Collection<ExpressionExperiment> expressionExperiments = this.arrayDesignService
                            .getExpressionExperiments( ad );
                    for ( ExpressionExperiment ee : expressionExperiments ) {
                        results.add( SearchResult.from( ExpressionExperiment.class, ee, 0.8, Collections.singletonMap( "arrayDesign", ad.getShortName() + " - " + ad.getName() ), String.format( "ArrayDesignService.getExpressionExperiments(%s)", ad ) ) );
                    }
                }
            }
            if ( watch.getTime() > 1000 ) {
                SearchServiceImpl.log.warn( String.format( "Expression Experiment platform search for %s took %d ms, %d hits.",
                        settings, watch.getTime(), results.size() ) );
            }
        }

        String message = String.format( ">>>>>>> Expression Experiment search for %s took %d ms, %d hits.", settings, totalTime.getTime(), results.size() );
        if ( totalTime.getTime() > 1000 ) {
            SearchServiceImpl.log.warn( message );
        } else {
            SearchServiceImpl.log.debug( message );
        }

        return results;
    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
     */
    private SearchResultSet<Gene> geneSearch( final SearchSettings settings ) throws SearchException {

        StopWatch watch = StopWatch.createStarted();

        SearchResultSet<Gene> combinedGeneList = new SearchResultSet<>( settings );

        combinedGeneList.addAll( this.searchSource.searchGene( settings ) );

        // stop here in the fast search mode
        if ( settings.getMode() == SearchSettings.SearchMode.FAST ) {
            return combinedGeneList;
        }

        // expand the search by including probes-associated genes
        if ( combinedGeneList.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            Collection<SearchResult<?>> geneCsList = this.searchSource.searchCompositeSequenceAndGene( settings );
            for ( SearchResult<?> res : geneCsList ) {
                if ( Gene.class.equals( res.getResultType() ) )
                    //noinspection unchecked
                    combinedGeneList.add( ( SearchResult<Gene> ) res );
            }
        }

        if ( watch.getTime() > 1000 ) {
            SearchServiceImpl.log.warn( String.format( "Gene search for %s took %d ms; %d results.",
                    settings, watch.getTime(), combinedGeneList.size() ) );
        }

        return combinedGeneList;
    }

    private void initializeNameToTaxonMap() {

        Collection<? extends Taxon> taxonCollection = taxonService.loadAll();

        for ( Taxon taxon : taxonCollection ) {
            if ( taxon.getScientificName() != null )
                nameToTaxonMap.put( taxon.getScientificName().trim().toLowerCase(), taxon );
            if ( taxon.getCommonName() != null )
                nameToTaxonMap.put( taxon.getCommonName().trim().toLowerCase(), taxon );
        }

        // Loop through again breaking up multi-word taxon database names.
        // Doing this is a separate loop so that these names take lower precedence when matching than the full terms in
        // the generated keySet.
        for ( Taxon taxon : taxonCollection ) {
            this.addTerms( taxon, taxon.getCommonName() );
            this.addTerms( taxon, taxon.getScientificName() );
        }

    }

    private void addTerms( Taxon taxon, String taxonName ) {
        String[] terms;
        if ( StringUtils.isNotBlank( taxonName ) ) {
            terms = taxonName.split( "\\s+" );
            // Only continue for multi-word
            if ( terms.length > 1 ) {
                for ( String s : terms ) {
                    if ( !nameToTaxonMap.containsKey( s.trim().toLowerCase() ) ) {
                        nameToTaxonMap.put( s.trim().toLowerCase(), taxon );
                    }
                }
            }
        }
    }

    /**
     * Infer a {@link Taxon} from the search settings.
     */
    @Nullable
    private Taxon inferTaxon( SearchSettings settings ) throws SearchException {
        // split the query around whitespace characters, limit the splitting to 4 terms (may be excessive)
        // remove quotes and other characters tha can interfere with the exact match
        Set<String> searchTerms = extractTerms( settings );

        for ( String term : searchTerms ) {
            if ( nameToTaxonMap.containsKey( term ) ) {
                return nameToTaxonMap.get( term );
            }
        }

        // no match found, on taxon is inferred
        return null;
    }
}
