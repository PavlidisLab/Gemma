/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.security.acl;

import gemma.gsec.acl.domain.AclService;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.AclAuthorizationStrategyImpl;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.jdbc.BasicLookupStrategy;
import org.springframework.security.acls.jdbc.JdbcMutableAclService;
import org.springframework.security.acls.jdbc.LookupStrategy;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AclCache;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.PermissionGrantingStrategy;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.acls.model.UnloadedSidException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Spring-native ACL stack, replacing gsec's Hibernate-backed AclServiceImpl/AclDaoImpl. Wires stock
 * Spring Security 6 {@link JdbcMutableAclService} against the canonical four-table schema
 * (acl_class / acl_sid / acl_object_identity / acl_entry) — see prod migration db.1.33.0.sql and
 * test seed init-acls.sql. No SQL overrides are needed because the schema matches what Spring
 * Security ships out of the box.
 * <p>
 * gsec's domain types ({@code AclObjectIdentity}, {@code AclSid}, {@code AclEntry}) remain
 * Hibernate-mapped read-only against the same tables — Gemma's business DAOs still issue HQL
 * queries against them (see AclQueryUtils, ExpressionExperimentDaoImpl, etc.) and we don't want to
 * touch that surface in this migration. Writes flow through JDBC; reads via either JDBC (Spring
 * Security) or HQL (Gemma business code), both pointing at the same physical tables.
 * <p>
 * The exposed {@code aclService} bean implements gsec's {@link AclService} interface (a thin
 * extension of Spring's {@link org.springframework.security.acls.model.AclService} +
 * {@link org.springframework.security.acls.model.MutableAclService}) via the inner
 * {@link GsecAclServiceAdapter}: standard methods delegate to {@code JdbcMutableAclService},
 * gsec extension methods (openSession, session-aware readAclById, deleteSid) are stubbed or
 * implemented via raw JDBC. The adapter exists because {@code AclAdvice} and
 * {@code AclLinterServiceImpl} are statically typed against gsec's interface.
 */
@Configuration
public class GemmaAclConfiguration {

    /**
     * Authority required for ACL administration operations. The constructor of
     * {@link AclAuthorizationStrategyImpl} takes three authorities (own/audit/admin); Gemma uses a
     * single GROUP_ADMIN for all three. {@code AclAuthorizationStrategyImpl} grants the operation if
     * the principal is the owner, holds {@code BasePermission.ADMINISTRATION} on the ACL, or holds
     * any of these authorities.
     */
    private static final String ADMIN_AUTHORITY = "GROUP_ADMIN";

    @Bean
    public org.springframework.security.acls.domain.AuditLogger aclAuditLogger() {
        return new ConsoleAuditLogger();
    }

    @Bean
    public PermissionGrantingStrategy permissionGrantingStrategy(
            org.springframework.security.acls.domain.AuditLogger auditLogger ) {
        return new DefaultPermissionGrantingStrategy( auditLogger );
    }

    @Bean
    public AclAuthorizationStrategy aclAuthorizationStrategy(
            org.springframework.security.access.hierarchicalroles.RoleHierarchy roleHierarchy ) {
        GrantedAuthority admin = new SimpleGrantedAuthority( ADMIN_AUTHORITY );
        return new RoleHierarchyAwareAclAuthorizationStrategy( roleHierarchy, admin, admin, admin );
    }

