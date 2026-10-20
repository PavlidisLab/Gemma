package ubic.gemma.core.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class StreamDrainerTest {

    @Test
    void keepsAShortStreamWhole() throws InterruptedException {
        StreamDrainer drainer = StreamDrainer.start( stream( "an error\n" ), "test" );
        assertThat( drainer.await( 10, TimeUnit.SECONDS ) ).isEqualTo( "an error\n" );
    }

    @Test
    void keepsTheEndOfALongStream() throws InterruptedException {
        StreamDrainer drainer = StreamDrainer.start( stream( "a".repeat( 200_000 ) + "the actual error" ), "test", 1000 );
        String text = drainer.await( 10, TimeUnit.SECONDS );
        assertThat( text )
                .startsWith( "[earlier output omitted]\n" )
                .endsWith( "the actual error" )
                .hasSize( "[earlier output omitted]\n".length() + 1000 );
    }

    private static ByteArrayInputStream stream( String s ) {
        return new ByteArrayInputStream( s.getBytes( StandardCharsets.UTF_8 ) );
    }
}
