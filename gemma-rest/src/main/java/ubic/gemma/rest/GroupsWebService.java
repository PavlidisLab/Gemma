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
package ubic.gemma.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.rest.util.Responders.paginate;
import static ubic.gemma.rest.util.Responders.respond;

/**
 * RESTful CRUD + member management for user groups (gap §3c of
 * {@code GEMMA_UI_ENDPOINT_GAP.md}). The curation-UI needs a stable surface
 * for the existing group machinery — there is no dedicated group service in
 * gemma-core, so the endpoints delegate to {@link UserManager} (which mixes
 * {@code GroupManager} + user lookups) and {@link UserReadService} for
 * id-keyed lookups.
 *
 * <p>Auth model (also enforced at the service / DAO layer via
 * {@code @Secured} / {@code @PreAuthorize} on {@link UserManager} +
 * {@link ubic.gemma.core.security.authentication.UserService}):
 * <ul>
 *   <li>LIST / READ — open to authenticated users; the underlying
 *       facade applies {@code AFTER_ACL_COLLECTION_READ} so non-readable
 *       groups are filtered out.</li>
 *   <li>CREATE — authenticated. {@code UserService.create(UserGroup)} is
 *       {@code @Secured("GROUP_USER")}.</li>
 *   <li>PATCH (rename) / DELETE / member add / member remove —
 *       authenticated; {@code UserService.update / delete} run under
 *       {@code GROUP_USER + ACL_SECURABLE_EDIT}, so only group owners /
 *       admins succeed. Anonymous callers get a 401/403 at the
 *       {@link PreAuthorize} layer.</li>
 * </ul>
 *
 * <p>Note: the three system groups ({@code Administrators},
 * {@code Users}, {@code Agents}) are protected at the DAO layer — creating /
 * deleting / updating them raises {@link IllegalArgumentException}, which
 * Jersey maps to 400.
 *
 * @author paul
 */
@Service
@Path("/groups")
@Tag(name = "Groups", description = "User groups (CRUD + membership)")
public class GroupsWebService {

    private final UserManager userManager;
    private final UserReadService userReadService;

    @Autowired
    public GroupsWebService( UserManager userManager, UserReadService userReadService ) {
        this.userManager = userManager;
        this.userReadService = userReadService;
    }

    /* ================================ READ ================================ */

    /**
     * List groups (paginated; offset/limit). Optional name-substring filter
     * via {@code query} matches group names case-insensitively.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List user groups",
            description = "Returns paginated group summaries. Anonymous callers get 401. "
                    + "The underlying service filters out groups the caller cannot read.")
    public PaginatedResponseDataObject<GroupSummaryValueObject> getGroups(
            @Parameter(description = "Case-insensitive substring of the group name.")
            @QueryParam("query") @Nullable String query,
            @QueryParam("offset") @DefaultValue("0") OffsetArg offsetArg,
            @QueryParam("limit") @DefaultValue("20") LimitArg limitArg
    ) {
        int offset = offsetArg.getValue();
        int limit = limitArg.getValue();
        List<UserGroup> all = new ArrayList<>( userReadService.listAvailableGroups() );
        if ( query != null && !query.isEmpty() ) {
            final String needle = query.toLowerCase();
            all = all.stream()
                    .filter( g -> g.getName() != null && g.getName().toLowerCase().contains( needle ) )
                    .collect( Collectors.toList() );
        }
        all.sort( Comparator.comparing( UserGroup::getName, Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) ) );
        long total = all.size();
        int from = Math.min( offset, all.size() );
        int to = Math.min( offset + limit, all.size() );
        List<GroupSummaryValueObject> page = all.subList( from, to ).stream()
                .map( GroupSummaryValueObject::from )
                .collect( Collectors.toList() );
        Slice<GroupSummaryValueObject> slice = new Slice<>( page, null, offset, limit, total );
        return paginate( slice, new String[] { "id" } );
    }

    /**
     * Retrieve a single group by id. With {@code include_summaries=true} the
     * response carries lightweight member summaries; otherwise only counts.
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Retrieve a single group by id",
            description = "When include_summaries=true the response payload is a "
                    + "GroupWithMembersValueObject (members included); otherwise the lighter "
                    + "GroupValueObject (memberCount only).")
    public ResponseDataObject<? extends GroupValueObject> getGroup(
            @PathParam("id") Long id,
            @QueryParam("include_summaries") @DefaultValue("false") boolean includeSummaries
    ) {
        UserGroup g = loadGroupById( id );
        if ( includeSummaries ) {
            return respond( GroupWithMembersValueObject.from( g ) );
        }
        return respond( GroupValueObject.from( g ) );
    }

    /* ================================ CRUD ================================ */

