package ubic.gemma.core.analysis.service;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

/**
 * Interface for a service that serialize entities.
 * @param <T> the type of entity to serialize
 * @author poirigui
 */
public interface FileService<T> {

    /**
     * Write a given entity to an appendable with a given content type.
     */
    void write( T entity, Writer writer, String contentType ) throws IOException;

    void write( T entity, File file, String contentType ) throws IOException;
}
