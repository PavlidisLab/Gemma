package ubic.gemma.cli.util;

import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.apps.TestCli;
import ubic.gemma.cli.util.test.BaseCliIntegrationTest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

/**
 * Test various behaviours of CLIs when injected as bean.
 * @author poirigui
 */
public class MarkCLIsAsPrototypesTest extends BaseCliIntegrationTest {

    @Autowired
    private BeanFactory beanFactory;

    @Test
    public void testCliBeanIsPrototype() {
        assertTrue( beanFactory.isPrototype( "testCli" ) );
        TestCli bean1 = beanFactory.getBean( TestCli.class );
        TestCli bean2 = beanFactory.getBean( TestCli.class );
        assertNotSame( bean1, bean2 );
    }

    @Test
    public void testUnrelatedBeanIsSingleton() {
        UnrelatedBean bean3 = beanFactory.getBean( UnrelatedBean.class );
        UnrelatedBean bean4 = beanFactory.getBean( UnrelatedBean.class );
        assertSame( bean3, bean4 );
    }

    @Test
    public void testSingletonShouldNotBeInitializedEagerly() {
        assertThatThrownBy( () -> beanFactory.getBean( SingletonThatShouldNotBeInitialized.class ) )
                .isInstanceOf( BeanCreationException.class )
                .cause()
                .isInstanceOf( RuntimeException.class )
                .hasMessage( "I should not be initialized." );
    }

    @Component
    static class SingletonThatShouldNotBeInitialized implements InitializingBean {
        @Override
        public void afterPropertiesSet() {
            throw new RuntimeException( "I should not be initialized." );
        }
    }

    @Component
    static class UnrelatedBean {

    }
}
