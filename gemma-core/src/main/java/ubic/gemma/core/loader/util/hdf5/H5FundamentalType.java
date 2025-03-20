package ubic.gemma.core.loader.util.hdf5;

import hdf.hdf5lib.HDF5Constants;

/**
 * Represents all the fundamental H5 types from which all other types are derived.
 * @author poirigui
 */
public enum H5FundamentalType {

    INTEGER( HDF5Constants.H5T_INTEGER ),
    FLOAT( HDF5Constants.H5T_FLOAT ),
    STRING( HDF5Constants.H5T_STRING ),
    BITFIELD( HDF5Constants.H5T_BITFIELD ),
    OPAQUE( HDF5Constants.H5T_OPAQUE ),
    COMPOUND( HDF5Constants.H5T_COMPOUND ),
    REFERENCE( HDF5Constants.H5T_REFERENCE ),
    ENUM( HDF5Constants.H5T_ENUM ),
    VLEN( HDF5Constants.H5T_VLEN ),
    ARRAY( HDF5Constants.H5T_ARRAY );

    private final int typeId;

    H5FundamentalType( int typeId ) {
        this.typeId = typeId;
    }

    public static H5FundamentalType valueOf( int typeId ) {
        for ( H5FundamentalType value : values() ) {
            if ( value.typeId == typeId ) {
                return value;
            }
        }
        throw new IllegalArgumentException( "No such fundamental type: " + typeId );
    }
}
