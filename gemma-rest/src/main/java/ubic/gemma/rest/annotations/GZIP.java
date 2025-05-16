package ubic.gemma.rest.annotations;

import javax.ws.rs.core.MediaType;
import java.lang.annotation.*;

/**
 * Used to annotate endpoints that will have their payload compressed with gzip unconditionally.
 * <p>
 * Note that using this annotation will disregard any form of content encoding negotiation for the endpoint. This should
 * only be used on endpoints that produce significant payloads.
 *
 * @see ubic.gemma.rest.providers.GzipHeaderDecorator
 * @see ubic.gemma.rest.providers.GzipHeaderDecoratorAfterGZipEncoder
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GZIP {

    /**
     * If non-empty, only contents compatible with any of the specified media type will be compressed.
     * @see javax.ws.rs.core.MediaType#isCompatible(MediaType)
     */
    String[] mediaTypes() default {};

    /**
     * Indicate that the payload is already compressed.
     * <p>
     * When that is the case, the decorator should only append a {@code Content-Encoding: gzip} header, but not alter
     * the entity.
     */
    boolean alreadyCompressed() default false;
}
