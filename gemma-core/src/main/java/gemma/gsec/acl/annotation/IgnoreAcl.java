package gemma.gsec.acl.annotation;

import gemma.gsec.acl.BaseAclAdvice;

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
