package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;

import javax.annotation.WillClose;

import static hdf.hdf5lib.H5.*;

/**
 * Represents an HDF5 attribute.
 * @author poirigui
 */
public class H5Attribute implements AutoCloseable {

    static H5Attribute open( long locId, String name ) {
        long attrId = H5Aopen( locId, name, HDF5Constants.H5P_DEFAULT );
        return new H5Attribute( attrId );
    }

    static H5Attribute open( long locId, String path, String name ) {
        long attrId = H5Aopen_by_name( locId, path, name, HDF5Constants.H5P_DEFAULT, HDF5Constants.H5P_DEFAULT );
        return new H5Attribute( attrId );
    }

    private final long attrId;

    private H5Attribute( long attrId ) {
        this.attrId = attrId;
    }

    @WillClose
    public int[] toIntegerVector() {
        int[] vec = new int[( int ) size()];
        H5Aread_int( attrId, HDF5Constants.H5T_NATIVE_INT32, vec );
        close();
        return vec;
    }

    @WillClose
    public String[] toStringVector() {
        String[] buf = new String[( int ) size()];
        H5AreadVL( attrId, H5Type.STRING, buf );
        close();
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
