package ubic.gemma.core.util;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.util.StopWatch;
import ubic.gemma.persistence.util.AbstractAsyncFactoryBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class AsyncFactoryTest extends AbstractJUnit4SpringContextTests {
    public static class MyService {
        public MyService() throws InterruptedException {
            try {
                Thread.sleep( 200 );
            } catch ( InterruptedException e ) {
                System.out.println( "Got interrupted!" );
                throw e;
            }
        }

    }

    public static class MyServiceFactory extends AbstractAsyncFactoryBean<MyService> {

        public MyServiceFactory() {
            super( Executors.newFixedThreadPool( 4 ) );
        }

        @Override
        public MyService getObject() throws Exception {
            return new MyService();
        }

        @Override
        public Class<?> getObjectType() {
            return MyService.class;
        }

        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    @Configuration
    static class AsyncFactoryBeanTestContextConfiguration {

        @Bean
        MyServiceFactory myService() {
            return new MyServiceFactory();
        }
    }

    @Autowired
    private ConfigurableBeanFactory beanFactory;

    @Test
    public void testGetBeanAsync() {
        Future<MyService> future = beanFactory.getBean( MyServiceFactory.class ).getObjectAsync();
        Future<MyService> future2 = beanFactory.getBean( MyServiceFactory.class ).getObjectAsync();
        Assert.assertNotSame( future, future2 );
    }

    @Test
    public void testGetBeanAsyncThenWait() throws ExecutionException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        List<Future<MyService>> futures = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            futures.add( factory.getObjectAsync() );
        }
        for ( Future<MyService> future : futures ) {
            future.get();
        }
        stopWatch.stop();
        // loading them all sequentially would take 10s
        Assert.assertTrue( stopWatch.getTotalTimeMillis() < 1000 );
    }

    @Test
    public void testGetBeanAsyncThenCancel() {
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        List<Future<MyService>> futures = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            futures.add( factory.getObjectAsync() );
        }
        factory.destroy();
        for ( Future<MyService> future : futures ) {
            Assert.assertTrue( future.isCancelled() );
        }
    }

    @Test
    public void testGetBean() {
        MyService myService = beanFactory.getBean( MyService.class );
        MyService myService1 = beanFactory.getBean( MyService.class );
        Assert.assertNotSame( myService, myService1 );
    }
}
