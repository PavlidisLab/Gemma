/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.common.auditAndSecurity;

import gemma.gsec.AuthorityConstants;
import org.apache.commons.lang3.ArrayUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroup
 */
@Repository
public class UserGroupDaoImpl extends AbstractDao<UserGroup> implements UserGroupDao {

    private static final String[] PROTECTED_GROUP_NAMES = {
            AuthorityConstants.USER_GROUP_NAME,
            AuthorityConstants.ADMIN_GROUP_NAME,
            AuthorityConstants.AGENT_GROUP_NAME
    };

    @Autowired
    public UserGroupDaoImpl( SessionFactory sessionFactory ) {
        super( UserGroup.class, sessionFactory );
    }

    @Override
    public UserGroup find( UserGroup entity ) {
        if ( entity.getId() != null ) {
            return super.find( entity );
        } else {
            return this.findByName( entity.getName() );
        }
    }

    @Override
    public UserGroup findByName( String name ) {
        return ( UserGroup ) this.getSessionFactory().getCurrentSession()
                .createQuery( "from UserGroup as userGroup where userGroup.name = :name" ).setParameter( "name", name )
                .uniqueResult();
    }

    @Override
    public Collection<UserGroup> findGroupsForUser( User user ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ug from UserGroup ug inner join ug.groupMembers memb where memb = :user" )
                .setParameter( "user", user ).list();
    }

    @Override
    public UserGroup create( final UserGroup userGroup ) {
        Assert.isTrue( !ArrayUtils.contains( PROTECTED_GROUP_NAMES, userGroup.getName() ),
                "Cannot create group with name: " + userGroup.getName() );
        return super.create( userGroup );
    }

    @Override
    public void remove( UserGroup userGroup ) {
        Assert.isTrue( !ArrayUtils.contains( PROTECTED_GROUP_NAMES, userGroup.getName() ),
                "Cannot remove group with name: " + userGroup.getName() );
        super.remove( userGroup );
    }

    @Override
    public void update( UserGroup userGroup ) {
        Assert.isTrue( !ArrayUtils.contains( PROTECTED_GROUP_NAMES, userGroup.getName() ),
                "Cannot update group with name: " + userGroup.getName() );
        super.update( userGroup );
    }
}