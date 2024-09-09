package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import org.springframework.util.Assert;

import javax.annotation.WillClose;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5I_INVALID_HID;

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
        Assert.isTrue( attrId >= 0 );
        this.attrId = attrId;
    }

    @WillClose
    public boolean[] toBooleanVector() {
        try {
            int[] vec = new int[( int ) size()];
            H5Aread_int( attrId, HDF5Constants.H5T_NATIVE_HBOOL, vec );
            boolean[] bools = new boolean[( int ) size()];
            for ( int i = 0; i < vec.length; i++ ) {
                bools[i] = vec[i] != 0;
            }
            return bools;
        } finally {
            close();
        }
    }

    @WillClose
    public int[] toIntegerVector() {
        try {
            int[] vec = new int[( int ) size()];
            H5Aread_int( attrId, HDF5Constants.H5T_NATIVE_INT32, vec );
            return vec;
        } finally {
            close();
        }
    }

    @WillClose
    public String[] toStringVector() {
        try {
            String[] buf = new String[( int ) size()];
            H5AreadVL( attrId, H5Type.STRING, buf );
            return buf;
        } finally {
            close();
        }
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
