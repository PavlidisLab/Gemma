package ubic.gemma.core.loader.util.anndata;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5File;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static ubic.gemma.core.loader.util.anndata.Utils.checkEncoding;

@CommonsLog
public class AnnData implements Closeable {

    public static AnnData open( Path path ) throws IOException {
        H5File h5File = H5File.open( path );
        try {
            return new AnnData( h5File );
        } catch ( Exception e ) {
            h5File.close();
            throw e;
        }
    }

    private final H5File h5File;

    private AnnData( H5File h5File ) {
        checkEncoding( h5File, "anndata" );
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
     * Obtain the raw {@code X} layer if this AnnData object has been filtered.
     * <p>
     * This, along {@link #getRawVar()} are not part of the <a href="https://anndata.readthedocs.io/en/latest/fileformat-prose.html">on-disk specification</a>,
     * but will be included if the AnnData object has been sliced/filtered. Note that since this is filtered,
     * {@link #getRawVar()} should be used to refer to the relevant column annotations.
     */
    @Nullable
    public Layer getRawX() {
        if ( h5File.exists( "raw" ) && h5File.exists( "raw/X" ) ) {
            return new Layer( h5File, "raw/X" );
        } else {
            return null;
        }
    }

    /**
     * Obtain the raw {@code var} dataframe if this AnnData object has been filtered.
     */
    @Nullable
    public Dataframe<?> getRawVar() {
        if ( h5File.exists( "raw" ) && h5File.exists( "raw/var" ) ) {
            return new Dataframe<>( h5File.getGroup( "raw/var" ), null );
        } else {
            return null;
        }
    }

    /**
     * Obtain the raw {@code var} dataframe if this AnnData object has been filtered.
     */
    @Nullable
    public <K> Dataframe<K> getRawVar( Class<K> indexClass ) {
        if ( h5File.exists( "raw" ) && h5File.exists( "raw/var" ) ) {
            return new Dataframe<>( h5File.getGroup( "raw/var" ), indexClass );
        } else {
            return null;
        }
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
    public void close() throws IOException {
        h5File.close();
    }

    @Override
    public String toString() {
        return h5File.getPath().getFileName().toString();
    }
}
