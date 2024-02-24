package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;

import static hdf.hdf5lib.H5.*;

public class H5Type implements AutoCloseable {

    public static final long IEEE_F64BE = HDF5Constants.H5T_IEEE_F64BE;

    /**
     * Represents a UTF-8 variable-length string.
     */
    public static final long STRING;

    static {
        long type = H5Tcopy( HDF5Constants.H5T_C_S1 );
        H5Tset_size( type, HDF5Constants.H5T_VARIABLE );
        H5Tset_cset( type, HDF5Constants.H5T_CSET_UTF8 );
        H5Tset_strpad( type, HDF5Constants.H5T_STR_NULLPAD );
        STRING = type;
    }

    private final long typeId;

    H5Type( long typeId ) {
        this.typeId = typeId;
    }

    public H5FundamentalType getFundamentalType() {
        return H5FundamentalType.valueOf( H5Tget_class( typeId ) );
    }

    @Override
    public String toString() {
        return getFundamentalType().toString();
    }

    @Override
    public void close() {
        H5Tclose( typeId );
    }
}
