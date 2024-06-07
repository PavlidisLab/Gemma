package ubic.gemma.model.common;

import java.lang.annotation.*;

/**
 * Annotate class representing value objects.
 * @author poirigui
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ValueObject {
}
