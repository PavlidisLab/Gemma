package ubic.gemma.model;

import java.lang.annotation.*;

/**
 * Annotate class representing value objects.
 * @author poirigui
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ValueObject {
}