    /**
     * Spring Security's {@link AclAuthorizationStrategyImpl#securityCheck} compares the
     * configured "required authority" (e.g. {@code GROUP_ADMIN}) against the raw
     * {@link org.springframework.security.core.Authentication#getAuthorities() authentication
     * authorities}, ignoring the role hierarchy. That breaks the {@code @Secured("RUN_AS_ADMIN")}
     * pattern Gemma uses for signup / user-management methods: {@code RunAsManagerImpl} grants the
     * RunAs token {@code GROUP_RUN_AS_ADMIN}, which the hierarchy escalates to {@code GROUP_ADMIN},
     * but the strategy's bare {@code contains} check never sees the expansion and the subsequent
     * ACE-level {@code isGranted} step then trips on the empty ACL of a freshly-created entity.
     * Pre-migration the gsec stack had the same code shape but tests like {@code AclAdviceTest#testSignup}
     * passed for unrelated reasons (the legacy {@code gsec.acl.domain.AclImpl#isGranted} returned
     * {@code false} on no-match instead of throwing, so AccessDeniedException — not NotFoundException —
     * leaked through and was caught upstream); after migrating to Spring Security's stock
     * {@code AclImpl} (via JdbcMutableAclService) the no-match path throws and the test fails.
     * <p>
     * This subclass expands the authentication's authorities through the role hierarchy before the
     * strategy inspects them, so {@code GROUP_RUN_AS_ADMIN} satisfies a {@code GROUP_ADMIN}
     * requirement — which is what the {@code GROUP_RUN_AS_ADMIN > GROUP_ADMIN} entry in
     * applicationContext-gsec.xml's hierarchy is for.
     */
    static class RoleHierarchyAwareAclAuthorizationStrategy extends AclAuthorizationStrategyImpl {

        private final org.springframework.security.access.hierarchicalroles.RoleHierarchy roleHierarchy;

        RoleHierarchyAwareAclAuthorizationStrategy(
                org.springframework.security.access.hierarchicalroles.RoleHierarchy roleHierarchy,
                GrantedAuthority... gas ) {
            super( gas );
            this.roleHierarchy = roleHierarchy;
        }

        @Override
        public void securityCheck( org.springframework.security.acls.model.Acl acl, int changeType ) {
            org.springframework.security.core.Authentication original =
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if ( original == null || roleHierarchy == null ) {
                super.securityCheck( acl, changeType );
                return;
            }
            java.util.Collection<? extends GrantedAuthority> expanded =
                    roleHierarchy.getReachableGrantedAuthorities( original.getAuthorities() );
            if ( expanded.size() == original.getAuthorities().size() ) {
                super.securityCheck( acl, changeType );
                return;
            }
            org.springframework.security.core.context.SecurityContext saved =
                    org.springframework.security.core.context.SecurityContextHolder.getContext();
            org.springframework.security.core.context.SecurityContext temp =
                    org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            temp.setAuthentication( new ExpandedAuthoritiesAuthentication( original, expanded ) );
            org.springframework.security.core.context.SecurityContextHolder.setContext( temp );
            try {
                super.securityCheck( acl, changeType );
            } finally {
                org.springframework.security.core.context.SecurityContextHolder.setContext( saved );
            }
        }
    }

    /**
     * Wraps an existing {@link org.springframework.security.core.Authentication} but reports the
     * role-hierarchy-expanded set of authorities. Used only to satisfy {@link
     * AclAuthorizationStrategyImpl#securityCheck}'s strict {@code contains} check; never persisted
     * or returned outside that one call.
     */
    static class ExpandedAuthoritiesAuthentication implements org.springframework.security.core.Authentication {
        private static final long serialVersionUID = 1L;
        private final org.springframework.security.core.Authentication delegate;
        private final java.util.Collection<? extends GrantedAuthority> authorities;

        ExpandedAuthoritiesAuthentication( org.springframework.security.core.Authentication delegate,
                                           java.util.Collection<? extends GrantedAuthority> authorities ) {
            this.delegate = delegate;
            this.authorities = authorities;
        }

        @Override
        public java.util.Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
        @Override public Object getCredentials() { return delegate.getCredentials(); }
        @Override public Object getDetails() { return delegate.getDetails(); }
        @Override public Object getPrincipal() { return delegate.getPrincipal(); }
        @Override public boolean isAuthenticated() { return delegate.isAuthenticated(); }
        @Override public void setAuthenticated( boolean isAuthenticated ) { delegate.setAuthenticated( isAuthenticated ); }
        @Override public String getName() { return delegate.getName(); }
    }

    @Bean
    public AclCache aclCache( CacheManager cacheManager,
                              PermissionGrantingStrategy permissionGrantingStrategy,
                              AclAuthorizationStrategy aclAuthorizationStrategy ) {
        Cache cache = cacheManager.getCache( "aclCache" );
        if ( cache == null ) {
            // Fall back to a ConcurrentMapCache so we work even if the application's CacheManager
            // hasn't pre-registered an "aclCache" region. Production CacheManager (gemma.cache)
            // should declare one for proper eviction tuning.
            cache = new org.springframework.cache.concurrent.ConcurrentMapCache( "aclCache" );
        }
        return new SpringCacheBasedAclCache( cache, permissionGrantingStrategy, aclAuthorizationStrategy );
    }

