package ubic.gemma.core.util.test;

import org.assertj.core.api.AbstractAssert;
import ubic.gemma.core.util.CLI;

/**
 * AssertJ assertions for {@link CLI}.
 */
public class CliAssert extends AbstractAssert<CliAssert, CLI> {

    private String[] args = {};

    public CliAssert( CLI cli ) {
        super( cli, CliAssert.class );
    }

    public CliAssert withArguments( String... args ) {
        this.args = args;
        return myself;
    }

    public CliAssert hasCommandName( String commandName ) {
        objects.assertEqual( info, commandName, actual.getCommandName() );
        return myself;
    }

    public void succeeds() {
        objects.assertEqual( info, 0, actual.executeCommand( args ) );
    }
}
