package ubic.gemma.core.security.authentication;

import gemma.gsec.authentication.UserExistsException;
import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Collection;

/**
 * Override a few definition from gsec so that we can use Gemma-specific implementations safely.
 * @author poirigui
 */
public interface UserService extends gemma.gsec.authentication.UserService {

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