    @Bean
    public LookupStrategy lookupStrategy( DataSource dataSource,
                                          AclCache aclCache,
                                          AclAuthorizationStrategy aclAuthorizationStrategy,
                                          PermissionGrantingStrategy permissionGrantingStrategy ) {
        return new BasicLookupStrategy( dataSource, aclCache, aclAuthorizationStrategy, permissionGrantingStrategy );
    }

    /**
     * The primary ACL bean Gemma consumes (under id {@code aclService}). Implements gsec's
     * {@link AclService} interface so existing field injections in AclAdvice / AclLinterServiceImpl
     * continue to compile. Delegates the standard Spring Security methods to a real
     * {@link JdbcMutableAclService}.
     */
    @Bean(name = "aclService")
    public AclService aclService( DataSource dataSource,
                                  LookupStrategy lookupStrategy,
                                  AclCache aclCache ) {
        JdbcMutableAclService jdbc = new JdbcMutableAclService( dataSource, lookupStrategy, aclCache );
        // MySQL identity retrieval. Stock JdbcMutableAclService defaults to "call identity()" (H2)
        // which fails on MySQL. Override to MySQL's session-scoped LAST_INSERT_ID.
        jdbc.setClassIdentityQuery( "SELECT @@IDENTITY" );
        jdbc.setSidIdentityQuery( "SELECT @@IDENTITY" );
        return new GsecAclServiceAdapter( jdbc, dataSource );
    }

    // Renovations Phase 3: GsecAwareJdbcMutableAclService is no longer needed — the parallel
    // gsec.acl.domain.AclPrincipalSid / AclGrantedAuthoritySid types are no longer constructed
    // in security-path code (those classes still exist as Hibernate-mapped entities for HQL
    // queries against acl_sid, but security code always uses Spring's stock PrincipalSid /
    // GrantedAuthoritySid). JdbcMutableAclService therefore only ever sees Spring sids and the
    // type-translation override in createOrRetrieveSidPrimaryKey would be a no-op.

    // -----------------------------------------------------------------------------------------
    // Adapter implementing gsec's AclService over Spring's JdbcMutableAclService
    // -----------------------------------------------------------------------------------------

    /**
     * Adapts {@link JdbcMutableAclService} to gsec's {@link AclService} interface. The standard
     * Spring Security methods delegate directly. gsec's extension methods are either no-ops
     * (Hibernate session injection — irrelevant for JDBC) or raw-JDBC implementations (deleteSid).
     */
    static class GsecAclServiceAdapter implements AclService {

        private final JdbcMutableAclService delegate;
        private final JdbcTemplate jdbcTemplate;

        GsecAclServiceAdapter( JdbcMutableAclService delegate, DataSource dataSource ) {
            this.delegate = delegate;
            this.jdbcTemplate = new JdbcTemplate( dataSource );
        }

        // ---- Spring's MutableAclService -----------------------------------

        @Override
        public MutableAcl createAcl( ObjectIdentity objectIdentity ) {
            return delegate.createAcl( normalize( objectIdentity ) );
        }

        @Override
        public void deleteAcl( ObjectIdentity objectIdentity, boolean deleteChildren ) {
            delegate.deleteAcl( normalize( objectIdentity ), deleteChildren );
        }

        @Override
        public MutableAcl updateAcl( MutableAcl acl ) throws NotFoundException {
            dedupeEntries( acl );
            return delegate.updateAcl( acl );
        }

