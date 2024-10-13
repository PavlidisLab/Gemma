package ubic.gemma.persistence.service;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Base class to use to pretend to offer filtering, but actually supporting no filterable properties.
 * <p>
 * This is necessary because {@link AbstractFilteringVoEnabledDao} reroutes VO loading methods from {@link BaseVoEnabledDao}
 * through the {@link Filter}-based mechanism for consistency, so it is not a suitable base class if you don't want or
 * need to implement the filtering aspect of the interface.
 */
public abstract class AbstractNoopFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    private final String message = String.format( "Filtering %s is not supported.", elementClass.getName() );

    protected AbstractNoopFilteringVoEnabledDao( Class<? extends O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public final Set<String> getFilterableProperties() {
        return Collections.emptySet();
    }

    @Override
    public final Class<?> getFilterablePropertyType( String propertyName ) throws IllegalArgumentException {
        throw new UnsupportedOperationException( message );
    }

    @Nullable
    @Override
    public final String getFilterablePropertyDescription( String propertyName ) throws IllegalArgumentException {
        throw new UnsupportedOperationException( message );
    }

    @Nullable
    @Override
    public List<Object> getFilterablePropertyAllowedValues( String property ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public boolean getFilterablePropertyIsUsingSubquery( String property ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, String value, SubqueryMode subqueryMode ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, Collection<String> values, SubqueryMode subqueryMode ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, T value ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, Collection<T> values ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final Sort getSort( String property, @Nullable Sort.Direction direction, Sort.NullMode nullMode ) throws IllegalArgumentException {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final long count( @Nullable Filters filters ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        throw new UnsupportedOperationException( message );
    }

    @Override
    public final List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        throw new UnsupportedOperationException( message );
    }
}
