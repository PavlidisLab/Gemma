package ubic.gemma.persistence.service;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;
import ubic.gemma.model.common.Identifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.noInteractions;

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

        public MyDao( SessionFactory sessionFactory, int batchSize ) {
            super( MyEntity.class, sessionFactory, batchSize );
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
        ClassMetadata myEntityClassMetadata = mock( ClassMetadata.class );
        when( myEntityClassMetadata.getIdentifierPropertyName() ).thenReturn( "id" );
        when( sessionFactory.getClassMetadata( MyEntity.class ) ).thenReturn( myEntityClassMetadata );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.getFlushMode() ).thenReturn( FlushMode.AUTO );
    }

    @Test
    public void testBatchSizeFlushRightAway() {
        myDao = new MyDao( sessionFactory, 1 );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).persist( any( MyEntity.class ) );
        verify( session, times( 10 ) ).flush();
        verify( session, times( 10 ) ).clear();
    }

    @Test
    public void testBatchSizeUnlimited() {
        myDao = new MyDao( sessionFactory, Integer.MAX_VALUE );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).persist( any( MyEntity.class ) );
        verify( session, VerificationModeFactory.times( 0 ) ).flush();
        verify( session, times( 0 ) ).clear();
    }

    @Test
    public void testBatchSizeSmall() {
        myDao = new MyDao( sessionFactory, 10 );
        Collection<MyEntity> entities = myDao.create( generateEntities( 10 ) );
        assertThat( entities ).hasSize( 10 );
        verify( session, times( 10 ) ).persist( any( MyEntity.class ) );
        verify( session, VerificationModeFactory.times( 1 ) ).flush();
        verify( session, times( 1 ) ).clear();
    }

    @Test
    public void testBatchingNotAdvisableWhenFlushModeIsManual() {
        myDao = new MyDao( sessionFactory, 10 );
        when( session.getFlushMode() ).thenReturn( FlushMode.MANUAL );
        Collection<MyEntity> entities = myDao.create( generateEntities( 20 ) );
        assertThat( entities ).hasSize( 20 );
        verify( session, times( 20 ) ).persist( any( MyEntity.class ) );
        verify( session, times( 0 ) ).flush();
        verify( session, times( 0 ) ).clear();
    }

    @Test
    public void testLoadByCollection() {
        myDao = new MyDao( sessionFactory );
        Criteria mockCriteria = mock( Criteria.class );
        when( mockCriteria.add( any() ) ).thenReturn( mockCriteria );
        when( session.createCriteria( MyEntity.class ) ).thenReturn( mockCriteria );
        List<Long> ids = Arrays.asList( 1L, 2L, 3L, 4L, 5L );
        myDao.load( ids );
        verify( session ).createCriteria( MyEntity.class );
        verify( mockCriteria ).add( argThat( criterion -> criterion.toString().equals( Restrictions.in( "id", ids ).toString() ) ) );
        verify( mockCriteria ).list();
    }

    private Collection<MyEntity> generateEntities( int count ) {
        Collection<MyEntity> result = new ArrayList<>();
        for ( int i = 0; i < count; i++ ) {
            result.add( new MyEntity() );
        }
        return result;
    }
}
