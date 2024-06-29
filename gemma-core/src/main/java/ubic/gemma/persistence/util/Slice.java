package ubic.gemma.persistence.util;

import ubic.gemma.core.lang.Nullable;

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Represents a slice of {@link List}.
 */
public class Slice<O> extends AbstractList<O> implements List<O> {

    private final List<O> data;
    @Nullable
    private final Sort sort;
    @Nullable
    private final Integer offset;
    @Nullable
    private final Integer limit;
    @Nullable
    private final Long totalElements;

    public Slice( List<O> elements, @Nullable Sort sort, @Nullable Integer offset, @Nullable Integer limit, @Nullable Long totalElements ) {
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
    @Nullable
    public Sort getSort() {
        return this.sort;
    }

    /**
     * @return an offset, or null if unspecified
     */
    @Nullable
    public Integer getOffset() {
        return this.offset;
    }

    /**
     * @return a limit, or null if unspecified
     */
    @Nullable
    public Integer getLimit() {
        return this.limit;
    }

    /**
     *
     * @return the total number of elements, or null if unspecified.
     */
    @Nullable
    public Long getTotalElements() {
        return this.totalElements;
    }

    public <S> Slice<S> map( Function<? super O, ? extends S> converter ) {
        return new Slice<>( this.stream().map( converter ).collect( Collectors.toList() ), sort, offset, limit, totalElements );
    }
}
