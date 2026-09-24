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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.security.authentication.UserReadService;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.rest.GroupsWebService.GroupCreateRequest;
import ubic.gemma.rest.GroupsWebService.GroupSummaryValueObject;
import ubic.gemma.rest.GroupsWebService.GroupUpdateRequest;
import ubic.gemma.rest.GroupsWebService.GroupValueObject;
import ubic.gemma.rest.GroupsWebService.GroupWithMembersValueObject;
import ubic.gemma.rest.GroupsWebService.MemberAddRequest;
import ubic.gemma.rest.util.PaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.LimitArg;
import ubic.gemma.rest.util.args.OffsetArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure Mockito unit tests for {@link GroupsWebService}. Verifies the
 * WebService → UserManager / UserReadService wiring and the VO projection
 * shape without standing up Jersey.
 *
 * @author paul
 */
@ExtendWith(MockitoExtension.class)
public class GroupsWebServiceTest {

    @Mock
    private UserManager userManager;

    @Mock
    private UserReadService userReadService;

    @InjectMocks
    private GroupsWebService webService;

    private User alice;
    private User bob;
    private UserGroup group;

    @BeforeEach
    public void setUp() {
        alice = new User();
        alice.setId( 1L );
        alice.setUserName( "alice" );
        alice.setEmail( "alice@example.com" );
        alice.setEnabled( true );

        bob = new User();
        bob.setId( 2L );
        bob.setUserName( "bob" );
        bob.setEmail( "bob@example.com" );
        bob.setEnabled( true );

        group = new UserGroup();
        group.setId( 100L );
        group.setName( "lab-foo" );
        group.setDescription( "Foo lab" );
        group.setGroupMembers( new HashSet<>( Collections.singletonList( alice ) ) );
    }

    /* ============================ READ ============================ */

    @Test
    public void testGetGroups_paginates() {
        UserGroup g2 = new UserGroup();
        g2.setId( 101L );
        g2.setName( "lab-bar" );
        g2.setGroupMembers( new HashSet<>() );
        when( userReadService.listAvailableGroups() ).thenReturn( Arrays.asList( group, g2 ) );

        PaginatedResponseDataObject<GroupSummaryValueObject> resp = webService.getGroups(
                null, OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );

        assertThat( resp.getData() ).hasSize( 2 );
        // Alphabetical (case-insensitive): lab-bar before lab-foo
        assertThat( resp.getData().get( 0 ).getName() ).isEqualTo( "lab-bar" );
        assertThat( resp.getData().get( 1 ).getName() ).isEqualTo( "lab-foo" );
        assertThat( resp.getData().get( 1 ).getMemberCount() ).isEqualTo( 1 );
    }

    @Test
    public void testGetGroups_queryFilter() {
        UserGroup g2 = new UserGroup();
        g2.setId( 101L );
        g2.setName( "other-team" );
        g2.setGroupMembers( new HashSet<>() );
        when( userReadService.listAvailableGroups() ).thenReturn( Arrays.asList( group, g2 ) );

        PaginatedResponseDataObject<GroupSummaryValueObject> resp = webService.getGroups(
                "lab", OffsetArg.valueOf( "0" ), LimitArg.valueOf( "20" ) );

        assertThat( resp.getData() ).hasSize( 1 );
        assertThat( resp.getData().get( 0 ).getName() ).isEqualTo( "lab-foo" );
    }

