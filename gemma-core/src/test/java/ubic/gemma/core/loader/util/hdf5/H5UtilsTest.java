package ubic.gemma.core.loader.util.hdf5;

import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertNotNull;

public class H5UtilsTest {

    @Test
    public void testGetVersion() {
        assertNotNull( H5Utils.getH5Version() );
    }
}