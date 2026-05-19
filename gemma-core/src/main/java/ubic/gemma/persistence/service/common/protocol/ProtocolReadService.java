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
package ubic.gemma.persistence.service.common.protocol;

import org.springframework.lang.Nullable;
import ubic.gemma.model.common.protocol.Protocol;

import java.util.List;

/**
 * Read-only retrieval service for {@link Protocol}.
 * <p>
 * Phase 3 of the {@link ProtocolService} decomposition (strangler fig). This
 * service houses the DAO-bound read cluster previously implemented directly on
 * the {@code ProtocolServiceImpl} facade: {@code findByName} and
 * {@code loadAllUniqueByName}. Both methods delegate straight to
 * {@link ProtocolDao}.
 * <p>
 * Write-side methods (the inherited {@code BaseService} mutators
 * {@code create}, {@code findOrCreate}, {@code remove} from
 * {@code SecurableBaseImmutableService}, which carry {@code @Secured("GROUP_USER")}
 * and {@code @Secured("GROUP_USER","ACL_SECURABLE_EDIT")}) stay on the
 * {@link ProtocolService} facade.
 * <p>
 * Callers should generally keep using {@link ProtocolService} as the facade --
 * the facade delegates to this service. Direct injection is appropriate where a
 * class is logically read-only (CLIs listing protocols, REST endpoints, etc.).
 * <p>
 * ACL / {@code @Secured} annotations live on {@link ProtocolService} (the
 * caller-facing facade interface); enforcement happens at the facade proxy
 * boundary, not here. The read methods on the facade carry
 * {@code @Secured("IS_AUTHENTICATED_ANONYMOUSLY")} plus a
 * {@code @PostAuthorize}/{@code @PostFilter} {@code hasPermission(...,'READ')}
 * check -- the new read impl is unsecured at the AOP boundary on purpose, so
 * intra-{@code gemma-core} callers that already hold an authenticated session
 * bypass duplicate ACL checks.
 *
 * @see ProtocolService
 */
public interface ProtocolReadService {

    @Nullable
    Protocol findByName( String protocolName );

    List<Protocol> loadAllUniqueByName();
}
