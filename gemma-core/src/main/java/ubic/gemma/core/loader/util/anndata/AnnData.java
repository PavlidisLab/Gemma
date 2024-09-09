package ubic.gemma.core.loader.util.anndata;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5File;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@CommonsLog
public class AnnData implements AutoCloseable {

    public static AnnData open( Path path ) throws IOException {
        return new AnnData( H5File.open( path ) );
    }

    private final H5File h5File;

    private AnnData( H5File h5File ) {
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

    public Dataframe<?> getObs() {
        return new Dataframe<>( h5File.getGroup( "obs" ), null );
    }

    public <K> Dataframe<K> getObs( Class<K> indexClass ) {
        return new Dataframe<>( h5File.getGroup( "obs" ), indexClass );
    }

    public Dataframe<?> getVar() {
        return new Dataframe<>( h5File.getGroup( "var" ), null );
    }

    public <K> Dataframe<K> getVar( Class<K> indexClass ) {
        return new Dataframe<>( h5File.getGroup( "var" ), indexClass );
    }

    /**
     * Obtain the main layer named {@code X}.
     */
    @Nullable
    public Layer getX() {
        if ( h5File.exists( "X" ) ) {
            return new Layer( h5File, "X" );
        } else {
            return null;
        }
    }

    /**
     * Obtain all the layer names under {@code layers/} path.
     */
    public List<String> getLayers() {
        if ( h5File.exists( "layers" ) ) {
            Assert.isTrue( Objects.equals( h5File.getStringAttribute( "layers", "encoding-type" ), "dict" ) );
            Assert.isTrue( h5File.hasAttribute( "layers", "encoding-type" ) );
            return h5File.getChildren( "layers" );
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Obtain a layer by name.
     */
    public Layer getLayer( String layerName ) {
        Assert.isTrue( Objects.equals( h5File.getStringAttribute( "layers", "encoding-type" ), "dict" ) );
        Assert.isTrue( h5File.hasAttribute( "layers", "encoding-type" ) );
        return new Layer( h5File, "layers/" + layerName );
    }

    /**
     * Obtain additional free-form data stored under {@code uns}.
     */
    @Nullable
    public Mapping getUns() {
        if ( h5File.exists( "uns" ) ) {
            return new Mapping( h5File.getGroup( "uns" ) );
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
