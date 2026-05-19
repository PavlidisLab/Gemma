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
import ubic.gemma.model.common.description.DatabaseEntry;

import java.util.Collection;
import java.util.List;

/**
 * Read-only retrieval service for {@link DatabaseEntry}.
 * <p>
 * Phase 3 of the {@link DatabaseEntryService} decomposition (strangler fig). Houses the
 * DAO-bound read cluster previously implemented directly on the
 * {@code DatabaseEntryServiceImpl} facade: {@code load(Long)}, {@code loadAll()},
 * {@code countAll()}, {@code findByAccession(String)} and the only non-inherited facade
 * read, {@code findLatestByAccession(String)}. All methods delegate to
 * {@link DatabaseEntryDao} and orchestrate no other collaborators.
 * <p>
 * Write-side methods ({@code create}, {@code findOrCreate}, {@code remove}, plus the
 * inherited {@code BaseImmutableService} mutators) and the {@code FilteringVoEnabledService}
 * VO-with-filters surface stay on the {@link DatabaseEntryService} facade. (The facade is
 * what {@code DatabaseEntryArgService} -- and through it, {@code AbstractEntityArgService}
 * -- is parameterised on; that filtering surface is not part of the read decomp.)
 * <p>
 * Note: {@code DatabaseEntry} is coupled to {@code ExternalDatabase} via the persister
 * cache mechanism, and {@code CommonPersister.persistDatabaseEntry} stays on the legacy
 * persister path pending the cache-lift pilot. This read service is purely facade-side;
 * the persister arm is not affected.
 * <p>
 * Callers should generally keep using {@link DatabaseEntryService} as the facade -- the
 * facade delegates to this service. Direct injection is appropriate where a class is
 * logically read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link DatabaseEntryService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary, so
 * this interface is intentionally unsecured. (Historically {@code DatabaseEntry} reads were
 * {@code @Secured("IS_AUTHENTICATED_ANONYMOUSLY")} and writes {@code @Secured("GROUP_USER")},
 * but the current facade carries no explicit {@code @Secured} on these methods -- the
 * read/write split here is a refactor only, not an ACL change.)
 *
 * @see DatabaseEntryService
 */
public interface DatabaseEntryReadService {

    @Nullable
    DatabaseEntry load( Long id );

    Collection<DatabaseEntry> loadAll();

    long countAll();

    /**
     * @see DatabaseEntryDao#findByAccession(String)
     */
    List<DatabaseEntry> findByAccession( String accession );

    /**
     * Find the latest (as per its version or ID) database entry by accession.
     */
    @Nullable
    DatabaseEntry findLatestByAccession( String accession );
}
