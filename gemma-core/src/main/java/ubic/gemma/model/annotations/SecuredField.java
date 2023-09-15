package ubic.gemma.model.annotations;

import org.springframework.security.access.annotation.Secured;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a {@link Secured} field.
 * @author poirigui
 * @see Secured
 */
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SecuredField {
    /**
     * List of configuration attributes.
     */
    String[] value();

    /**
     * What to do when the user is not authorized to access the field.
     */
    Policy policy() default Policy.SET_NULL;

    enum Policy {
        /**
         * Omit the value from serialization.
         */
        OMIT,
        /**
         * Set the value to NULL.
         */
        SET_NULL,
        /**
         * Raise an {@link org.springframework.security.access.AccessDeniedException} exception.
         */
        RAISE_EXCEPTION
    }
}
