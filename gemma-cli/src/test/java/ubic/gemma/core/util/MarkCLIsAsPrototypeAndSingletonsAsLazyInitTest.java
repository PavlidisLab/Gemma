package ubic.gemma.core.util;

import org.apache.commons.cli.Options;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.apps.GemmaCLI;
import ubic.gemma.core.apps.TestCli;
import ubic.gemma.core.util.test.BaseCLIIntegrationTest;

import javax.annotation.Nullable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.*;

/**
 * Test various behaviours of CLIs when injected as bean.
 * @author poirigui
 */
public class MarkCLIsAsPrototypeAndSingletonsAsLazyInitTest extends BaseCLIIntegrationTest {

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
    public void testCliBeanWithComponentIsPrototype() {
        TestCliAsComponent bean3 = beanFactory.getBean( TestCliAsComponent.class );
        TestCliAsComponent bean4 = beanFactory.getBean( TestCliAsComponent.class );
        assertNotSame( bean3, bean4 );
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

    @Component
    static class TestCliAsComponent implements CLI {

        @Nullable
        @Override
        public String getCommandName() {
            return "";
        }

        @Nullable
        @Override
        public String getShortDesc() {
            return "";
        }

        @Override
        public GemmaCLI.CommandGroup getCommandGroup() {
            return null;
        }

        @Override
        public Options getOptions() {
            return null;
        }

        @Override
        public boolean allowPositionalArguments() {
            return false;
        }

        @Override
        public int executeCommand( String... args ) {
            return 0;
        }
    }
}
