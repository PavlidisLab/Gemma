package ubic.gemma.core.analysis.service;

import java.io.File;
import java.io.IOException;

public interface TsvFileService<T> {

    /**
     * Write the given entity to tabular format.
     * @param entity
     * @param appendable
     * @throws IOException
     */
    void writeTsvToAppendable( T entity, Appendable appendable ) throws IOException;
}
