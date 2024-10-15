package ubic.gemma.core.loader.util.hdf5;

import java.io.IOException;

public class TruncatedH5FileException extends IOException {

    public TruncatedH5FileException( String message ) {
        super( message );
    }
}
