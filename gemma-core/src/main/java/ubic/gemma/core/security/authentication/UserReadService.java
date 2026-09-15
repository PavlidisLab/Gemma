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
package ubic.gemma.core.security.authentication;

import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Read-only retrieval service for {@link User} / {@link UserGroup} / {@link GroupAuthority}.
 * <p>
 * Phase 3 of the {@link UserService} decomposition (strangler fig). This service houses
 * the DAO-bound read cluster previously implemented directly on {@code UserServiceImpl}:
 * {@code load}, {@code loadAll}, {@code findByUserName}, {@code findByEmail},
 * {@code findGroupByName}, {@code groupExists}, {@code findGroupsForUser},
 * {@code listAvailableGroups}, and {@code loadGroupAuthorities}. All methods delegate
 * directly to {@code UserDao} / {@code UserGroupDao} and orchestrate no other
 * collaborators.
 * <p>
 * Write-side methods ({@code create}, {@code update}, {@code delete},
 * {@code addUserToGroup}, {@code removeUserFromGroup}, {@code addGroupAuthority},
 * {@code removeGroupAuthority}) stay on the {@link UserService} facade.
 * <p>
 * <b>Authentication-flow note:</b> {@link UserService} is NOT a Spring Security
 * {@code UserDetailsService} — that role is played by {@code UserManagerImpl}, which
 * depends on {@link UserService} (the facade) for both reads and writes. The facade
 * delegates reads to this service internally, so the login path
 * ({@code UserManagerImpl.loadUserByUsername} → {@code userService.findByUserName} +
 * {@code userService.loadGroupAuthorities}) continues to work unchanged. Callers
 * should generally keep using {@link UserService} as the facade — the facade delegates
 * to this service. Direct injection of {@link UserReadService} is appropriate where a
 * class is logically read-only (e.g. REST endpoints that only resolve user IDs).
 * <p>
 * ACL / {@code @Secured} / {@code @PostAuthorize} annotations live on
 * {@link UserService} / {@link BaseUserService} (the caller-facing facade interfaces);
 * enforcement happens at the facade proxy boundary, so this interface is intentionally
 * unsecured at the AOP boundary. The facade declares {@code @Secured("GROUP_USER")} +
 * {@code @PostAuthorize} on {@code findByEmail} and {@code findGroupByName},
 * {@code @Secured("GROUP_ADMIN")} on {@code loadAll}, and
 * {@code AFTER_ACL_COLLECTION_READ} on {@code findGroupsForUser} /
 * {@code listAvailableGroups} — those checks still fire when the facade is the call
 * site; intra-{@code gemma-core} callers that inject this service directly bypass the
 * duplicate ACL check and so MUST be reads that don't need permission filtering, OR
 * must be on the login path where unsecured reads are required by design
 * ({@code findByUserName}, {@code loadGroupAuthorities}).
 *
 * @author paul
 * @see UserService
 */
public interface UserReadService {

    @Nullable
    User load( Long id );

    Collection<User> loadAll();

    /**
     * @return user or null if they don't exist.
     */
    @Nullable
    User findByUserName( String userName );

    @Nullable
    User findByEmail( String email );

    @Nullable
    UserGroup findGroupByName( String name );

    boolean groupExists( String name );

    Collection<UserGroup> findGroupsForUser( User user );

    /**
     * A list of all groups available — facade-level ACL filtering is bypassed here.
     */
    Collection<UserGroup> listAvailableGroups();

    Collection<GroupAuthority> loadGroupAuthorities( User user );
}
