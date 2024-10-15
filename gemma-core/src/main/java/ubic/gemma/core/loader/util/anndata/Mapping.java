package ubic.gemma.core.loader.util.anndata;

import org.springframework.util.Assert;
import ubic.gemma.core.loader.util.hdf5.H5Group;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents an AnnData mapping.
 * @author poirigui
 */
public class Mapping implements AutoCloseable {

    private final H5Group group;

    public Mapping( H5Group group ) {
        Assert.isTrue( Objects.equals( group.getStringAttribute( "encoding-type" ), "dict" ) );
        Assert.isTrue( group.hasAttribute( "encoding-type" ) );
        this.group = group;
    }

    public Set<String> getKeys() {
        return new HashSet<>( group.getChildren() );
    }

    @Override
    public void close() {
        group.close();
    }
}
