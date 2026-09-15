package ubic.gemma.rest.annotations;

import java.lang.annotation.*;

/**
 * Container for repeated {@link GZIP} declarations.
 * <p>
 * A resource method that negotiates between representations may need a different compression strategy per media
 * type. {@code /resultSets/{resultSet}} is the case that forced this: its JSON representation is generated in-band
 * and wants encoder-based compression, while its TSV representation is served straight off a pre-gzipped cache file
 * via sendfile and only wants the header appended. Those are opposite settings of {@link GZIP#alreadyCompressed()},
 * so they cannot be expressed by a single annotation.
 * <p>
 * Never reference this type directly — write two {@link GZIP} annotations and let the compiler wrap them.
 *
 * @see GZIP
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface GZIPs {
    GZIP[] value();
}
