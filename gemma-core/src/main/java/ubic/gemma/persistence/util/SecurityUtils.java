package ubic.gemma.persistence.util;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.acl.domain.AclEntry;
import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclPrincipalSid;
import gemma.gsec.util.SecurityUtil;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.Sid;

public class SecurityUtils {

    /**
     * Checks ACL related properties from the AclObjectIdentity.
     * Some of the code is adapted from {@link gemma.gsec.util.SecurityUtil}, but allows usage without an Acl object.
     *
     * @param aoi the acl object identity of an object whose permissions are to be checked.
     * @return an array of booleans that represent permissions of currently logged in user as follows:
     * <ol>
     * <li>is object public</li>
     * <li>can user write to object</li>
     * <li>is object shared</li>
     * </ol>
     * (note that actual indexing in the array starts at 0).
     */
    public static boolean[] getPermissions( AclObjectIdentity aoi ) {
        boolean isPublic = false;
        boolean canWrite = false;
        boolean isShared = false;

        for ( AclEntry ace : aoi.getEntries() ) {
            if ( SecurityUtil.isUserAdmin() ) {
                canWrite = true;
            } else if ( SecurityUtil.isUserAnonymous() ) {
                canWrite = false;
            } else {
                if ( ( ace.getMask() & BasePermission.WRITE.getMask() ) != 0 || ( ace.getMask() & BasePermission.ADMINISTRATION.getMask() ) != 0 ) {
                    Sid sid = ace.getSid();
                    if ( sid instanceof AclGrantedAuthoritySid ) {
                        //noinspection unused //FIXME if user is in granted group then he can write probably
                        String grantedAuthority = ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority();
                    } else if ( sid instanceof AclPrincipalSid ) {
                        if ( ( ( AclPrincipalSid ) sid ).getPrincipal().equals( SecurityUtil.getCurrentUsername() ) ) {
                            canWrite = true;
                        }
                    }
                }
            }

            // Check public and shared - code adapted from SecurityUtils, only we do not hold an ACL object.
            if ( ( ace.getMask() & BasePermission.READ.getMask() ) != 0 ) {
                Sid sid = ace.getSid();
                if ( sid instanceof AclGrantedAuthoritySid ) {
                    String grantedAuthority = ( ( AclGrantedAuthoritySid ) sid ).getGrantedAuthority();

                    if ( grantedAuthority.equals( AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) && ace
                            .isGranting() ) {
                        isPublic = true;
                    }
                    if ( grantedAuthority.startsWith( "GROUP_" ) && ace.isGranting() ) {
                        if ( !grantedAuthority.equals( AuthorityConstants.AGENT_GROUP_AUTHORITY ) && !grantedAuthority
                                .equals( AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) {
                            isShared = true;
                        }
                    }
                }
            }
        }

        return new boolean[] { isPublic, canWrite, isShared };
    }
}
