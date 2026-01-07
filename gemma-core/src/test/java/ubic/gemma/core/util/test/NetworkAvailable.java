package ubic.gemma.core.util.test;

import java.lang.annotation.*;
import java.net.UnknownHostException;

/**
 * Test annotation to indicate that a test requires network access or access to a specific resource.
 *
 * @author poirigui
 * @see NetworkAvailableRule
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface NetworkAvailable {

    /**
     * Assume that one (or multiple) resources identified by URLs are available.
     * <p>
     * The assumption comprise the following tests:
     * <ul>
     * <li>that the hostname can be resolved (thus no {@link UnknownHostException} is raised</li>
     * <li>that the status code is in the 100, 200 or 300 family (for HTTP URLs)</li>
     * <li>that the connection can be established in 1 second or less, or a value specified by {@link #timeoutMillis()}</li>
     * </ul>
     * Only a connection is established; the resource itself is not consumed.
     */
    String[] url() default {};

    /**
     * Timeout in milliseconds for the connection attempt if a resource URL is specified via {@link #url()}.
     * <p>
     * The timeout applies to each URL individually.
     */
    int timeoutMillis() default 1000;
}
