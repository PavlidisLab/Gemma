package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;
import ubic.gemma.model.common.Identifiable;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AbstractDaoTest {

    public static class MyEntity implements Identifiable {

        private long id;

        @Override
        public Long getId() {
            return id;
        }

        public void setId( long id ) {
            this.id = id;
        }
    }

    public static class MyDao extends AbstractDao<MyEntity> {

        public MyDao( SessionFactory sessionFactory ) {
            super( MyEntity.class, sessionFactory );
        }
    }

    private SessionFactory sessionFactory;
    private Session session;
    private MyDao myDao;

    private long i = 0;

    @Before
    public void setUp() {
        session = mock( Session.class );
        sessionFactory = mock( SessionFactory.class );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.save( any() ) ).thenAnswer( arg -> {
            i++;
            arg.getArgument( 0, MyEntity.class ).setId( i );
            return i;
        } );
        myDao = new MyDao( sessionFactory );
    }

    @Test
    public void testBatchSizeFlushRightAway() {
        myDao.setBatchSize( 1 );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).save( any( MyEntity.class ) );
        verify( session, times( 10 ) ).flush();
        verify( session, times( 10 ) ).clear();
    }

    @Test
    public void testBatchSizeUnlimited() {
        myDao.setBatchSize( Integer.MAX_VALUE );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).save( any( MyEntity.class ) );
        verify( session, VerificationModeFactory.times( 0 ) ).flush();
        verify( session, times( 0 ) ).clear();
    }

    @Test
    public void testBatchSizeSmall() {
        myDao.setBatchSize( 10 );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).save( any( MyEntity.class ) );
        verify( session, VerificationModeFactory.times( 1 ) ).flush();
        verify( session, times( 1 ) ).clear();
    }

    private Collection<MyEntity> generateEntities( int count ) {
        Collection<MyEntity> result = new ArrayList<>();
        for ( int i = 0; i < count; i++ ) {
            result.add( new MyEntity() );
        }
        return result;
    }
}