package ubic.gemma.core.datastructure.matrix.io;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

public class MexMatrixBundlerTest {

    @Test
    public void test() throws IOException, URISyntaxException {
        Path p = Paths.get( requireNonNull( getClass().getResource( "/ubic/gemma/core/datastructure/matrix/io/data.mex" ) ).toURI() );
        MexMatrixBundler bundler = new MexMatrixBundler();
        assertEquals( 7168, bundler.calculateSize( p ) );
        try ( ByteArrayOutputStream stream = new ByteArrayOutputStream() ) {
            bundler.bundle( p, stream );
            assertEquals( 7168, stream.size() );
        }
    }
}