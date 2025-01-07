package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.exceptions.HDF5Exception;

public class H5Exception extends RuntimeException {

    private final HDF5Exception cause;

    public H5Exception( HDF5Exception cause ) {
        this.cause = cause;
    }

    @Override
    public HDF5Exception getCause() {
        return cause;
    }
}
