package ubic.gemma.core.security;

import ubic.gemma.core.security.authentication.UserExistsException;
import ubic.gemma.core.security.authentication.BaseUserService;
import ubic.gemma.core.security.model.GroupAuthority;
import ubic.gemma.core.security.model.User;
import ubic.gemma.core.security.model.UserGroup;

import java.util.Collection;
import java.util.Collections;

public class UserServiceImpl implements BaseUserService {
    @Override
    public void addGroupAuthority( UserGroup group, String authority ) {

    }

    @Override
    public void addUserToGroup( UserGroup group, User user ) {

    }

    @Override
    public User create( User user ) throws UserExistsException {
        return null;
    }

    @Override
    public UserGroup create( UserGroup group ) {
        return null;
    }

    @Override
    public void delete( User user ) {

    }

    @Override
    public void delete( UserGroup group ) {

    }

    @Override
    public User findByEmail( String email ) {
        return null;
    }

    @Override
    public User findByUserName( String userName ) {
        return null;
    }

    @Override
    public UserGroup findGroupByName( String name ) {
        return null;
    }

    @Override
    public Collection<UserGroup> findGroupsForUser( User user ) {
        return Collections.emptyList();
    }

    @Override
    public boolean groupExists( String name ) {
        return false;
    }

    @Override
    public Collection<UserGroup> listAvailableGroups() {
        return Collections.emptyList();
    }

    @Override
    public User load( Long id ) {
        return null;
    }

    @Override
    public Collection<User> loadAll() {
        return Collections.emptyList();
    }

    @Override
    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        return Collections.emptyList();
    }

    @Override
    public void removeGroupAuthority( UserGroup group, String authority ) {

    }

    @Override
    public void removeUserFromGroup( User user, UserGroup group ) {

    }

    @Override
    public void update( User user ) {

    }

    @Override
    public void update( UserGroup group ) {

    }
}
