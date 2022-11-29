package ubic.gemma.persistence.util;

import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a slice of {@link List}.
 */
public class Slice<O> extends AbstractList<O> implements List<O> {

    /**
     * Create an empty slice with zero elements.
     */
    public static <O> Slice<O> empty() {
        return new Slice<>( Collections.emptyList(), null, null, null, 0L );
    }

    /**
     * Create a slice from a {@link List}.
     */
    public static <O> Slice<O> fromList( List<O> list ) {
        return new Slice<>( list, null, null, null, ( long ) list.size() );
    }

    private final List<O> data;
    private final Sort sort;
    private final Integer offset;
    private final Integer limit;
    private final Long totalElements;

    public Slice( List<O> elements, @Nullable Sort sort, @Nullable Integer offset, @Nullable Integer limit, Long totalElements ) {
        this.data = elements;
        this.sort = sort;
        this.offset = offset;
        this.limit = limit;
        this.totalElements = totalElements;
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
     * This is unfortunately necessary because it it blindly casted.
     * This is necessary
     * @param elem
     * @return
     */
    @Override
    @Deprecated
    public boolean add( O elem ) {
        return data.add( elem );
    }

    /**
     * Unfortunately, we need to implement this because gsec explicitly remove items that are not accessible by the
     * current user in {@link gemma.gsec.acl.afterinvocation.AclAfterFilterValueObjectCollectionProvider}.
     */
    @Override
    @Deprecated
    public boolean remove( Object elem ) {
        return data.remove( elem );
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

    public <S> Slice<S> map( Function<? super O, ? extends S> converter ) {
        return new Slice<>( this.stream().map( converter ).collect( Collectors.toList() ), sort, offset, limit, totalElements );
    }
}
