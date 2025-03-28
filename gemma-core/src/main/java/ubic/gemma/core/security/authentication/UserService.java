package ubic.gemma.core.security.authentication;

import gemma.gsec.authentication.UserExistsException;
import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

/**
 * Override a few definition from gsec so that we can use Gemma-specific implementations safely.
 * <p>
 * Avoid using this service directly, use {@link UserManager} instead.
 * @author poirigui
 */
public interface UserService extends gemma.gsec.authentication.UserService {

    @Override
    User load( Long id );

    @Override
    User findByUserName( String s );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    User findByEmail( String s );

    @Override
    @Secured({ "GROUP_USER", "AFTER_ACL_READ" })
    UserGroup findGroupByName( String s );

    @Override
    @Secured({ "GROUP_ADMIN" })
    User create( gemma.gsec.model.User user ) throws UserExistsException;

    @Override
    @Secured({ "GROUP_USER" })
    UserGroup create( gemma.gsec.model.UserGroup userGroup );
}
