package ubic.gemma.core.util.test;

import ubic.gemma.core.security.gsec.acl.AclAuthorizationStrategyImpl;
import ubic.gemma.core.security.gsec.acl.AclSidRetrievalStrategyImpl;
import ubic.gemma.core.security.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import ubic.gemma.core.security.gsec.acl.domain.AclDao;
import ubic.gemma.core.security.gsec.acl.domain.AclDaoImpl;
import ubic.gemma.core.security.gsec.acl.domain.AclService;
import ubic.gemma.core.security.gsec.acl.domain.AclServiceImpl;
import org.hibernate.SessionFactory;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.hierarchicalroles.NullRoleHierarchy;
import org.springframework.security.acls.domain.AclAuthorizationStrategy;
import org.springframework.security.acls.domain.ConsoleAuditLogger;
import org.springframework.security.acls.domain.DefaultPermissionGrantingStrategy;
import org.springframework.security.acls.domain.SpringCacheBasedAclCache;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.SidRetrievalStrategy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ubic.gemma.core.context.TestComponent;

/**
 * Configuration for enabling ACLs in a {@link BaseDatabaseTest}.
 *
 * @author poirigui
 */
@Configuration
@TestComponent
public class BaseDatabaseAclConfig {

    @Bean
    public AclDao aclDao( SessionFactory sessionFactory, SidRetrievalStrategy sidRetrievalStrategy ) {
        // The required-authority must match what test users actually carry. Gemma tests use
        // @WithMockUser(authorities="GROUP_ADMIN") or runAsAdmin() which grants GROUP_ADMIN — the
        // prior "ADMIN" wiring was a stale relic and made gsec's AclAuthorizationStrategyImpl
        // reject every ACL modification because no one ever has the literal "ADMIN" authority.
        AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
                new GrantedAuthority[] { new SimpleGrantedAuthority( "GROUP_ADMIN" ), new SimpleGrantedAuthority( "GROUP_ADMIN" ), new SimpleGrantedAuthority( "GROUP_ADMIN" ) },
                sidRetrievalStrategy );
        return new AclDaoImpl( sessionFactory,
                aclAuthorizationStrategy,
                new SpringCacheBasedAclCache( new ConcurrentMapCache( "acl" ),
                        new DefaultPermissionGrantingStrategy( new ConsoleAuditLogger() ),
                        aclAuthorizationStrategy ) );
    }

    @Bean
    public AclService aclService( AclDao aclDao ) {
        return new AclServiceImpl( aclDao );
    }

    @Bean
    public ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy() {
        return new ObjectIdentityRetrievalStrategyImpl();
    }

    @Bean
    public SidRetrievalStrategy sidRetrievalStrategy() {
        return new AclSidRetrievalStrategyImpl( new NullRoleHierarchy() );
    }
}
