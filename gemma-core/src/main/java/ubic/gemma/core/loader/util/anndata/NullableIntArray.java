package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;

public class NullableIntArray extends NullableArray<Integer> {

    public NullableIntArray( H5Group group ) {
        super( group, "nullable-integer" );
    }

    @Override
    @Nullable
    public Integer get( int i ) {
        int v = values.slice( i, i + 1 ).toIntegerVector()[0];
        boolean m = mask.slice( i, i + 1 ).toBooleanVector()[0];
        return m ? v : null;
    }

    public Integer[] toIntegerArray() {
        int[] v = values.toIntegerVector();
        boolean[] m = mask.toBooleanVector();
        Integer[] result = new Integer[v.length];
        for ( int i = 0; i < v.length; i++ ) {
            result[i] = m[i] ? v[i] : null;
        }
        return result;
    }
}
