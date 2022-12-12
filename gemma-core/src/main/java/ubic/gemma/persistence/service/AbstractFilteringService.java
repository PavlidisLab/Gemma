package ubic.gemma.persistence.service;

import lombok.Value;
import org.apache.commons.lang3.ClassUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class AbstractFilteringService<O extends Identifiable> extends AbstractService<O> implements FilteringService<O> {

    private final FilteringDao<O> mainDao;

    protected AbstractFilteringService( FilteringDao<O> mainDao ) {
        super( mainDao );
        this.mainDao = mainDao;
    }

    @Override
    public final ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) {
        ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
        return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, value );
    }

    @Override
    public final ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) {
        ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
        return ObjectFilter.parseObjectFilter( propertyMeta.objectAlias, propertyMeta.propertyName, propertyMeta.propertyType, operator, values );
    }

    @Override
    public final Sort getSort( String property, @Nullable Sort.Direction direction ) {
        // this only serves as a pre-condition to ensure that the propertyName exists
        ObjectFilterPropertyMeta propertyMeta = getObjectFilterPropertyMeta( property );
        return Sort.by( propertyMeta.objectAlias, propertyMeta.propertyName, direction );
    }

    @Value
    protected static class ObjectFilterPropertyMeta {
        String objectAlias;
        String propertyName;
        Class<?> propertyType;
    }

    /**
     * Obtain various meta-information used to infer what to use in a {@link ObjectFilter} or {@link Sort}.
     *
     * This is used by {@link #getObjectFilter(String, ObjectFilter.Operator, String)} and {@link #getSort(String, Sort.Direction)}.
     *
     * @throws IllegalArgumentException if no such propertyName exists in {@link O}
     * @see #getObjectFilter(String, ObjectFilter.Operator, String)
     * @see #getObjectFilter(String, ObjectFilter.Operator, Collection)
     * @see #getSort(String, Sort.Direction)
     */
    protected ObjectFilterPropertyMeta getObjectFilterPropertyMeta( String propertyName ) throws IllegalArgumentException {
        return new ObjectFilterPropertyMeta( mainDao.getObjectAlias(), propertyName, resolveObjectFilterPropertyType( propertyName, mainDao.getElementClass() ) );
    }

    /**
     * Helper to resolve the type of a property in a given class.
     */
    protected static Class<?> resolveObjectFilterPropertyType( String propertyName, Class<?> clazz ) {
        try {
            return getDeclaredFieldType( propertyName, clazz );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( String.format( "Could not resolve property '%s' on %s.", propertyName, clazz.getName() ), e );
        }
    }

    /**
     * Checks if property of given name exists in the given class. If the given string specifies
     * nested properties (E.g. curationDetails.troubled), only the substring before the first dot is evaluated and the
     * rest of the string is processed in a new recursive iteration.
     *
     * @param property the property to check for. If the string contains dot characters ('.'), only the part
     *                 before the first dot will be evaluated. Substring after the dot will be checked against the
     *                 type of the field retrieved from the substring before the dot.
     * @param cls      the class to check the property on.
     * @return the class of the property last in the line of nesting.
     */
    private static Class<?> getDeclaredFieldType( String property, Class<?> cls ) throws NoSuchFieldException {
        String[] parts = property.split( "\\.", 2 );
        Field field = getDeclaredField( cls, parts[0] );
        Class<?> subCls = field.getType();

        if ( Collection.class.isAssignableFrom( subCls ) ) {
            ParameterizedType pt = ( ParameterizedType ) field.getGenericType();
            for ( Type type : pt.getActualTypeArguments() ) {
                if ( type instanceof Class ) {
                    subCls = ( Class<?> ) type;
                    break;
                }
            }
        }

        if ( parts.length > 1 ) {
            return getDeclaredFieldType( parts[1], subCls );
        } else {
            return subCls;
        }
    }

    /**
     * Recursive version of {@link Class#getDeclaredField(String)} that also checks in the superclass hierarchy.
     * @see Class#getDeclaredField(String)
     */
    private static Field getDeclaredField( Class<?> cls, String field ) throws NoSuchFieldException {
        try {
            return cls.getDeclaredField( field );
        } catch ( NoSuchFieldException e ) {
            if ( cls.getSuperclass() != null ) {
                return getDeclaredField( cls.getSuperclass(), field );
            } else {
                throw e;
            }
        }
    }
}
