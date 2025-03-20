package ubic.gemma.core.security.authentication;

import gemma.gsec.authentication.GroupManager;
import gemma.gsec.authentication.UserDetailsManager;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.model.common.auditAndSecurity.User;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Overrides gsec's UserManager to provide Gemma-specific types.
 * @author poirigui
 */
public interface UserManager extends UserDetailsManager, GroupManager {

    @Secured({ "GROUP_USER", "RUN_AS_ADMIN" })
    User findByEmail( String s ) throws UsernameNotFoundException;

    User findByUserName( String s ) throws UsernameNotFoundException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_USER" })
    Collection<String> findGroupsForUser( String username ) throws UsernameNotFoundException;

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    boolean userWithEmailExists( String emailAddress );

    /**
     * Obtain the {@link User} corresponding to the currently logged in user.
     * @return the user, or null if no user is logged in
     */
    @Nullable
    User getCurrentUser();

    /**
     * Obtain the username of the currently logged in user.
     * <p>
     * If no user is logged in, the principal of the anonymous authentication token is returned.
     */
    @Override
    String getCurrentUsername();

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
