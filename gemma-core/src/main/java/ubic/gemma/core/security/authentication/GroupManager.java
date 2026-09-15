package ubic.gemma.core.security.authentication;

import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;

/**
 * Extension of {@link org.springframework.security.provisioning.GroupManager}.
 *
 * @author poirigui
 */
public interface GroupManager extends org.springframework.security.provisioning.GroupManager {

    /**
     * Find all the group a user is in.
     */
    Collection<String> findGroupsForUser( String username ) throws UsernameNotFoundException;

    /**
     * Check if a group with a given name exists.
     */
    boolean groupExists( String name );

    /**
     * Update the human-readable description of a group. Spring's {@link org.springframework.security.provisioning.GroupManager}
     * has no equivalent — the description field is Gemma-specific (on the {@code UserGroup} entity).
     *
     * @param groupName   the group to update (looked up by name).
     * @param description new description; {@code null} clears the field.
     */
    void setGroupDescription( String groupName, @org.springframework.lang.Nullable String description );
}
