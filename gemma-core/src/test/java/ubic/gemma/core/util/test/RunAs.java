package ubic.gemma.core.util.test;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.*;

/**
 * Indicate the role that a test method or class executes with.
 * <p>
 * This is very similar to {@link org.springframework.security.test.context.support.WithMockUser}, but supports roles
 * that are common in Gemma.
 * @see RunAsSecurityContextFactory
 * @see org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@WithSecurityContext(factory = RunAsSecurityContextFactory.class)
public @interface RunAs {

    Role value();

    enum Role {
        ADMIN,
        AGENT,
        ANONYMOUS
    }
}
