/*
 * The gemma-core project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.core.security.model.Securable;
import org.hibernate.Session;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;  // referenced in javadoc
import org.springframework.security.acls.domain.AclImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AlreadyExistsException;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test-only {@link AclService} that stores ACLs in an in-memory {@link Map} using real Spring
 * Security {@link AclImpl} instances. Lets unit tests for {@link AclEventListener} and
 * {@link BaseAclAdvice} run without Spring, Hibernate, JDBC, or a database while still
 * exercising the same {@link MutableAcl#setParent}, {@link MutableAcl#setEntriesInheriting}
 * and ACE-list semantics that production code depends on.
 * <p>
 * Equality with both {@link ObjectIdentityImpl} (Spring's canonical) and gsec's
 * {@code AclObjectIdentity} is handled by normalizing every incoming OID through
 * {@link #normalize(ObjectIdentity)} before map lookups — same shape as
 * {@code GsecAclServiceAdapter} in production.
 */
public class InMemoryAclService implements AclService {

    /**
     * Permissive strategy — never blocks ACL mutations. Production uses
     * {@link AclAuthorizationStrategyImpl} which requires the current authentication to be
     * the ACL owner or to carry an admin authority, but in tests we routinely need to set up
     * ACL state from a principal that doesn't (yet) own the entity. The trade-off: this
     * fixture won't catch a production bug that depends on the security-check behaviour;
     * tests that care should authenticate as the relevant owner/admin before calling
     * {@link #updateAcl}.
     */
    private final AclAuthorizationStrategy authStrategy = ( acl, changeType ) -> {};
    private final PermissionGrantingStrategy pgs =
            new DefaultPermissionGrantingStrategy( new ConsoleAuditLogger() );
    private final Map<ObjectIdentity, AclImpl> acls = new HashMap<>();
    private final AtomicLong aclIdSeq = new AtomicLong( 1 );

    @Override
    public MutableAcl createAcl( ObjectIdentity objectIdentity ) {
        ObjectIdentity key = normalize( objectIdentity );
        if ( acls.containsKey( key ) ) {
            throw new AlreadyExistsException( "ACL already exists for " + key );
        }
        // loadedSids=null means "all SIDs are considered loaded" — see AclImpl.isSidLoaded.
        // Using Collections.emptyList() would cause AclImpl.isGranted to throw
        // UnloadedSidException for any SID, which is wrong for an in-memory test fixture
        // where we never partially-load.
        AclImpl acl = new AclImpl( key, aclIdSeq.getAndIncrement(), authStrategy, pgs, null,
                null, true, currentOwnerSid() );
        acls.put( key, acl );
        return acl;
    }

    @Override
    public void deleteAcl( ObjectIdentity objectIdentity, boolean deleteChildren ) {
        ObjectIdentity key = normalize( objectIdentity );
        acls.remove( key );
        if ( deleteChildren ) {
            // Cascade: drop any ACL whose parent references the deleted OID.
            acls.entrySet().removeIf( e -> {
                org.springframework.security.acls.model.Acl parent = e.getValue().getParentAcl();
                return parent != null && key.equals( parent.getObjectIdentity() );
            } );
        }
    }

    @Override
    public MutableAcl updateAcl( MutableAcl acl ) throws NotFoundException {
        ObjectIdentity key = normalize( acl.getObjectIdentity() );
        if ( !acls.containsKey( key ) ) {
            throw new NotFoundException( "No ACL for " + key );
        }
        // The caller mutated the instance we returned earlier; just keep the same reference.
        return acl;
    }

