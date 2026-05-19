/**
 * Gsec is an attempt to generalise the security framework of Gemma to make it usable in other projects such as
 * ASPIREdb.
 * <p>
 * It is based on Spring Security, with several extensions (and some things it does not support).
 * <p>
 * Top-level concerns:
 * <ul>
 * <li>Adding ACLs to entities at creation time.
 * <li>Entity and Method security.
 * <li>Managing security on entities
 * </ul>
 * <p>
 * It does not address authentication and user management. However, it does define some standard group authorities:
 * ADMIN, USER, ANONYMOUS, AGENT and applies appropriate permissions to these to objects
 *
 * <h2>Using Gsec</h2>
 * <p>
 * Use Gemma as an example for how it is set up.
 * <p>
 * Include the Acl*hbm.xml hibernate configuration files in your SessionFactory declaration.
 * <p>
 * Use those files in your DDL generation to add the tables to your database.
 * <p>
 * Run the SQL in init-acl-indices.sql to further tune the Acl tables.
 * <p>
 * Subclass BaseAclAdvice and wire into your CRUD methods. For example Gemma uses the
 * SystemArchitectureAspect.modifier() pointcut.
 * <p>
 * Include the applicationContext-component-scan.xml bean definitions file in your context configuration.
 * <p>
 * Any other security configuration will have to be provided in additional bean declarations.
 *
 * <h2>The Gsec secure entity data model</h2>
 * <p>
 * We define four types of Secure entities (Secure means they are subject to authorization checks), which are indicated
 * by implementing one of four interfaces:
 * <ul>
 * <li>Securable, which is the generic top-level interface. Secure objects have an owner, and may or may not have
 * associated objects which inherit security from the parent. Similarly Secure objects can have a parent, from which
 * they will inherit permissions if the parent is defined.
 * <li>SecuredChild, which are objects that never have their own specific permissions, but inherit them from a
 * Securable. SecuredChildren have ACLs, but no ACEs (at least, if they exist they are not used)
 * <li>SecuredNotChild, which denotes objects that have their own security and never inherit from an associated parent
 * object.
 * <li>SecureValueObject, which allows adding security filtering to methods that return value objects instead of actual
 * entities.
 * </ul>
 *
 * <h2>Adding ACLs to entities at creation time</h2>
 * <p>
 * Applications wire an implementation of BaseAclAdvice to their methods that create or update Securables.
 * </p>
 * <h2>Method security</h2>
 * <p>
 * Gsec provides a range of implementations of AbstractAclProvider to filter the return objects coming from @Secured
 * methods, including support for value objects, filtering maps by values or by keys, etc.
 * <h2>Managing security on entities</h2>
 * <p>
 * Gsec defines a SecurityService interface for various manipulations and checking of permissions on Securables.
 * Currently no implementation is provided (awaiting refactoring out of Gemma).
 * <p>
 * Gsec also provides some utility methods in SecurityUtil.
 *
 * <h2>TODO</h2>
 * <ul>
 * <li>The special AclPrincipalSid and AclGrantedAuthoritySid might not be needed, to be replaced with the generic
 * PrincipalSid and GrantedAuthoritySid from spring security.
 * This would also mean some of the other classes can be reverted to the generic ones provided from spring
 * security.</li>
 * </ul>
 */
@ParametersAreNonnullByDefault
package gemma.gsec;

import javax.annotation.ParametersAreNonnullByDefault;