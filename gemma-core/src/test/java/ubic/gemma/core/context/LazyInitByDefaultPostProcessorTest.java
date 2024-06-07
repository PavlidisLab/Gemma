package ubic.gemma.core.context;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LazyInitByDefaultPostProcessorTest {

    private static final AtomicBoolean contextRefreshed = new AtomicBoolean( false );

    @Configuration
    @TestComponent
    static class Context {

        @Bean
        public static LazyInitByDefaultPostProcessor lazyInitByDefaultPostProcessor() {
            assertThat( contextRefreshed ).isFalse();
            return new LazyInitByDefaultPostProcessor();
        }

        @Bean
        public Object bean1() {
            assertThat( contextRefreshed ).isTrue();
            return new Object();
        }

        @Bean
        @Lazy
        public Object bean2() {
            assertThat( contextRefreshed ).isTrue();
            return new Object();
        }

        @Bean
        @Lazy(false)
        public Object bean3() {
            assertThat( contextRefreshed ).isFalse();
            return new Object();
        }
    }

    @Lazy
    @Component("bean4")
    @TestComponent
    static class Bean4 implements InitializingBean {

        @Override
        public void afterPropertiesSet() throws Exception {
            assertThat( contextRefreshed ).isTrue();
        }
    }

    @Lazy(false)
    @Component("bean5")
    @TestComponent
    static class Bean5 implements InitializingBean {

        @Override
        public void afterPropertiesSet() throws Exception {
            assertThat( contextRefreshed ).isFalse();
        }
    }

    @Component("bean6")
    @TestComponent
    static class Bean6 implements InitializingBean {

        @Override
        public void afterPropertiesSet() throws Exception {
            assertThat( contextRefreshed ).isTrue();
        }
    }

    @Before
    public void setUp() {
        contextRefreshed.set( false );
    }

    @Test
    public void test() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext( Context.class, Bean4.class, Bean5.class, Bean6.class );
        contextRefreshed.set( true );
        ctx.getBean( "bean1" );
        ctx.getBean( "bean2" );
        ctx.getBean( "bean3" );
        ctx.getBean( "bean4" );
        ctx.getBean( "bean5" );
        ctx.getBean( "bean6" );
    }
}