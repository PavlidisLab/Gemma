/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only retrieval service for {@link Characteristic} and {@link Statement}.
 * <p>
 * Phase 3 of the {@link CharacteristicService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code CharacteristicServiceImpl} facade: {@code browse}, {@code findExperimentsByUris},
 * {@code findByParentClasses}, {@code findByUri}, {@code findBestByUri},
 * {@code findByValueStartingWith}, {@code findByValueLike},
 * {@code findByValueUriOrValueStartingWith}, {@code countByValueUri},
 * {@code findValueGroupedByValueUri}, {@code getParents}, {@code findByCategoryStartingWith},
 * {@code findByCategoryUri}, {@code findByAnyValue}, {@code findByAnyValueStartingWith},
 * {@code findByAnyUri}, {@code findByPredicate}, {@code findByPredicateUri},
 * {@code findByObject}, and {@code findByObjectUri}. All methods delegate to
 * {@link CharacteristicDao} and {@code StatementDao} (with simple aggregation /
 * Hibernate-initialize wrapping where appropriate) and orchestrate no other collaborators.
 * <p>
 * Write-side methods ({@code create}, {@code save}, {@code update}, {@code remove}, plus
 * the inherited {@code BaseService} mutators) stay on the {@link CharacteristicService}
 * facade.
 * <p>
 * Callers should generally keep using {@link CharacteristicService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class
 * is logically read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link CharacteristicService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured.
 *
 * @see CharacteristicService
 */
public interface CharacteristicReadService {

    /**
     * Browse through the characteristics, excluding GO annotations.
     */
    List<Characteristic> browse( int start, int limit );

    /**
     * Browse through the characteristics, excluding GO annotations.
     */
    List<Characteristic> browse( int start, int limit, String sortField, boolean descending );

    /**
     * @see CharacteristicDao#findExperimentsByUris(Collection, boolean, boolean, boolean, Taxon, int, boolean)
     */
    Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean loadEEs, boolean rankByLevel );

    /**
     * Find characteristics that have a particular parent class or lack thereof.
     */
    Collection<Characteristic> findByParentClasses( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, @Nullable String category, int maxResults );

    /**
     * Looks for an exact match of the given string to a valueUri in the characteristic database.
     */
    Collection<Characteristic> findByUri( String uri, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Find the best possible characteristic for a given URI.
     */
    @Nullable
    Characteristic findBestByUri( String uri );

    /**
     * Returns a collection of characteristics that have a value starting with the given string.
     */
    Collection<Characteristic> findByValueStartingWith( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Returns a collection of characteristics that have a value matching the given SQL {@code LIKE} pattern.
     */
    Collection<Characteristic> findByValueLike( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Find characteristics that have a value (prefix) or value URI (exact match) matching the given string.
     */
    Map<String, Characteristic> findByValueUriOrValueStartingWith( String search, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    Map<String, Long> countByValueUri( Collection<String> uris, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents );

    /**
     * @see CharacteristicDao#findValueGroupedByValueUri(Collection, boolean, boolean, boolean, int)
     */
    Map<String, String> findValueGroupedByValueUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean includePredicates, boolean includeObjects, int maxResults );

    /**
     * @param thawParents if true, the parents will be initialized if they are proxies
     * @see CharacteristicDao#getParents(Collection, Collection, boolean)
     */
    Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean thawParents );

    Collection<Characteristic> findByCategoryStartingWith( String queryPrefix, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    Collection<Characteristic> findByCategoryUri( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults );

    /**
     * Find a characteristic by any value it contains including its category, value, predicates and objects.
     */
    Collection<? extends Characteristic> findByAnyValue( String value );

    /**
     * Find a characteristic by any value it contains including its category, value, predicates and objects that starts
     * with the given query.
     */
    Collection<? extends Characteristic> findByAnyValueStartingWith( String value );

    /**
     * Find a characteristic or statement by any URI it contains including its category, value, predicates and objects.
     */
    Collection<? extends Characteristic> findByAnyUri( String uri );

    Collection<Statement> findByPredicate( String value );

    Collection<Statement> findByPredicateUri( String uri );

    Collection<Statement> findByObject( String value );

    Collection<Statement> findByObjectUri( String uri );
}
