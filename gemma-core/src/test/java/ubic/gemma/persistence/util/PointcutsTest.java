package ubic.gemma.persistence.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.persistence.retry.Retryable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class PointcutsTest extends BaseTest {

    @Configuration
    @TestComponent
    @EnableAspectJAutoProxy
    static class AuditAdviceTestContextConfiguration {
        @Bean
        public Dao dao() {
            return new Dao();
        }

        @Bean
        public MyService myService() {
            return new MyService();
        }

        @Bean
        public MyComponent myComponent() {
            return new MyComponent();
        }

        @Bean
        public MyAspect myAspect() {
            return mock( MyAspect.class );
        }
    }

    @Repository
    static class Dao {

        public void create( Object ee ) {
        }

        public void read() {
        }

        public Object read( Long id ) {
            return new Object();
        }

        public void update( Object ee ) {
        }

        public Object save( Object ee ) {
            return new Object();
        }

        public void delete( Object ee ) {
        }

        /**
         * A deleter with extra arguments.
         */
        public void delete( Object ee, boolean force ) {
        }

        /**
         * Modifier without arguments should never by advised.
         */
        public void delete() {
        }

        public void remove( Object ee ) {
        }
    }

    @Service
    @Transactional
    static class MyService {

        public void create( Object ee ) {

        }

        public void read( Object ee ) {

        }

        public void update( Object ee ) {

        }

        public void delete( Object ee ) {

        }

        @Retryable
        public void mightFail() {
        }

        /**
         * Package-private, should never be advised.
         */
        void mightFail2() {
        }

        /**
         * Private, should never by advised.
         */
        private void mightFailInternal() {
        }
    }

    @Component
    static class MyComponent {

        @Transactional
        public void create( Object ee ) {

        }

        public void read( Object ee ) {

        }

        public void update( Object ee ) {

        }

        public void delete( Object ee ) {

        }
    }

    @Aspect
    static class MyAspect {

        @Before("ubic.gemma.persistence.util.Pointcuts.creator()")
        public void doCreateAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.loader()")
        public void doReadAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.updater()")
        public void doUpdateAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.saver()")
        public void doSaveAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.deleter()")
        public void doDeleteAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.transactionalMethod()")
        public void doTransactionalAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.retryableOrTransactionalServiceMethod()")
        public void doRetryAdvice( JoinPoint jp ) {
        }
    }

    @Autowired
    private Dao dao;

    @Autowired
    private MyService myService;

    @Autowired
    private MyComponent myComponent;

    @Autowired
    private MyAspect myAspect;

    @After
    public void tearDown() {
        reset( myAspect );
    }

    @Test
    public void testCrud() {
        dao.create( new Object() );
        verify( myAspect ).doCreateAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        myService.create( new Object() );
        verify( myAspect ).doTransactionalAdvice( any() );
        verify( myAspect ).doRetryAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        myComponent.create( new Object() );
        verify( myAspect ).doTransactionalAdvice( any() );
        verifyNoMoreInteractions( myAspect );
    }

    @Test
    public void testCrudRead() {
        dao.read();
        verify( myAspect ).doReadAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        dao.read( 1L );
        verify( myAspect ).doReadAdvice( any() );
        verifyNoMoreInteractions( myAspect );
    }

    @Test
    public void testCrudSave() {
        dao.save( new Object() );
        verify( myAspect ).doSaveAdvice( any() );
        verifyNoMoreInteractions( myAspect );
    }

    @Test
    public void testCrudDelete() {
        dao.delete( new Object() );
        verify( myAspect ).doDeleteAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        dao.delete( new Object(), true );
        verify( myAspect ).doDeleteAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        dao.delete();
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        dao.remove( new Object() );
        verify( myAspect ).doDeleteAdvice( any() );
        verifyNoMoreInteractions( myAspect );
    }

    @Test
    public void testTransactionalService() {
        myService.create( new Object() );
        verify( myAspect ).doTransactionalAdvice( any() );
        verify( myAspect ).doRetryAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        myComponent.create( new Object() );
        verify( myAspect ).doTransactionalAdvice( any() );
        verifyNoMoreInteractions( myAspect );
    }

    @Test
    public void testRetryableMethod() {
        myService.mightFail();
        verify( myAspect ).doRetryAdvice( any() );
        verify( myAspect ).doTransactionalAdvice( any() );
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        myService.mightFail2();
        verifyNoMoreInteractions( myAspect );
        reset( myAspect );

        myService.mightFailInternal();
        verifyNoMoreInteractions( myAspect );
    }
}
