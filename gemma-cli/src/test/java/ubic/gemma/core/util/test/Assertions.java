package ubic.gemma.core.util.test;

import ubic.gemma.core.util.CLI;

/**
 * Extended assertions for AssertJ.
 * @author poirigui
 */
public class Assertions extends org.assertj.core.api.Assertions {

    public static CliAssert<?> assertThat( CLI cli ) {
        return new CliAssert<>( cli );
    }
}
