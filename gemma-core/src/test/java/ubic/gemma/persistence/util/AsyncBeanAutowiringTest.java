package ubic.gemma.persistence.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@DirtiesContext
@ContextConfiguration
public class AsyncBeanAutowiringTest extends AbstractJUnit4SpringContextTests {

    public static class MyService {
    }

    public static class MyServiceFactory extends AbstractAsyncFactoryBean<MyService> {

        @Override
        public MyService createObject() {
            return new MyService();
        }

        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    public static class MyService2 {
    }

    public static class MyService2Factory extends AbstractAsyncFactoryBean<MyService2> {

        @Override
        public MyService2 createObject() {
            return new MyService2();
        }

        @Override
        public boolean isSingleton() {
            return false;
        }
    }

    @Configuration
    @TestComponent
    static class AsyncBeanAutowiringTestContextConfiguration {
        @Bean
        public MyServiceFactory myService() {
            return new MyServiceFactory();
        }

        @Bean
        public MyService2Factory myService2() {
            return new MyService2Factory();
        }
    }

    @Autowired
    BeanFactory beanFactory;

    @Autowired
    Future<MyService> myService;

    @Autowired
    Future<MyService2> myService2;

    @Autowired
    MyServiceFactory myServiceFactory;

    @Test
    public void testAutowiredBean() {
        Assertions.assertThat( myService ).succeedsWithin( 1, TimeUnit.SECONDS );
        Assertions.assertThat( myService2 ).succeedsWithin( 1, TimeUnit.SECONDS );
    }

    @Test
    public void testInjectViaBeanFactory() {
        Assertions.assertThat( beanFactory.getBean( MyServiceFactory.class ).getObject() ).succeedsWithin( 1, TimeUnit.SECONDS );
    }

    @Test
    public void testInjectViaServiceFactory() {
        Assertions.assertThat( myServiceFactory.getObject() ).succeedsWithin( 1, TimeUnit.SECONDS );
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testInjectBeanDirectly() {
        beanFactory.getBean( MyService.class );
    }

}
