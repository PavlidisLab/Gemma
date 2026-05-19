package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
import ubic.gemma.model.common.auditAndSecurity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ContextConfiguration
public class UserDaoTest extends BaseDatabaseTest5 {

    @Configuration
    @TestComponent
    static class CC extends BaseDatabaseTestContextConfiguration {

        @Bean
        public UserDao userDao( SessionFactory sessionFactory ) {
            return new UserDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private UserDao userDao;

    /**
     * Usernames are immutable, but Hibernate should be configured to disregard any changes.
     */
    @Test
    public void testUpdateUsername() {
        User user = User.Factory.newInstance( "foobar" );
        user = userDao.create( user );

        user.setUserName( "barfoo" );

        userDao.update( user );
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        user = userDao.reload( user );
        assertEquals( "foobar", user.getUserName() );
    }
}