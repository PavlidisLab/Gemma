package ubic.gemma.core.security.authorization.acl;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration for the ACL linter.
 *
 * @author poirigui
 */
@Getter
@Builder
public class AclLinterConfig {
    /**
     * Check for ACL entries that point to non-existing securables.
     */
    boolean lintDanglingIdentities;
    /**
     * Check for securables that lack ACL entries.
     */
    boolean lintSecurablesLackingIdentities;
    /**
     * Check for {@link ubic.gemma.model.common.auditAndSecurity.SecuredChild} that lack a parent ACL entry.
     */
    boolean lintChildWithoutParent;
    /**
     * Check for {@link ubic.gemma.model.common.auditAndSecurity.SecuredNotChild} that have a parent ACL entry.
     */
    boolean lintNotChildWithParent;
    /**
     * Check for missing or incorrect permissions on ACL entries.
     * <p>
     * There should be at least a GROUP_ADMIN entry with complete permission and granting capability.
     */
    boolean lintPermissions;
    /**
     * If true, attempt to fix any issues are found.
     */
    boolean applyFixes;
}
