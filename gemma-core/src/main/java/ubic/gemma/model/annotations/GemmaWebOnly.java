package ubic.gemma.model.annotations;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicate that a property is exclusively used for Gemma Web.
 * <p>
 * Fields and getters annotated with this are excluded from Jackson JSON serialization and will not appear in the Gemma
 * RESTful API.
 * @author poirigui
 * @see GemmaRestOnly for properties or types that should be exclusive to Gemma REST instead
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonIgnore
public @interface GemmaWebOnly {
}
