package ubic.gemma.core.datastructure;

import java.util.List;

/**
 * A subclass of {@link List} for sparsely stored lists.
 * @author poirigui
 */
public interface SparseList<T> extends List<T> {

    /**
     * The number of elements actually stored.
     */
    int storageSize();
}
