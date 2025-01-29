package ubic.gemma.core.loader.util.anndata;

import ubic.gemma.core.loader.util.hdf5.H5Group;

import java.util.HashSet;
import java.util.Set;

import static ubic.gemma.core.loader.util.anndata.Utils.checkEncoding;

/**
 * Represents an AnnData mapping.
 * @author poirigui
 */
public class Mapping implements AutoCloseable {

    private final H5Group group;

    public Mapping( H5Group group ) {
        checkEncoding( group, "dict" );
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