    @Override
    public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity ) {
        ObjectIdentity key = normalize( parentIdentity );
        List<ObjectIdentity> out = new ArrayList<>();
        for ( AclImpl acl : acls.values() ) {
            org.springframework.security.acls.model.Acl p = acl.getParentAcl();
            if ( p != null && key.equals( p.getObjectIdentity() ) ) {
                out.add( acl.getObjectIdentity() );
            }
        }
        return out.isEmpty() ? null : out;
    }

    @Override
    public org.springframework.security.acls.model.Acl readAclById( ObjectIdentity object ) throws NotFoundException {
        ObjectIdentity key = normalize( object );
        AclImpl acl = acls.get( key );
        if ( acl == null ) {
            throw new NotFoundException( "No ACL for " + key );
        }
        return acl;
    }

    @Override
    public org.springframework.security.acls.model.Acl readAclById( ObjectIdentity object, List<Sid> sids ) {
        return readAclById( object );
    }

    @Override
    public Map<ObjectIdentity, org.springframework.security.acls.model.Acl> readAclsById( List<ObjectIdentity> objects ) {
        return readAclsById( objects, null );
    }

    @Override
    public Map<ObjectIdentity, org.springframework.security.acls.model.Acl> readAclsById(
            List<ObjectIdentity> objects, @Nullable List<Sid> sids ) {
        Map<ObjectIdentity, org.springframework.security.acls.model.Acl> out = new LinkedHashMap<>();
        for ( ObjectIdentity oi : objects ) {
            out.put( oi, readAclById( oi ) );
        }
        return out;
    }

    @Override
    public org.springframework.security.acls.model.Acl readAclById( ObjectIdentity oid, Session session ) {
        return readAclById( oid );
    }

    @Override
    public Session openSession() {
        // No-op Proxy stand-in. Returning null breaks
        // AclEntryAfterInvocationStreamFilteringProvider.decide which threads the session into
        // onClose(session::close) — method-reference resolution NPEs on a null receiver. The
        // stand-in's no-arg methods (notably close()) silently no-op; any other invocation that
        // a future caller might add returns a zero value or null via defaultValueFor.
        return ( Session ) java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] { Session.class },
                ( proxy, method, args ) -> {
                    Class<?> rt = method.getReturnType();
                    if ( rt == boolean.class ) return false;
                    if ( rt == byte.class ) return ( byte ) 0;
                    if ( rt == short.class ) return ( short ) 0;
                    if ( rt == int.class ) return 0;
                    if ( rt == long.class ) return 0L;
                    if ( rt == float.class ) return 0f;
                    if ( rt == double.class ) return 0d;
                    if ( rt == char.class ) return ( char ) 0;
                    return null;
                } );
    }

    @Override
    public void deleteSid( Sid sid ) {
        // Test stub — no-op.
    }

    /**
     * Snapshot of currently-stored ACL count, for size assertions.
     */
    public int size() {
        return acls.size();
    }

    /**
     * Create-or-get the ACL for {@code entity} and grant {@code permission} to a principal Sid
     * (the kind you get from a logged-in user). Convenience for after-invocation provider
     * tests: instead of constructing AclImpl + insertAce + updateAcl by hand, write
     * {@code acls.grantToPrincipal(entity, "alice", BasePermission.READ)}.
     */
    public MutableAcl grantToPrincipal( Securable entity, String principal, Permission permission ) {
        return grant( entity, new PrincipalSid( principal ), permission );
    }

    /**
     * Same as {@link #grantToPrincipal} but grants to a granted-authority Sid (group / role).
     */
    public MutableAcl grantToAuthority( Securable entity, String authority, Permission permission ) {
        return grant( entity, new GrantedAuthoritySid( authority ), permission );
    }

    /**
     * Create-or-get the ACL for {@code entity} without granting anything — useful when a test
     * needs the entity to be ACL-known but with no permissions for the test principal.
     */
    public MutableAcl ensureAcl( Securable entity ) {
        ObjectIdentity oi = oidFor( entity );
        AclImpl acl = acls.get( oi );
        if ( acl == null ) {
            acl = ( AclImpl ) createAcl( oi );
        }
        return acl;
    }

    private MutableAcl grant( Securable entity, Sid sid, Permission permission ) {
        MutableAcl acl = ensureAcl( entity );
        acl.insertAce( acl.getEntries().size(), permission, sid, true );
        updateAcl( acl );
        return acl;
    }

    private static ObjectIdentity oidFor( Securable entity ) {
        return new ObjectIdentityImpl( entity.getClass().getName(), entity.getId() );
    }

    /**
     * Direct map-style access to the stored ACL by entity OID, bypassing the NotFoundException
     * contract — useful in assertions that want a nullable view.
     */
    @Nullable
    public AclImpl peek( ObjectIdentity oid ) {
        return acls.get( normalize( oid ) );
    }

    /**
     * Normalize incoming OIDs to {@link ObjectIdentityImpl} so {@code equals/hashCode} are
     * symmetric across gsec's {@code AclObjectIdentity} and Spring's canonical impl. Mirrors
     * production {@code GsecAclServiceAdapter.normalize}.
     */
    private static ObjectIdentity normalize( ObjectIdentity oid ) {
        if ( oid == null || oid instanceof ObjectIdentityImpl ) {
            return oid;
        }
        return new ObjectIdentityImpl( oid.getType(), oid.getIdentifier() );
    }

    /**
     * Owner sid for newly-created ACLs — derived from {@link SecurityContextHolder} like
     * production does, but tolerates an empty context for tests that don't care about owner.
     */
    private Sid currentOwnerSid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if ( auth != null && auth.getPrincipal() != null ) {
            return new PrincipalSid( auth.getPrincipal().toString() );
        }
        return new PrincipalSid( "test-owner" );
    }
}
