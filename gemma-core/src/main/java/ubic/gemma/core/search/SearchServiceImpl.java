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
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Autowired(required = false)
    private PlatformTransactionManager transactionManager;

    /**
     * Used only to ask whether a search result's entity is attached to the ambient session
     * before attempting the entity-path VO conversion. Optional so unit-test contexts that
     * wire no SessionFactory keep the pre-check-free behaviour.
     */
    @Autowired(required = false)
    private SessionFactory sessionFactory;

    /** Composite source assembled from all registered {@link SearchSource} beans. */
    private CompositeSearchSource searchSource;

    /**
     * Sub-transaction template used to isolate the entity-path VO conversion from the outer
     * search transaction. When a converter on a detached entity throws {@code LazyInitException},
     * its own {@code @Transactional} advisor marks the surrounding transaction rollback-only;
     * without containment, the outer commit then fails with {@code UnexpectedRollbackException}
     * even though the catch-and-promote fallback has already produced valid results. Running
     * the brittle convert call in {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} keeps
     * the rollback flag scoped to the sub-transaction. Null in unit-test contexts that don't
     * wire a {@code PlatformTransactionManager}; the entity-path then runs in the outer txn
     * (same behaviour as before — tests don't reproduce the txn-poisoning anyway).
     */
    private TransactionTemplate entityPathTxTemplate;

    private final Map<Class<? extends Identifiable>, Class<? extends IdentifiableValueObject<?>>> supportedResultTypes = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        searchSource = new CompositeSearchSource( searchSources );
        initializeSupportedResultTypes();
        if ( transactionManager != null ) {
            entityPathTxTemplate = new TransactionTemplate( transactionManager );
            entityPathTxTemplate.setPropagationBehavior( TransactionDefinition.PROPAGATION_REQUIRES_NEW );
            entityPathTxTemplate.setReadOnly( true );
            entityPathTxTemplate.setName( "SearchServiceImpl.entityPathVoConvert" );
        }
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

    /**
     * Convert entities to VOs in a {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW}
     * sub-transaction so a {@code LazyInitException} thrown by an inner converter (and
     * promoted to rollback-only by its own {@code @Transactional} advisor) does not poison
     * the outer search transaction. The caller's catch on {@link ConversionFailedException}
     * still sees the wrapped exception and falls through to the id-path. When no
     * {@code PlatformTransactionManager} is wired (unit-test contexts) we run the convert
     * inline; tests don't exercise the @Transactional advice chain that produces the
     * poisoning, so the contained-rollback semantics are moot there.
     */
    @SuppressWarnings("unchecked")
    private List<IdentifiableValueObject<?>> convertEntityPathIsolated( List<Identifiable> entities,
                                                                        TypeDescriptor entityCollectionType,
                                                                        TypeDescriptor voListType ) {
        if ( entityPathTxTemplate == null ) {
            return ( List<IdentifiableValueObject<?>> ) valueObjectConversionService.convert(
                    entities, entityCollectionType, voListType );
        }
        return entityPathTxTemplate.execute( status -> ( List<IdentifiableValueObject<?>> ) valueObjectConversionService.convert(
                entities, entityCollectionType, voListType ) );
    }

    /**
     * Whether the entity is attached to the ambient Hibernate session, and can therefore
     * attempt the entity-path VO conversion at all — see the partition in
     * {@code loadValueObjects}. Answers "attached" when no SessionFactory is wired
     * (unit-test contexts), preserving the pre-check-free behaviour there; answers
     * "detached" when there is a SessionFactory but no ambient session, since nothing
     * lazy could initialize in that situation either.
     */
    private boolean isAttachedToCurrentSession( Identifiable entity ) {
        if ( sessionFactory == null ) {
            return true;
        }
        try {
            return sessionFactory.getCurrentSession().contains( entity );
        } catch ( HibernateException e ) {
            return false;
        }
    }

    /**
     * Walk the cause chain to the deepest Throwable and return its message — used by the
     * VO-conversion fallback to surface "LazyInitializationException: AuditEvent#…" in the
     * log line instead of the wrapping ConversionFailedException's generic stringification.
     */
    private static String rootMessage( Throwable t ) {
        Throwable cur = t;
        while ( cur.getCause() != null && cur.getCause() != cur ) {
            cur = cur.getCause();
        }
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResult<? extends IdentifiableValueObject<?>>> loadValueObjects( Collection<SearchResult<?>> searchResults ) throws IllegalArgumentException {
        if ( searchResults.isEmpty() ) {
            return Collections.emptyList();
        }
        // Group by result type so we can dispatch to the batched collection-based VO converter
        // (ServiceBasedValueObjectConverter#voListFromEntities / #voListFromIds) instead of
        // calling loadValueObject(entity) once per hit. The per-hit path triggers
        // postProcessValueObjects(singletonList(vo)) for each result, which makes the batched
        // IN-queries on Gene/ArrayDesign/ExpressionExperiment fire once per result rather than
        // once per page. See handoffs/RECCE_HSEARCH_NPLUS1.md.
        Map<Class<? extends Identifiable>, List<SearchResult<?>>> byType = new LinkedHashMap<>();
        for ( SearchResult<?> sr : searchResults ) {
            byType.computeIfAbsent( sr.getResultType(), k -> new ArrayList<>() ).add( sr );
        }
        // VO-by-id lookup, keyed by (resultType, id), so we can stitch results back to the
        // original input order. Per-type because two SearchResults could (in principle) share
        // an id across result types — keying by (type,id) keeps the lookup unambiguous.
        Map<Class<? extends Identifiable>, Map<Long, IdentifiableValueObject<?>>> voIndex = new HashMap<>();
        for ( Map.Entry<Class<? extends Identifiable>, List<SearchResult<?>>> e : byType.entrySet() ) {
            Class<? extends Identifiable> resultType = e.getKey();
            List<SearchResult<?>> group = e.getValue();
            Class<? extends IdentifiableValueObject<?>> voType = supportedResultTypes.get( resultType );
            if ( voType == null ) {
                throw new IllegalArgumentException( "Result type " + resultType + " is not supported for VO conversion." );
            }
            // Split into entities-present vs id-only buckets so each side can use its batched converter.
            List<Identifiable> entities = new ArrayList<>( group.size() );
            List<Long> idsOnly = new ArrayList<>();
            for ( SearchResult<?> sr : group ) {
                Identifiable entity = sr.getResultObject();
                // A detached entity cannot survive the entity path: its converter walks lazy
                // associations (the audit events behind a curatable's status), and the
                // HibernateSearch source returns DETACHED entities on the anonymous /search
                // path. Before this check every such result attempted the entity path, failed
                // on LazyInitializationException inside the sub-transaction, was retried once
                // by the generic retry advice, and only then promoted — a WARN and three
                // wasted steps on EVERY search (measured 6/6 on 2026-08-19). Detachment is
                // knowable up front; asking is cheaper than failing. The catch below stays as
                // the backstop for attached entities whose conversion fails anyway.
                if ( entity != null && isAttachedToCurrentSession( entity ) ) {
                    entities.add( entity );
                } else {
                    idsOnly.add( sr.getResultId() );
                }
            }
            Map<Long, IdentifiableValueObject<?>> perType = voIndex.computeIfAbsent( resultType, k -> new HashMap<>() );
            // Entity-path: fast (no re-query) but only safe when the entity returned by the
            // search source still has a Hibernate session attached and any lazy associations
            // its converter walks (e.g. AuditEvent on ExpressionExperiment / ArrayDesign) are
            // initialized. The HibernateSearch source returns detached entities for the
            // anonymous /search path (2026-05-25 frink hit: ConversionFailedException ->
            // LazyInitializationException on AuditEvent). Two layers of defence:
            //   1. Run the convert call in a REQUIRES_NEW sub-transaction so the inner
            //      converter's @Transactional advisor sets rollback-only on the sub-txn, not
            //      the outer search txn — otherwise the outer commit then throws
            //      UnexpectedRollbackException and the whole response (incl. successful
            //      Gene/Taxon/etc. hits) collapses to a 500.
            //   2. On ConversionFailedException, promote the detached entities to id-only
            //      and fall through to the ID path, which fetches a fresh attached set via
            //      load(ids) in the (still-clean) outer transaction.
            if ( !entities.isEmpty() ) {
                try {
                    TypeDescriptor entityCollectionType = TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( resultType ) );
                    TypeDescriptor voListType = TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( voType ) );
                    List<IdentifiableValueObject<?>> vos = convertEntityPathIsolated( entities, entityCollectionType, voListType );
                    if ( vos != null ) {
                        for ( IdentifiableValueObject<?> vo : vos ) {
                            if ( vo != null ) {
                                perType.put( vo.getId(), vo );
                            }
                        }
                    }
                } catch ( ConversionFailedException convEx ) {
                    log.warn( "Entity-path VO conversion failed for " + resultType.getSimpleName()
                            + " (" + entities.size() + " entities) — promoting to id-path. Cause: "
                            + rootMessage( convEx ) );
                    for ( Identifiable ent : entities ) {
                        if ( ent.getId() != null ) {
                            idsOnly.add( ent.getId() );
                        }
                    }
                } catch ( ConverterNotFoundException ex ) {
                    throw new IllegalArgumentException( "Result type " + resultType + " is not supported for VO conversion.", ex );
                }
            }
            if ( !idsOnly.isEmpty() ) {
                try {
                    TypeDescriptor idCollectionType = TypeDescriptor.collection( Collection.class, TypeDescriptor.valueOf( Long.class ) );
                    TypeDescriptor voListType = TypeDescriptor.collection( List.class, TypeDescriptor.valueOf( voType ) );
                    @SuppressWarnings("unchecked")
                    List<IdentifiableValueObject<?>> vos = ( List<IdentifiableValueObject<?>> ) valueObjectConversionService.convert( idsOnly, idCollectionType, voListType );
                    if ( vos != null ) {
                        for ( IdentifiableValueObject<?> vo : vos ) {
                            if ( vo != null ) {
                                perType.put( vo.getId(), vo );
                            }
                        }
                    }
                } catch ( ConverterNotFoundException ex ) {
                    throw new IllegalArgumentException( "Result type " + resultType + " is not supported for VO conversion.", ex );
                }
            }
        }
        // Reassemble in original iteration order. Results whose entity has been removed
        // (VO lookup miss) come through with a null result object — matching the prior
        // contract where loadValueObject could return a SearchResult wrapping null.
        List<SearchResult<? extends IdentifiableValueObject<?>>> out = new ArrayList<>( searchResults.size() );
        for ( SearchResult<?> sr : searchResults ) {
            Map<Long, IdentifiableValueObject<?>> perType = voIndex.get( sr.getResultType() );
            IdentifiableValueObject<?> vo = perType != null ? perType.get( sr.getResultId() ) : null;
            out.add( sr.withResultObject( vo ) );
        }
        return out;
    }
}
