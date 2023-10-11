package ubic.gemma.persistence.service;

import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.verification.VerificationModeFactory;
import ubic.gemma.model.common.Identifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    @Before
    public void setUp() {
        session = mock( Session.class );
        sessionFactory = mock( SessionFactory.class );
        ClassMetadata myEntityClassMetadata = mock( ClassMetadata.class );
        when( myEntityClassMetadata.getIdentifierPropertyName() ).thenReturn( "id" );
        when( myEntityClassMetadata.getMappedClass() ).thenReturn( MyEntity.class );
        when( sessionFactory.getClassMetadata( MyEntity.class ) ).thenReturn( myEntityClassMetadata );
        when( sessionFactory.getCurrentSession() ).thenReturn( session );
        when( session.getFlushMode() ).thenReturn( FlushMode.AUTO );
    }

    private static abstract class MyEntityProxy extends MyEntity implements HibernateProxy {

    }

    @Test
    public void testLoadByCollection() {
        myDao = new MyDao( sessionFactory );
        Criteria mockCriteria = mock( Criteria.class );
        when( mockCriteria.add( any() ) ).thenReturn( mockCriteria );
        when( session.createCriteria( MyEntity.class ) ).thenReturn( mockCriteria );
        when( session.load( any( Class.class ), any() ) ).thenAnswer( a -> {
            MyEntityProxy entity = mock( MyEntityProxy.class );
            LazyInitializer lazyInitializer = mock( LazyInitializer.class );
            when( lazyInitializer.isUninitialized() ).thenReturn( true );
            when( entity.getId() ).thenReturn( a.getArgument( 1 ) );
            when( entity.getHibernateLazyInitializer() ).thenReturn( lazyInitializer );
            return entity;
        } );
        List<Long> ids = Arrays.asList( 1L, 2L, 3L, 4L, 5L );
        myDao.load( ids );
        verify( session ).load( MyEntity.class, 1L );
        verify( session ).load( MyEntity.class, 2L );
        verify( session ).load( MyEntity.class, 3L );
        verify( session ).load( MyEntity.class, 4L );
        verify( session ).load( MyEntity.class, 5L );
        verify( session ).createCriteria( MyEntity.class );
        verifyNoMoreInteractions( session );
        verify( mockCriteria ).add( argThat( criterion -> criterion.toString().equals( Restrictions.in( "id", ids ).toString() ) ) );
        verify( mockCriteria ).list();
    }
}
