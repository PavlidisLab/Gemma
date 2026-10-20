package ubic.gemma.core.loader.util.hdf5;

import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class H5UtilsTest {

    @Test
    public void testGetVersion() {
        assertNotNull( H5Utils.getH5Version() );
    }
}