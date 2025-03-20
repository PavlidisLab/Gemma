package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5File;

/**
 * Represents a layer.
 * @author poirigui
 */
public class Layer {

    private final H5File h5File;
    private final String path;
    private final String encodingType;

    public Layer( H5File h5File, String path ) {
        Assert.isTrue( path.equals( "X" ) || path.startsWith( "layers/" ) || path.equals( "raw/X" ),
                "A layer path must either be 'X', 'raw.X' or start with 'layers/'." );
        Assert.isTrue( h5File.exists( path ), "No layer at " + path + "." );
        if ( !h5File.hasAttribute( path, "encoding-type" ) ) {
            throw new MissingEncodingAttributeException( "The layer at " + path + " does not have an 'encoding-type' attribute set." );
        }
        if ( !h5File.hasAttribute( path, "encoding-version" ) ) {
            throw new MissingEncodingAttributeException( "The layer at " + path + " does not have an 'encoding-version' attribute set." );
        }
        this.h5File = h5File;
        this.path = path;
        this.encodingType = h5File.getStringAttribute( path, "encoding-type" );
    }

    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Indicate where this layer is located relative to the H5 file.
     * <p>
     * This is either {@code X} or {@code layers/{layerName}}.
     */
    public String getPath() {
        return path;
    }

    /**
     * Get the encoding type of this layer.
     * <p>
     * Layers are either dense or sparse, use {@link #isSparse()} or {@link #isDense()} to verify that.
     */
    public String getType() {
        return encodingType;
    }

    /**
     * Check if the matrix encoded in this layer is sparse.
     */
    public boolean isSparse() {
        return encodingType.equals( "csr_matrix" ) || encodingType.equals( "csc_matrix" );
    }

    /**
     * Check if the matrix encoded in this layer is dense.
     */
    public boolean isDense() {
        return encodingType.equals( "array" );
    }

    /**
     * Obtain the matrix encoded in this layer.
     */
    public Matrix getMatrix() {
        if ( isSparse() ) {
            return getSparseMatrix();
        } else if ( isDense() ) {
            return getDenseMatrix();
        } else {
            throw new UnsupportedOperationException( String.format( "Layers of type %s is not supported.", encodingType ) );
        }
    }

    /**
     * Obtain the sparse matrix encoded in this layer.
     */
    public SparseMatrix getSparseMatrix() {
        Assert.isTrue( isSparse(), "The layer at " + path + " is not sparse." );
        return new SparseMatrix( h5File.getGroup( path ) );
    }

    /**
     * Obtain the dense matrix encoded in this layer.
     */
    public DenseMatrix getDenseMatrix() {
        Assert.isTrue( isDense(), "The layer at " + path + " is not dense." );
        return new DenseMatrix( h5File.getDataset( path ) );
    }

    @Override
    public String toString() {
        return path;
    }
}
