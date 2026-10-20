package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5FundamentalType;
import ubic.gemma.core.loader.util.hdf5.H5Type;

import org.springframework.lang.NonNull;

/**
 * An array backend by a H5 enum dataset.
 * <p>
 * This is similar to a {@link CategoricalArray} with the main difference that enumeration do not support missing
 * values and thus {@link #get(int)} never returns {@code null}.
 *
 * @author poirigui
 */
public class EnumArray implements Array<String> {

    private final String[] values;
    private final int[] codes;

    public EnumArray( H5Dataset dataset ) {
        try ( H5Type type = dataset.getType() ) {
            Assert.isTrue( type.getFundamentalType() == H5FundamentalType.ENUM , "expected true");
            values = type.getMemberNames();
        }
        this.codes = dataset.toIntegerVector();
    }

    @NonNull
    @Override
    public String get( int i ) {
        return values[codes[i]];
    }

    public int getInt( int i ) {
        return codes[i];
    }

    @Override
    public int size() {
        return codes.length;
    }

    @Override
    public void close() {
    }
}
