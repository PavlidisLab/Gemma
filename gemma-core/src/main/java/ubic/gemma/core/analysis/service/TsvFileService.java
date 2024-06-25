package ubic.gemma.core.analysis.service;

import java.io.IOException;
import java.io.Writer;

/**
 * Interface for services that produce TSV serialization.
 * @param <T> the entity being serialized
 */
public interface TsvFileService<T> extends FileService<T> {

    /**
     * Write the given entity to tabular format.
     */
    void writeTsv( T entity, Writer writer ) throws IOException;
}
