package ubic.gemma.core.security.gsec.acl.annotation;

import ubic.gemma.core.security.gsec.acl.BaseAclAdvice;

import java.lang.annotation.*;

/**
 * Mark a method as ignored for ACL.
 * @see BaseAclAdvice
 * @author poirigui
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreAcl {
}
