package ubic.gemma.core.context;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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
            return true;
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

    @Autowired
    List<Future<MyService>> myServiceList;

    @Autowired
    Map<String, Future<MyService>> myServiceByName;

    @Autowired
    List<MyServiceFactory> myServiceFactories;

    @Test
    public void testAutowiredBean() {
        assertThat( myService ).succeedsWithin( 1, TimeUnit.SECONDS )
                .isInstanceOf( MyService.class );
        assertThat( myService2 ).succeedsWithin( 1, TimeUnit.SECONDS )
                .isInstanceOf( MyService2.class );
    }

    @Test
    @Ignore("Injecting a list of parametrized beans is not supported by Spring 3 (see issue https://github.com/PavlidisLab/Gemma/issues/612)")
    public void testAutowiredList() {
        assertThat( myServiceList ).containsExactly( myService );
    }

    @Test
    @Ignore("Injecting a map of parametrized beans is not supported by Spring 3 (see issue https://github.com/PavlidisLab/Gemma/issues/612)")
    public void testAutowiredMap() {
        assertThat( myServiceByName ).containsExactlyEntriesOf( Collections.singletonMap( "myService", myService ) );
    }

    @Test
    public void testAutowiredListOfFactories() {
        assertThat( myServiceFactories ).hasSize( 1 ).allSatisfy( f -> {
            assertThat( f.isInitialized() ).isTrue();
            assertThat( f.getObject() ).isSameAs( myService );
        } );
    }

    @Test
    public void testInjectViaBeanFactory() {
        assertThat( beanFactory.getBean( MyServiceFactory.class ).getObject() ).succeedsWithin( 1, TimeUnit.SECONDS );
    }

    @Test
    public void testInjectViaServiceFactory() {
        assertThat( myServiceFactory.getObject() ).succeedsWithin( 1, TimeUnit.SECONDS );
    }

    @Test(expected = NoSuchBeanDefinitionException.class)
    public void testInjectBeanDirectly() {
        beanFactory.getBean( MyService.class );
    }

}