        /**
         * Remove duplicate ACEs (same sid + permission + granting) in-place. Spring Security's
         * {@code AclImpl.insertAce} doesn't dedupe and {@code JdbcMutableAclService.updateAcl}
         * writes whatever it's given. gsec's pre-migration {@code AclDaoImpl.update} deduplicated
         * implicitly via {@code Set<AclEntry>} with a content-based equals on
         * (granting, mask, sid); business code (e.g.
         * {@code SecurityServiceImpl.makeReadableByGroup}) relies on that behaviour to avoid
         * compounding duplicates on repeated grants. Preserve the contract by deduping here.
         */
        private static void dedupeEntries( MutableAcl acl ) {
            List<org.springframework.security.acls.model.AccessControlEntry> entries =
                    new java.util.ArrayList<>( acl.getEntries() );
            java.util.Set<String> seen = new java.util.HashSet<>();
            // Walk from the end so deleteAce(i) doesn't shift indices we still need.
            for ( int i = entries.size() - 1; i >= 0; i-- ) {
                org.springframework.security.acls.model.AccessControlEntry ace = entries.get( i );
                String sidKey;
                Sid sid = ace.getSid();
                if ( sid instanceof org.springframework.security.acls.domain.PrincipalSid ) {
                    sidKey = "P:" + ( ( org.springframework.security.acls.domain.PrincipalSid ) sid ).getPrincipal();
                } else if ( sid instanceof org.springframework.security.acls.domain.GrantedAuthoritySid ) {
                    sidKey = "G:" + ( ( org.springframework.security.acls.domain.GrantedAuthoritySid ) sid ).getGrantedAuthority();
                } else {
                    sidKey = sid == null ? "null" : sid.toString();
                }
                String key = sidKey + "|" + ace.getPermission().getMask() + "|" + ace.isGranting();
                if ( !seen.add( key ) ) {
                    acl.deleteAce( i );
                }
            }
        }

        // ---- Spring's AclService ------------------------------------------

        @Override
        public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity ) {
            return delegate.findChildren( normalize( parentIdentity ) );
        }

        @Override
        public Acl readAclById( ObjectIdentity object ) throws NotFoundException {
            return delegate.readAclById( normalize( object ) );
        }

        @Override
        public Acl readAclById( ObjectIdentity object, List<Sid> sids ) throws NotFoundException {
            return delegate.readAclById( normalize( object ), sids );
        }

