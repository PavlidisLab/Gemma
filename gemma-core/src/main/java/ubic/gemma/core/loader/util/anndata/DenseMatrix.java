package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Type;

import static ubic.gemma.core.loader.util.anndata.Utils.checkEncoding;

/**
 * Represents a dense AnnData matrix.
 * @author poirigui
 */
public class DenseMatrix implements Matrix {

    private final H5Dataset dataset;
    private final int[] shape;

    public DenseMatrix( H5Dataset dataset ) {
        checkEncoding( dataset, "array" );
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

    @Override
    public H5Dataset getData() {
        return dataset;
    }

    @Override
    public void close() {
        dataset.close();
    }
}
