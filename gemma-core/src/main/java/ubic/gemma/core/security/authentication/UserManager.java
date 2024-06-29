package ubic.gemma.core.security.authentication;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Collection;

/**
 * Overrides gsec's UserManager to provide Gemma-specific types.
 * @author poirigui
 */
public interface UserManager extends gemma.gsec.authentication.UserManager {

    /**
     * @deprecated Use {@link #findByEmail(String)}, this is a typo in gsec.
     */
    @Deprecated
    @Override
    @Secured({ "GROUP_USER", "RUN_AS_ADMIN" })
    User findbyEmail( String emailAddress ) throws UsernameNotFoundException;

    @Override
    @Secured({ "GROUP_USER", "RUN_AS_ADMIN" })
    User findByEmail( String s ) throws UsernameNotFoundException;

    @Override
    User findByUserName( String s ) throws UsernameNotFoundException;

    @Override
    @Nullable
    UserGroup findGroupByName( String s );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_USER" })
    Collection<String> findGroupsForUser( String username ) throws UsernameNotFoundException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    boolean userWithEmailExists( String emailAddress );

    @Override
    @Nullable
    User getCurrentUser();

    @Override
    @Secured("GROUP_ADMIN")
    Collection<User> loadAll();

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    boolean userExists( String username );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    void createUser( UserDetails user );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    void updateUser( UserDetails user );

    /**
     * Update the groups a user belong to.
     */
    @Secured({ "GROUP_ADMIN" })
    void updateUserGroups( UserDetails user, Collection<String> groups );

    @Override
    @Secured("GROUP_ADMIN")
    void deleteUser( String username );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    String changePasswordForUser( String email, String username, String newPassword );

    @Override
    @Secured("GROUP_USER")
    void changePassword( String oldPassword, String newPassword );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    boolean validateSignupToken( String username, String key );
}
