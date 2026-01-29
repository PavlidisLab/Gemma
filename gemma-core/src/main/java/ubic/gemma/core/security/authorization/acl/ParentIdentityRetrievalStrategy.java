package ubic.gemma.core.security.authorization.acl;

import org.springframework.security.acls.model.ObjectIdentity;

import javax.annotation.Nullable;

/**
 * Strategy for locating parent ACL identities.
 *
 * @author poirigui
 */
public interface ParentIdentityRetrievalStrategy {

    /**
     * Obtain the parent ACL identity for the given ACL identity.
     *
     * @return the parent ACL identity if it can be determined, null otherwise
     */
    @Nullable
    ObjectIdentity getParentIdentity( ObjectIdentity aoi );
}
