package ubic.gemma.core.util.test;

import gemma.gsec.acl.AclAuthorizationStrategyImpl;
import gemma.gsec.acl.AclSidRetrievalStrategyImpl;
import gemma.gsec.acl.ObjectIdentityRetrievalStrategyImpl;
import gemma.gsec.acl.domain.AclDao;
import gemma.gsec.acl.domain.AclDaoImpl;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.acl.domain.AclServiceImpl;
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
        AclAuthorizationStrategy aclAuthorizationStrategy = new AclAuthorizationStrategyImpl(
                new GrantedAuthority[] { new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ), new SimpleGrantedAuthority( "ADMIN" ) },
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
