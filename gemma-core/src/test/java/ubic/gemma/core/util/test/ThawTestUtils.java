/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.util.test;

import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.function.Function;

/**
 * Helpers for tests that need to assert lazy-association initialization state
 * before and after a service's {@code thaw(...)} call.
 *
 * <p>The default test transactional session keeps the entity managed with its
 * bag often pre-hydrated from cascade-saves earlier in the same test method.
 * That makes "is bag NOT initialized" assertions fail before the {@code thaw()}
 * even runs. {@code BaseDatabaseTest5} disables L2 cache, so the staleness lives
 * entirely in the first-level cache.
 *
 * <p>Opening a fresh {@link Session} sidesteps that first-level cache and gives
 * a clean view of the entity, with all lazy associations uninitialized. The
 * session is closed before returning, so the result is detached — fine for
 * downstream {@code service.thaw(...)} calls that re-attach via
 * {@code ensureInSession()} inside their own read-only transaction, and fine
 * for {@link org.hibernate.Hibernate#isInitialized} which is safe on detached
 * proxies.
 *
 * @author Gemma
 */
public final class ThawTestUtils {

    private ThawTestUtils() {}

    /**
     * Load an entity in a fresh {@link Session}, then close that session, returning
     * a detached instance whose lazy associations are unrealised.
     *
     * @param sessionFactory the test session factory
     * @param type           entity class
     * @param id             primary key
     * @param <T>            entity type
     * @return the detached instance with all lazy associations uninitialized
     */
    public static <T> T loadDetachedInFreshSession( SessionFactory sessionFactory, Class<T> type, Long id ) {
        try ( Session session = sessionFactory.openSession() ) {
            return session.get( type, id );
        }
    }

    /**
     * Run a query inside a fresh {@link Session} and return its result detached.
     * The query function executes against a brand-new session (independent of
     * the thread-bound transactional session) so any entities it loads come
     * back with their lazy associations uninitialized. The session is closed in
     * a try-with-resources after the function returns.
     *
     * @param sessionFactory the test session factory
     * @param query          function that issues a query against the fresh session
     * @param <T>            result type
     * @return the value produced by {@code query}, with proxies detached
     */
    public static <T> T queryDetachedInFreshSession( SessionFactory sessionFactory, Function<Session, T> query ) {
        try ( Session session = sessionFactory.openSession() ) {
            return query.apply( session );
        }
    }
}
