package ubic.gemma.core.util.test;

import org.assertj.core.api.AbstractAssert;
import ubic.gemma.core.util.CLI;

public class CliAssert extends AbstractAssert<CliAssert, CLI> {

    private String[] command;

    public CliAssert( CLI cli ) {
        super( cli, CliAssert.class );
    }

    public CliAssert withCommand( String... command ) {
        this.command = command;
        return myself;
    }

    public CliAssert succeeds() {
        this.objects.assertEqual( info, actual.executeCommand( command ), 0 );
        return myself;
    }
}
