package ubic.gemma.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a property or type is only visible not visible outside of Gemma REST.
 * TODO: honor this annotation in Gemma Web
 * @author poirigui
 * @see GemmaWebOnly for making properties or types exclusive to Gemma Web instead
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface GemmaRestOnly {
}
