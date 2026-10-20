package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.cli.util.CLI;

import javax.sql.DataSource;
import java.io.Console;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static ubic.gemma.cli.util.test.Assertions.assertThat;

/**
 * initializeDatabase and updateDatabase must ask for confirmation unless {@code -force} is passed.
 * <p>
 * Both used to check whether {@code -force} was <em>defined</em> rather than passed, which is always true, so neither
 * ever prompted. The data source is a plain mock, so nothing here can reach a database: a run that skips the prompt
 * fails on the mock instead.
 */
public class DatabaseCliConfirmationTest {

    private static final int ABORTED = 3;

    @Test
    public void testInitializeDatabasePromptsWithoutForce() throws Exception {
        assertPromptsWithoutForce( new InitializeDatabaseCli() );
    }

    @Test
    public void testInitializeDatabaseDoesNotPromptWithForce() throws Exception {
        assertDoesNotPromptWithForce( new InitializeDatabaseCli() );
    }

    @Test
    public void testUpdateDatabasePromptsWithoutForce() throws Exception {
        assertPromptsWithoutForce( new UpdateDatabaseCli() );
    }

    @Test
    public void testUpdateDatabaseDoesNotPromptWithForce() throws Exception {
        assertDoesNotPromptWithForce( new UpdateDatabaseCli() );
    }

    private void assertPromptsWithoutForce( CLI cli ) throws Exception {
        DataSource dataSource = mock();
        ReflectionTestUtils.setField( cli, "dataSource", dataSource );
        Console console = mock();
        when( console.readLine( anyString(), any() ) ).thenReturn( "no" );
        assertThat( cli )
                .withConsole( console )
                .failsWith( ABORTED );
        verify( console ).readLine( anyString(), any() );
        verify( dataSource, never() ).getConnection();
    }

    private void assertDoesNotPromptWithForce( CLI cli ) {
        DataSource dataSource = mock();
        ReflectionTestUtils.setField( cli, "dataSource", dataSource );
        Console console = mock();
        assertThat( cli )
                .withConsole( console )
                .withArguments( "-force" )
                // the mock data source cannot be initialized or updated
                .fails();
        verify( console, never() ).readLine( anyString(), any() );
    }
}
