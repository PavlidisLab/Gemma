package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.exceptions.HDF5Exception;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5F_ACC_RDONLY;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;
import static ubic.gemma.core.loader.util.hdf5.H5Utils.convertH5Exception;

public class H5File extends H5Location implements AutoCloseable {

    /**
     * Open an HDF5 file.
     * @throws FileNotFoundException    if the file does not exist
     * @throws TruncatedH5FileException if the file is truncated
     * @throws IOException              for any other file-related errors
     */
    public static H5File open( Path path ) throws IOException {
        try {
            return new H5File( H5Fopen( path.toString(), H5F_ACC_RDONLY, H5P_DEFAULT ) );
        } catch ( HDF5Exception e ) {
            throw convertH5Exception( e );
        }
    }

    private final long fileId;

    private H5File( long fileId ) {
        super( fileId );
        this.fileId = fileId;
    }

    public Path getPath() {
        return Paths.get( H5Fget_name( fileId ) );
    }

    @Override
    public void close() throws IOException {
        try {
            H5Fclose( fileId );
        } catch ( HDF5Exception e ) {
            throw convertH5Exception( e );
        }
    }
}
