package ubic.gemma.core.security.authorization.acl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.acls.afterinvocation.AclEntryAfterInvocationProvider;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.AclService;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * After-invocation provider for the {@code AFTER_ACL_READ_QUIET} config attribute: same ACL
 * check as the stock {@link AclEntryAfterInvocationProvider} (READ or ADMINISTRATION on the
 * returned single domain object), but {@linkplain AccessDeniedException denial} is converted
 * to a {@code null} return value rather than propagated.
 * <p>
 * Replaces {@code gemma.gsec.acl.afterinvocation.AclEntryAfterInvocationProvider} (the gsec
 * subclass with {@code quiet=true}) as part of the Phase 3 AfterInvocation modernization
 * (Phase B). Behaviorally identical to the gsec provider but lives in gemma-core so we can
 * retire the gsec class without touching the 17 {@code @Secured(..., "AFTER_ACL_READ_QUIET")}
 * call sites. Required because the modern {@code @PostAuthorize} annotation can only allow
 * or throw — it has no "return null on denial" mode — and Gemma's web/REST controllers
 * pervasively rely on the null return to distinguish "not found / not visible" from "found
 * but access denied" (the latter maps to HTTP 403, the former to 404 via
 * {@code EntityNotFoundException}).
 *
 * @see AclEntryAfterInvocationProvider the stock single-object check (READ + ADMINISTRATION)
 * @see ubic.gemma.core.security.MethodSecurityConfig wiring registration
 */
public class AclEntryAfterInvocationQuietReadProvider extends AclEntryAfterInvocationProvider {

    private static final Log log = LogFactory.getLog( AclEntryAfterInvocationQuietReadProvider.class );

    /**
     * The single config attribute string this provider responds to. Must match the value
     * used in {@code @Secured({..., "AFTER_ACL_READ_QUIET"})} annotations across the
     * codebase.
     */
    public static final String ATTRIBUTE = "AFTER_ACL_READ_QUIET";

    /**
     * Default permission set: READ or ADMINISTRATION (any one suffices). Matches the wiring
     * gsec used for the {@code afterAclReadQuiet} bean and the inline SpEL used by Phase A's
     * {@code @PostAuthorize} migrations.
     */
    public static final List<Permission> DEFAULT_PERMISSIONS = Arrays.asList(
            BasePermission.ADMINISTRATION,
            BasePermission.READ
    );

    public AclEntryAfterInvocationQuietReadProvider( AclService aclService ) {
        this( aclService, DEFAULT_PERMISSIONS );
    }

    public AclEntryAfterInvocationQuietReadProvider( AclService aclService, List<Permission> requirePermission ) {
        super( aclService, ATTRIBUTE, requirePermission );
    }

    @Override
    public Object decide( Authentication authentication, Object object, Collection<ConfigAttribute> config, Object returnedObject )
            throws AccessDeniedException {
        try {
            return super.decide( authentication, object, config, returnedObject );
        } catch ( AccessDeniedException ade ) {
            // Only swallow the denial if THIS provider's attribute is actually present in
            // the @Secured list. If the attribute belongs to a sibling provider that
            // happens to share supports() — there shouldn't be any in practice — let it
            // throw. This mirrors the guard the gsec quiet provider applied.
            for ( ConfigAttribute attr : config ) {
                if ( ATTRIBUTE.equals( attr.getAttribute() ) ) {
                    if ( log.isDebugEnabled() ) {
                        log.debug( "Access denied on " + returnedObject + "; returning null per AFTER_ACL_READ_QUIET semantics" );
                    }
                    return null;
                }
            }
            throw ade;
        }
    }
}
