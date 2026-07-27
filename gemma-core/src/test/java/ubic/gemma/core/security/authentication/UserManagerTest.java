package ubic.gemma.core.security.authentication;

import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.security.authentication.UserDetailsImpl;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserDaoImpl;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDao;
import ubic.gemma.persistence.service.common.auditAndSecurity.UserGroupDaoImpl;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ContextConfiguration
public class UserManagerTest extends BaseDatabaseTest5 {

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
        public UserReadService userReadService( UserDao userDao, UserGroupDao userGroupDao ) {
            return new UserReadServiceImpl( userDao, userGroupDao );
        }

        @Bean
        public UserManager userManager() {
            return new UserManagerImpl();
        }

        @Bean
        public AuthenticationTrustResolver authenticationTrustResolver() {
            return new AuthenticationTrustResolverImpl();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
            return mock();
        }
    }

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void testChangePasswordRejectsWrongCurrentPassword() {
        try {
            User user = createUser();
            user.setPassword( "STORED_HASH" );
            sessionFactory.getCurrentSession().flush();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken( "foo", "x", Collections.emptyList() ) );
            // mocked encoder.matches(...) defaults to false → the supplied current password is wrong
            assertThatThrownBy( () -> userManager.changePassword( "wrong-current", "a-fine-new-password" ) )
                    .isInstanceOf( BadCredentialsException.class );
            // password must be untouched
            sessionFactory.getCurrentSession().flush();
            sessionFactory.getCurrentSession().evict( user );
            User reloaded = ( User ) sessionFactory.getCurrentSession().get( User.class, user.getId() );
            assertThat( reloaded.getPassword() ).isEqualTo( "STORED_HASH" );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testChangePasswordRejectsTooShortNewPassword() {
        try {
            User user = createUser();
            user.setPassword( "STORED_HASH" );
            sessionFactory.getCurrentSession().flush();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken( "foo", "x", Collections.emptyList() ) );
            // correct current password, but the new one is below the minimum length
            when( passwordEncoder.matches( "right-current", "STORED_HASH" ) ).thenReturn( true );
            assertThatThrownBy( () -> userManager.changePassword( "right-current", "short" ) )
                    .isInstanceOf( IllegalArgumentException.class );
            sessionFactory.getCurrentSession().flush();
            sessionFactory.getCurrentSession().evict( user );
            User reloaded = ( User ) sessionFactory.getCurrentSession().get( User.class, user.getId() );
            assertThat( reloaded.getPassword() ).isEqualTo( "STORED_HASH" );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testAdminChangePasswordEncodesAndStores() {
        User user = createUser();
        user.setPassword( "OLD_HASH" );
        sessionFactory.getCurrentSession().flush();
        when( passwordEncoder.encode( "a-brand-new-password" ) ).thenReturn( "NEW_ENCODED_HASH" );

        userManager.adminChangePassword( "foo", "a-brand-new-password" );

        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( user );
        User reloaded = ( User ) sessionFactory.getCurrentSession().get( User.class, user.getId() );
        assertThat( reloaded.getPassword() ).isEqualTo( "NEW_ENCODED_HASH" );
    }

    @Test
    public void testAdminChangePasswordRejectsTooShortPassword() {
        createUser();
        assertThatThrownBy( () -> userManager.adminChangePassword( "foo", "short" ) )
                .isInstanceOf( IllegalArgumentException.class );
    }

    @Test
    public void testAdminChangePasswordUnknownUser() {
        assertThatThrownBy( () -> userManager.adminChangePassword( "nobody", "a-fine-new-password" ) )
                .isInstanceOf( UsernameNotFoundException.class );
    }

    @Test
    public void testUpdateUser() {
        User user = createUser();
        assertTrue( user.isEnabled() );
        UserDetailsImpl ud = new UserDetailsImpl( user );
        ud.setEnabled( false );
        userManager.updateUser( ud );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().evict( user );
        User reloadedUser = ( User ) sessionFactory.getCurrentSession().get( User.class, user.getId() );
        assertFalse( reloadedUser.isEnabled() );
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
        User user = User.Factory.newInstance( "foo" );
        user.setEnabled( true );
        sessionFactory.getCurrentSession().persist( user );
        return user;
    }
}