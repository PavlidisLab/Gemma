package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;

import static hdf.hdf5lib.H5.*;

/**
 * Represents an HDF5 attribute.
 * @author poirigui
 */
public class H5Attribute implements AutoCloseable {

    /**
     * Represents a UTF-8 variable-length string.
     */
    private static final long UTF8_VARIABLE_STRING;

    static {
        long type = H5Tcopy( HDF5Constants.H5T_C_S1 );
        H5Tset_size( type, HDF5Constants.H5T_VARIABLE );
        H5Tset_cset( type, HDF5Constants.H5T_CSET_UTF8 );
        H5Tset_strpad( type, HDF5Constants.H5T_STR_NULLPAD );
        UTF8_VARIABLE_STRING = type;
    }

    static H5Attribute open( long locId, String name ) {
        long attrId = H5Aopen( locId, name, HDF5Constants.H5P_DEFAULT );
        return new H5Attribute( attrId );
    }

    public static H5Attribute open( long locId, String path, String name ) {
        long attrId = H5Aopen_by_name( locId, path, name, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT );
        return new H5Attribute( attrId );
    }

    private final long attrId;

    private H5Attribute( long attrId ) {
        this.attrId = attrId;
    }

    public int[] toIntegerVector() {
        int[] vec = new int[( int ) size()];
        H5Aread_int( attrId, HDF5Constants.H5T_NATIVE_INT32, vec );
        return vec;
    }

    public String[] toStringVector() {
        String[] buf = new String[( int ) size()];
        H5Aread_VLStrings( attrId, UTF8_VARIABLE_STRING, buf );
        return buf;
    }

    public long size() {
        long spaceId = H5Aget_space( attrId );
        try {
            return H5Sget_select_npoints( spaceId );
        } finally {
            H5Sclose( spaceId );
        }
    }

    @Override
    public void close() {
        H5Aclose( attrId );
    }
}