        @Override
        public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects ) throws NotFoundException {
            if ( objects == null || objects.isEmpty() ) {
                return java.util.Collections.emptyMap();
            }
            return rekey( objects, delegate.readAclsById( normalize( objects ) ) );
        }

        @Override
        public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects, List<Sid> sids )
                throws NotFoundException, UnloadedSidException {
            // Spring Security's BasicLookupStrategy asserts notEmpty(objects); callers in
            // after-invocation collection filters can legitimately end up with an empty list (e.g.
            // when an association extractor filters out all targets). Short-circuit to an empty map
            // so the filter pass returns "no rows" rather than aborting with IllegalArgumentException.
            if ( objects == null || objects.isEmpty() ) {
                return java.util.Collections.emptyMap();
            }
            return rekey( objects, delegate.readAclsById( normalize( objects ), sids ) );
        }

        /**
         * Re-key the result map by the caller's original ObjectIdentity instances. Spring Security's
         * lookup builds Map keys using {@link org.springframework.security.acls.domain.ObjectIdentityImpl}
         * from DB rows; callers passing in gsec's {@link gemma.gsec.acl.domain.AclObjectIdentity} get
         * back a map whose keys won't satisfy {@code result.get(originalOid)} because of the
         * type-strict equals on both sides. Map each caller-supplied OID to the Acl that lives under
         * its normalized counterpart in the delegate's result.
         */
        private Map<ObjectIdentity, Acl> rekey( List<ObjectIdentity> originals, Map<ObjectIdentity, Acl> byNormalized ) {
            Map<ObjectIdentity, Acl> out = new java.util.LinkedHashMap<>( originals.size() );
            for ( ObjectIdentity oid : originals ) {
                Acl acl = byNormalized.get( normalize( oid ) );
                if ( acl != null ) {
                    out.put( oid, acl );
                }
            }
            return out;
        }

        // ---- ObjectIdentity normalization ---------------------------------

        /**
         * Spring Security's {@link org.springframework.security.acls.domain.ObjectIdentityImpl#equals}
         * and {@code hashCode} are not symmetric with gsec's
         * {@link gemma.gsec.acl.domain.AclObjectIdentity}: ObjectIdentityImpl#equals does an
         * {@code instanceof ObjectIdentityImpl} check (returns false for AclObjectIdentity), and its
         * hashCode formula (31*type + identifier) differs from AclObjectIdentity's
         * Objects.hash(type, identifier). BasicLookupStrategy returns a
         * {@code Map<ObjectIdentity, Acl>} keyed by ObjectIdentityImpl instances it builds from DB
         * rows; if the caller passed in an AclObjectIdentity, {@code containsKey} fails and
         * JdbcAclService throws NotFoundException even though the row exists. Normalize at the
         * adapter boundary so every OID handed to JdbcMutableAclService is an ObjectIdentityImpl.
         */
        private static ObjectIdentity normalize( ObjectIdentity oid ) {
            if ( oid == null || oid instanceof org.springframework.security.acls.domain.ObjectIdentityImpl ) {
                return oid;
            }
            return new org.springframework.security.acls.domain.ObjectIdentityImpl( oid.getType(), oid.getIdentifier() );
        }

        private static List<ObjectIdentity> normalize( List<ObjectIdentity> oids ) {
            List<ObjectIdentity> out = new java.util.ArrayList<>( oids.size() );
            for ( ObjectIdentity oid : oids ) {
                out.add( normalize( oid ) );
            }
            return out;
        }

        // ---- gsec extensions ----------------------------------------------

        /**
         * gsec's session-aware overload existed so callers could thread a specific Hibernate session
         * through ACL reads (avoiding session conflicts in batch operations). Spring Security's JDBC
         * stack doesn't have or need this concept — it manages its own connections via DataSource.
         * Ignore the session argument.
         */
        @Override
        public Acl readAclById( ObjectIdentity oid, Session session ) throws NotFoundException {
            return delegate.readAclById( oid );
        }

        /**
         * gsec used this to vend a fresh Hibernate session for ACL operations. Returning null
         * signals to callers that no separate session is in play; ACL ops ride whatever
         * transaction is active on the configured DataSource.
         */
        @Override
        public Session openSession() {
            return null;
        }

        /**
         * Delete a SID and all ACEs referencing it. gsec's original implementation went through
         * Hibernate; here we do it via JDBC against the canonical Spring schema. Wrapped in the
         * caller's @Transactional context, sharing the Hibernate-bound connection.
         */
        @Override
        public void deleteSid( Sid sid ) {
            // Identify the acl_sid row first. AclSid subclasses carry the principal/grantedAuthority
            // name; resolve to the (principal, sid) pair Spring Security uses.
            boolean isPrincipal;
            String name;
            if ( sid instanceof org.springframework.security.acls.domain.PrincipalSid ) {
                isPrincipal = true;
                name = ( ( org.springframework.security.acls.domain.PrincipalSid ) sid ).getPrincipal();
            } else if ( sid instanceof org.springframework.security.acls.domain.GrantedAuthoritySid ) {
                isPrincipal = false;
                name = ( ( org.springframework.security.acls.domain.GrantedAuthoritySid ) sid ).getGrantedAuthority();
            } else {
                throw new IllegalArgumentException( "Unsupported Sid type: " + sid.getClass().getName() );
            }

            // queryForObject throws EmptyResultDataAccessException on zero rows; for a deleteSid
            // call against an already-missing sid we want a no-op, not an exception.
            List<Long> sidIds = jdbcTemplate.queryForList(
                    "SELECT id FROM acl_sid WHERE principal = ? AND sid = ?",
                    Long.class, isPrincipal, name );
            if ( sidIds.isEmpty() ) {
                return; // already gone
            }
            Long sidId = sidIds.get( 0 );

            // Order matters: delete ACEs first, then the SID itself. acl_object_identity.owner_sid
            // FKs are nullable; null them out for any AOIs owned by this SID.
            jdbcTemplate.update( "DELETE FROM acl_entry WHERE sid = ?", sidId );
            jdbcTemplate.update( "UPDATE acl_object_identity SET owner_sid = NULL WHERE owner_sid = ?", sidId );
            jdbcTemplate.update( "DELETE FROM acl_sid WHERE id = ?", sidId );
        }
    }
}
