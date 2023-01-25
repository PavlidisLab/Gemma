package ubic.gemma.core.apps;

import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import ubic.gemma.core.util.CLI;
import ubic.gemma.core.util.test.BaseCliIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CliIntegrationTest extends BaseCliIntegrationTest {

    @Autowired
    private BeanFactory ctx;
    @Autowired
    private List<CLI> clis;

    @Test
    public void test() {
        assertThat( clis ).hasSizeGreaterThan( 10 );
        for ( CLI cli : clis ) {
            assertThat( cli.getCommandName() )
                    .isNotNull()
                    .isNotEmpty()
                    .doesNotContainAnyWhitespaces();
            assertThat( cli.getCommandGroup() )
                    .isNotNull();
        }
    }

    /**
     * With the prototype scope, a new CLI instance is created every time the bean is injected.
     */
    @Test
    public void testScopePrototypeUsed() {
        CLI cli1 = ctx.getBean( ExternalDatabaseUpdaterCli.class );
        CLI cli2 = ctx.getBean( ExternalDatabaseUpdaterCli.class );
        assertThat( cli1 ).isNotSameAs( cli2 );
    }
}
