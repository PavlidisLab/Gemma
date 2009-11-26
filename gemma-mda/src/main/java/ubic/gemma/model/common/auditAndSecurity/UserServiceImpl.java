/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserService
 * @author pavlidis
 * @version $Id$
 */
@Service
public class UserServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserServiceBase {

    public void addGroupAuthority( UserGroup group, String authority ) {
        this.getUserGroupDao().addAuthority( group, authority );

    }

    public void addUserToGroup( UserGroup group, User user) {
        group.getGroupMembers().add( user );
        this.getUserGroupDao().update( group );
    }

    public UserGroup create( UserGroup group ) {
        return this.getUserGroupDao().create( group );
    }

    public void delete( User user ) {

        for ( UserGroup group : this.getUserDao().loadGroups( user ) ) {
            group.getGroupMembers().remove( user );
            this.getUserGroupDao().update( group );
        }

        this.getUserDao().remove( user );
    }

    public void delete( UserGroup group ) {
        this.getUserGroupDao().remove( group );
    }

    public UserGroup findGroupByName( String name ) {
        return this.getUserGroupDao().findByUserGroupName( name );
    }

    public Collection<UserGroup> findGroupsForUser( User user ) {
        return this.getUserGroupDao().findGroupsForUser( user );
    }

    @SuppressWarnings("unchecked")
    public Collection<UserGroup> listAvailableGroups() {
        return ( Collection<UserGroup> ) this.getUserGroupDao().loadAll();
    }

    public Collection<GroupAuthority> loadGroupAuthorities( User u ) {
        return this.getUserDao().loadGroupAuthorities( u );
    }

    public void removeGroupAuthority( UserGroup group, String authority ) {
        this.getUserGroupDao().removeAuthority( group, authority );
    }

    public void removeUserFromGroup( User user, UserGroup group ) {
        group.getGroupMembers().remove( user );
        this.getUserGroupDao().update( group );

        /*
         * TODO: if the group is empty, should we delete it? Not if it is GROUP_USER or ADMIN, but perhaps otherwise.
         */
    }

    public void update( UserGroup group ) {
        this.getUserGroupDao().update( group );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#saveUser(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @Override
    protected User handleCreate( ubic.gemma.model.common.auditAndSecurity.User user ) throws UserExistsException {

        if ( user.getUserName() == null ) {
            throw new IllegalArgumentException( "UserName cannot be null" );
        }

        if ( this.getUserDao().findByUserName( user.getUserName() ) != null ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

        if ( this.findByEmail( user.getEmail() ) != null ) {
            throw new UserExistsException( "A user with email address '" + user.getEmail() + "' already exists." );
        }

        try {
            return this.getUserDao().create( user );
        } catch ( DataIntegrityViolationException e ) {
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        } catch ( org.springframework.dao.InvalidDataAccessResourceUsageException e ) {
            // shouldn't happen if we don't have duplicates in the first place...but just in case.
            throw new UserExistsException( "User '" + user.getUserName() + "' already exists!" );
        }

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleFindByEmail(java.lang.String)
     */
    @Override
    protected User handleFindByEmail( String email ) throws Exception {
        return this.getUserDao().findByEmail( email );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleFindByUserName(java.lang.String)
     */
    @Override
    protected User handleFindByUserName( String userName ) throws Exception {
        return this.getUserDao().findByUserName( userName );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#FindById(long)
     */
    @Override
    protected User handleLoad( Long id ) throws java.lang.Exception {
        return this.getUserDao().load( id );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserService#getUsers(ubic.gemma.model.common.auditAndSecurity.User)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected java.util.Collection<User> handleLoadAll() throws java.lang.Exception {
        return ( Collection<User> ) this.getUserDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.auditAndSecurity.UserServiceBase#handleUpdate(ubic.gemma.model.common.auditAndSecurity
     * .User)
     */
    @Override
    protected void handleUpdate( User user ) throws Exception {
        this.getUserDao().update( user );
    }

    public boolean groupExists( String name ) {
        return this.getUserGroupDao().findByUserGroupName( name ) != null;
    }

}