    @Test
    public void testGetGroup_summariesOff() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );

        ResponseDataObject<? extends GroupValueObject> resp = webService.getGroup( 100L, false, false );

        assertThat( resp.getData() ).isInstanceOf( GroupValueObject.class );
        assertThat( resp.getData() ).isNotInstanceOf( GroupWithMembersValueObject.class );
        assertThat( resp.getData().getId() ).isEqualTo( 100L );
        assertThat( resp.getData().getMemberCount() ).isEqualTo( 1 );
    }

    @Test
    public void testGetGroup_summariesOn_includesMembers() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );

        ResponseDataObject<? extends GroupValueObject> resp = webService.getGroup( 100L, true, false );

        assertThat( resp.getData() ).isInstanceOf( GroupWithMembersValueObject.class );
        GroupWithMembersValueObject withMembers = ( GroupWithMembersValueObject ) resp.getData();
        assertThat( withMembers.getMembers() ).hasSize( 1 );
        assertThat( withMembers.getMembers().get( 0 ).getUserName() ).isEqualTo( "alice" );
        assertThat( withMembers.getMembers().get( 0 ).getEmail() ).isEqualTo( "alice@example.com" );
    }

    @Test
    public void testGetGroup_notFound() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );
        assertThatThrownBy( () -> webService.getGroup( 999L, false, false ) )
                .isInstanceOf( NotFoundException.class );
    }

    /* ============================ CREATE ============================ */

    @Test
    public void testCreateGroup_happyPath() {
        when( userManager.groupExists( "newgrp" ) ).thenReturn( false );
        when( userReadService.findGroupByName( "newgrp" ) ).thenReturn( group );

        GroupCreateRequest req = new GroupCreateRequest();
        req.setName( "newgrp" );
        req.setDescription( "A new group" );

        Response resp = webService.createGroup( req );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.CREATED.getStatusCode() );
        verify( userManager ).createGroup( "newgrp", Collections.emptyList() );
    }

    @Test
    public void testCreateGroup_missingName_throws400() {
        GroupCreateRequest req = new GroupCreateRequest();
        assertThatThrownBy( () -> webService.createGroup( req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testCreateGroup_nullBody_throws400() {
        assertThatThrownBy( () -> webService.createGroup( null ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testCreateGroup_systemGroupName_throws400() {
        GroupCreateRequest req = new GroupCreateRequest();
        req.setName( "Administrators" );
        assertThatThrownBy( () -> webService.createGroup( req ) )
                .isInstanceOf( BadRequestException.class );
        verify( userManager, never() ).createGroup( eqs( "Administrators" ), anyList() );
    }

    @Test
    public void testCreateGroup_existingName_throws400() {
        when( userManager.groupExists( "lab-foo" ) ).thenReturn( true );
        GroupCreateRequest req = new GroupCreateRequest();
        req.setName( "lab-foo" );
        assertThatThrownBy( () -> webService.createGroup( req ) )
                .isInstanceOf( BadRequestException.class );
    }

    /* ============================ PATCH ============================ */

    @Test
    public void testUpdateGroup_rename() {
        UserGroup renamed = new UserGroup();
        renamed.setId( 100L );
        renamed.setName( "lab-renamed" );
        renamed.setGroupMembers( new HashSet<>() );
        // First lookup returns original; after rename, refresh returns renamed.
        when( userReadService.listAvailableGroups() )
                .thenReturn( Collections.singletonList( group ) )
                .thenReturn( Collections.singletonList( renamed ) );
        when( userManager.groupExists( "lab-renamed" ) ).thenReturn( false );

        GroupUpdateRequest req = new GroupUpdateRequest();
        req.setName( "lab-renamed" );

        ResponseDataObject<GroupValueObject> resp = webService.updateGroup( 100L, req );

        assertThat( resp.getData().getName() ).isEqualTo( "lab-renamed" );
        verify( userManager ).renameGroup( "lab-foo", "lab-renamed" );
    }

    @Test
    public void testUpdateGroup_systemGroupCannotRename() {
        UserGroup admin = new UserGroup();
        admin.setId( 1L );
        admin.setName( "Administrators" );
        admin.setGroupMembers( new HashSet<>() );
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( admin ) );

        GroupUpdateRequest req = new GroupUpdateRequest();
        req.setName( "evil" );
        assertThatThrownBy( () -> webService.updateGroup( 1L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testUpdateGroup_nullBody_throws400() {
        assertThatThrownBy( () -> webService.updateGroup( 100L, null ) )
                .isInstanceOf( BadRequestException.class );
    }

    /* ============================ DELETE ============================ */

    @Test
    public void testDeleteGroup_happyPath() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );

        Response resp = webService.deleteGroup( 100L );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        verify( userManager ).deleteGroup( "lab-foo" );
    }

    @Test
    public void testDeleteGroup_systemGroup_throws400() {
        UserGroup admin = new UserGroup();
        admin.setId( 1L );
        admin.setName( "Administrators" );
        admin.setGroupMembers( new HashSet<>() );
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( admin ) );

        assertThatThrownBy( () -> webService.deleteGroup( 1L ) )
                .isInstanceOf( BadRequestException.class );
        verify( userManager, never() ).deleteGroup( "Administrators" );
    }

    @Test
    public void testDeleteGroup_notFound() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.emptyList() );
        assertThatThrownBy( () -> webService.deleteGroup( 999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    /* ============================ MEMBERS ============================ */

    @Test
    public void testAddMember_byUsername() {
        UserGroup refreshed = new UserGroup();
        refreshed.setId( 100L );
        refreshed.setName( "lab-foo" );
        refreshed.setGroupMembers( new HashSet<>( Arrays.asList( alice, bob ) ) );
        when( userReadService.listAvailableGroups() )
                .thenReturn( Collections.singletonList( group ) )
                .thenReturn( Collections.singletonList( refreshed ) );
        when( userReadService.findByUserName( "bob" ) ).thenReturn( bob );

        MemberAddRequest req = new MemberAddRequest();
        req.setUsername( "bob" );

        ResponseDataObject<GroupWithMembersValueObject> resp = webService.addMember( 100L, req );

        verify( userManager ).addUserToGroup( "bob", "lab-foo" );
        assertThat( resp.getData().getMembers() ).hasSize( 2 );
    }

    @Test
    public void testAddMember_byUserId() {
        UserGroup refreshed = new UserGroup();
        refreshed.setId( 100L );
        refreshed.setName( "lab-foo" );
        refreshed.setGroupMembers( new HashSet<>( Arrays.asList( alice, bob ) ) );
        when( userReadService.listAvailableGroups() )
                .thenReturn( Collections.singletonList( group ) )
                .thenReturn( Collections.singletonList( refreshed ) );
        when( userReadService.load( 2L ) ).thenReturn( bob );

        MemberAddRequest req = new MemberAddRequest();
        req.setUserId( 2L );

        webService.addMember( 100L, req );

        verify( userManager ).addUserToGroup( "bob", "lab-foo" );
    }

    @Test
    public void testAddMember_neitherUsernameNorUserId_throws400() {
        MemberAddRequest req = new MemberAddRequest();
        assertThatThrownBy( () -> webService.addMember( 100L, req ) )
                .isInstanceOf( BadRequestException.class );
    }

    @Test
    public void testAddMember_unknownUser_throws404() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );
        when( userReadService.findByUserName( "ghost" ) ).thenReturn( null );

        MemberAddRequest req = new MemberAddRequest();
        req.setUsername( "ghost" );
        assertThatThrownBy( () -> webService.addMember( 100L, req ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    public void testRemoveMember_happyPath() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );
        when( userReadService.load( 1L ) ).thenReturn( alice );

        Response resp = webService.removeMember( 100L, 1L );

        assertThat( resp.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        verify( userManager ).removeUserFromGroup( "alice", "lab-foo" );
    }

    @Test
    public void testRemoveMember_unknownUser_throws404() {
        when( userReadService.listAvailableGroups() ).thenReturn( Collections.singletonList( group ) );
        when( userReadService.load( 999L ) ).thenReturn( null );

        assertThatThrownBy( () -> webService.removeMember( 100L, 999L ) )
                .isInstanceOf( NotFoundException.class );
    }

    /* ============================ HELPER ============================ */

    @Test
    public void testSummariesForGroupNames_skipsUnknown() {
        when( userReadService.findGroupByName( "lab-foo" ) ).thenReturn( group );
        when( userReadService.findGroupByName( "ghost" ) ).thenReturn( null );

        List<GroupSummaryValueObject> out = webService.summariesForGroupNames(
                Arrays.asList( "lab-foo", "ghost" ) );

        assertThat( out ).hasSize( 1 );
        assertThat( out.get( 0 ).getName() ).isEqualTo( "lab-foo" );
    }

    @Test
    public void testSummariesForGroupNames_emptyInput() {
        assertThat( webService.summariesForGroupNames( Collections.emptyList() ) ).isEmpty();
        assertThat( webService.summariesForGroupNames( null ) ).isEmpty();
    }

    /* ============================ AUTH ============================ */

    /**
     * Annotation-level guard: all endpoints (incl. reads) carry
     * {@code @PreAuthorize("isAuthenticated()")} so the curation-UI's auth
     * filter is consistently enforced regardless of method-security AOP edge
     * cases for read-only methods.
     */
    @Test
    public void testEndpoints_requireAuthentication() throws NoSuchMethodException {
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "getGroups",
                String.class, OffsetArg.class, LimitArg.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "getGroup",
                Long.class, boolean.class, boolean.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "createGroup",
                GroupCreateRequest.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "updateGroup",
                Long.class, GroupUpdateRequest.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "deleteGroup",
                Long.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "addMember",
                Long.class, MemberAddRequest.class ) );
        assertPreAuthorizeIsAuthenticated( GroupsWebService.class.getMethod( "removeMember",
                Long.class, Long.class ) );
    }

    private static void assertPreAuthorizeIsAuthenticated( java.lang.reflect.Method m ) {
        PreAuthorize annot = m.getAnnotation( PreAuthorize.class );
        assertThat( annot )
                .as( "method %s must carry @PreAuthorize", m.getName() )
                .isNotNull();
        assertThat( annot.value() )
                .as( "method %s must require isAuthenticated()", m.getName() )
                .contains( "isAuthenticated()" );
    }

    // --- argument matcher helpers ---
    private static String eqs( String s ) {
        return org.mockito.ArgumentMatchers.eq( s );
    }

    private static java.util.List<org.springframework.security.core.GrantedAuthority> anyList() {
        return org.mockito.ArgumentMatchers.anyList();
    }
}
