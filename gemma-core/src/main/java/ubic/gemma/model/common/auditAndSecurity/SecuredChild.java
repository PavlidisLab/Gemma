package ubic.gemma.model.common.auditAndSecurity;

import gemma.gsec.acl.domain.AclObjectIdentity;

import javax.annotation.Nullable;
import javax.persistence.Transient;

/**
 *
 * @param <OT> type of the security owner
 */
public interface SecuredChild<OT extends Securable> extends Securable, gemma.gsec.model.SecuredChild {

    /**
     * Obtain the security owner of this secured child.
     * <p>
     * Secured children should always have an owner in the ACL table, but that relation is not always direct or mapped.
     * A more robust way of checking is to look up the parent via {@link AclObjectIdentity#getParentObject()}. If the
     * security owner cannot be determined, the implementation should define a setter so it can be temporarily assigned
     * for creating the ACL identity.
     *
     * @return the security owner, or null if not known.
     */
    @Nullable
    @Override
    @Transient
    OT getSecurityOwner();
}
