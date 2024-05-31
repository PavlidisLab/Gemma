package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
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
import ubic.gemma.core.context.TestComponent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

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

    public static class ExceptionWithoutMessage extends Exception {

        public ExceptionWithoutMessage() {
        }
    }

    public static class ExceptionWithMessage extends Exception {

        public ExceptionWithMessage( String message ) {
            super( message );
        }
    }

    @Test
    public void testLoadOrFail() {
        try {
            myService.loadOrFail( 12L, ExceptionWithoutMessage::new );
            failBecauseExceptionWasNotThrown( ExceptionWithoutMessage.class );
        } catch ( ExceptionWithoutMessage e ) {
            assertThat( e ).hasMessage( null );
        }
        try {
            myService.loadOrFail( 12L, ExceptionWithMessage::new, "No ExternalDatabase with ID 12." );
            failBecauseExceptionWasNotThrown( ExceptionWithMessage.class );
        } catch ( ExceptionWithMessage e ) {
            assertThat( e )
                    .hasMessageContaining( "ExternalDatabase" )
                    .hasMessageContaining( "12" );
        }
    }

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
    public void testEnsureInSessionReturnsOriginalCollectionIfAlreadyInSession() {
        List<ExternalDatabase> entities = new ArrayList<>();
        for ( int k = 0; k < 100; k++ ) {
            entities.add( createFixture() );
        }
        assertThat( myService.ensureInSession( entities ) )
                .isSameAs( entities )
                .containsExactlyElementsOf( entities );
    }

    @Test
    public void testEnsureInSessionPreserveInputOrder() {
        List<ExternalDatabase> entities = new ArrayList<>();

        for ( int k = 0; k < 100; k++ ) {
            entities.add( createFixture() );
        }

        // elements must honor the input order
        Collections.shuffle( entities );

        // evict everything, they must be reloaded in the same order
        sessionFactory.getCurrentSession().clear();
        assertThat( myService.ensureInSession( entities ) )
                .isNotSameAs( entities )
                .containsExactlyElementsOf( entities );
    }

    @Test
    public void testEnsureInSessionWhenSomeElementsAreNotInSessionOnlyLoadThoseElements() {
        List<ExternalDatabase> entities = new ArrayList<>();

        for ( int k = 0; k < 100; k++ ) {
            if ( k % 15 == 0 ) {
                entities.add( ExternalDatabase.Factory.newInstance( "test" + k, DatabaseType.OTHER ) ); // a transient instance
            } else {
                entities.add( createFixture() );
            }
        }

        sessionFactory.getCurrentSession().flush();

        // evict every 1 in 10 elements
        for ( int k = 0; k < 100; k += 10 ) {
            sessionFactory.getCurrentSession().evict( entities.get( k ) );
        }

        List<ExternalDatabase> loadedEntities = new ArrayList<>( myService.ensureInSession( entities ) );

        assertThat( loadedEntities )
                .containsExactlyElementsOf( entities );

        // only those elements are reloaded (if non-transient)
        for ( int k = 0; k < 100; k++ ) {
            if ( k % 15 != 0 && k % 10 == 0 ) {
                assertThat( loadedEntities.get( k ) )
                        .isNotSameAs( entities.get( k ) )
                        .isEqualTo( entities.get( k ) );
            } else {
                assertThat( loadedEntities.get( k ) )
                        .isSameAs( entities.get( k ) );
            }
        }
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