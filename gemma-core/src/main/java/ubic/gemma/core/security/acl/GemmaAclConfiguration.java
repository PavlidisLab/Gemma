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
    public AclAuthorizationStrategy aclAuthorizationStrategy() {
        GrantedAuthority admin = new SimpleGrantedAuthority( ADMIN_AUTHORITY );
        return new AclAuthorizationStrategyImpl( admin, admin, admin );
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
            return delegate.createAcl( objectIdentity );
        }

        @Override
        public void deleteAcl( ObjectIdentity objectIdentity, boolean deleteChildren ) {
            delegate.deleteAcl( objectIdentity, deleteChildren );
        }

        @Override
        public MutableAcl updateAcl( MutableAcl acl ) throws NotFoundException {
            return delegate.updateAcl( acl );
        }

        // ---- Spring's AclService ------------------------------------------

        @Override
        public List<ObjectIdentity> findChildren( ObjectIdentity parentIdentity ) {
            return delegate.findChildren( parentIdentity );
        }

        @Override
        public Acl readAclById( ObjectIdentity object ) throws NotFoundException {
            return delegate.readAclById( object );
        }

        @Override
        public Acl readAclById( ObjectIdentity object, List<Sid> sids ) throws NotFoundException {
            return delegate.readAclById( object, sids );
        }

        @Override
        public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects ) throws NotFoundException {
            return delegate.readAclsById( objects );
        }

        @Override
        public Map<ObjectIdentity, Acl> readAclsById( List<ObjectIdentity> objects, List<Sid> sids )
                throws NotFoundException, UnloadedSidException {
            return delegate.readAclsById( objects, sids );
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
            if ( sid instanceof gemma.gsec.acl.domain.AclPrincipalSid ) {
                isPrincipal = true;
                name = ( ( gemma.gsec.acl.domain.AclPrincipalSid ) sid ).getPrincipal();
            } else if ( sid instanceof gemma.gsec.acl.domain.AclGrantedAuthoritySid ) {
                isPrincipal = false;
                name = ( ( gemma.gsec.acl.domain.AclGrantedAuthoritySid ) sid ).getGrantedAuthority();
            } else if ( sid instanceof org.springframework.security.acls.domain.PrincipalSid ) {
                isPrincipal = true;
                name = ( ( org.springframework.security.acls.domain.PrincipalSid ) sid ).getPrincipal();
            } else if ( sid instanceof org.springframework.security.acls.domain.GrantedAuthoritySid ) {
                isPrincipal = false;
                name = ( ( org.springframework.security.acls.domain.GrantedAuthoritySid ) sid ).getGrantedAuthority();
            } else {
                throw new IllegalArgumentException( "Unsupported Sid type: " + sid.getClass().getName() );
            }

            Long sidId = jdbcTemplate.queryForObject(
                    "SELECT id FROM acl_sid WHERE principal = ? AND sid = ?",
                    Long.class, isPrincipal, name );
            if ( sidId == null ) {
                return; // already gone
            }

            // Order matters: delete ACEs first, then the SID itself. acl_object_identity.owner_sid
            // FKs are nullable; null them out for any AOIs owned by this SID.
            jdbcTemplate.update( "DELETE FROM acl_entry WHERE sid = ?", sidId );
            jdbcTemplate.update( "UPDATE acl_object_identity SET owner_sid = NULL WHERE owner_sid = ?", sidId );
            jdbcTemplate.update( "DELETE FROM acl_sid WHERE id = ?", sidId );
        }
    }
}
