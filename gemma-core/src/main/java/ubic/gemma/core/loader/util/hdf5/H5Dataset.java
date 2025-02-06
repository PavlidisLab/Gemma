package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import org.springframework.util.Assert;

import javax.annotation.Nullable;
import javax.annotation.WillClose;
import java.util.Optional;

import static hdf.hdf5lib.H5.*;
import static hdf.hdf5lib.HDF5Constants.H5I_INVALID_HID;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

/**
 * Represents an HDF5 dataset.
 * @author poirigui
 */
public class H5Dataset implements AutoCloseable {

    static H5Dataset open( long locId, String path ) {
        return new H5Dataset( H5Dopen( locId, path, H5P_DEFAULT ) );
    }

    private final long datasetId;

    private H5Dataset( long datasetId ) {
        Assert.isTrue( datasetId != H5I_INVALID_HID );
        this.datasetId = datasetId;
    }

    public H5Type getType() {
        return new H5Type( H5Dget_type( datasetId ) );
    }

    public Optional<H5Attribute> getAttribute( String name ) {
        if ( !H5Aexists( datasetId, name ) ) {
            return Optional.empty();
        }
        return Optional.of( H5Attribute.open( datasetId, name ) );
    }

    @Nullable
    public String getStringAttribute( String name ) {
        return getAttribute( name )
                .map( H5Attribute::toStringVector )
                .map( s -> s[0] )
                .orElse( null );
    }

    public boolean hasAttribute( String name ) {
        return H5Aexists( datasetId, name );
    }

    /**
     * Obtain a single double value.
     */
    public double getDouble( long i ) {
        try ( H5Dataspace d = slice( i, i + 1 ) ) {
            return d.toDoubleVector()[0];
        }
    }

    /**
     * Obtain a single integer value.
     */
    public int getInteger( long i ) {
        try ( H5Dataspace d = slice( i, i + 1 ) ) {
            return d.toIntegerVector()[0];
        }
    }

    /**
     * Obtain a single boolean value.
     */
    public boolean getBoolean( long i ) {
        try ( H5Dataspace d = slice( i, i + 1 ) ) {
            return d.toBooleanVector()[0];
        }
    }

    /**
     * Obtain a 1D slice of the dataset.
     */
    public H5Dataspace slice( long start, long end ) {
        Assert.isTrue( start >= 0 && end <= size() && start <= end, "Invalid slice: [" + start + ", " + end + "[" );
        long diskSpaceId = H5Dget_space( datasetId );
        H5Sselect_hyperslab( diskSpaceId, HDF5Constants.H5S_SELECT_SET, new long[] { start }, null, new long[] { end - start }, null );
        return new H5Dataspace( diskSpaceId );
    }

