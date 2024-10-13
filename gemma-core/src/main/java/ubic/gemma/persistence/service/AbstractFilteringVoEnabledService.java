package ubic.gemma.persistence.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base implementation for {@link FilteringVoEnabledService}.
 */
public abstract class AbstractFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>>
        extends AbstractService<O> implements FilteringVoEnabledService<O, VO>, BaseService<O> {

    private final FilteringVoEnabledDao<O, VO> voDao;

    @Autowired
    private AccessDecisionManager accessDecisionManager;

    protected AbstractFilteringVoEnabledService( FilteringVoEnabledDao<O, VO> voDao ) {
        super( voDao );
        this.voDao = voDao;
    }

    @Override
    public String getIdentifierPropertyName() {
        return voDao.getIdentifierPropertyName();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.loadIds( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public List<O> load( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.load( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return voDao.load( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public VO loadValueObject( O entity ) {
        return voDao.loadValueObject( entity );
    }

    @Override
    @Transactional(readOnly = true)
    public VO loadValueObjectById( Long entityId ) {
        return voDao.loadValueObjectById( entityId );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjects( Collection<O> entities ) {
        return voDao.loadValueObjects( entities );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjectsByIds( Collection<Long> entityIds ) {
        return voDao.loadValueObjectsByIds( entityIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadAllValueObjects() {
        return voDao.loadAllValueObjects();
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit ) {
        return voDao.loadValueObjects( filters, sort, offset, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort ) {
        return voDao.loadValueObjects( filters, sort );
    }

    @Override
    @Transactional(readOnly = true)
    public long count( @Nullable Filters filters ) {
        return voDao.count( filters );
    }

    public Set<String> getFilterableProperties() {
        return voDao.getFilterableProperties();
    }

    public Class<?> getFilterablePropertyType( String property ) {
        return voDao.getFilterablePropertyType( property );
    }

    @Nullable
    public String getFilterablePropertyDescription( String property ) {
        return voDao.getFilterablePropertyDescription( property );
    }

    @Nullable
    @Override
    public List<Object> getFilterablePropertyAllowedValues( String property ) {
        return voDao.getFilterablePropertyAllowedValues( property );
    }

    @Nullable
    @Override
    public List<MessageSourceResolvable> getFilterablePropertyResolvableAllowedValuesLabels( String p ) {
        List<Object> filterable = getFilterablePropertyAllowedValues( p );
        if ( filterable != null ) {
            return filterable.stream()
                    .map( f -> this.getCodesForAllowedValue( p, f ) )
                    .collect( Collectors.toList() );
        } else {
            return null;
        }
    }

    @Override
    public boolean getFilterablePropertyIsUsingSubquery( String property ) {
        return voDao.getFilterablePropertyIsUsingSubquery( property );
    }

    /**
     * Create a {@link MessageSourceResolvable} for an allowed value of a filter.
     * <p>
     * If the value is an enumeration, a code for the enum class +
     */
    private MessageSourceResolvable getCodesForAllowedValue( String propertyName, Object allowedValue ) {
        String[] codes;
        String key = allowedValue.toString();
        if ( allowedValue instanceof Enum ) {
            codes = new String[] {
                    voDao.getElementClass().getName() + "." + propertyName + "." + key + ".label",
                    voDao.getElementClass().getSimpleName() + "." + "." + propertyName + "." + key + ".label",
                    allowedValue.getClass().getName() + "." + key + ".label",
                    allowedValue.getClass().getSimpleName() + "." + key + ".label"
            };
        } else {
            codes = new String[] {
                    voDao.getElementClass().getName() + "." + propertyName + "." + key + ".label",
                    voDao.getElementClass().getSimpleName() + "." + propertyName + "." + key + ".label"
            };
        }
        return new DefaultMessageSourceResolvable( codes, key );
    }

    @Nullable
    @Override
    public Collection<ConfigAttribute> getFilterablePropertyConfigAttributes( String property ) {
        return null;
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, String value ) throws IllegalArgumentException {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, operator, value );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, String value, SubqueryMode subqueryMode ) throws IllegalArgumentException {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, operator, value, subqueryMode );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, Collection<String> values ) throws IllegalArgumentException {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, operator, values );
    }

    @Override
    public Filter getFilter( String property, Filter.Operator operator, Collection<String> values, SubqueryMode subqueryMode ) throws IllegalArgumentException {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, operator, values, subqueryMode );
    }

    @Override
    public <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, T value ) {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, propertyType, operator, value );
    }

    @Override
    public <T> Filter getFilter( String property, Class<T> propertyType, Filter.Operator operator, Collection<T> values ) {
        checkIfPropertyIsAccessible( property );
        return voDao.getFilter( property, propertyType, operator, values );
    }

    @Override
    public Sort getSort( String property, @Nullable Sort.Direction direction, Sort.NullMode nullMode ) throws IllegalArgumentException {
        checkIfPropertyIsAccessible( property );
        return voDao.getSort( property, direction, nullMode );
    }

    private void checkIfPropertyIsAccessible( String property ) {
        Collection<ConfigAttribute> configAttributes = getFilterablePropertyConfigAttributes( property );
        if ( configAttributes != null ) {
            accessDecisionManager.decide( SecurityContextHolder.getContext().getAuthentication(), null, configAttributes );
        }
    }
}
