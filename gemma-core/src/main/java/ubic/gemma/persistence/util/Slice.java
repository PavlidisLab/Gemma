package ubic.gemma.persistence.util;

import org.hibernate.Query;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a slice of {@link List}.
 */
public class Slice<O> extends AbstractList<O> implements List<O> {

    private final List<O> data;

    private final Sort sort;
    public final Integer offset;
    public final Integer limit;
    private final Long totalElements;

    public Slice( List<O> elements, Sort sort, Integer offset, Integer limit, Long totalElements ) {
        this.data = elements;
        this.sort = sort;
        this.offset = offset;
        this.limit = limit;
        this.totalElements = totalElements;
    }

    /**
     * Creates an empty, unsorted slice.
     */
    public Slice() {
        this( new ArrayList<>(), null, 0, 0, 0L );
    }

    @Override
    public O get( int i ) {
        return data.get( i );
    }

    @Override
    public int size() {
        return data.size();
    }

    /**
     * @return a sort, or null if unspecified
     */
    public Sort getSort() {
        return this.sort;
    }

    /**
     * @return an offset, or null if unspecified
     */
    public Integer getOffset() {
        return this.offset;
    }

    /**
     * @return a limit, or null if unspecified
     */
    public Integer getLimit() {
        return this.limit;
    }

    /**
     *
     * @return the total number
     */
    public Long getTotalElements() {
        return this.totalElements;
    }
}
