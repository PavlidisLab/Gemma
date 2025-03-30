package ubic.gemma.cli.util.test;

import ubic.gemma.cli.util.CLI;

public class Assertions {

    public static CliAssert assertThat( CLI cli ) {
        return new CliAssert( cli );
    }
}
