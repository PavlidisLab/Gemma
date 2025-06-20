package ubic.gemma.core.loader.util.anndata;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Group;
import ubic.gemma.core.loader.util.hdf5.H5Type;

import java.util.Objects;

/**
 * Represents a sparse AnnData matrix.
 * @author poirigui
 */
public class SparseMatrix implements Matrix {

    private final H5Group group;
    private final int[] shape;
    private final long[] indptr;

    public SparseMatrix( H5Group group ) {
        if ( !group.hasAttribute( "encoding-type" ) ) {
            throw new MissingEncodingAttributeException( "The H5 group does not have an 'encoding-type' attribute set." );
        }
        if ( !Objects.equals( group.getStringAttribute( "encoding-type" ), "csr_matrix" )
                && !Objects.equals( group.getStringAttribute( "encoding-type" ), "csc_matrix" ) ) {
            throw new InvalidEncodingAttributeException( "The H5 group does not have an 'encoding-type' attribute set to 'csr_matrix' or 'csc_matrix'." );
        }
        if ( !group.hasAttribute( "encoding-version" ) ) {
            throw new MissingEncodingAttributeException( "The H5 group does not have an 'encoding-version' attribute set." );
        }
        this.group = group;
        this.shape = group.getAttribute( "shape" )
                .map( H5Attribute::toIntegerVector )
                .orElseThrow( () -> new IllegalArgumentException( "The sparse matrix does not have a shape attribute." ) );
        Assert.isTrue( this.shape.length == 2 );
        this.indptr = group.getDataset( "indptr" ).toLongVector();
        int expectedIndptrLength = isCsr() ? shape[0] + 1 : shape[1] + 1;
        Assert.isTrue( indptr.length == expectedIndptrLength, "The 'indptr' dataset must contain " + expectedIndptrLength + " elements." );
        Assert.isTrue( indptr[0] == 0, "The first element of 'indptr' must be 0." );
        long nnz = group.getDataset( "data" ).size();
        Assert.isTrue( indptr[expectedIndptrLength - 1] == nnz, "The last element of 'indptr' must be the number of non-zeroes." );
        Assert.isTrue( ArrayUtils.isSorted( indptr ), "The 'indptr' dataset must be sorted." );
        Assert.isTrue( group.exists( "data" ) );
        Assert.isTrue( group.exists( "indices" ) );
    }

    /**
     * Indicate if this matrix is stored in CSR.
     */
    public boolean isCsr() {
        return Objects.equals( group.getStringAttribute( "encoding-type" ), "csr_matrix" );
    }

    /**
     * Indicate if this matrix is stored in CSC.
     */
    public boolean isCsc() {
        return Objects.equals( group.getStringAttribute( "encoding-type" ), "csc_matrix" );
    }

    @Override
    public int[] getShape() {
        return shape;
    }

    @Override
    public H5Type getDataType() {
        try ( H5Dataset data = group.getDataset( "data" ) ) {
            return data.getType();
        }
    }

    public long[] getIndptr() {
        return indptr;
    }

    @Override
    public H5Dataset getData() {
        return group.getDataset( "data" );
    }

    public H5Dataset getIndices() {
        return group.getDataset( "indices" );
    }

    @Override
    public void close() {
        group.close();
    }
}
