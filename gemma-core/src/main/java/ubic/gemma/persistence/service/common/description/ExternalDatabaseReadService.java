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

import ubic.gemma.model.common.description.ExternalDatabase;

import java.util.Collection;
import java.util.List;

/**
 * Read-only retrieval service for {@link ExternalDatabase}.
 * <p>
 * Phase 3 of the {@link ExternalDatabaseService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on the
 * {@code ExternalDatabaseServiceImpl} facade: {@code loadAllWithAuditTrail},
 * {@code loadWithExternalDatabases}, {@code findByName},
 * {@code findByNameWithExternalDatabases}, {@code findByNameWithAuditTrail}, and
 * {@code findAllByNameIn}. All methods delegate to {@link ExternalDatabaseDao} (with
 * simple {@link org.hibernate.Hibernate#initialize(Object)} wrapping where appropriate)
 * and orchestrate no other collaborators.
 * <p>
 * Write-side methods ({@code create}, {@code findOrCreate}, {@code update},
 * {@code updateReleaseDetails}, {@code updateReleaseLastUpdated}, {@code remove}, plus
 * the inherited {@code BaseService} mutators) stay on the {@link ExternalDatabaseService}
 * facade.
 * <p>
 * Callers should generally keep using {@link ExternalDatabaseService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a class
 * is logically read-only (REST endpoints, CLIs, browser controllers, intra-core readers).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ExternalDatabaseService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy boundary,
 * so this interface is intentionally unsecured. (The facade declares
 * {@code @Secured("GROUP_ADMIN")} on {@code loadAllWithAuditTrail} and
 * {@code @Secured("GROUP_AGENT")} on {@code findByNameWithAuditTrail} -- those checks
 * still fire when the facade is the call site; intra-{@code gemma-core} callers that
 * inject this service directly bypass the duplicate ACL check.)
 *
 * @see ExternalDatabaseService
 */
public interface ExternalDatabaseReadService {

    Collection<ExternalDatabase> loadAllWithAuditTrail();

    ExternalDatabase loadWithExternalDatabases( Long id );

    ExternalDatabase findByName( String name );

    ExternalDatabase findByNameWithExternalDatabases( String name );

    ExternalDatabase findByNameWithAuditTrail( String name );

    List<ExternalDatabase> findAllByNameIn( List<String> names );
}
