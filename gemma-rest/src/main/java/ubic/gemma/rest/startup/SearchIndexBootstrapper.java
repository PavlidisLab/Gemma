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
package ubic.gemma.rest.startup;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.indexer.IndexerService;
import ubic.gemma.model.common.Identifiable;

import java.util.Collection;

/**
 * Boot-time check: enumerate every {@code @Indexed} entity Hibernate Search 7 knows
 * about and trigger a one-shot mass-reindex for any that come back with zero documents.
 * Runs once per JVM after the Spring context is fully ready.
 * <p>
 * Rationale: on a fresh container (new {@code /data/gemma} volume, never had
 * {@code searchIndex} run against it) the per-entity Lucene directories are empty,
 * so curation-UI typeaheads against {@code /genes/search}, {@code /datasets/search},
 * etc. return {@code []}. Forcing a fresh deploy through the legacy
 * {@code IndexGemmaCLI} just to make search work is a deployment-time footgun
 * we keep stepping on; doing it lazily at first boot makes the container
 * "search-ready" by the time the admin can poke at it.
 * <p>
 * Disable with {@code gemma.search.bootstrap.enabled=false} (or
 * {@code -Dgemma.search.bootstrap.enabled=false}) if you need to manage indexing
 * out-of-band — e.g. you maintain a known-good index volume across restarts.
 * Heavy indices (gene + dataset) take minutes; light ones finish in seconds.
 *
 * @author phase 3 / first-boot search bring-up
 */
@Component
@Slf4j
public class SearchIndexBootstrapper {

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private IndexerService indexerService;

    @Value("${gemma.search.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    private volatile boolean ran = false;

    /**
     * Fires once after the Spring context refreshes. {@link ContextRefreshedEvent} can
     * fire more than once on multi-context setups (e.g. WAR + servlet child context); the
     * {@link #ran} guard ensures we only survey + reindex on the first refresh.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onApplicationReady() {
        if ( ran ) {
            return;
        }
        ran = true;
        if ( !bootstrapEnabled ) {
            log.info( "Search-index bootstrap is disabled (gemma.search.bootstrap.enabled=false); "
                    + "rely on IndexGemmaCLI or POST /admin/search/indices to populate." );
            return;
        }
        SearchMapping mapping = Search.mapping( sessionFactory );
        Collection<? extends SearchIndexedEntity<?>> indexed = mapping.allIndexedEntities();
        if ( indexed.isEmpty() ) {
            log.warn( "Search-index bootstrap: no @Indexed entities were discovered by Hibernate Search; nothing to do." );
            return;
        }
        // Survey current doc counts, collect the empties, then dispatch a
        // SINGLE virtual thread that reindexes them sequentially. We learned
        // the hard way (2026-05-25 frink boot) that fanning out one VT per
        // entity exhausts the Hibernate JDBC pool: each MassIndexer grabs a
        // connection for its run, and with ~17 indexed entities the 10-slot
        // pool times out the rest after 10s. Serial is fine — first-boot
        // indexing isn't on a hot path and the cumulative wall-time is what
        // we'd budget anyway.
        java.util.List<Class<? extends Identifiable>> needsRebuild = new java.util.ArrayList<>();
        try ( Session session = sessionFactory.openSession() ) {
            for ( SearchIndexedEntity<?> ent : indexed ) {
                Class<?> raw = ent.javaClass();
                if ( !Identifiable.class.isAssignableFrom( raw ) ) {
                    log.debug( "Search-index bootstrap: skipping {} — not an Identifiable.", raw.getName() );
                    continue;
                }
                @SuppressWarnings("unchecked")
                Class<? extends Identifiable> clazz = ( Class<? extends Identifiable> ) raw;
                long count;
                try {
                    count = Search.session( session )
                            .search( clazz )
                            .where( f -> f.matchAll() )
                            .fetchTotalHitCount();
                } catch ( RuntimeException e ) {
                    log.warn( "Search-index bootstrap: couldn't count {} (likely missing/corrupt index, will rebuild): {}",
                            clazz.getSimpleName(), e.getMessage() );
                    count = 0;
                }
                if ( count > 0 ) {
                    log.info( "Search-index bootstrap: {} index has {} docs — skipping.", clazz.getSimpleName(), count );
                    continue;
                }
                needsRebuild.add( clazz );
            }
        }
        if ( needsRebuild.isEmpty() ) {
            log.info( "Search-index bootstrap: every @Indexed entity has docs; nothing to rebuild." );
            return;
        }
        log.info( "Search-index bootstrap: {} indices are empty — kicking off serial mass-reindex in background ({}).",
                needsRebuild.size(),
                needsRebuild.stream().map( Class::getSimpleName ).collect( java.util.stream.Collectors.joining( ", " ) ) );
        Thread.startVirtualThread( () -> {
            for ( Class<? extends Identifiable> clazz : needsRebuild ) {
                log.info( "Search-index bootstrap: reindexing {} …", clazz.getSimpleName() );
                try {
                    indexerService.index( clazz );
                    log.info( "Search-index bootstrap: {} reindex completed.", clazz.getSimpleName() );
                } catch ( RuntimeException e ) {
                    log.error( "Search-index bootstrap: {} reindex failed.", clazz.getSimpleName(), e );
                }
            }
            log.info( "Search-index bootstrap: all queued reindexes finished." );
        } );
    }
}
