package ubic.gemma.core.context;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.test.BaseTest5;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class AsyncSingletonFactoryTest extends BaseTest5 {

    public static class MyService {
        public MyService() throws InterruptedException {
            Thread.sleep( 200 );
        }
    }

    public static class MyServiceFactory extends AbstractAsyncFactoryBean<MyService> {

        @Override
        public MyService createObject() throws Exception {
            return new MyService();
        }

        @Override
        public boolean isSingleton() {
            return true;
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
        Assertions.assertSame( future, future2 );
    }

    @Test
    public void testGetBeanAndGetBeanAsync() throws Exception {
        Future<MyService> future = beanFactory.getBean( MyServiceFactory.class ).getObject();
        MyService myService = beanFactory.getBean( MyServiceFactory.class ).getObject().get();
        Assertions.assertSame( future.get(), myService );
    }

    @Test
    public void testGetBeanThenCancel() throws Exception {
        MyServiceFactory factory = beanFactory.getBean( MyServiceFactory.class );
        Future<Future<MyService>> myServiceFuture = Executors.newSingleThreadExecutor().submit( factory::getObject );
        Thread.sleep( 20 );
        Assertions.assertTrue( factory.isInitialized() );
        Assertions.assertTrue( myServiceFuture.isDone() );
        Assertions.assertFalse( myServiceFuture.get().isDone() );
        factory.destroy();
        // this should raise a
        Assertions.assertTrue( myServiceFuture.get().isCancelled() );
    }

    @Test
    public void testGetBeanAsyncThenCancel() {
        Future<MyService> future = beanFactory.getBean( MyServiceFactory.class ).getObject();
        beanFactory.destroySingletons();
        Assertions.assertTrue( future.isCancelled() );
    }

    @Test
    public void testGetBean() throws ExecutionException, InterruptedException {
        MyService myService = beanFactory.getBean( MyServiceFactory.class ).getObject().get();
        MyService myService1 = beanFactory.getBean( MyServiceFactory.class ).getObject().get();
        Assertions.assertSame( myService, myService1 );
    }
}
