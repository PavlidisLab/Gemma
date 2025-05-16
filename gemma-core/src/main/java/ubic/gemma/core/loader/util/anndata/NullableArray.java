package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;

import static ubic.gemma.core.loader.util.anndata.Utils.checkEncoding;

public abstract class NullableArray<T> implements Array<T>, AutoCloseable {

    protected final H5Dataset values;
    protected final H5Dataset mask;

    public NullableArray( H5Group group, String encodingType ) {
        checkEncoding( group, encodingType );
        this.values = group.getDataset( "values" );
        this.mask = group.getDataset( "mask" );
        Assert.isTrue( values.size() == mask.size(), "Values and mask must have the same size." );
    }

    @Override
    @Nullable
    public abstract T get( int i );

    @Override
    public int size() {
        return ( int ) values.size();
    }

    @Override
    public void close() {
        values.close();
        mask.close();
    }
}
