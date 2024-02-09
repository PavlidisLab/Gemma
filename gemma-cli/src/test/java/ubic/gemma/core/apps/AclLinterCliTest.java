package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.persistence.util.TestComponent;

import static ubic.gemma.core.util.test.Assertions.assertThat;

@ContextConfiguration
public class AclLinterCliTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class AclLinterCliTestContextConfiguration extends BaseDatabaseTestContextConfiguration {

        @Bean
        public AclLinterCli aclLinterCli() {
            return new AclLinterCli();
        }
    }

    @Autowired
    private AclLinterCli aclLinterCli;

    @Test
    public void test() {
        assertThat( aclLinterCli )
                .withArguments()
                .hasCommandName( "lintAcls" )
                .succeeds();
    }
}