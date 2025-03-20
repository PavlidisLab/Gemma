package ubic.gemma.core.loader.util.hdf5;

import static hdf.hdf5lib.H5.H5Gclose;
import static hdf.hdf5lib.H5.H5Gopen;
import static hdf.hdf5lib.HDF5Constants.H5P_DEFAULT;

public class H5Group extends H5Location implements AutoCloseable {

    static H5Group open( long locId, String path ) {
        return new H5Group( H5Gopen( locId, path, H5P_DEFAULT ) );
    }

    private final long groupId;

    private H5Group( long groupId ) {
        super( groupId );
        this.groupId = groupId;
    }

    @Override
    public void close() {
        H5Gclose( groupId );
    }
}
