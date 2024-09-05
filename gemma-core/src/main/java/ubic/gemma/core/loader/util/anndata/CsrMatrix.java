package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Attribute;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import java.util.Objects;

public class CsrMatrix implements AutoCloseable {

    private final H5Group group;
    private final int[] shape;
    private final int[] indptr;

    public CsrMatrix( H5Group group ) {
        Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "csr_matrix" ),
                "The H5 group does not have an 'encoding-type' attribute set to 'csr_matrix'." );
        Assert.isTrue( group.hasAttribute( "encoding-version" ) );
        this.shape = group.getAttribute( "shape" )
                .map( H5Attribute::toIntegerVector )
                .orElseThrow( () -> new IllegalArgumentException( "The sparse matrix does not have a shape attribute." ) );
        Assert.isTrue( this.shape.length == 2 );
        this.group = group;
        try ( H5Dataset indptr = group.getDataset( "indptr" ) ) {
            this.indptr = indptr.toIntegerVector();
        }
        assert indptr.length == shape[0] + 1;
    }

    public int[] getShape() {
        return shape;
    }

    public int[] getIndptr() {
        return indptr;
    }

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
