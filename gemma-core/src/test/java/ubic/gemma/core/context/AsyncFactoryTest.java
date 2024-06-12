package ubic.gemma.core.context;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class AsyncFactoryTest extends AbstractJUnit4SpringContextTests {

    public static class MyService {
        public MyService() throws InterruptedException {
            Thread.sleep( 200 );
        }
    }

    public static class MyServiceFactory extends AbstractAsyncFactoryBean<MyService> {

        public MyServiceFactory() {
            super( Executors.newFixedThreadPool( 8 ) );
        }

        @Override
        public MyService createObject() throws Exception {
            return new MyService();
        }

        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    @Configuration
    @TestComponent
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
        Future<MyService> future = beanFactory.getBean( MyServiceFactory.class ).getObject();
        Future<MyService> future2 = beanFactory.getBean( MyServiceFactory.class ).getObject();
        Assert.assertNotSame( future, future2 );
    }

    @Test
    public void testGetBeanAsyncThenWait() throws ExecutionException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        List<Future<MyService>> futures = new ArrayList<>();
        for ( int i = 0; i < 10; i++ ) {
            futures.add( factory.getObject() );
        }
        for ( Future<MyService> future : futures ) {
            future.get();
        }
        stopWatch.stop();
        // loading them all sequentially would take 2s
        Assert.assertTrue( stopWatch.getTotalTimeMillis() < 500 );
    }

    @Test
    public void stressTest() throws Exception {
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        ExecutorService executor = Executors.newFixedThreadPool( 16 );
        List<Future<Future<MyService>>> futures = new ArrayList<>();
        for ( int i = 0; i < 10000; i++ ) {
            futures.add( executor.submit( factory::getObject ) );
        }
        executor.shutdown();
        // this should finish very quickly, but the future creation will still be pending
        Assert.assertTrue( executor.awaitTermination( 5, TimeUnit.SECONDS ) );
        // cancel all pending creation
        factory.destroy();
        // wait after all the pending bean creation
        for ( Future<Future<MyService>> f : futures ) {
            Assert.assertTrue( f.isDone() );
            Assert.assertTrue( f.get().isDone() || f.get().isCancelled() );
        }
    }

    @Test
    public void testGetBeanAsyncThenCancel() {
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        List<Future<MyService>> futures = new ArrayList<>();
        for ( int i = 0; i < 100; i++ ) {
            futures.add( factory.getObject() );
        }
        factory.destroy();
        for ( Future<MyService> future : futures ) {
            Assert.assertTrue( future.isCancelled() );
        }
    }

    @Test
    public void testGetBean() throws ExecutionException, InterruptedException {
        MyService myService = beanFactory.getBean( MyServiceFactory.class ).getObject().get();
        MyService myService1 = beanFactory.getBean( MyServiceFactory.class ).getObject().get();
        Assert.assertNotSame( myService, myService1 );
    }
}
