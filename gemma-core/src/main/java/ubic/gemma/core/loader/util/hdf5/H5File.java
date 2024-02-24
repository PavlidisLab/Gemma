package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5FileInterfaceException;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

public class H5File extends H5Location implements AutoCloseable {

    /**
     * Open an HDF5 file.
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException           for any other file-related errors
     */
    public static H5File open( Path path ) throws IOException {
        try {
            return new H5File( H5Fopen( path.toString(), H5F_ACC_RDONLY, H5P_DEFAULT ) );
        } catch ( HDF5FileInterfaceException e ) {
            if ( e.getMinorErrorNumber() == HDF5Constants.H5E_CANTOPENFILE ) {
                throw new FileNotFoundException( e.getMessage() );
            } else {
                throw new IOException( e.getMessage() );
            }
        }
    }

    private final long fd;

    private H5File( long fd ) {
        super( fd );
        this.fd = fd;
    }

    public Path getPath() {
        return Paths.get( H5Fget_name( fd ) );
    }

    @Override
    public void close() throws HDF5LibraryException {
        H5Fclose( fd );
    }
}
