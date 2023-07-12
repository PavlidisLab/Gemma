package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDao;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseDaoImpl;
import ubic.gemma.persistence.util.TestComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class AbstractServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class AbstractServiceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExternalDatabaseDao externalDatabaseDao( SessionFactory sessionFactory ) {
            return new ExternalDatabaseDaoImpl( sessionFactory );
        }

        @Bean
        public MyService myService( ExternalDatabaseDao dao ) {
            return new MyService( dao );
        }
    }

    @Autowired
    private MyService myService;

    private int i = 0;

    @Test
    public void testEnsureInSession() {
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance( "test", DatabaseType.OTHER );

        // a transient instance
        assertThat( myService.ensureInSession( ed ) ).isSameAs( ed );

        ed = myService.save( ed );
        assertThat( ed.getId() ).isNotNull();

        // a persistent instance is reused
        assertThat( myService.ensureInSession( ed ) ).isSameAs( ed );

        sessionFactory.getCurrentSession().evict( ed );

        // an evicted instance must be reloaded
        assertThat( myService.ensureInSession( ed ) ).isNotSameAs( ed );
    }

    @Test
    @Ignore
    public void testEnsureInSessionWithCollection() {
        List<ExternalDatabase> entities = new ArrayList<>();

        for ( int k = 0; k < 100; k++ ) {
            if ( k % 20 == 0 ) {
                entities.add( ExternalDatabase.Factory.newInstance( "test" + ( ++i ), DatabaseType.OTHER ) );
            } else {
                entities.add( createFixture() );
            }
        }

        ExternalDatabase firstEntity = entities.iterator().next();
        assertThat( myService.ensureInSession( entities ) )
                .isSameAs( entities )
                .containsExactlyElementsOf( entities )
                .first()
                .isEqualTo( firstEntity )
                .isSameAs( firstEntity );

        // elements must honor the input order
        Collections.shuffle( entities );
        assertThat( myService.ensureInSession( entities ) )
                .containsExactlyElementsOf( entities );

        // evict one element
        sessionFactory.getCurrentSession().evict( firstEntity );

        assertThat( myService.ensureInSession( entities ) )
                .isNotSameAs( entities )
                .containsExactlyElementsOf( entities )
                .first()
                .isEqualTo( firstEntity )
                .isNotSameAs( firstEntity );

        // evict everything
        sessionFactory.getCurrentSession().clear();

        firstEntity = entities.iterator().next();
        assertThat( myService.ensureInSession( entities ) )
                .containsExactlyElementsOf( entities )
                .first()
                .isEqualTo( firstEntity )
                .isNotSameAs( firstEntity );
    }

    private ExternalDatabase createFixture() {
        return myService.create( ExternalDatabase.Factory.newInstance( "test" + ( ++i ), DatabaseType.OTHER ) );
    }

    private static class MyService extends AbstractService<ExternalDatabase> {

        public MyService( BaseDao<ExternalDatabase> mainDao ) {
            super( mainDao );
        }

        @Override
        public ExternalDatabase ensureInSession( ExternalDatabase entity ) {
            return super.ensureInSession( entity );
        }

        @Override
        public Collection<ExternalDatabase> ensureInSession( Collection<ExternalDatabase> entities ) {
            return super.ensureInSession( entities );
        }
    }
}