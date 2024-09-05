package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AnnData implements AutoCloseable {

    public static AnnData open( Path path ) throws IOException {
        return new AnnData( H5File.open( path ) );
    }

    private final H5File h5File;

    public AnnData( H5File h5File ) throws IOException {
        String encodingType = h5File.getStringAttribute( "encoding-type" );
        if ( !Objects.equals( encodingType, "anndata" ) ) {
            h5File.close();
            throw new IllegalArgumentException( "The HDF5 file does not have its 'encoding-type' set to 'anndata'." );
        }
        if ( !h5File.hasAttribute( "encoding-version" ) ) {
            h5File.close();
            throw new IllegalArgumentException( "The HDF5 file does not have an 'encoding-version' attribute set." );
        }
        this.h5File = h5File;
    }

    public Dataframe getObs() {
        return new Dataframe( h5File.getGroup( "obs" ) );
    }

    public Dataframe getVar() {
        return new Dataframe( h5File.getGroup( "var" ) );
    }

    @Nullable
    public Layer getX() {
        if ( h5File.exists( "X" ) ) {
            return new Layer( h5File, "X" );
        } else {
            return null;
        }
    }

    public List<String> getLayers() {
        if ( h5File.exists( "layers" ) ) {
            return h5File.getChildren( "layers" );
        } else {
            return Collections.emptyList();
        }
    }

    public Layer getLayer( String layerName ) {
        return new Layer( h5File, "layers/" + layerName );
    }

    @Nullable
    public H5Group getUns() {
        if ( h5File.exists( "uns" ) ) {
            return h5File.getGroup( "uns" );
        } else {
            return null;
        }
    }

    @Override
    public void close() {
        h5File.close();
    }

    @Override
    public String toString() {
        return h5File.getPath().getFileName().toString();
    }
}
