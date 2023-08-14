package ubic.gemma.rest.annotations;

import java.lang.annotation.*;

@Repeatable(CacheControls.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface CacheControl {
    int maxAge() default -1;
    boolean isPrivate() default false;

    /**
     * Authorities to whom this cache directive applies.
     */
    String[] authorities() default {};
}
