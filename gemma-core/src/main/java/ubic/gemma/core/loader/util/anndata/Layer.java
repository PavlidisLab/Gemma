package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Dataset;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.loader.util.hdf5.H5Group;

public class Layer {

    private final H5File h5File;
    private final String path;

    public Layer( H5File h5File, String path ) {
        Assert.isTrue( h5File.exists( path ), "No layer at " + path + "." );
        Assert.isTrue( h5File.hasAttribute( path, "encoding-type" ) );
        Assert.isTrue( h5File.hasAttribute( path, "encoding-version" ) );
        this.h5File = h5File;
        this.path = path;
    }

    public String getType() {
        return h5File.getStringAttribute( path, "encoding-type" );
    }

    public H5Group getGroup() {
        return h5File.getGroup( path );
    }

    public H5Dataset getDataset() {
        return h5File.getDataset( path );
    }

    public String getPath() {
        return path;
    }
}
