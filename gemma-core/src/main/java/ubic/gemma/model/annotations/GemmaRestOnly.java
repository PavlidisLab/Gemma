package ubic.gemma.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a property or type is only visible not visible outside of Gemma REST.
 * TODO: honor this annotation in Gemma Web
 * @author poirigui
 * @see WithheldFromApi for keeping a property off the RESTful API, with a stated reason
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GemmaRestOnly {
}
