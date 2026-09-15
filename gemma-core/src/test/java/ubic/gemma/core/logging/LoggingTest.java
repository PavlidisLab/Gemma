package ubic.gemma.core.logging;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class LoggingTest {

    @Test
    public void test() {
        log.info( "test" );
        log.warn( "test" );
        log.error( "test" );
    }
}
