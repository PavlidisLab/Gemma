package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;
import hdf.hdf5lib.callbacks.H5A_iterate_t;
import hdf.hdf5lib.callbacks.H5L_iterate_opdata_t;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static hdf.hdf5lib.H5.*;

/**
 * Represents a location which is either a {@link H5File} or a {@link H5Group}.
 */
public abstract class H5Location {

    private final long locId;

    protected H5Location( long locId ) {
        this.locId = locId;
    }

    public List<String> getAttributes() {
        List<String> attributes = new ArrayList<>();
        H5Aiterate( locId, HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_NATIVE, 0, ( loc_id, name, info, op_data ) -> {
            attributes.add( name );
            return 0;
        }, new H5A_iterate_t() {
        } );
        return attributes;
    }

    public List<String> getAttributes( String path ) {
        List<String> attributes = new ArrayList<>();
        H5Aiterate_by_name( locId, path, HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_NATIVE, 0, ( loc_id, name, info, op_data ) -> {
            attributes.add( name );
            return 0;
        }, new H5A_iterate_t() {
        }, HDF5Constants.H5P_DEFAULT );
        return attributes;
    }

    public Optional<H5Attribute> getAttribute( String name ) {
        if ( !H5Aexists( locId, name ) ) {
            return Optional.empty();
        }
        return Optional.of( H5Attribute.open( locId, name ) );
    }

    public Optional<H5Attribute> getAttribute( String path, String name ) {
        if ( !H5Aexists_by_name( locId, path, name, HDF5Constants.H5P_DEFAULT ) ) {
            return Optional.empty();
        }
        return Optional.of( H5Attribute.open( locId, path, name ) );
    }

    @Nullable
    public String getStringAttribute( String name ) {
        return getAttribute( name )
                .map( H5Attribute::toStringVector )
                .map( s -> s[0] )
                .orElse( null );
    }

    /**
     * Obtain an attribute for a given path.
     */
    @Nullable
    public String getStringAttribute( String path, String name ) {
        return getAttribute( path, name )
                .map( H5Attribute::toStringVector )
                .map( s -> s[0] )
                .orElse( null );
    }

    /**
     * Check if an attribute exists.
     */
    public boolean hasAttribute( String name ) {
        return H5Aexists( locId, name );
    }

    /**
     * Check if an attribute for a given path exists.
     */
    public boolean hasAttribute( String path, String name ) {
        return H5Aexists_by_name( locId, path, name, HDF5Constants.H5P_DEFAULT );
    }

    /**
     * Check if a given path exists relative to this location.
     */
    public boolean exists( String path ) {
        return H5Lexists( locId, path, HDF5Constants.H5P_DEFAULT );
    }

    /**
     * Obtain the children of the given H5 location.
     */
    public List<String> getChildren() {
        List<String> paths = new ArrayList<>();
        H5Literate( locId, HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_NATIVE, 0, ( loc_id, name, info, op_data ) -> {
            paths.add( name );
            return 0;
        }, new H5L_iterate_opdata_t() {
        } );
        return paths;
    }

    public List<String> getChildren( String path ) {
        List<String> paths = new ArrayList<>();
        H5Literate_by_name( locId, path, HDF5Constants.H5_INDEX_NAME, HDF5Constants.H5_ITER_NATIVE, 0, ( loc_id, name, info, op_data ) -> {
            paths.add( name );
            return 0;
        }, new H5L_iterate_opdata_t() {
        }, HDF5Constants.H5P_DEFAULT );
        return paths;
    }

    public H5Group getGroup( String path ) {
        return H5Group.open( locId, path );
    }

    public H5Dataset getDataset( String path ) {
        return H5Dataset.open( locId, path );
    }
}
