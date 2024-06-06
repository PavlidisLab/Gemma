package ubic.gemma.core.security.authentication;

import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDaoImpl;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDaoImpl;
import ubic.gemma.core.context.TestComponent;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@ContextConfiguration
public class UserManagerTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class UserManagerImplTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public UserDao userDao( SessionFactory sessionFactory ) {
            return new UserDaoImpl( sessionFactory );
        }

        @Bean
        public UserGroupDao userGroupDao( SessionFactory sessionFactory ) {
            return new UserGroupDaoImpl( sessionFactory );
        }

        @Bean
        public SecurityService securityService() {
            return mock( SecurityService.class );
        }

        @Bean
        public UserService userService() {
            return new UserServiceImpl();
        }

        @Bean
        public UserManager userManager() {
            return new UserManagerImpl();
        }
    }

    @Autowired
    private UserManager userManager;

    @Test
    public void testUpdateUser() {
        User user = createUser();
        assertTrue( user.getEnabled() );
        UserDetailsImpl ud = new UserDetailsImpl( user );
        ud.setEnabled( false );
        userManager.updateUser( ud );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( user );
        User reloadedUser = ( User ) sessionFactory.getCurrentSession().get( User.class, user.getId() );
        assertFalse( reloadedUser.getEnabled() );
    }

    @Test
    public void testUpdateGroups() {
        User user = createUser();
        assertTrue( user.getGroups().isEmpty() );
        userManager.updateUserGroups( new UserDetailsImpl( user ), Collections.singletonList( "Users" ) );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( user );
        user = userManager.findByUserName( "foo" );
        assertThat( user.getGroups() )
                .extracting( "name" )
                .containsExactly( "Users" );
    }

    private User createUser() {
        User user = new User();
        user.setUserName( "foo" );
        user.setEnabled( true );
        sessionFactory.getCurrentSession().persist( user );
        return user;
    }
}