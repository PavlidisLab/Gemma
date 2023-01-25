package ubic.gemma.core.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import ubic.gemma.core.util.test.BaseCliTest;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;

import static org.mockito.Mockito.*;
import static ubic.gemma.core.util.test.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class AbstractTransactionalCLITest extends BaseCliTest {

    @Configuration
    @TestComponent
    static class AbstractTransactionalCLITestContextConfiguration extends BaseCliTestContextConfiguration {

        @Bean
        public WorkingCLI workingCLI() {
            return new WorkingCLI();
        }

        @Bean
        public FailingCLI failingCLI() {
            return new FailingCLI();
        }

        @Bean
        public PlatformTransactionManager platformTransactionManager() {
            return mock( PlatformTransactionManager.class );
        }
    }

    static abstract class MockedTransactionalCLI extends AbstractTransactionalCLI {

        @Override
        protected void buildOptions( Options options ) {

        }

        @Override
        protected void processOptions( CommandLine commandLine ) throws ParseException {

        }

        @Override
        protected void doWork() throws Exception {
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
            return CLI.CommandGroup.MISC;
        }
    }

    static class WorkingCLI extends MockedTransactionalCLI {

        @Override
        protected void doWork() {
        }
    }

    public static class FailingCLI extends MockedTransactionalCLI {

        @Override
        protected void doWork() throws Exception {
            throw new Exception( "" );
        }
    }

    @Autowired
    private WorkingCLI workingCLI;
    @Autowired
    private FailingCLI failingCLI;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @After
    public void tearDown() {
        reset( platformTransactionManager );
    }

    @Test
    public void testRollbackOnFailure() {
        TransactionStatus status = new SimpleTransactionStatus();
        when( platformTransactionManager.getTransaction( any() ) ).thenReturn( status );
        assertThat( workingCLI ).succeeds();
        assertThat( status.isRollbackOnly() ).isFalse();
        verify( platformTransactionManager ).commit( status );
    }

    @Test
    public void test() {
        TransactionStatus status = new SimpleTransactionStatus();
        when( platformTransactionManager.getTransaction( any() ) ).thenReturn( status );
        assertThat( failingCLI ).fails();
        assertThat( status.isRollbackOnly() ).isTrue();
        // the rollback is actually done in the PTM's commit logic by checking isRollbackOnly()
        verify( platformTransactionManager ).commit( status );
    }
}