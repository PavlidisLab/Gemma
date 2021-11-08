package ubic.gemma.persistence.service;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import java.util.Collection;

/**
 * Base implementation for {@link FilteringVoEnabledDao}.
 *
 * @param <O>
 * @param <VO>
 * @author poirigui
 */
public abstract class AbstractFilteringVoEnabledDao<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends AbstractVoEnabledDao<O, VO> implements FilteringVoEnabledDao<O, VO> {

    protected AbstractFilteringVoEnabledDao( Class<O> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws ObjectFilterException {
        try {
            return ObjectFilter.parseObjectFilter( getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, elementClass ), operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new ObjectFilterException( "Could not create an object filter for " + property + ".", e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws ObjectFilterException {
        try {
            return ObjectFilter.parseObjectFilter( getObjectAlias(), property, EntityUtils.getDeclaredFieldType( property, elementClass ), operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new ObjectFilterException( "Could not create an object filter for " + property + " using a collection.", e );
        }
    }

    @Override
    public Sort getSort( String property, Sort.Direction direction ) throws NoSuchFieldException {
        // this only serves as a pre-condition to ensure that the property exists
        EntityUtils.getDeclaredField( elementClass, property );
        return Sort.by( ObjectFilterQueryUtils.formPropertyName( getObjectAlias(), property ), direction );
    }
}