    @WillClose
    public boolean[] toBooleanVector() {
        try {
            int[] buf = new int[( int ) size()];
            H5Dread( datasetId, HDF5Constants.H5T_NATIVE_HBOOL, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
            boolean[] bools = new boolean[( int ) size()];
            for ( int i = 0; i < buf.length; i++ ) {
                bools[i] = buf[i] != 0;
            }
            return bools;
        } finally {
            close();
        }
    }

    @WillClose
    public int[] toIntegerVector() {
        try {
            int[] buf = new int[( int ) size()];
            H5Dread( datasetId, HDF5Constants.H5T_NATIVE_INT32, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
            return buf;
        } finally {
            close();
        }
    }

    @WillClose
    public double[] toDoubleVector() {
        try {
            double[] buf = new double[( int ) size()];
            H5Dread( datasetId, HDF5Constants.H5T_NATIVE_DOUBLE, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
            return buf;
        } finally {
            close();
        }
    }

    @WillClose
    public String[] toStringVector() {
        try {
            if ( getType().getFundamentalType() == H5FundamentalType.ENUM ) {
                return getType().getMemberNames();
            } else {
                String[] buf = new String[( int ) size()];
                H5Dread_VLStrings( datasetId, H5Type.STRING, HDF5Constants.H5S_ALL, HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            }
        } finally {
            close();
        }
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

    public long[] getShape() {
        long spaceId = H5Dget_space( datasetId );
        try {
            int dims = H5Sget_simple_extent_ndims( spaceId );
            long[] result = new long[dims];
            H5Sget_simple_extent_dims( spaceId, result, null );
            return result;
        } finally {
            H5Sclose( spaceId );
        }
    }

    /**
     * Represents a dataspace selection of a {@link H5Dataset}.
     * <p>
     * When collecting the space (i.e. using {@link #toByteVector(long)}}), the underlying HDF5 resource is closed
     * automatically.
     */
    public class H5Dataspace implements AutoCloseable {

        private final long diskSpaceId;

        private H5Dataspace( long diskSpaceId ) {
            this.diskSpaceId = diskSpaceId;
        }

        /**
         * Write the dataspace into a slice of a target.
         * <p>
         * The start and end are expressed in terms of number of datapoints. The slice must contain exactly {@link #size()}
         * datapoints to be valid.
         * <p>
         * The dataspace is closed once the operation is completed and should no-longer be used.
         * @param buf        a target buffer of size {@code (end - start) * sizeof(scalarType)}
         * @param start      start of a slice. inclusive
         * @param end        end of a slice, exclusive
         * @param scalarType a datatype to write into the buffer
         */
        @WillClose
        public void toByteVector( byte[] buf, int start, int end, long scalarType ) {
            Assert.isTrue( start >= 0 && start < end, "Invalid slice [" + start + ", " + end + "[." );
            long sourceSize = size();
            long targetSize = end - start;
            long scalarSize = H5Tget_size( scalarType );
            Assert.isTrue( sourceSize == targetSize, "The target slice should accommodate exactly " + sourceSize + " scalars, its size " + sourceSize );
            Assert.isTrue( end * scalarSize <= buf.length, "The target slice should fit in the buffer." );
            Assert.isTrue( buf.length % scalarSize == 0, "The size of the targe tbuffer must be a multiple of " + scalarSize + "." );
            long memSpaceId = H5Screate_simple( 1, new long[] { buf.length / scalarSize }, null );
            H5Sselect_hyperslab( memSpaceId, HDF5Constants.H5S_SELECT_SET, new long[] { start }, null, new long[] { end - start }, null );
            try {
                H5Dread( datasetId, scalarType, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
            } finally {
                H5Sclose( memSpaceId );
                close();
            }
        }

        @WillClose
        public byte[] toByteVector( long scalarType ) {
            long memSpaceId = H5Screate_simple( 1, new long[] { size() }, null );
            try {
                byte[] buf = new byte[( int ) size() * ( int ) H5Tget_size( scalarType )];
                H5Dread( datasetId, scalarType, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            } finally {
                H5Sclose( memSpaceId );
                close();
            }
        }

        public boolean[] toBooleanVector() {
            long memSpaceId = H5Screate_simple( 1, new long[] { size() }, null );
            try {
                int[] buf = new int[( int ) size()];
                H5Dread_int( datasetId, HDF5Constants.H5T_NATIVE_HBOOL, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                boolean[] bools = new boolean[( int ) size()];
                for ( int i = 0; i < buf.length; i++ ) {
                    bools[i] = buf[i] != 0;
                }
                return bools;
            } finally {
                H5Sclose( memSpaceId );
                close();
            }
        }

        @WillClose
        public int[] toIntegerVector() {
            long memSpaceId = H5Screate_simple( 1, new long[] { size() }, null );
            try {
                int[] buf = new int[( int ) size()];
                H5Dread_int( datasetId, HDF5Constants.H5T_NATIVE_INT32, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            } finally {
                H5Sclose( memSpaceId );
                close();
            }
        }

        @WillClose
        public double[] toDoubleVector() {
            long memSpaceId = H5Screate_simple( 1, new long[] { size() }, null );
            try {
                double[] buf = new double[( int ) size()];
                H5Dread_double( datasetId, HDF5Constants.H5T_NATIVE_DOUBLE, memSpaceId, diskSpaceId, HDF5Constants.H5P_DEFAULT, buf );
                return buf;
            } finally {
                H5Sclose( memSpaceId );
                close();
            }
        }

        /**
         * Obtain the size of this dataspace.
         */
        public long size() {
            return H5Sget_select_npoints( diskSpaceId );
        }

        @Override
        public void close() {
            H5Sclose( diskSpaceId );
        }
    }
}
