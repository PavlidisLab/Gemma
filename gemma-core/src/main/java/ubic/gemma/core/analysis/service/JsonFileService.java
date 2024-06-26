package ubic.gemma.core.analysis.service;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface for service that provides JSON serialization.
 * @param <T> the type of entity being serialized to JSON
 * @author poirigui
 */
public interface JsonFileService<T> extends FileService<T> {

    /**
     * Write a given entity to JSON.
     */
    void writeJson( T entity, Writer writer ) throws IOException;
}
