package ubic.gemma.core.loader.util.hdf5;

import org.junit.Test;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

public class H5FileTest {

    @Test
    public void testOpenFileThatDoesNotExist() {
        Path p = Paths.get( "some-random-file.hdf5" );
        assumeThat( p ).doesNotExist();
        //noinspection resource
        assertThatThrownBy( () -> H5File.open( p ) )
                .isInstanceOf( FileNotFoundException.class );
    }
}