package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.exceptions.HDF5Exception;
import hdf.hdf5lib.exceptions.HDF5FileInterfaceException;
import hdf.hdf5lib.exceptions.HDF5LibraryException;

import java.io.FileNotFoundException;
import java.io.IOException;

import static hdf.hdf5lib.H5.H5get_libversion;

public class H5Utils {

    /**
     * Obtain the version of the HDF5 library.
     */
    public static String getH5Version() {
        int[] v = new int[3];
        H5get_libversion( v );
        return String.format( "%d.%d.%d", v[0], v[1], v[2] );
    }

    /**
     * Convert an HDF5 exception to an IOException or a {@link H5Exception}.
     */
    public static IOException convertH5Exception( HDF5Exception h5Exception ) throws IOException, H5Exception {
        if ( h5Exception instanceof HDF5FileInterfaceException ) {
            HDF5LibraryException e = ( HDF5LibraryException ) h5Exception;
            if ( e.getMinorErrorNumber() == HDF5Constants.H5E_CANTOPENFILE ) {
                throw new FileNotFoundException( e.getMessage() );
            } else if ( e.getMinorErrorNumber() == HDF5Constants.H5E_TRUNCATED ) {
                throw new TruncatedH5FileException( e.getMessage() );
            } else {
                // TODO: convert other error codes
                throw new IOException( e );
            }
        }
        throw new H5Exception( h5Exception );
    }
}
