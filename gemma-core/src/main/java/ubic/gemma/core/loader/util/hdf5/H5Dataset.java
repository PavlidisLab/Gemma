package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import org.springframework.util.Assert;

import javax.annotation.WillClose;
import java.util.Optional;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

/**
 * Represents an HDF5 dataset.
 * @author poirigui
 */
public class H5Dataset implements AutoCloseable {

    static H5Dataset open( long locId, String path ) {
        return new H5Dataset( locId, H5Dopen( locId, path, H5P_DEFAULT ) );
    }

    private final long locId;
    private final long datasetId;

    private H5Dataset( long locId, long datasetId ) {
        this.locId = locId;
        this.datasetId = datasetId;
    }

    public H5Type getType() {
        return new H5Type( H5Dget_type( datasetId ) );
    }

    public Optional<H5Attribute> getAttribute( String name ) {
        if ( !H5Aexists( locId, name ) ) {
            return Optional.empty();
        }
        return Optional.of( H5Attribute.open( locId, name ) );
    }

    /**
     * Obtain a 1D slice of the dataset.
     */
    public H5Dataspace slice( long start, long end ) {
        Assert.isTrue( start >= 0 && end <= size() && start < end, "Invalid slice: [" + start + ", " + end + "[" );
        long diskSpaceId = H5Dget_space( datasetId );
        H5Sselect_hyperslab( diskSpaceId, HDF5Constants.H5S_SELECT_SET, new long[] { start }, null, new long[] { end - start }, null );
        return new H5Dataspace( diskSpaceId );
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
        H5Dread_VLStrings( datasetId, H5Type.STRING, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
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
     * Represents a dataspace selection of a {@link H5Dataset}.
     * <p>
     * When collecting the space (i.e. using {@link #toByteVector(long)}}), the underlying HDF5 resource is closed
     * automatically.
     */
    public class H5Dataspace implements AutoCloseable {

        private final long diskSpaceId;
        private final long memSpaceId;

        private H5Dataspace( long diskSpaceId ) {
            this.diskSpaceId = diskSpaceId;
            this.memSpaceId = H5Screate_simple( 1, new long[] { H5Sget_select_npoints( this.diskSpaceId ) }, null );
        }

        @WillClose
        public byte[] toByteVector( long scalarType ) {
            try {
                byte[] buf = new byte[( int ) H5Sget_select_npoints( memSpaceId ) * ( int ) H5Tget_size( scalarType )];
                H5Dread( datasetId, scalarType, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            } finally {
                close();
            }
        }

        @WillClose
        public int[] toIntegerVector() {
            try {
                int[] buf = new int[( int ) H5Sget_select_npoints( memSpaceId )];
                H5Dread_int( datasetId, HDF5Constants.H5T_NATIVE_INT, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            } finally {
                close();
            }
        }

        @Override
        public void close() {
            H5Sclose( memSpaceId );
            H5Sclose( diskSpaceId );
        }
    }
}
