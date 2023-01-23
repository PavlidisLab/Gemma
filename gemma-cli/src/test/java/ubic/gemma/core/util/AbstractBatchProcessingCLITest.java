package ubic.gemma.core.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;

import static ubic.gemma.core.util.test.Assertions.assertThat;

@ContextConfiguration
public class AbstractBatchProcessingCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class AbstractBatchProcessingCLITestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        @Scope(BeanDefinition.SCOPE_PROTOTYPE)
        public DelegatingBatchProcessingCLI delegatingBatchProcessingCLI() {
            return new DelegatingBatchProcessingCLI();
        }

    }

    static class DelegatingBatchProcessingCLI extends AbstractBatchProcessingCLI {


        private Runnable delegate;

        public void setDelegate( Runnable delegate ) {
            this.delegate = delegate;
        }

        @Override
        protected void buildBatchOptions( Options options ) {
        }

        @Override
        protected void processBatchOptions( CommandLine commandLine ) {
        }

        @Override
        protected void doBatchWork() {
            delegate.run();
        }

        @Override
        public String getCommandName() {
            return "test";
        }

        @Nullable
        @Override
        public String getShortDesc() {
            return null;
        }

        @Override
        public CLI.CommandGroup getCommandGroup() {
            return CommandGroup.MISC;
        }
    }

    @Autowired
    private DelegatingBatchProcessingCLI batchProcessingCLI;

    @Test
    public void test() throws Exception {
        batchProcessingCLI.setDelegate( () -> {
        } );
        assertThat( batchProcessingCLI ).succeeds();
        assertThat( batchProcessingCLI.getLastException() ).isNull();
    }

    @Test
    public void testFailing() {
        batchProcessingCLI.setDelegate( () -> {
            batchProcessingCLI.getExecutorService().submit( () -> {
                batchProcessingCLI.addErrorObject( "test", "test" );
                try {
                    Thread.sleep( 100 );
                } catch ( InterruptedException e ) {
                    throw new RuntimeException( e );
                }
            } );
        } );
        assertThat( batchProcessingCLI ).fails();
        assertThat( batchProcessingCLI.getLastException() )
                .isNotNull()
                .isInstanceOf( AbstractBatchProcessingCLI.BatchProcessingFailureException.class );
    }
}