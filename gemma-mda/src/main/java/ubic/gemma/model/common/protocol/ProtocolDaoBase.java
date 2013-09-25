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
package ubic.gemma.model.common.protocol;

import java.util.Collection;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Protocol</code>.
 * 
 * @see Protocol
 */
public abstract class ProtocolDaoBase extends HibernateDaoSupport implements ProtocolDao {

    /**
     * @see ProtocolDao#create(int, java.util.Collection)
     */

    @Override
    public java.util.Collection<? extends Protocol> create( final Collection<? extends Protocol> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Protocol.create - 'entities' can not be null" );
        }

        for ( java.util.Iterator<? extends Protocol> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            create( entityIterator.next() );
        }
        return entities;
    }

    @Override
    public Collection<? extends Protocol> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProtocolImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ProtocolDao#create(int transform, Protocol)
     */
    @Override
    public Protocol create( Protocol protocol ) {
        if ( protocol == null ) {
            throw new IllegalArgumentException( "Protocol.create - 'protocol' can not be null" );
        }
        this.getHibernateTemplate().save( protocol );
        return protocol;
    }

    /**
     * @see ProtocolDao#load(int, java.lang.Long)
     */

    @Override
    public Protocol load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Protocol.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ProtocolImpl.class, id );
        return ( Protocol ) entity;
    }

    /**
     * @see ProtocolDao#loadAll(int)
     */

    @Override
    public java.util.Collection<? extends Protocol> loadAll() {
        final java.util.Collection<? extends Protocol> results = this.getHibernateTemplate().loadAll(
                ProtocolImpl.class );
        return results;
    }

    /**
     * @see ProtocolDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Protocol.remove - 'id' can not be null" );
        }
        Protocol entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends Protocol> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Protocol.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ProtocolDao#remove(Protocol)
     */
    @Override
    public void remove( Protocol protocol ) {
        if ( protocol == null ) {
            throw new IllegalArgumentException( "Protocol.remove - 'protocol' can not be null" );
        }
        this.getHibernateTemplate().delete( protocol );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection<? extends Protocol> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Protocol.update - 'entities' can not be null" );
        }

        for ( java.util.Iterator<? extends Protocol> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            update( entityIterator.next() );
        }

    }

    /**
     * @see ProtocolDao#update(Protocol)
     */
    @Override
    public void update( Protocol protocol ) {
        if ( protocol == null ) {
            throw new IllegalArgumentException( "Protocol.update - 'protocol' can not be null" );
        }
        this.getHibernateTemplate().update( protocol );
    }

}