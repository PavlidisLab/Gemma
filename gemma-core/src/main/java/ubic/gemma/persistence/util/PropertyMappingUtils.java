package ubic.gemma.persistence.util;

/**
 * Utilities for dealing with {@link PropertyMapping}.
 * @author poirigui
 * @see Filter
 * @see Sort
 */
public class PropertyMappingUtils {

    /**
     * Form a property suitable for a Criteria or HQL query.
     */
    public static String formProperty( PropertyMapping propertyMapping ) {
        if ( propertyMapping.getObjectAlias() != null ) {
            return propertyMapping.getObjectAlias() + "." + propertyMapping.getPropertyName();
        } else {
            return propertyMapping.getPropertyName();
        }
    }
}
