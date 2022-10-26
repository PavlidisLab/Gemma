package ubic.gemma.persistence.service;

import org.hibernate.HibernateException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AbstractDaoIntegrationTest extends BaseSpringContextTest {

    @Repository
    static class MyRepository {
        public void methodThatRaisesHibernateException() {
            throw new HibernateException( "test" );
        }
    }

    @Autowired
    private MyRepository myRepository;

    @Test
    public void testExceptionTranslation() {
        assertThatThrownBy( myRepository::methodThatRaisesHibernateException )
                .isInstanceOf( DataAccessException.class )
                .cause()
                .isInstanceOf( HibernateException.class );
    }
}
