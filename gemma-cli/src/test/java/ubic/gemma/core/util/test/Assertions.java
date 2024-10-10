package ubic.gemma.core.util.test;

import ubic.gemma.core.util.CLI;

public class Assertions {

    public static CliAssert assertThat( CLI cli ) {
        return new CliAssert( cli );
    }
}
