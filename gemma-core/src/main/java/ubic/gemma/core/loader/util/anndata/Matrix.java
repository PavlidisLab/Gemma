package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Type;

/**
 * Represents an AnnData matrix.
 * @author poirigui
 * @see SparseMatrix
 * @see DenseMatrix
 */
public interface Matrix extends AutoCloseable {

    /**
     * Obtain the shape of this matrix.
     */
    int[] getShape();

    /**
     * Obtain the data type used for the scalars of this matrix.
     */
    H5Type getDataType();

    /**
     * Release the underlying H5 resource for this matrix.
     */
    @Override
    void close();
}
