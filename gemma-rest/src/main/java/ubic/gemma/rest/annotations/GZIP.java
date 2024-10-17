package ubic.gemma.rest.annotations;

import ubic.gemma.rest.providers.GzipHeaderDecorator;

import java.lang.annotation.*;

/**
 * Used to annotate endpoints that will have their payload compressed with gzip unconditionally.
 * <p>
 * Note that using this annotation will disregard any form of content encoding negotiation for the endpoint. This should
 * only be used on endpoints that produce significant payloads.
 *
 * @see GzipHeaderDecorator
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GZIP {

    /**
     * Indicate that the payload is already compressed.
     */
    boolean alreadyCompressed() default false;
}
