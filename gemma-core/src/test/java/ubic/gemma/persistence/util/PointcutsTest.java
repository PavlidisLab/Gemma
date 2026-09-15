package ubic.gemma.persistence.util;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.persistence.retry.Retryable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ContextConfiguration
public class PointcutsTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @EnableAspectJAutoProxy
    static class PointcutsTestContextConfiguration {
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

    /**
     * Remaining live pointcuts after Audit Phase C retired the blanket DAO advices
     * (creator / updater / saver / deleter / loader / daoMethod). Only the
     * transactional + retryable pointcuts are wired in production today.
     */
    @Aspect
    static class MyAspect {

        @Before("ubic.gemma.persistence.util.Pointcuts.transactionalMethod()")
        public void doTransactionalAdvice( JoinPoint jp ) {
        }

        @Before("ubic.gemma.persistence.util.Pointcuts.retryableOrTransactionalServiceMethod()")
        public void doRetryAdvice( JoinPoint jp ) {
        }
    }

    @Autowired
    private MyService myService;

    @Autowired
    private MyComponent myComponent;

    @Autowired
    private MyAspect myAspect;

    @AfterEach
    public void tearDown() {
        reset( myAspect );
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
