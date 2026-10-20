package ubic.gemma.core.security.authentication;

import ubic.gemma.core.security.authentication.GroupManager;
import ubic.gemma.core.security.authentication.UserDetailsImpl;
import ubic.gemma.core.security.authentication.UserDetailsManager;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import ubic.gemma.model.common.auditAndSecurity.User;

import org.springframework.lang.Nullable;
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

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    UserDetailsImpl createUser( String username, String email, String password );

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

    /**
     * Mark the named user as deleted without removing the row. Sets
     * {@code deletedAt} = now, {@code deletedBy} = the supplied admin username,
     * and {@code enabled} = false. ACL sids, audit-event authorship FKs, and
     * other references to the row are preserved. Hard delete remains available
     * via {@link #deleteUser(String)} for the rare cases where the row truly
     * has no dependents.
     */
    @Secured("GROUP_ADMIN")
    void softDeleteUser( String username, String deletedByUsername );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    String changePasswordForUser( String email, String username, String newPassword );

    @Override
    @Secured("GROUP_USER")
    void changePassword( String oldPassword, String newPassword );

    /**
     * Administrative password reset: set a new password for the named user without
     * requiring their current password. Distinct from {@link #changePasswordForUser(String, String, String)}
     * (the email-confirmation reset flow, which disables the account and issues a
     * signup token) — this leaves the account enabled and immediately usable. The
     * new password is encoded before storage.
     */
    @Secured("GROUP_ADMIN")
    void adminChangePassword( String username, String newPassword );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "RUN_AS_ADMIN" })
    boolean validateSignupToken( String username, String key );
}
