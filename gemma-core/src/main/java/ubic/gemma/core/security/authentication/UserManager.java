package ubic.gemma.core.security.authentication;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Collection;

/**
 * Overrides gsec's UserManager to provide Gemma-specific types.
 * @author poirigui
 */
public interface UserManager extends gemma.gsec.authentication.UserManager {

    @Override
    User findByEmail( String s ) throws UsernameNotFoundException;

    @Override
    User findByUserName( String s ) throws UsernameNotFoundException;

    @Override
    UserGroup findGroupByName( String s );

    @Override
    User getCurrentUser();

    @Override
    Collection<User> loadAll();
}
