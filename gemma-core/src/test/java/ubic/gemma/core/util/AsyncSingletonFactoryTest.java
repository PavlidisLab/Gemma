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
import ubic.gemma.persistence.util.AbstractAsyncFactoryBean;

import java.util.concurrent.Future;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration
public class AsyncSingletonFactoryTest extends AbstractJUnit4SpringContextTests {

    public static class MyService {
        public MyService() throws InterruptedException {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                System.out.println( "Got interrupted!" );
                throw e;
            }
        }

    }

    public static class MyServiceFactory extends AbstractAsyncFactoryBean<MyService> {

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
            return true;
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
        Assert.assertSame( future, future2 );
    }

    @Test
    public void testGetBeanAsyncThenCancel() {
        Future<MyService> future = beanFactory.getBean( MyServiceFactory.class ).getObjectAsync();
        beanFactory.destroySingletons();
        Assert.assertTrue( future.isCancelled() );
    }

    @Test
    public void testGetBean() {
        MyService myService = beanFactory.getBean( MyService.class );
        MyService myService1 = beanFactory.getBean( MyService.class );
        Assert.assertSame( myService, myService1 );
    }
}
