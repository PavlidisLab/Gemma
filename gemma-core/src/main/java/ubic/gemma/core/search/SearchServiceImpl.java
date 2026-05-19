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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Free-text search service: delegates per-result-type lookups to a {@link CompositeSearchSource}
 * composed of all registered {@link SearchSource} beans (e.g. {@code HibernateSearchSource},
 * {@code DatabaseSearchSource}). Preserves the value-object conversion path on the way out.
 *
 * <p>Restored as part of HS-7 search restoration Step 3 (see SEARCH_RECCE.md). The pre-strip
 * convenience surface (taxon inference from query, dedicated blacklist gating, ontology
 * single-term expansion) is deliberately deferred until the ontology source comes back in
 * Step 3+ / Section 6.</p>
 *
 * @author klc
 * @author paul
 * @author keshav
 */
@Service
@Slf4j
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

        private <T extends Identifiable> void addAll( Collection<SearchResult<T>> sr ) {
            for ( SearchResult<T> r : sr ) {
                super.add( r.getResultType(), r );
            }
        }
    }

    @Autowired
    private List<SearchSource> searchSources;

    @Autowired
    @Qualifier("valueObjectConversionService")
    private ConversionService valueObjectConversionService;

    /** Composite source assembled from all registered {@link SearchSource} beans. */
    private CompositeSearchSource searchSource;

    private final Map<Class<? extends Identifiable>, Class<? extends IdentifiableValueObject<?>>> supportedResultTypes = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        searchSource = new CompositeSearchSource( searchSources );
        initializeSupportedResultTypes();
    }

    @Override
    public Set<String> getFields( Class<? extends Identifiable> resultType, SearchSettings.SearchMode searchMode ) {
        return searchSources.stream()
                .filter( s -> s instanceof FieldAwareSearchSource )
                .map( s -> ( ( FieldAwareSearchSource ) s ).getFields( resultType, searchMode ) )
                .flatMap( Set::stream )
                .collect( Collectors.toSet() );
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResultMap search( SearchSettings settings, SearchContext context ) throws SearchException {
        SearchResultMapImpl results = new SearchResultMapImpl();
        if ( StringUtils.isBlank( settings.getQuery() ) ) {
            return results;
        }
        // Note: per-result-type dispatch mirrors the pre-strip behaviour. Composite is responsible
        // for actually fanning out to each accepting source (Hibernate Search + DAO fallback).
        if ( settings.hasResultType( ArrayDesign.class ) ) {
            results.addAll( searchSource.searchArrayDesign( settings, context ) );
        }
        if ( settings.hasResultType( BibliographicReference.class ) ) {
            results.addAll( searchSource.searchBibliographicReference( settings, context ) );
        }
        if ( settings.hasResultType( ExpressionExperimentSet.class ) ) {
            results.addAll( searchSource.searchExperimentSet( settings, context ) );
        }
        if ( settings.hasResultType( BioSequence.class ) ) {
            results.addAll( searchSource.searchBioSequence( settings, context ) );
        }
        if ( settings.hasResultType( CompositeSequence.class ) ) {
            results.addAll( searchSource.searchCompositeSequence( settings, context ) );
        }
        if ( settings.hasResultType( ExpressionExperiment.class ) ) {
            results.addAll( searchSource.searchExpressionExperiment( settings, context ) );
        }
        if ( settings.hasResultType( Gene.class ) ) {
            results.addAll( searchSource.searchGene( settings, context ) );
        }
        if ( settings.hasResultType( GeneSet.class ) ) {
            results.addAll( searchSource.searchGeneSet( settings, context ) );
        }
        if ( settings.hasResultType( BlacklistedEntity.class ) ) {
            results.addAll( searchSource.searchBlacklistedEntities( settings, context ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResultMap search( SearchSettings settings ) throws SearchException {
        return search( settings, new SearchContext( null, null ) );
    }

    @Override
    public Set<Class<? extends Identifiable>> getSupportedResultTypes() {
        return supportedResultTypes.keySet();
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

    private void canConvertFromId( Class<? extends IdentifiableValueObject<?>> to ) {
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
            T resultObject = searchResult.getResultObject();
            //noinspection unchecked
            return searchResult.withResultObject( ( U ) valueObjectConversionService.convert(
                    resultObject != null ? resultObject : searchResult.getResultId(),
                    supportedResultTypes.get( searchResult.getResultType() ) ) );
        } catch ( ConverterNotFoundException e ) {
            throw new IllegalArgumentException( "Result type " + searchResult.getResultType() + " is not supported for VO conversion.", e );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResult<? extends IdentifiableValueObject<?>>> loadValueObjects( Collection<SearchResult<?>> searchResults ) throws IllegalArgumentException {
        return searchResults.stream()
                .map( sr -> ( SearchResult<? extends IdentifiableValueObject<?>> ) loadValueObject( sr ) )
                .collect( Collectors.toList() );
    }
}
