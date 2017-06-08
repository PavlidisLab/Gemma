/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.auditAndSecurity.UserGroup</code>.
 * </p>
 *
 * @see ubic.gemma.model.common.auditAndSecurity.UserGroup
 */
public abstract class UserGroupDaoBase extends HibernateDaoSupport implements UserGroupDao {

    @Override
    public Collection<UserGroup> create( final Collection<UserGroup> entities ) {
        for ( UserGroup e : entities ) {
            this.create( e );
        }
        return entities;
    }

    @Override
    public UserGroup findByName( final String name ) {
        return ( UserGroup ) this.getSession().createQuery( "from UserGroup as userGroup where userGroup.name = :name" )
                .setParameter( "name", name ).uniqueResult();
    }

    @Override
    public Collection<UserGroup> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSession().createQuery( "from UserGroup where id in (:ids)" ).setParameterList( "ids", ids )
                .list();
    }

    @Override
    public UserGroup load( final Long id ) {
        return ( UserGroup ) this.getSession().get( UserGroup.class, id );
    }

    @Override
    public Collection<UserGroup> loadAll() {
        //noinspection unchecked
        return this.getSession().createCriteria( UserGroup.class ).list();
    }

    @Override
    public void remove( Collection<UserGroup> entities ) {
        for ( UserGroup e : entities ) {
            this.getSession().delete( e );
        }
    }

    @Override
    public void update( final Collection<UserGroup> entities ) {
        for ( UserGroup entity : entities ) {
            update( entity );
        }
    }

    @Override
    public void update( UserGroup entity ) {
        this.getSession().update( entity );
    }
}