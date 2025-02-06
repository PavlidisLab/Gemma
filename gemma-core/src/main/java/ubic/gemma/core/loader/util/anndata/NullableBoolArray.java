package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;

public class NullableBoolArray extends NullableArray<Boolean> {

    public NullableBoolArray( H5Group group ) {
        super( group, "nullable-boolean" );
    }

    @Override
    @Nullable
    public Boolean get( int i ) {
        return mask.getBoolean( i ) ? values.getBoolean( i ) : null;
    }

    public Boolean[] toBooleanArray() {
        boolean[] v = values.toBooleanVector();
        boolean[] m = mask.toBooleanVector();
        Boolean[] result = new Boolean[v.length];
        for ( int i = 0; i < v.length; i++ ) {
            result[i] = m[i] ? v[i] : null;
        }
        return result;
    }
}