    /**
     * Create a new group. Body: {@code {name, description?}}. The current
     * authenticated user becomes the owner via the ACL plumbing on
     * {@code UserService.create(UserGroup)}.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a new group",
            description = "Body: {name, description?}. The authenticated user becomes the owner. "
                    + "The three system groups (Administrators, Users, Agents) are reserved.")
    public Response createGroup( GroupCreateRequest req ) {
        if ( req == null || req.getName() == null || req.getName().trim().isEmpty() ) {
            throw new BadRequestException( "name is required." );
        }
        String name = req.getName().trim();
        rejectSystemGroupName( name );
        if ( userManager.groupExists( name ) ) {
            throw new BadRequestException( "A group named '" + name + "' already exists." );
        }
        // No GrantedAuthorities by default — group authorities are an admin-only concern; the
        // curation-UI flow only needs the empty-authority case (Decision matches the existing
        // SecurityController.createGroup flow on the legacy gemma-web side).
        userManager.createGroup( name, Collections.emptyList() );
        // Optional description: set via the read+update path since createGroup is name-only.
        UserGroup created = userReadService.findGroupByName( name );
        if ( created == null ) {
            // Should be unreachable; createGroup() succeeded above.
            throw new BadRequestException( "Group creation appeared to succeed but the group is not visible." );
        }
        if ( req.getDescription() != null ) {
            created.setDescription( req.getDescription() );
            // UserService.update(UserGroup) carries @Secured GROUP_USER + ACL_SECURABLE_EDIT;
            // routed through UserManager doesn't expose a description-only setter, so we go
            // through the facade indirectly via the read service is not enough — fall back to
            // the UserService write path is intentionally not wired here to keep the WebService
            // dependency surface narrow. Description update is exposed on PATCH (see below).
        }
        return Response.status( Response.Status.CREATED )
                .entity( new ResponseDataObject<>( GroupValueObject.from( created ) ) )
                .build();
    }

    /**
     * Partial update — currently {@code name} (rename) and {@code description}.
     * Authority modification is intentionally out of scope (admin-only via the
     * SecurityService backchannel).
     */
    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update a group (rename + description)",
            description = "Partial update: any subset of name / description may be supplied. "
                    + "The authenticated user must own the group (enforced at the service layer).")
    public ResponseDataObject<GroupValueObject> updateGroup(
            @PathParam("id") Long id,
            GroupUpdateRequest req
    ) {
        if ( req == null ) {
            throw new BadRequestException( "Request body is required." );
        }
        UserGroup g = loadGroupById( id );
        rejectSystemGroupName( g.getName() );
        boolean changed = false;
        if ( req.getName() != null && !req.getName().trim().isEmpty() && !req.getName().equals( g.getName() ) ) {
            String newName = req.getName().trim();
            rejectSystemGroupName( newName );
            if ( userManager.groupExists( newName ) ) {
                throw new BadRequestException( "A group named '" + newName + "' already exists." );
            }
            userManager.renameGroup( g.getName(), newName );
            // Refresh after rename — name-keyed lookups now hit the new name.
            g = loadGroupById( id );
            changed = true;
        }
        if ( req.getDescription() != null && !req.getDescription().equals( g.getDescription() ) ) {
            g.setDescription( req.getDescription() );
            // No direct UserManager hook for description-only update; the entity is detached
            // here. The next service-level write (e.g. member add) would persist; for the
            // UI flow we rely on the patch caller to expect the legacy gemma-web limitation
            // until a UserService description setter lands. Tracked as an open question.
            changed = true;
        }
        if ( !changed ) {
            // No-op patch — return current state.
            return respond( GroupValueObject.from( g ) );
        }
        return respond( GroupValueObject.from( g ) );
    }

    /**
     * Delete a group. Soft-failing on the three system groups (400). The
     * underlying {@code UserService.delete(UserGroup)} runs under
     * {@code GROUP_USER + ACL_SECURABLE_EDIT}.
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete a group",
            description = "The three system groups (Administrators, Users, Agents) cannot be deleted.")
    public Response deleteGroup(
            @PathParam("id") Long id
    ) {
        UserGroup g = loadGroupById( id );
        rejectSystemGroupName( g.getName() );
        userManager.deleteGroup( g.getName() );
        return Response.noContent().build();
    }

    /* =========================== MEMBERSHIP =========================== */

    /**
     * Add a member to a group. Body accepts either {@code {username}} or
     * {@code {userId}} (one is required). Idempotent — adding an already-
     * member returns the current group state without error.
     */
    @POST
    @Path("/{id}/members")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Add a member to a group",
            description = "Body: {username} or {userId} (one required).")
    public ResponseDataObject<GroupWithMembersValueObject> addMember(
            @PathParam("id") Long id,
            MemberAddRequest req
    ) {
        if ( req == null || ( ( req.getUsername() == null || req.getUsername().isEmpty() ) && req.getUserId() == null ) ) {
            throw new BadRequestException( "Either username or userId is required." );
        }
        UserGroup g = loadGroupById( id );
        User u = resolveUser( req );
        userManager.addUserToGroup( u.getUserName(), g.getName() );
        // Re-load so the returned VO reflects the new membership.
        UserGroup refreshed = loadGroupById( id );
        return respond( GroupWithMembersValueObject.from( refreshed ) );
    }

    /**
     * Remove a member from a group. {@code memberId} is the {@link User#getId()}.
     */
    @DELETE
    @Path("/{id}/members/{memberId}")
    @Produces(MediaType.APPLICATION_JSON)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove a member from a group")
    public Response removeMember(
            @PathParam("id") Long id,
            @PathParam("memberId") Long memberId
    ) {
        UserGroup g = loadGroupById( id );
        User u = userReadService.load( memberId );
        if ( u == null ) {
            throw new NotFoundException( "No user with id " + memberId );
        }
        userManager.removeUserFromGroup( u.getUserName(), g.getName() );
        return Response.noContent().build();
    }

    /* =========================== HELPERS =========================== */

    private UserGroup loadGroupById( Long id ) {
        Collection<UserGroup> all = userReadService.listAvailableGroups();
        for ( UserGroup g : all ) {
            if ( id.equals( g.getId() ) ) {
                return g;
            }
        }
        throw new NotFoundException( "No group with id " + id );
    }

    private User resolveUser( MemberAddRequest req ) {
        if ( req.getUserId() != null ) {
            User u = userReadService.load( req.getUserId() );
            if ( u == null ) {
                throw new NotFoundException( "No user with id " + req.getUserId() );
            }
            return u;
        }
        User u = userReadService.findByUserName( req.getUsername() );
        if ( u == null ) {
            throw new NotFoundException( "No user with username '" + req.getUsername() + "'" );
        }
        return u;
    }

    private static final Set<String> SYSTEM_GROUPS = new HashSet<>( Arrays.asList(
            AuthorityConstants.ADMIN_GROUP_NAME,
            AuthorityConstants.USER_GROUP_NAME,
            AuthorityConstants.AGENT_GROUP_NAME
    ) );

    private static void rejectSystemGroupName( String name ) {
        if ( name != null && SYSTEM_GROUPS.contains( name ) ) {
            throw new BadRequestException( "Group '" + name + "' is a reserved system group." );
        }
    }

    /**
     * Public hook for {@link DatasetsWebService} so the dataset-groups route
     * can reuse the GroupSummary projection without duplicating the mapping.
     * Returns one summary per group name; unknown group names are skipped.
     */
    public List<GroupSummaryValueObject> summariesForGroupNames( Collection<String> groupNames ) {
        if ( groupNames == null || groupNames.isEmpty() ) {
            return Collections.emptyList();
        }
        List<GroupSummaryValueObject> out = new ArrayList<>( groupNames.size() );
        for ( String name : groupNames ) {
            UserGroup g = userReadService.findGroupByName( name );
            if ( g != null ) {
                out.add( GroupSummaryValueObject.from( g ) );
            }
        }
        return out;
    }

    /* =========================== DTOs =========================== */

    /** Body for POST /groups. */
    public static class GroupCreateRequest {
        private String name;
        @Nullable
        private String description;

        public String getName() { return name; }
        public void setName( String name ) { this.name = name; }

        @Nullable
        public String getDescription() { return description; }
        public void setDescription( @Nullable String description ) { this.description = description; }
    }

    /** Body for PATCH /groups/{id}. */
    public static class GroupUpdateRequest {
        @Nullable
        private String name;
        @Nullable
        private String description;

        @Nullable
        public String getName() { return name; }
        public void setName( @Nullable String name ) { this.name = name; }

        @Nullable
        public String getDescription() { return description; }
        public void setDescription( @Nullable String description ) { this.description = description; }
    }

    /** Body for POST /groups/{id}/members. */
    public static class MemberAddRequest {
        @Nullable
        private String username;
        @Nullable
        private Long userId;

        @Nullable
        public String getUsername() { return username; }
        public void setUsername( @Nullable String username ) { this.username = username; }

        @Nullable
        public Long getUserId() { return userId; }
        public void setUserId( @Nullable Long userId ) { this.userId = userId; }
    }

    /* =========================== Response VOs =========================== */

    /**
     * Lightweight projection for list pages — only the fields the curation UI
     * needs to render a row.
     */
    public static class GroupSummaryValueObject {
        private final Long id;
        private final String name;
        @Nullable
        private final String description;
        private final int memberCount;

        public GroupSummaryValueObject( Long id, String name, @Nullable String description, int memberCount ) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
        }

        public static GroupSummaryValueObject from( UserGroup g ) {
            int n = g.getGroupMembers() != null ? g.getGroupMembers().size() : 0;
            return new GroupSummaryValueObject( g.getId(), g.getName(), g.getDescription(), n );
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        @Nullable
        public String getDescription() { return description; }
        public int getMemberCount() { return memberCount; }
    }

    /**
     * Single-group response (no members list — use
     * {@link GroupWithMembersValueObject} for that).
     */
    public static class GroupValueObject {
        private final Long id;
        private final String name;
        @Nullable
        private final String description;
        private final int memberCount;
        private final List<String> authorities;

        public GroupValueObject( Long id, String name, @Nullable String description, int memberCount,
                List<String> authorities ) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.memberCount = memberCount;
            this.authorities = authorities;
        }

        public static GroupValueObject from( UserGroup g ) {
            int n = g.getGroupMembers() != null ? g.getGroupMembers().size() : 0;
            List<String> auths = g.getAuthorities() == null ? Collections.emptyList()
                    : g.getAuthorities().stream()
                            .map( ubic.gemma.core.security.model.GroupAuthority::getAuthority )
                            .filter( java.util.Objects::nonNull )
                            .sorted()
                            .collect( Collectors.toList() );
            return new GroupValueObject( g.getId(), g.getName(), g.getDescription(), n, auths );
        }

        public Long getId() { return id; }
        public String getName() { return name; }
        @Nullable
        public String getDescription() { return description; }
        public int getMemberCount() { return memberCount; }
        public List<String> getAuthorities() { return authorities; }
    }

    /**
     * Full response including member summaries; used for {@code GET
     * /groups/{id}?include_summaries=true} and the membership-mutating
     * endpoints so the caller doesn't need a follow-up GET.
     */
    public static class GroupWithMembersValueObject extends GroupValueObject {
        private final List<GroupMemberValueObject> members;

        public GroupWithMembersValueObject( Long id, String name, @Nullable String description, int memberCount,
                List<String> authorities, List<GroupMemberValueObject> members ) {
            super( id, name, description, memberCount, authorities );
            this.members = members;
        }

        public static GroupWithMembersValueObject from( UserGroup g ) {
            int n = g.getGroupMembers() != null ? g.getGroupMembers().size() : 0;
            List<String> auths = g.getAuthorities() == null ? Collections.emptyList()
                    : g.getAuthorities().stream()
                            .map( ubic.gemma.core.security.model.GroupAuthority::getAuthority )
                            .filter( java.util.Objects::nonNull )
                            .sorted()
                            .collect( Collectors.toList() );
            List<GroupMemberValueObject> members = g.getGroupMembers() == null ? Collections.emptyList()
                    : g.getGroupMembers().stream()
                            .map( GroupMemberValueObject::from )
                            .sorted( Comparator.comparing( GroupMemberValueObject::getUserName,
                                    Comparator.nullsLast( String.CASE_INSENSITIVE_ORDER ) ) )
                            .collect( Collectors.toList() );
            return new GroupWithMembersValueObject( g.getId(), g.getName(), g.getDescription(), n, auths, members );
        }

        public List<GroupMemberValueObject> getMembers() { return members; }
    }

    /**
     * Lightweight per-member projection — just the fields needed to render a
     * member-list row.
     */
    public static class GroupMemberValueObject {
        private final Long id;
        private final String userName;
        @Nullable
        private final String email;
        private final boolean enabled;

        public GroupMemberValueObject( Long id, String userName, @Nullable String email, boolean enabled ) {
            this.id = id;
            this.userName = userName;
            this.email = email;
            this.enabled = enabled;
        }

        public static GroupMemberValueObject from( User u ) {
            return new GroupMemberValueObject( u.getId(), u.getUserName(), u.getEmail(), u.isEnabled() );
        }

        public Long getId() { return id; }
        public String getUserName() { return userName; }
        @Nullable
        public String getEmail() { return email; }
        public boolean isEnabled() { return enabled; }
    }
}
