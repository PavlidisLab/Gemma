package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;
import java.util.Arrays;

import static ubic.gemma.core.loader.util.anndata.Utils.checkEncoding;

/**
 * Represents a categorical array.
 * @author poirigui
 */
public class CategoricalArray<T> implements Array<T> {

    private final H5Group group;
    private final T[] categories;
    private final int[] codes;
    private final boolean ordered;

    public CategoricalArray( H5Group group, Class<T> categoryType ) {
        checkEncoding( group, "categorical" );
        Assert.isTrue( group.hasAttribute( "ordered" ) );
        this.group = group;
        if ( String.class.isAssignableFrom( categoryType ) ) {
            //noinspection unchecked
            this.categories = ( T[] ) group.getDataset( "categories" ).toStringVector();
        } else if ( Integer.class.isAssignableFrom( categoryType ) ) {
            //noinspection unchecked
            this.categories = ( T[] ) Arrays.stream( group.getDataset( "categories" ).toIntegerVector() ).boxed().toArray();
        } else {
            throw new IllegalArgumentException( "Unsupported scalar type for vector " + categoryType );
        }
        this.codes = group.getDataset( "codes" ).toIntegerVector();
        //noinspection resource
        this.ordered = group.getAttribute( "ordered" )
                .orElseThrow( IllegalArgumentException::new )
                .toBooleanVector()[0];
    }

    public T[] getCategories() {
        return categories;
    }

    public int[] getCodes() {
        return codes;
    }

    public boolean isOrdered() {
        return ordered;
    }

    @Nullable
    @Override
    public T get( int i ) {
        return codes[i] != -1 ? categories[codes[i]] : null;
    }

    @Override
    public int size() {
        return codes.length;
    }

    @Override
    public void close() {
        group.close();
    }

    @Override
    public String toString() {
        return codes.length + " values from " + Arrays.toString( categories );
    }
}
