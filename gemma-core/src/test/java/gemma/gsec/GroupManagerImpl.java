package gemma.gsec;

import gemma.gsec.authentication.GroupManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GroupManagerImpl implements GroupManager {

    @Override
    public Collection<String> findGroupsForUser( String username ) throws UsernameNotFoundException {
        return Collections.emptyList();
    }

    @Override
    public boolean groupExists( String name ) {
        return false;
    }

    @Override
    public List<String> findAllGroups() {
        return Collections.emptyList();
    }

    @Override
    public List<String> findUsersInGroup( String groupName ) {
        return Collections.emptyList();
    }

    @Override
    public void createGroup( String groupName, List<GrantedAuthority> authorities ) {

    }

    @Override
    public void deleteGroup( String groupName ) {

    }

    @Override
    public void renameGroup( String oldName, String newName ) {

    }

    @Override
    public void addUserToGroup( String username, String group ) {

    }

    @Override
    public void removeUserFromGroup( String username, String groupName ) {

    }

    @Override
    public List<GrantedAuthority> findGroupAuthorities( String groupName ) {
        return Collections.emptyList();
    }

    @Override
    public void addGroupAuthority( String groupName, GrantedAuthority authority ) {

    }

    @Override
    public void removeGroupAuthority( String groupName, GrantedAuthority authority ) {

    }
}
