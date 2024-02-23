package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import org.springframework.util.Assert;

import javax.annotation.Nullable;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

/**
 * Represents an HDF5 dataset.
 * @author poirigui
 */
public class H5Dataset implements AutoCloseable {

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

    static H5Dataset open( long locId, String path ) {
        return new H5Dataset( locId, H5Dopen( locId, path, H5P_DEFAULT ) );
    }

    private final long locId;
    private final long datasetId;

    private H5Dataset( long locId, long datasetId ) {
        this.locId = locId;
        this.datasetId = datasetId;
    }

    @Nullable
    public H5Attribute getAttribute( String name ) {
        if ( !H5Aexists( locId, name ) ) {
            return null;
        }
        return H5Attribute.open( locId, name );
    }

    @Nullable
    public H5Attribute getAttribute( String path, String name ) {
        if ( !H5Aexists_by_name( locId, path, name, HDF5Constants.H5P_DEFAULT ) ) {
            return null;
        }
        return H5Attribute.open( locId, path, name );
    }

    /**
     * Obtain a 1D slice of the dataset.
     */
    public H5Dataspace slice( int start, int end ) {
        Assert.isTrue( start >= 0 && end <= size() && start < end, "Invalid slice" );
        long diskSpaceId = H5Dget_space( datasetId );
        H5Sselect_hyperslab( diskSpaceId, HDF5Constants.H5S_SELECT_SET, new long[] { start }, null, new long[] { end - start }, null );
        H5Dataspace ds = new H5Dataspace( diskSpaceId );
        assert H5Sget_select_npoints( diskSpaceId ) == end - start;
        return ds;
    }

    public byte[] toByteVector( int scalarType ) {
        byte[] buf = new byte[( int ) size() * ( int ) H5Tget_size( scalarType )];
        H5Dread( datasetId, scalarType, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
        return buf;
    }

    public int[] toIntegerVector() {
        int[] buf = new int[( int ) size()];
        H5Dread( datasetId, HDF5Constants.H5T_NATIVE_INT32, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
        return buf;
    }

    public double[] toDoubleVector() {
        double[] buf = new double[( int ) size()];
        H5Dread( datasetId, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
        return buf;
    }

    public String[] toStringVector() {
        String[] buf = new String[( int ) size()];
        H5Dread_VLStrings( datasetId, UTF8_VARIABLE_STRING, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
        return buf;
    }

    public long size() {
        long space = H5Dget_space( datasetId );
        try {
            return H5Sget_select_npoints( space );
        } finally {
            H5Sclose( space );
        }
    }

    @Override
    public void close() {
        H5Dclose( datasetId );
    }

    /**
     * Represents a dataspace of a {@link H5Dataset}.
     */
    public class H5Dataspace implements AutoCloseable {

        private final long diskSpaceId;

        private H5Dataspace( long diskSpaceId ) {
            this.diskSpaceId = diskSpaceId;
        }

        public byte[] toByteVector( long scalarType ) {
            byte[] buf = new byte[( int ) size() * ( int ) H5Tget_size( scalarType )];
            assert buf.length == size() * 8;
            H5Dread( datasetId, scalarType, HDF5Constants.H5S_ALL, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
            return buf;
        }

        public int[] toIntegerVector() {
            System.out.println( "about to check size..." );
            int[] buf = new int[( int ) size()];
            System.out.println( "about to read " + buf.length + " integers..." );
            H5Dread_int( datasetId, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
            return buf;
        }

        public double[] toDoubleVector() {
            double[] buf = new double[( int ) size()];
            H5Dread_double( datasetId, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
            return buf;
        }

        public long size() {
            return H5Sget_select_npoints( diskSpaceId );
        }

        @Override
        public void close() {
            H5Sclose( diskSpaceId );
        }
    }
}
