package ubic.gemma.core.logging;

import lombok.extern.apachecommons.CommonsLog;
import org.junit.Test;

@CommonsLog
public class LoggingTest {

    @Test
    public void test() {
        log.info( "test" );
        log.warn( "test" );
        log.error( "test" );
    }
}
