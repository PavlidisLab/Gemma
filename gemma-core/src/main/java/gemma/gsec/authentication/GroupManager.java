package gemma.gsec.authentication;

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
}
