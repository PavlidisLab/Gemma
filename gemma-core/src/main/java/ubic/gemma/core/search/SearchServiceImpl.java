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

import com.google.common.collect.Sets;
import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.text.StringEscapeUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneSearchService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.source.CompositeSearchSource;
import ubic.gemma.core.search.source.DatabaseSearchSource;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GeneEvidenceValueObject;
import ubic.gemma.persistence.service.BaseVoEnabledService;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;
import static ubic.gemma.core.search.source.DatabaseSearchSourceUtils.prepareDatabaseQuery;

/**
 * This service is used for performing searches using free text or exact matches to items in the database.
 * <h2>Implementation notes</h2>
 * Internally, there are generally two kinds of searches performed, precise database searches looking for exact matches
 * in the database and compass/lucene searches which look for matches in the stored index.
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

        @Nonnull
        @Override
        public <T extends Identifiable> List<SearchResult<T>> get( Class<T> searchResultType ) {
            List<SearchResult<?>> results = this.getOrDefault( searchResultType, Collections.emptyList() );
            //noinspection unchecked
            return results.stream()
                    .filter( e -> searchResultType.isAssignableFrom( e.getResultType() ) )
                    .map( e -> ( SearchResult<T> ) e )
                    .collect( Collectors.toList() );
        }

        private <T extends Identifiable> void add( SearchResult<T> searchResult ) {
            assert searchResult.getResultObject() == null || searchResult.getResultType().isAssignableFrom( searchResult.getResultObject().getClass() );
            super.add( searchResult.getResultType(), searchResult );
        }

        private <T extends Identifiable> void addAll( Collection<SearchResult<T>> searchResult ) {
            for ( SearchResult<T> sr : searchResult ) {
                this.add( sr.getResultType(), sr );
            }
        }
    }


    /**
     * Penalty hit for indirect hit (multiplicative).
     * <p>
     * For example, if a platform is matched by a gene hit (score = 1.0), the score will be multiplied by this penalty
     * (score = 0.8 * 1.0 = 0.8).
     */
    private static final double INDIRECT_HIT_PENALTY = 0.8;

    private static final String NCBI_GENE = "ncbi_gene";

    private final Map<String, Taxon> nameToTaxonMap = new LinkedHashMap<>();

    /* sources */
    @Autowired
    @Qualifier("compassSearchSource")
    private SearchSource compassSearchSource;
    @Autowired
    @Qualifier("databaseSearchSource")
    private SearchSource databaseSearchSource;

    // TODO: move all this under DatabaseSearchSource
    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService experimentSetService;
    @Autowired
    private GeneSearchService geneSearchService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;

    // TODO: use services instead of DAO here
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;
    @Autowired
    private TaxonService taxonService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;

    private final Set<Class<? extends Identifiable>> supportedResultTypes = new HashSet<>();
    private final ConfigurableConversionService resultObjectConversionService = new GenericConversionService();

    /**
     * A composite search source.
     */
    private SearchSource searchSource;

    /*
     * This is the method used by the main search page.
     */
    @Override
    @Transactional(readOnly = true)
    public SearchResultMap search( SearchSettings settings ) throws SearchException {
        return doSearch( settings );
    }

    /*
     * This is only used for gene and gene-set searches?
     */
    @Override
    @Transactional(readOnly = true)
    public SearchResultMap speedSearch( SearchSettings settings ) throws SearchException {
        return doSearch( settings.withMode( SearchSettings.SearchMode.FAST ) );
    }

    /*
     * Many calls will end up here.
     */
    @Override
    @Transactional(readOnly = true)
    public SearchResultMap search( SearchSettings settings, boolean fillObjects,
            boolean webSpeedSearch ) throws SearchException {
        return doSearch( settings.withFillResults( fillObjects ).withMode( webSpeedSearch ? SearchSettings.SearchMode.FAST : SearchSettings.SearchMode.NORMAL ) );
    }

    private SearchResultMap doSearch( SearchSettings settings ) throws SearchException {
        if ( !supportedResultTypes.containsAll( settings.getResultTypes() ) ) {
            throw new IllegalArgumentException( "The search settings contains unsupported result types:" + Sets.difference( settings.getResultTypes(), supportedResultTypes ) + "." );
        }

        StopWatch timer = StopWatch.createStarted();

        SearchResultMap results;
        if ( settings.isTermQuery() ) {
            // we only attempt an ontology search if the uri looks remotely like a url.
            results = this.ontologyUriSearch( settings );
        } else {
            results = this.generalSearch( settings );
        }

        if ( !settings.isFillResults() ) {
            results.forEach( ( k, v ) -> v.forEach( sr -> sr.setResultObject( null ) ) );
        }

        if ( !results.isEmpty() ) {
            log.info( String.format( "Search for %s yielded %d results in %d ms.", settings, results.size(), timer.getTime( TimeUnit.MILLISECONDS ) ) );
        }

        return results;

    }

    /*
     * NOTE used via the DataSetSearchAndGrabToolbar -> DatasetGroupEditor
     */

    @Override
    @Transactional(readOnly = true)
    public <T extends Identifiable> List<SearchResult<T>> search( SearchSettings settings, Class<T> resultClass ) throws SearchException {
        // only search for the requested class
        settings = settings.withResultTypes( Collections.singleton( resultClass ) );
        // Note: resultClass is guaranteed to exist in the returned mapping
        return this.search( settings ).get( resultClass );
    }

    @Override
    public Set<Class<? extends Identifiable>> getSupportedResultTypes() {
        return supportedResultTypes;
    }

    @Override
    @Transactional(readOnly = true)
    public <T extends Identifiable, U extends IdentifiableValueObject<T>> SearchResult<U> loadValueObject( SearchResult<T> searchResult ) throws IllegalArgumentException {
        // null if a valid state if the original result is provisional, then we return a provisional VO
        T resultObject = searchResult.getResultObject();
        U convertedResultObject = null;
        if ( resultObject != null ) {
            try {
                //noinspection unchecked
                convertedResultObject = ( U ) resultObjectConversionService.convert( resultObject, IdentifiableValueObject.class );
            } catch ( ConverterNotFoundException e ) {
                throw new IllegalArgumentException( "Result type " + searchResult.getResultType() + " is not supported for VO conversion.", e );
            }
        }
        return SearchResult.from( searchResult, convertedResultObject );
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        searchSource = new CompositeSearchSource( Arrays.asList( databaseSearchSource, compassSearchSource ) );
        initializeSupportedResultTypes();
        initializeResultObjectConversionService();
        this.initializeNameToTaxonMap();
    }

    private void initializeSupportedResultTypes() {
        supportedResultTypes.add( ArrayDesign.class );
        supportedResultTypes.add( BibliographicReference.class );
        supportedResultTypes.add( BioSequence.class );
        supportedResultTypes.add( CompositeSequence.class );
        supportedResultTypes.add( ExpressionExperiment.class );
        supportedResultTypes.add( ExpressionExperimentSet.class );
        supportedResultTypes.add( Gene.class );
        supportedResultTypes.add( GeneSet.class );
        supportedResultTypes.add( PhenotypeAssociation.class );
    }

    private void initializeResultObjectConversionService() {
        resultObjectConversionService.addConverter( ArrayDesign.class, IdentifiableValueObject.class, o -> {
            ArrayDesign ad = ( ArrayDesign ) o;
            // this is a work-around for Compass-loaded AD which do not have a primary taxon loaded, although it is a
            // mandatory property
            if ( ad.getPrimaryTaxon() == null ) {
                return arrayDesignService.loadValueObjectById( ad.getId() );
            } else {
                return arrayDesignService.loadValueObject( ad );
            }
        } );
        addVoConverter( BibliographicReference.class, bibliographicReferenceService );
        addVoConverter( BioSequence.class, bioSequenceService );
        // FIXME: this is used for phenotypes, but really we should be using {@link PhenotypeAssociation}
        resultObjectConversionService.addConverter( CharacteristicValueObject.class, IdentifiableValueObject.class, o -> o );
        addVoConverter( CompositeSequence.class, compositeSequenceService );
        addVoConverter( ExpressionExperiment.class, expressionExperimentService );
        addVoConverter( ExpressionExperimentSet.class, experimentSetService );
        addVoConverter( Gene.class, geneService );
        // FIXME: GeneSetService does not implement BaseVoEnabledService
        resultObjectConversionService.addConverter( GeneSet.class, IdentifiableValueObject.class, o -> geneSetService.loadValueObject( ( GeneSet ) o ) );
    }

    private <O extends Identifiable, VO extends IdentifiableValueObject<O>> void addVoConverter( Class<O> fromClass, BaseVoEnabledService<O, VO> service ) {
        //noinspection unchecked
        resultObjectConversionService.addConverter( fromClass, IdentifiableValueObject.class, o -> service.loadValueObject( ( O ) o ) );
    }

    /**
     * Checks whether settings have the search genes flag and does the search if needed.
     *
     * @param results the results to which should any new results be accreted.
     */
    private void accreteResultsGenes( LinkedHashSet<SearchResult<?>> results, SearchSettings settings ) throws SearchException {
        if ( settings.hasResultType( Gene.class ) ) {
            Collection<SearchResult<Gene>> genes = this.getGenesFromSettings( settings );
            results.addAll( genes );
        }
    }

    /**
     * Checks settings for all do-search flags, except for gene (see
     * {@link #accreteResultsGenes(LinkedHashSet, SearchSettings)}), and does the search if needed.
     *
     * @param results  the results to which should any new results be accreted.
     * @param settings search settings
     */
    private void accreteResultsOthers( LinkedHashSet<SearchResult<?>> results, SearchSettings settings ) throws SearchException {

        if ( settings.hasResultType( ExpressionExperiment.class ) ) {
            results.addAll( this.expressionExperimentSearch( settings ) );
        }

        Collection<SearchResult<CompositeSequence>> compositeSequences = null;
        if ( settings.hasResultType( CompositeSequence.class ) ) {
            compositeSequences = this.compositeSequenceSearch( settings );
            results.addAll( compositeSequences );
        }

        if ( settings.hasResultType( ArrayDesign.class ) ) {
            results.addAll( this.arrayDesignSearch( settings, compositeSequences ) );
        }

        if ( settings.hasResultType( BioSequence.class ) ) {
            Collection<SearchResult<Gene>> genes = this.getGenesFromSettings( settings );
            Collection<SearchResult<?>> bioSequencesAndGenes = this.bioSequenceAndGeneSearch( settings, genes );

            // split results so that accreteResults can be properly typed

            //noinspection unchecked
            Collection<SearchResult<BioSequence>> bioSequences = bioSequencesAndGenes.stream()
                    .filter( result -> BioSequence.class.isAssignableFrom( result.getResultType() ) )
                    .map( result -> ( SearchResult<BioSequence> ) result )
                    .collect( Collectors.toSet() );
            results.addAll( bioSequences );

            //noinspection unchecked
            Collection<SearchResult<Gene>> gen = bioSequencesAndGenes.stream()
                    .filter( result -> Gene.class.isAssignableFrom( result.getResultType() ) )
                    .map( result -> ( SearchResult<Gene> ) result )
                    .collect( Collectors.toSet() );
            results.addAll( gen );
        }

        if ( settings.hasResultType( Gene.class ) && settings.isUseGo() ) {
            try {
                // FIXME: add support for OR, but there's a bug in baseCode that prevents this https://github.com/PavlidisLab/baseCode/issues/22
                String query = settings.getQuery().replaceAll( "\\s+OR\\s+", "" );
                results.addAll( this.dbHitsToSearchResult(
                        Gene.class, geneSearchService.getGOGroupGenes( query, settings.getTaxon() ), 0.8, "From GO group", "GeneSearchService.getGOGroupGenes" ) );
            } catch ( OntologySearchException e ) {
                throw new BaseCodeOntologySearchException( e );
            }
        }

        if ( settings.hasResultType( BibliographicReference.class ) ) {
            results.addAll( this.compassSearchSource.searchBibliographicReference( settings ) );
        }

        if ( settings.hasResultType( GeneSet.class ) ) {
            results.addAll( this.geneSetSearch( settings ) );
        }

        if ( settings.hasResultType( ExpressionExperimentSet.class ) ) {
            results.addAll( this.experimentSetSearch( settings ) );
        }

        if ( settings.hasResultType( PhenotypeAssociation.class ) ) {
            results.addAll( searchPhenotype( settings ) );
        }
    }

    /**
     * Find phenotypes.
     */
    private Collection<SearchResult<CharacteristicValueObject>> searchPhenotype( SearchSettings settings ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();
        try {
            // FIXME: add support for OR, but there's a bug in baseCode that prevents this https://github.com/PavlidisLab/baseCode/issues/22
            String query = settings.getQuery().replaceAll( "\\s+OR\\s+", "" );
            return this.phenotypeAssociationManagerService.searchInDatabaseForPhenotype( query, settings.getMaxResults() ).stream()
                    .map( r -> SearchResult.from( PhenotypeAssociation.class, r, 1.0, "PhenotypeAssociationManagerService.searchInDatabaseForPhenotype" ) )
                    .collect( Collectors.toCollection( SearchResultSet::new ) );
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( "Failed to search for phenotype associations.", e );
        }
    }

    //    /**
    //     * Convert biomaterial hits into their associated ExpressionExperiments
    //     *
    //     * @param results      will go here
    //     * @param biomaterials
    //     */
    //    private void addEEByBiomaterials( Collection<SearchResult> results, Map<BioMaterial, SearchResult> biomaterials ) {
    //        if ( biomaterials.size() == 0 ) {
    //            return;
    //        }
    //        Map<ExpressionExperiment, BioMaterial> ees = expressionExperimentService
    //                .findByBioMaterials( biomaterials.keySet() );
    //        for ( ExpressionExperiment ee : ees.keySet() ) {
    //            SearchResult searchResult = biomaterials.get( ees.get( ee ) );
    //            results.add( new SearchResult( ee, searchResult.getScore() * SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
    //                    searchResult.getHighlightedText() + " (BioMaterial characteristic)" ) );
    //        }
    //    }
    //
    //    /**
    //     * Convert biomaterial hits into their associated ExpressionExperiments
    //     *
    //     * @param results      will go here
    //     * @param biomaterials
    //     */
    //    private void addEEByBiomaterialIds( Collection<SearchResult> results, Map<Long, SearchResult> biomaterials ) {
    //        if ( biomaterials.size() == 0 ) {
    //            return;
    //        }
    //        Map<ExpressionExperiment, Long> ees = expressionExperimentService
    //                .findByBioMaterialIds( biomaterials.keySet() );
    //        for ( ExpressionExperiment ee : ees.keySet() ) {
    //            SearchResult searchResult = biomaterials.get( ees.get( ee ) );
    //            results.add( new SearchResult( ee, searchResult.getScore() * SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
    //                    searchResult.getHighlightedText() + " (BioMaterial characteristic)" ) );
    //        }
    //    }
    //
    //    /**
    //     * Convert factorValue hits into their associated ExpressionExperiments
    //     *
    //     * @param results      will go here
    //     * @param factorValues
    //     */
    //    private void addEEByFactorvalueIds( Collection<SearchResult> results, Map<Long, SearchResult> factorValues ) {
    //        if ( factorValues.size() == 0 ) {
    //            return;
    //        }
    //        Map<ExpressionExperiment, Long> ees = expressionExperimentService
    //                .findByFactorValueIds( factorValues.keySet() );
    //        for ( ExpressionExperiment ee : ees.keySet() ) {
    //            SearchResult searchResult = factorValues.get( ees.get( ee ) );
    //            results.add( new SearchResult( ee, searchResult.getScore() * SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
    //                    searchResult.getHighlightedText() + " (FactorValue characteristic)" ) );
    //        }
    //
    //    }
    //
    //    /**
    //     * Convert factorValue hits into their associated ExpressionExperiments
    //     *
    //     * @param results      will go here
    //     * @param factorValues
    //     */
    //    private void addEEByFactorvalues( Collection<SearchResult> results, Map<FactorValue, SearchResult> factorValues ) {
    //        if ( factorValues.size() == 0 ) {
    //            return;
    //        }
    //        Map<ExpressionExperiment, FactorValue> ees = expressionExperimentService
    //                .findByFactorValues( factorValues.keySet() );
    //        for ( ExpressionExperiment ee : ees.keySet() ) {
    //            SearchResult searchResult = factorValues.get( ees.get( ee ) );
    //            results.add( new SearchResult( ee, searchResult.getScore() * SearchServiceImpl.INDIRECT_DB_HIT_PENALTY,
    //                    searchResult.getHighlightedText() + " (FactorValue characteristic)" ) );
    //        }
    //
    //    }

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

    private Collection<SearchResult<ExpressionExperimentSet>> experimentSetSearch( SearchSettings settings ) throws SearchException {
        return searchSource.searchExperimentSet( settings );
    }

    /**
     * A general search for array designs.
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     *
     * @param probeResults Collection of results from a previous CompositeSequence search. Can be null; otherwise used
     *                     to avoid a second search for probes. The array designs for the probes are added to the final
     *                     results.
     */
    private Collection<SearchResult<ArrayDesign>> arrayDesignSearch( SearchSettings settings,
            @Nullable Collection<SearchResult<CompositeSequence>> probeResults ) throws SearchException {

        StopWatch watch = StopWatch.createStarted();
        String searchString = prepareDatabaseQuery( settings );
        Collection<SearchResult<ArrayDesign>> results = new SearchResultSet<>();

        ArrayDesign shortNameResult = arrayDesignService.findByShortName( searchString );
        if ( shortNameResult != null ) {
            results.add( SearchResult.from( ArrayDesign.class, shortNameResult, DatabaseSearchSource.MATCH_BY_SHORT_NAME_SCORE, "ArrayDesignService.findByShortName" ) );
            return results;
        }

        Collection<ArrayDesign> nameResult = arrayDesignService.findByName( searchString );
        if ( nameResult != null && !nameResult.isEmpty() ) {
            for ( ArrayDesign ad : nameResult ) {
                results.add( SearchResult.from( ArrayDesign.class, ad, DatabaseSearchSource.MATCH_BY_NAME_SCORE, "ArrayDesignService.findByShortName" ) );
            }
            return results;
        }

        BlacklistedEntity b = blacklistedEntityService.findByAccession( searchString );
        if ( b != null ) {
            // FIXME: I'm not sure the ID is a good thing to put here
            results.add( SearchResult.from( ArrayDesign.class, b.getId(), DatabaseSearchSource.MATCH_BY_ACCESSION_SCORE, "Blacklisted accessions are not loaded into Gemma", "BlackListDao.findByAccession" ) );
            return results;
        }

        Collection<ArrayDesign> altNameResults = arrayDesignService.findByAlternateName( searchString );
        for ( ArrayDesign arrayDesign : altNameResults ) {
            results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, "ArrayDesignService.findByAlternateName" ) );
        }

        Collection<ArrayDesign> manufacturerResults = arrayDesignService.findByManufacturer( searchString );
        for ( ArrayDesign arrayDesign : manufacturerResults ) {
            results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, "ArrayDesignService.findByManufacturer" ) );
        }

        /*
         * FIXME: add merged platforms and subsumers
         */
        results.addAll( searchSource.searchArrayDesign( settings ) );

        Collection<SearchResult<CompositeSequence>> probes;
        if ( probeResults == null ) {
            probes = this.compassSearchSource.searchCompositeSequenceAndGene( settings ).stream()
                    // strip all the gene results
                    .filter( result -> CompositeSequence.class.isAssignableFrom( result.getResultType() ) )
                    .map( result -> SearchResult.from( result, ( CompositeSequence ) result.getResultObject() ) )
                    .collect( Collectors.toCollection( SearchResultSet::new ) );
        } else {
            probes = probeResults;
        }

        for ( SearchResult<CompositeSequence> r : probes ) {
            CompositeSequence cs = r.getResultObject();
            // This might happen as compass might not have indexed the AD for the CS
            if ( cs == null || cs.getArrayDesign() == null ) {
                continue;
            }
            // FIXME: this should not be necessary, the AD is eagerly fetched in the model definition (see https://github.com/PavlidisLab/Gemma/issues/483)
            Hibernate.initialize( cs.getArrayDesign() );
            results.add( SearchResult.from( ArrayDesign.class, cs.getArrayDesign(), INDIRECT_HIT_PENALTY * r.getScore(), "ArrayDesign associated to probes obtained by a Compass search." ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log.info( "Array Design search for '" + settings + "' took " + watch.getTime() + " ms" );

        return results;
    }

    /**
     * @param previousGeneSearchResults Can be null, otherwise used to avoid a second search for genes. The biosequences
     *                                  for the genes are added to the final results.
     */
    private Collection<SearchResult<?>> bioSequenceAndGeneSearch( SearchSettings settings,
            Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        Collection<SearchResult<?>> searchResults = searchSource.searchBioSequenceAndGene( settings, previousGeneSearchResults );

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Biosequence search for '" + settings + "' took " + watch.getTime() + " ms " + searchResults
                            .size() + " results." );

        return searchResults;
    }

    /**
     * Search via characteristics i.e. ontology terms.
     * <p>
     * This is an important type of search but also a point of performance issues. Searches for "specific" terms are
     * generally not a big problem (yielding less than 100 results); searches for "broad" terms can return numerous
     * (thousands)
     * results.
     */
    private Collection<SearchResult<ExpressionExperiment>> characteristicEESearch( final SearchSettings settings ) throws SearchException {

        Collection<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        StopWatch watch = StopWatch.createStarted();

        log.info( "Starting EE search for " + settings );
        String[] subclauses = prepareDatabaseQuery( settings ).split( "\\s+OR\\s+" );
        for ( String subclause : subclauses ) {
            /*
             * Note that the AND is applied only within one entity type. The fix would be to apply AND at this
             * level.
             */
            Collection<SearchResult<ExpressionExperiment>> classResults = this
                    .characteristicEESearchWithChildren( settings.withQuery( subclause ) );
            if ( classResults.size() > 0 ) {
                log.info( "... Found " + classResults.size() + " EEs matching " + subclause );
            }
            results.addAll( classResults );
        }

        SearchServiceImpl.log
                .info( "ExpressionExperiment search: " + settings + " -> " + results.size() + " characteristic-based hits "
                        + watch.getTime() + " ms" );

        return results;

    }

    /**
     * Perform a Experiment search based on annotations (anchored in ontology terms) - it does not have to be one word,
     * it could be "parkinson's disease"; it can also be a URI.
     *
     * @return collection of SearchResults (Experiments)
     */
    private Collection<SearchResult<ExpressionExperiment>> characteristicEESearchTerm( SearchSettings settings ) throws SearchException {
        // overall timer
        StopWatch watch = StopWatch.createStarted();
        // per-step timer
        StopWatch timer = StopWatch.create();

        Set<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        Collection<OntologyResource> terms = new HashSet<>();

        // Phase 1: We first search for individuals.
        Collection<OntologyIndividual> individuals;
        try {
            timer.start();
            individuals = ontologyService.findIndividuals( settings.getQuery() );
            terms.addAll( individuals );
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( e );
        } finally {
            timer.stop();
        }

        if ( watch.getTime() > 500 ) {
            SearchServiceImpl.log.warn( String.format( "Found %d terms (individual) matching '%s' in %d ms",
                    individuals.size(), settings.getQuery(), timer.getTime() ) );
        }

        // Phase 2: Search ontology classes matches to the query
        timer.reset();
        timer.start();
        Collection<OntologyTerm> matchingTerms;
        try {
            matchingTerms = ontologyService.findTerms( settings.getQuery() );
            terms.addAll( matchingTerms );
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( "Failed to find terms via ontology search.", e );
        } finally {
            timer.stop();
        }

        if ( watch.getTime() > 500 ) {
            SearchServiceImpl.log
                    .warn( String.format( "Found %d ontology classes matching '%s' in %d ms",
                            matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
        }

        /*
         * Search for child terms.
         */
        if ( !matchingTerms.isEmpty() ) {
            // TODO: move this logic in baseCode, this can be done far more efficiently with Jena API
            timer.reset();
            timer.start();
            terms.addAll( ontologyService.getChildren( matchingTerms, false, true ) );
            timer.stop();

            if ( watch.getTime() > 500 ) {
                SearchServiceImpl.log.warn(
                        String.format( "Found %d ontology subclasses or related terms matching '%s' in %d ms",
                                terms.size() - matchingTerms.size(), settings.getQuery(), timer.getTime() ) );
            }
        }

        timer.reset();
        timer.start();
        findExperimentsByTerms( terms, results, 0.9, settings );
        timer.stop();

        if ( watch.getTime() > 500 ) {
            SearchServiceImpl.log
                    .warn( String.format( "Retrieved %d datasets via characteristics for '%s' in %d ms",
                            results.size(), settings.getQuery(), timer.getTime() ) );
        }

        return results;
    }

    private void findExperimentsByTerms( Collection<? extends OntologyResource> individuals, Set<SearchResult<ExpressionExperiment>> results, double score, SearchSettings settings ) {
        Collection<String> uris = new HashSet<>( individuals.size() );
        Map<String, String> uri2value = new HashMap<>( individuals.size() );
        for ( OntologyResource individual : individuals ) {
            // bnodes can have null URIs, how annoying...
            if ( individual.getUri() != null ) {
                uris.add( individual.getUri() );
                uri2value.put( individual.getUri(), individual.getLabel() );
            }
        }
        findExperimentsByUris( uris, results, score, uri2value, settings );
    }

    private void findExperimentsByUris( Collection<String> uris, Set<SearchResult<ExpressionExperiment>> results, double score, Map<String, String> uri2value, SearchSettings settings ) {
        if ( isFilled( results, settings ) )
            return;

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits = characteristicService.findExperimentsByUris( uris, settings.getTaxon(), getLimit( results, settings ) );

        // collect all direct tags
        addExperimentsByUrisHits( hits, results, ExpressionExperiment.class, score, uri2value, settings );

        // collect experimental design-related terms
        addExperimentsByUrisHits( hits, results, ExperimentalDesign.class, 0.9 * score, uri2value, settings );

        // collect samples-related terms
        addExperimentsByUrisHits( hits, results, BioMaterial.class, 0.9 * score, uri2value, settings );
    }

    private void addExperimentsByUrisHits( Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> hits, Set<SearchResult<ExpressionExperiment>> results, Class<? extends Identifiable> clazz, double score, Map<String, String> uri2value, SearchSettings settings ) {
        Map<String, Set<ExpressionExperiment>> specificHits = hits.get( clazz );
        if ( specificHits == null )
            return;
        for ( Map.Entry<String, Set<ExpressionExperiment>> entry : specificHits.entrySet() ) {
            String uri = entry.getKey();
            String value = uri2value.get( uri );
            for ( ExpressionExperiment ee : entry.getValue() ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, score,
                        getHighlightTextForTerm( uri, value, clazz, settings.getContextPath() ),
                        String.format( "CharacteristicService.findExperimentsByUris with term [%s](%s)", value, uri ) ) );
            }
        }
    }

    /**
     * FIXME: move this code in Gemma Web
     */
    private static String getHighlightTextForTerm( String uri, String value, Class<? extends Identifiable> clazz, @Nullable String contextPath ) {
        String matchedText;
        try {
            matchedText = "Tagged term: <a href=\"" + ( contextPath != null ? contextPath : "" ) + "/searcher.html?query=" + URLEncoder.encode( uri, StandardCharsets.UTF_8.name() ) + "\">" + escapeHtml4( value ) + "</a> ";
        } catch ( UnsupportedEncodingException e ) {
            throw new RuntimeException( e );
        }
        if ( !clazz.isAssignableFrom( ExpressionExperiment.class ) ) {
            matchedText = matchedText + " via " + clazz.getSimpleName();
        }
        return matchedText;
    }

    /**
     * Search for the Experiment query in ontologies, including items that are associated with children of matching
     * query terms. That is, 'brain' should return entities tagged as 'hippocampus'. It can handle AND in searches, so
     * Parkinson's
     * AND neuron finds items tagged with both of those terms. The use of OR is handled by the caller.
     *
     * @param settings search settings
     * @return SearchResults of Experiments
     */
    private Collection<SearchResult<ExpressionExperiment>> characteristicEESearchWithChildren( SearchSettings settings ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        /*
         * The tricky part here is if the user has entered a boolean query. If they put in Parkinson's disease AND
         * neuron, then we want to eventually return entities that are associated with both. We don't expect to find
         * single characteristics that match both.
         *
         * But if they put in Parkinson's disease we don't want to do two queries.
         */
        String[] subparts = settings.getQuery().split( " AND " );

        // we would have to first deal with the separate queries, and then apply the logic.
        Collection<SearchResult<ExpressionExperiment>> allResults = new SearchResultSet<>();

        SearchServiceImpl.log
                .info( "Starting characteristic search: '" + settings.getQuery() );
        for ( String rawTerm : subparts ) {
            String trimmed = StringUtils.strip( rawTerm );
            if ( StringUtils.isBlank( trimmed ) ) {
                continue;
            }
            Collection<SearchResult<ExpressionExperiment>> subqueryResults = this.characteristicEESearchTerm( settings.withQuery( trimmed ) );
            if ( allResults.isEmpty() ) {
                allResults.addAll( subqueryResults );
            } else {
                // this is our Intersection operation.
                allResults.retainAll( subqueryResults );

                // aggregate the highlighted text.
                Map<SearchResult<ExpressionExperiment>, String> highlights = new HashMap<>();
                for ( SearchResult<ExpressionExperiment> sqr : subqueryResults ) {
                    highlights.put( sqr, sqr.getHighlightedText() );
                }

                for ( SearchResult<ExpressionExperiment> ar : allResults ) {
                    String k = highlights.get( ar );
                    if ( StringUtils.isNotBlank( k ) ) {
                        String highlightedText = ar.getHighlightedText();
                        if ( StringUtils.isBlank( highlightedText ) ) {
                            ar.setHighlightedText( k );
                        } else {
                            ar.setHighlightedText( highlightedText + "," + k );
                        }
                    }
                }
            }

            if ( watch.getTime() > 1000 ) {
                SearchServiceImpl.log.info( "Characteristic EE search for '" + rawTerm + "': " + allResults.size()
                        + " hits retained so far; " + watch.getTime() + "ms" );
                watch.reset();
                watch.start();
            }

            if ( isFilled( allResults, settings ) ) {
                return allResults;
            }
        }

        return allResults;

    }

    /**
     * Search by name of the composite sequence as well as gene.
     */
    private Collection<SearchResult<CompositeSequence>> compositeSequenceSearch( SearchSettings settings ) throws SearchException {

        StopWatch watch = StopWatch.createStarted();

        /*
         * FIXME: this at least partly ignores any array design that was set as a restriction, especially in a gene
         * search.
         */

        // Skip compass searching of composite sequences because it only bloats the results.
        Collection<SearchResult<?>> compositeSequenceResults = new HashSet<>( this.databaseSearchSource.searchCompositeSequenceAndGene( settings ) );

        /*
         * This last step is needed because the compassSearch for compositeSequences returns bioSequences too.
         */
        Collection<SearchResult<CompositeSequence>> finalResults = new SearchResultSet<>();
        for ( SearchResult<?> sr : compositeSequenceResults ) {
            if ( CompositeSequence.class.isAssignableFrom( sr.getResultType() ) ) {
                //noinspection unchecked
                finalResults.add( ( SearchResult<CompositeSequence> ) sr );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Composite sequence search for '" + settings + "' took " + watch.getTime() + " ms, "
                            + finalResults.size() + " results." );
        return finalResults;
    }

    //    private List<SearchResult> convertEntitySearchResutsToValueObjectsSearchResults(
    //            Collection<SearchResult> searchResults ) {
    //        List<SearchResult> convertedSearchResults = new ArrayList<>();
    //        StopWatch t = this.startTiming();
    //        for ( SearchResult searchResult : searchResults ) {
    //            // this is a special case ... for some reason.
    //            if ( BioSequence.class.isAssignableFrom( searchResult.getResultClass() ) ) {
    //                SearchResult convertedSearchResult = new SearchResult( BioSequenceValueObject
    //                        .fromEntity( bioSequenceService.thaw( ( BioSequence ) searchResult.getResultObject() ) ),
    //                        searchResult.getScore(), searchResult.getHighlightedText() );
    //                convertedSearchResults.add( convertedSearchResult );
    //            } else {
    //                convertedSearchResults.add( searchResult );
    //            }
    //        }
    //        if ( t.getTime() > 500 ) {
    //            log.info( "Conversion of " + searchResults.size() + " search results: " + t.getTime() + "ms" );
    //        }
    //        return convertedSearchResults;
    //    }

    //    /**
    //     * Takes a list of ontology terms, and classes of objects of interest to be returned. Looks through the
    //     * characteristic table for an exact match with the given ontology terms. Only tries to match the uri's.
    //     *
    //     * @param  classes Class of objects to restrict the search to (typically ExpressionExperiment.class, for
    //     *                 example).
    //     * @param  terms   A list of ontology terms to search for
    //     * @return         Collection of search results for the objects owning the found characteristics, where the owner is
    //     *                 of
    //     *                 class clazz
    //     */
    //    private Collection<SearchResult> databaseCharacteristicExactUriSearchForOwners( Collection<Class<?>> classes,
    //            Collection<OntologyTerm> terms ) {
    //
    //        // Collection<Characteristic> characteristicValueMatches = new ArrayList<Characteristic>();
    //        Collection<Characteristic> characteristicURIMatches = new ArrayList<>();
    //
    //        for ( OntologyTerm term : terms ) {
    //            // characteristicValueMatches.addAll( characteristicService.findByValue( term.getUri() ));
    //            characteristicURIMatches.addAll( characteristicService.findByUri( classes, term.getUri() ) );
    //        }
    //
    //        Map<Characteristic, Object> parentMap = characteristicService.getParents( classes, characteristicURIMatches );
    //        // parentMap.putAll( characteristicService.getParents(characteristicValueMatches ) );
    //
    //        return this.filterCharacteristicOwnersByClass( classes, parentMap );
    //    }

    //    /**
    //     * Convert characteristic hits from database searches into SearchResults.
    //     * @param entities map of classes to characteristics e.g. Experiment.class -> annotated characteristics
    //     * @param matchText used in highlighting
    //     *
    //     *  FIXME we need the ID of the annotated object if we do it this way
    //     */
    //    private Collection<SearchResult> dbCharacteristicHitsToSearchResultByClass( Map<Class<?>, Collection<Characteristic>> entities,
    //            String matchText ) {
    //        //   return this.dbHitsToSearchResult( entities, null, matchText );
    //
    //        List<SearchResult> results = new ArrayList<>();
    //        for ( Class<?> clazz : entities.keySet() ) {
    //
    //            for ( Characteristic c : entities.get( clazz ) ) {
    //                SearchResult esr = new SearchResult(clazz, /*ID NEEDED*/ , 1.0, matchText );
    //
    //                results.add( esr );
    //            }
    //
    //        }
    //        return results;
    //
    //    }

    /**
     * Convert hits from database searches into SearchResults.
     */
    private <T extends Identifiable> Collection<SearchResult<T>> dbHitsToSearchResult( Class<T> entityClass, Collection<T> entities, double score, String matchText, String source ) {
        StopWatch watch = StopWatch.createStarted();
        List<SearchResult<T>> results = new ArrayList<>( entities.size() );
        for ( T e : entities ) {
            if ( e == null ) {
                if ( log.isDebugEnabled() )
                    log.debug( "Null search result object" );
                continue;
            }
            if ( e.getId() == null ) {
                log.warn( "Search result object with null ID." );
            }
            results.add( SearchResult.from( entityClass, e, score, matchText, source ) );
        }
        if ( watch.getTime() > 1000 ) {
            log.info( "Unpack " + results.size() + " search resultsS: " + watch.getTime() + "ms" );
        }
        return results;
    }

    //    private void debugParentFetch( Map<Characteristic, Object> parentMap ) {
    //        /*
    //         * This is purely debugging.
    //         */
    //        if ( parentMap.size() > 0 ) {
    //            if ( SearchServiceImpl.log.isDebugEnabled() )
    //                SearchServiceImpl.log.debug( "Found " + parentMap.size() + " owners for " + parentMap.keySet().size()
    //                        + " characteristics:" );
    //        }
    //    }

    /**
     * A key method for experiment search. This search does both an database search and a compass search, and looks at
     * several different associations. To allow maximum flexibility, we try not to limit the number of results here (it
     * can be done via the settings object)
     * <p>
     * If the search matches a GEO ID, short name or full name of an experiment, the search ends. Otherwise, we search
     * free-text indices and ontology annotations.
     *
     * @param settings object; the maximum results can be set here but also has a default value defined by
     *                 SearchSettings.DEFAULT_MAX_RESULTS_PER_RESULT_TYPE
     * @return {@link Collection} of SearchResults
     */
    private Collection<SearchResult<ExpressionExperiment>> expressionExperimentSearch( final SearchSettings settings ) throws SearchException {

        StopWatch totalTime = StopWatch.createStarted();
        StopWatch watch = StopWatch.createStarted();

        SearchServiceImpl.log.info( ">>>>> Starting search for '" + settings + "'" );

        Set<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        // searches for GEO names, etc - "exact" matches.
        if ( settings.isUseDatabase() ) {
            results.addAll( this.databaseSearchSource.searchExpressionExperiment( settings ) );
            if ( watch.getTime() > 500 )
                SearchServiceImpl.log
                        .info( "Expression Experiment database search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );

            /*
             * If we get results here, probably we want to just stop immediately, because the user is searching for
             * something exact. In response to https://github.com/PavlidisLab/Gemma/issues/140 we continue if the user
             * has admin status.
             */
            if ( !results.isEmpty() && !SecurityUtil.isUserAdmin() ) {
                return results;
            }

            BlacklistedEntity b = blacklistedEntityService.findByAccession( prepareDatabaseQuery( settings ) );
            if ( b != null ) {
                results.add( SearchResult.from( ExpressionExperiment.class, b.getId(), DatabaseSearchSource.MATCH_BY_ACCESSION_SCORE, "Blacklisted accessions are not loaded into Gemma", "BlackListDao.findByAccession" ) );
                return results;
            }

            watch.reset();
            watch.start();
        }

        // special case: search for experiments associated with genes
        Collection<SearchResult<Gene>> geneHits = this.geneSearch( settings.withMode( SearchSettings.SearchMode.FAST ) );
        if ( geneHits.size() > 0 ) {
            // TODO: make sure this is being hit correctly.
            for ( SearchResult<Gene> gh : geneHits ) {
                Gene g = gh.getResultObject();
                if ( g == null ) {
                    continue;
                }
                Integer ncbiGeneId = g.getNcbiGeneId();
                String geneUri = "http://" + NCBI_GENE + "/" + ncbiGeneId; // this is just enough to fool the search into looking by NCBI ID, but check working as expected
                SearchSettings gss = SearchSettings.expressionExperimentSearch( geneUri );
                gss.setMaxResults( settings.getMaxResults() );
                gss.setTaxon( settings.getTaxon() );
                gss.setQuery( geneUri );
                // FIXME: there should be a nicer, typed way of doing ontology searches
                results.addAll( ontologyUriSearch( gss ).get( ExpressionExperiment.class ) );
            }
        }

        // fancy search that uses ontologies to infer related terms
        if ( settings.isUseCharacteristics() ) {
            results.addAll( this.characteristicEESearch( settings ) );
            if ( watch.getTime() > 500 )
                SearchServiceImpl.log
                        .info( "Expression Experiment search via characteristics for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
        }

        // searches for strings in associated free text including factorvalues and biomaterials
        // we have toyed with having this be done before the characteristic search
        if ( settings.isUseIndices() && !isFilled( results, settings ) ) {
            results.addAll( this.compassSearchSource.searchExpressionExperiment( settings ) );
            if ( watch.getTime() > 500 )
                SearchServiceImpl.log
                        .info( "Expression Experiment index search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );
            watch.reset();
            watch.start();
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
        if ( results.isEmpty() ) {
            watch.reset();
            watch.start();
            Collection<SearchResult<ArrayDesign>> matchingPlatforms = this.arrayDesignSearch( settings, null );
            for ( SearchResult<ArrayDesign> adRes : matchingPlatforms ) {
                ArrayDesign ad = adRes.getResultObject();
                if ( ad != null ) {
                    Collection<ExpressionExperiment> expressionExperiments = this.arrayDesignService
                            .getExpressionExperiments( ad );
                    if ( expressionExperiments.size() > 0 )
                        results.addAll( this.dbHitsToSearchResult( ExpressionExperiment.class, expressionExperiments,
                                0.8, ad.getShortName() + " - " + ad.getName(), String.format( "ArrayDesignService.getExpressionExperiments(%s)", ad ) ) );
                }
            }
            if ( watch.getTime() > 500 )
                SearchServiceImpl.log
                        .info( "Expression Experiment platform search for '" + settings + "' took " + watch.getTime()
                                + " ms, " + results.size() + " hits." );

            if ( !results.isEmpty() ) {
                return results;
            }
        }

        if ( !settings.isFillResults() ) {
            results.forEach( sr -> sr.setResultObject( null ) );
        }

        if ( totalTime.getTime() > 500 )
            SearchServiceImpl.log
                    .info( ">>>>>>> Expression Experiment search for '" + settings + "' took " + watch.getTime()
                            + " ms, " + results.size() + " hits." );

        return results;

    }

    //    /**
    //     *
    //     * @param  classes
    //     * @param  characteristic2entity
    //     * @return
    //     */
    //    private Collection<SearchResult> filterCharacteristicOwnersByClass( Map<Class<?>, Collection<Long>> parents, String uri, String value ) {
    //
    //        StopWatch t = this.startTiming();
    //        Map<Long, SearchResult> biomaterials = new HashMap<>();
    //        Map<Long, SearchResult> factorValues = new HashMap<>();
    //        Collection<SearchResult> results = new HashSet<>();
    //
    //        for ( Class<?> clazz : parents.keySet() ) {
    //            for ( Long id : parents.get( clazz ) ) {
    //                String matchedText;
    //
    //                if ( StringUtils.isNotBlank( uri ) ) {
    //                    matchedText = "Tagged term: <a href=\"" + Settings.getRootContext() + "/searcher.html?query=" + uri + "\">" + value + "</a>";
    //                } else {
    //                    matchedText = "Free text: " + value;
    //                }
    //
    //                if ( clazz.isAssignableFrom( BioMaterial.class ) ) {
    //                    biomaterials.put( id, new SearchResult( clazz, id, 1.0, matchedText ) );
    //                } else if ( clazz.isAssignableFrom( FactorValue.class ) ) {
    //                    factorValues.put( id, new SearchResult( clazz, id, 1.0, matchedText ) );
    //                } else if ( clazz.isAssignableFrom( ExpressionExperiment.class ) ) {
    //                    results.add( new SearchResult( clazz, id, 1.0, matchedText ) );
    //                } else {
    //                    throw new IllegalStateException();
    //                }
    //            }
    //
    //        }
    //
    //        this.addEEByFactorvalueIds( results, factorValues );
    //
    //        this.addEEByBiomaterialIds( results, biomaterials );
    //
    //        if ( t.getTime() > 500 ) {
    //            log.info( "Retrieving experiments associated with characteristics:  " + t.getTime() + "ms" );
    //        }
    //
    //        return results;
    //
    //    }

    /**
     * Makes no attempt at resolving the search query as a URI. Will tokenize the search query if there are control
     * characters in the String. URI's will get parsed into multiple query terms and lead to bad results.
     * <p>
     * Will try to resolve general terms like brain --> to appropriate OntologyTerms and search for objects tagged with
     * those terms (if isUseCharacte = true)
     */
    private SearchResultMap generalSearch( SearchSettings settings ) throws SearchException {
        // If nothing to search return nothing.
        if ( StringUtils.isBlank( settings.getQuery() ) ) {
            return new SearchResultMapImpl();
        }

        // attempt to infer a taxon from the query if missing
        if ( settings.getTaxon() == null ) {
            settings.setTaxon( inferTaxon( settings ) );
        }

        LinkedHashSet<SearchResult<?>> rawResults = new LinkedHashSet<>();

        // do gene first before we munge the query too much.
        this.accreteResultsGenes( rawResults, settings );

        this.accreteResultsOthers(
                rawResults,
                settings );

        return groupAndSortResultsByType( rawResults, settings );
    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
     */
    private Collection<SearchResult<Gene>> geneSearch( final SearchSettings settings ) throws SearchException {

        StopWatch watch = StopWatch.createStarted();

        Collection<SearchResult<Gene>> geneDbList = this.databaseSearchSource.searchGene( settings );

        if ( settings.getMode() == SearchSettings.SearchMode.FAST && geneDbList.size() > 0 ) {
            return geneDbList;
        }

        Set<SearchResult<Gene>> combinedGeneList = new HashSet<>( geneDbList );

        Collection<SearchResult<Gene>> geneCompassList = this.compassSearchSource.searchGene( settings );
        combinedGeneList.addAll( geneCompassList );

        if ( combinedGeneList.isEmpty() ) {
            Collection<SearchResult<?>> geneCsList = this.databaseSearchSource.searchCompositeSequenceAndGene( settings );
            for ( SearchResult<?> res : geneCsList ) {
                if ( Gene.class.isAssignableFrom( res.getResultType() ) )
                    //noinspection unchecked
                    combinedGeneList.add( ( SearchResult<Gene> ) res );
            }
        }

        /*
         * Possibly search for genes linked via a phenotype, but only if we don't have anything here.
         *
         */
        if ( combinedGeneList.isEmpty() ) {
            Collection<CharacteristicValueObject> phenotypeTermHits;
            try {
                // FIXME: add support for OR, but there's a bug in baseCode that prevents this https://github.com/PavlidisLab/baseCode/issues/22
                String query = settings.getQuery().replaceAll( "\\s+OR\\s+", "" );
                phenotypeTermHits = this.phenotypeAssociationManagerService
                        .searchInDatabaseForPhenotype( query, settings.getMaxResults() );
            } catch ( OntologySearchException e ) {
                throw new BaseCodeOntologySearchException( e );
            }

            for ( CharacteristicValueObject phenotype : phenotypeTermHits ) {
                Set<String> phenotypeUris = new HashSet<>();
                phenotypeUris.add( phenotype.getValueUri() );

                // DATABASE HIT!
                Collection<GeneEvidenceValueObject> phenotypeGenes = phenotypeAssociationManagerService
                        .findCandidateGenes( phenotypeUris, settings.getTaxon() );

                if ( !phenotypeGenes.isEmpty() ) {
                    SearchServiceImpl.log.info( String.format( "%d genes associated with %s via %s", phenotypeGenes.size(), phenotype, settings ) );

                    for ( GeneEvidenceValueObject gvo : phenotypeGenes ) {
                        Gene g = Gene.Factory.newInstance();
                        g.setId( gvo.getId() );
                        g.setTaxon( settings.getTaxon() );
                        // if ( gvo.getScore() != null ) {
                        // TODO If we get evidence quality, use that in the score.
                        // }
                        combinedGeneList.add( SearchResult.from( Gene.class, g, 1.0, phenotype.getValue() + " (" + phenotype.getValueUri() + ")", "PhenotypeAssociationManagerService.findCandidateGenes" ) );
                    }
                    if ( combinedGeneList.size() > 100 /* some limit */ ) {
                        break;
                    }
                }
            }
        }

        if ( watch.getTime() > 1000 )
            SearchServiceImpl.log
                    .info( "Gene search for " + settings + " took " + watch.getTime() + " ms; " + combinedGeneList
                            .size() + " results." );
        return combinedGeneList;
    }

    private Collection<SearchResult<GeneSet>> geneSetSearch( SearchSettings settings ) throws SearchException {
        return searchSource.searchGeneSet( settings );
    }

    //    /**
    //     * Given classes to search and characteristics (experiment search)
    //     *
    //     * @param classes Which classes of entities to look for
    //     */
    //    private Collection<SearchResult> getAnnotatedEntities( Collection<Class<?>> classes,
    //            Collection<Characteristic> cs ) {
    //
    //        //  time-critical
    //        Map<Characteristic, Object> characteristic2entity = characteristicService.getParents( classes, cs );
    //        Collection<SearchResult> matchedEntities = this
    //                .filterCharacteristicOwnersByClass( classes, characteristic2entity );
    //
    //        if ( SearchServiceImpl.log.isDebugEnabled() ) {
    //            this.debugParentFetch( characteristic2entity );
    //        }
    //        return matchedEntities;
    //    }

    /**
     * @return a collection of SearchResults holding all the genes resulting from the search with given SearchSettings.
     */
    private Collection<SearchResult<Gene>> getGenesFromSettings( SearchSettings settings ) throws SearchException {
        Collection<SearchResult<Gene>> genes = null;
        if ( settings.hasResultType( Gene.class ) ) {
            genes = this.geneSearch( settings );
        }
        return genes;
    }

    //    /**
    //     * @return List of ids for the entities held by the search results.
    //     */
    //    private List<Long> getIds( List<SearchResult> searchResults ) {
    //        List<Long> list = new ArrayList<>();
    //        for ( SearchResult r : searchResults ) {
    //            list.add( r.getId() );
    //        }
    //        assert list.size() == searchResults.size();
    //        return list;
    //    }

    /**
     * Group and sort results by type.
     *
     * @return map of result entity class (e.g. BioSequence or ExpressionExperiment) to SearchResult
     * @see SearchResult#getResultType()
     */
    private static SearchResultMap groupAndSortResultsByType(
            LinkedHashSet<SearchResult<?>> rawResults,
            SearchSettings settings ) {

        SearchResultMapImpl results = new SearchResultMapImpl();
        List<SearchResult<?>> sortedRawResults = rawResults.stream().sorted().collect( Collectors.toList() );

        // Get the top N results for each class.
        for ( SearchResult<?> sr : sortedRawResults ) {
            if ( !isFilled( results.get( sr.getResultType() ), settings ) ) {
                results.add( sr );
            }
        }

        return results;
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

    /**
     * @return results, if the settings.termUri is populated. This includes gene uris.
     */
    private SearchResultMap ontologyUriSearch( SearchSettings settings ) throws SearchException {
        SearchResultMapImpl results = new SearchResultMapImpl();

        // 1st check to see if the query is a URI (from an ontology).
        // Do this by seeing if we can find it in the loaded ontologies.
        // Escape with general utilities because might not be doing a lucene backed search. (just a hibernate one).
        String termUri = settings.getQuery();

        if ( !settings.isTermQuery() ) {
            return results;
        }

        String uriString = StringEscapeUtils.escapeJava( StringUtils.strip( termUri ) );

        /*
         * Gene search. We want experiments that are annotated. But also genes.
         */
        if ( StringUtils.containsIgnoreCase( uriString, SearchServiceImpl.NCBI_GENE ) ) {
            // Perhaps is a valid gene URL. Want to search for the gene in gemma.

            // Get the gene
            String ncbiAccessionFromUri = StringUtils.substringAfterLast( uriString, "/" );
            Gene g = null;

            try {
                g = geneService.findByNCBIId( Integer.parseInt( ncbiAccessionFromUri ) );
            } catch ( NumberFormatException e ) {
                // ok
            }
            if ( g != null ) {

                // 1st get objects tagged with the given gene identifier
                if ( settings.hasResultType( ExpressionExperiment.class ) ) { // FIXME maybe we always want this?
                    Set<SearchResult<ExpressionExperiment>> eeHits = new SearchResultSet<>();
                    Map<String, String> uri2value = new HashMap<>();
                    uri2value.put( termUri, g.getOfficialSymbol() );
                    this.findExperimentsByUris( Collections.singleton( termUri ), eeHits, 1.0, uri2value, settings );

                    if ( !eeHits.isEmpty() ) {
                        results.addAll( eeHits );
                    }
                }

                ////
                if ( settings.hasResultType( Gene.class ) ) {
                    results.add( SearchResult.from( Gene.class, g, DatabaseSearchSource.MATCH_BY_ID_SCORE, "GeneService.findByNCBIId" ) );

                }
            }
            return results;
        }

        /*
         * Not searching for a gene. Only other option is a direct URI search for experiments.
         */
        if ( settings.hasResultType( ExpressionExperiment.class ) ) {
            results.addAll( this.characteristicEESearchTerm( settings.withQuery( uriString ) ) );
        }

        return results;
    }

    //    /**
    //     * Retrieve entities from the persistent store (if we don't have them already)
    //     */
    //    private Collection<? extends Identifiable> retrieveResultEntities( Class<?> entityClass, List<SearchResult> results ) {
    //        List<Long> ids = this.getIds( results );
    //
    //        // FIXME: don't we want value objects?
    //        if ( ExpressionExperiment.class.isAssignableFrom( entityClass ) ) {
    //            return expressionExperimentService.load( ids );
    //        } else if ( ArrayDesign.class.isAssignableFrom( entityClass ) ) {
    //            return arrayDesignService.load( ids );
    //        } else if ( CompositeSequence.class.isAssignableFrom( entityClass ) ) {
    //            return compositeSequenceService.load( ids );
    //        } else if ( BibliographicReference.class.isAssignableFrom( entityClass ) ) {
    //            return bibliographicReferenceService.load( ids );
    //        } else if ( Gene.class.isAssignableFrom( entityClass ) ) {
    //            return geneService.load( ids );
    //        } else if ( BioSequence.class.isAssignableFrom( entityClass ) ) {
    //            return bioSequenceService.load( ids );
    //        } else if ( GeneSet.class.isAssignableFrom( entityClass ) ) {
    //            return geneSetService.load( ids );
    //        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
    //            return experimentSetService.load( ids );
    //        } else if ( Characteristic.class.isAssignableFrom( entityClass ) ) {
    //            Collection<Characteristic> chars = new ArrayList<>();
    //            for ( Long id : ids ) {
    //                chars.add( characteristicService.load( id ) );
    //            }
    //            return chars;
    //        } else if ( CharacteristicValueObject.class.isAssignableFrom( entityClass ) ) {
    //            // TEMP HACK this whole method should not be needed in many cases
    //            Collection<CharacteristicValueObject> chars = new ArrayList<>();
    //            for ( SearchResult result : results ) {
    //                if ( result.getResultClass().isAssignableFrom( CharacteristicValueObject.class ) ) {
    //                    chars.add( ( CharacteristicValueObject ) result.getResultObject() );
    //                }
    //            }
    //            return chars;
    //        } else if ( ExpressionExperimentSet.class.isAssignableFrom( entityClass ) ) {
    //            return experimentSetService.load( ids );
    //        } else if ( BlacklistedEntity.class.isAssignableFrom( entityClass ) ) {
    //            return blackListDao.load( ids );
    //        } else {
    //            throw new UnsupportedOperationException( "Don't know how to retrieve objects for class=" + entityClass );
    //        }
    //    }


    /**
     * Infer a {@link Taxon} from the search settings.
     */
    private Taxon inferTaxon( SearchSettings settings ) {
        // split the query around whitespace characters, limit the splitting to 4 terms (may be excessive)
        // remove quotes and other characters tha can interfere with the exact match
        String[] searchTerms = prepareDatabaseQuery( settings ).split( "\\s+", 4 );

        for ( String term : searchTerms ) {
            if ( nameToTaxonMap.containsKey( term ) ) {
                return nameToTaxonMap.get( term );
            }
        }

        // no match found, on taxon is inferred
        return null;
    }

    /**
     * Check if a collection of search results is already filled.
     *
     * @return true if the search results are filled and cannot accept more results, false otherwise
     */
    private static <T extends Identifiable> boolean isFilled( Collection<SearchResult<T>> results, SearchSettings settings ) {
        return settings.getMaxResults() > 0 && results.size() >= settings.getMaxResults();
    }

    /**
     * Obtain a limit suitable for the given search results and settings.
     *
     * @return the difference between the maximum results and the collection size or -1 if the settings are for
     * unlimited results
     * @throws IllegalArgumentException if the search results are already fully filled as per {@link #isFilled(Collection, SearchSettings)}
     */
    private static <T extends Identifiable> int getLimit( Collection<SearchResult<T>> results, SearchSettings settings ) {
        if ( isFilled( results, settings ) ) {
            throw new IllegalArgumentException( "Search results are already fully filled, have to checked the collection with isFilled()?" );
        }
        return settings.getMaxResults() > 0 ? settings.getMaxResults() - results.size() : -1;
    }
}
