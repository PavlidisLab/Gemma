package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import org.springframework.util.Assert;

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
        Assert.isTrue( typeId != HDF5Constants.H5I_INVALID_HID );
        this.typeId = typeId;
    }

    public H5FundamentalType getFundamentalType() {
        return H5FundamentalType.valueOf( H5Tget_class( typeId ) );
    }

    public String[] getMemberNames() {
        Assert.isTrue( H5Tget_class( typeId ) == HDF5Constants.H5T_ENUM );
        String[] members = new String[H5Tget_nmembers( typeId )];
        for ( int i = 0; i < members.length; i++ ) {
            members[i] = H5Tget_member_name( typeId, i );
        }
        return members;
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
