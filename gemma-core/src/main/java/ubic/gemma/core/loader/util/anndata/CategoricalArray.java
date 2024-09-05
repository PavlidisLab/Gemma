package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Represents a categorical array.
 */
public class CategoricalArray {

    private final String[] categories;
    private final int[] codes;

    public CategoricalArray( H5Group group ) {
        Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "categorical" ),
                "The H5 group does not have an 'encoding-type' attribute set to 'categorical'." );
        Assert.isTrue( group.hasAttribute( "encoding-version" ) );
        Assert.isTrue( group.hasAttribute( "ordered" ) );
        this.categories = group.getDataset( "categories" ).toStringVector();
        this.codes = group.getDataset( "codes" ).toIntegerVector();
    }

    public String[] getCategories() {
        return categories;
    }

    public int[] getCodes() {
        return codes;
    }

    @Nullable
    public String get( int i ) {
        return codes[i] != -1 ? categories[codes[i]] : null;
    }

    public String[] toStringVector() {
        String[] vec = new String[codes.length];
        for ( int i = 0; i < vec.length; i++ ) {
            vec[i] = codes[i] != -1 ? categories[codes[i]] : null;
        }
        return vec;
    }

    public int size() {
        return codes.length;
    }
}
