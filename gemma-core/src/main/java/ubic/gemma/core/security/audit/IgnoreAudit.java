package ubic.gemma.core.security.audit;

import java.lang.annotation.*;

/**
 * Mark a DAO method as ignored for auditing.
 * @author guillaume
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreAudit {
}
