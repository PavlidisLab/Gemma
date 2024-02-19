package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.nio.file.Path;

import static hdf.hdf5lib.H5.H5Fclose;
import static hdf.hdf5lib.H5.H5Fopen;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

public class H5File extends H5Location implements AutoCloseable {

    public static H5File open( Path path ) {
        return new H5File( H5Fopen( path.toString(), H5F_ACC_RDONLY, H5P_DEFAULT ) );
    }

    private final long fd;

    private H5File( long fd ) {
        super( fd );
        this.fd = fd;
    }

    @Override
    public void close() throws HDF5LibraryException {
        H5Fclose( fd );
    }
}
