package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Type;

import java.util.Objects;

/**
 * Represents a dense AnnData matrix.
 * @author poirigui
 */
public class DenseMatrix implements Matrix {

    private final H5Dataset dataset;
    private final int[] shape;

    public DenseMatrix( H5Dataset dataset ) {
        Assert.isTrue( Objects.equals( dataset.getStringAttribute( "encoding-type" ), "array" ),
                "The H5 dataset does not have an 'encoding-type' attribute set to 'array'." );
        Assert.isTrue( dataset.hasAttribute( "encoding-version" ),
                "The H5 dataset does not have an 'encoding-version' attribute." );
        this.dataset = dataset;
        long[] r = dataset.getShape();
        int[] ret = new int[r.length];
        for ( int i = 0; i < r.length; i++ ) {
            ret[i] = ( int ) r[i];
        }
        this.shape = ret;
    }

    @Override
    public int[] getShape() {
        return shape;
    }

    @Override
    public H5Type getDataType() {
        return dataset.getType();
    }

    public H5Dataset getData() {
        return dataset;
    }

    @Override
    public void close() {
        dataset.close();
    }
}