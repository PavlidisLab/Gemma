package ubic.gemma.core.util;

import lombok.SneakyThrows;
import org.apache.commons.lang3.ClassUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * Strictly validate a {@link BeanDefinition} against the <a href="https://en.wikipedia.org/wiki/JavaBeans">JavaBeans</a> contract.
 * @author poirigui
 */
class StrictBeanDefinitionValidator implements Validator {

    /**
     * These are mostly low-level standard Java class that cannot be made to comply with the JavaBeans specification.
     */
    private static final Class<?>[] IGNORED_PROPERTY_TYPES = { Collection.class, Iterator.class, Map.class,
            URL.class, URI.class, Date.class, Boolean.class, Enum.class };

    private final boolean allowImmutableProperties;

    /**
     * Create a new bean definition validator.
     */
    StrictBeanDefinitionValidator() {
        this( false );
    }

    /**
     * Create a new bean definition validator.
     *
     * @param allowImmutableProperties allow properties lacking a setter, which is not in compliance with the JavaBeans
     *                                 contract, but very common practice
     */
    StrictBeanDefinitionValidator( boolean allowImmutableProperties ) {
        this.allowImmutableProperties = allowImmutableProperties;
    }

    @Override
    public boolean supports( Class<?> clazz ) {
        return BeanDefinition.class.isAssignableFrom( clazz );
    }

    @SneakyThrows
    @Override
    public void validate( Object target, Errors errors ) {
        BeanDefinition beanDefinition = ( BeanDefinition ) target;
        Class<?> clazz = Class.forName( beanDefinition.getBeanClassName() );
        checkSerializable( clazz, errors );
        checkZeroArgumentConstructor( clazz, errors );
        checkFields( clazz, errors );
    }

    private void checkSerializable( Class<?> clazz, Errors errors ) throws IllegalArgumentException {
        // implements serializable
        if ( !Serializable.class.isAssignableFrom( clazz ) ) {
            errors.rejectValue( null, "BeanDefinition.notSerializable", "is not serializable" );
        }
    }

    private void checkZeroArgumentConstructor( Class<?> clazz, Errors errors ) throws IllegalArgumentException {
        // default public constructor
        if ( Arrays.stream( clazz.getDeclaredConstructors() )
                .noneMatch( c -> Modifier.isPublic( c.getModifiers() ) && c.getParameterCount() == 0 ) ) {
            errors.rejectValue( null, "BeanDefinition.noZeroArgPublicConstructor", "lacks a public zero-argument constructor" );
        }
    }

    private void checkFields( Class<?> clazz, Errors errors ) throws IllegalArgumentException {
        checkFieldsInternal( clazz, new HashSet<>(), errors );
    }

    private void checkFieldsInternal( Class<?> clazz, Set<Class<?>> seen, Errors errors ) {
        if ( seen.contains( clazz ) ) {
            return;
        }
        seen.add( clazz );
        for ( PropertyDescriptor pd : BeanUtils.getPropertyDescriptors( clazz ) ) {
            if ( pd.getPropertyType() == null || pd.getPropertyType().equals( Class.class ) ) {
                continue;
            }
            if ( pd.getReadMethod() == null ) {
                errors.rejectValue( pd.getName(), "StrictBeanDefinitionValidator.noGetterForField", "lacks a getter" );
            }
            if ( !allowImmutableProperties && pd.getWriteMethod() == null ) {
                errors.rejectValue( pd.getName(), "StrictBeanDefinitionValidator.noSetterForField", "lacks a setter" );
            }
            // further checks for non-primitive types
            if ( !isPropertyTypeIgnored( pd.getPropertyType() ) ) {
                try {
                    errors.pushNestedPath( pd.getName() );
                    checkSerializable( pd.getPropertyType(), errors );
                    checkZeroArgumentConstructor( pd.getPropertyType(), errors );
                    checkFieldsInternal( pd.getPropertyType(), seen, errors );
                } finally {
                    errors.popNestedPath();
                }
            }
        }
    }

    /**
     * Tell if a property type should be validated.
     *
     * TODO: check collection types, array types, map types etc.
     */
    private boolean isPropertyTypeIgnored( Class<?> propertyType ) {
        return ClassUtils.isPrimitiveOrWrapper( propertyType )
                || propertyType.isArray()
                || Arrays.stream( IGNORED_PROPERTY_TYPES ).anyMatch( t -> t.isAssignableFrom( propertyType ) );
    }
}
