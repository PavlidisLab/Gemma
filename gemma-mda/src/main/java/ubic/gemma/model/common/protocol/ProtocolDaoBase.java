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
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.protocol.Protocol</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.protocol.Protocol
 */
public abstract class ProtocolDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.protocol.ProtocolDao {

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#create(int, java.util.Collection)
     */

    public java.util.Collection<Protocol> create( final int transform, final java.util.Collection<Protocol> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Protocol.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<Protocol> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( transform, entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }
    
    
    @Override
    public Collection<? extends Protocol > load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProtocolImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#create(int transform,
     *      ubic.gemma.model.common.protocol.Protocol)
     */
    public Object create( final int transform, final ubic.gemma.model.common.protocol.Protocol protocol ) {
        if ( protocol == null ) {
            throw new IllegalArgumentException( "Protocol.create - 'protocol' can not be null" );
        }
        this.getHibernateTemplate().save( protocol );
        return this.transformEntity( transform, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#create(java.util.Collection)
     */

    
    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#create(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    public Protocol create( ubic.gemma.model.common.protocol.Protocol protocol ) {
        return ( ubic.gemma.model.common.protocol.Protocol ) this.create( TRANSFORM_NONE, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#find(int, java.lang.String,
     *      ubic.gemma.model.common.protocol.Protocol)
     */
    
    public Protocol find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.protocol.Protocol protocol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( protocol );
        argNames.add( "protocol" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.protocol.Protocol"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.protocol.Protocol ) result );
        return ( Protocol ) result;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#find(int, ubic.gemma.model.common.protocol.Protocol)
     */
    public Protocol find( final int transform, final ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.find( transform,
                "from ubic.gemma.model.common.protocol.Protocol as protocol where protocol.protocol = :protocol",
                protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#find(java.lang.String,
     *      ubic.gemma.model.common.protocol.Protocol)
     */
    public ubic.gemma.model.common.protocol.Protocol find( final java.lang.String queryString,
            final ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.find( TRANSFORM_NONE, queryString, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#find(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    public ubic.gemma.model.common.protocol.Protocol find( ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.find( TRANSFORM_NONE, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.common.protocol.Protocol)
     */
    
    public Protocol findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.protocol.Protocol protocol ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( protocol );
        argNames.add( "protocol" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.protocol.Protocol"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.protocol.Protocol ) result );
        return ( Protocol ) result;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#findOrCreate(int, ubic.gemma.model.common.protocol.Protocol)
     */
    public Protocol findOrCreate( final int transform, final ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.findOrCreate( transform,
                "from ubic.gemma.model.common.protocol.Protocol as protocol where protocol.protocol = :protocol",
                protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.common.protocol.Protocol)
     */
    public ubic.gemma.model.common.protocol.Protocol findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.findOrCreate( TRANSFORM_NONE, queryString, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#findOrCreate(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    public ubic.gemma.model.common.protocol.Protocol findOrCreate( ubic.gemma.model.common.protocol.Protocol protocol ) {
        return this.findOrCreate( TRANSFORM_NONE, protocol );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Protocol.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.common.protocol.ProtocolImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.protocol.Protocol ) entity );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#load(java.lang.Long)
     */

    @Override
    public Protocol load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.protocol.Protocol ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#loadAll()
     */

    
    @Override
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#loadAll(int)
     */

    
    public java.util.Collection<? extends Protocol> loadAll( final int transform ) {
        final java.util.Collection<? extends Protocol> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.protocol.ProtocolImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Protocol.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.protocol.Protocol entity = this.load( id );
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
     * @see ubic.gemma.model.common.protocol.ProtocolDao#remove(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    public void remove( ubic.gemma.model.common.protocol.Protocol protocol ) {
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
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends Protocol> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.protocol.ProtocolDao#update(ubic.gemma.model.common.protocol.Protocol)
     */
    @Override
    public void update( ubic.gemma.model.common.protocol.Protocol protocol ) {
        if ( protocol == null ) {
            throw new IllegalArgumentException( "Protocol.update - 'protocol' can not be null" );
        }
        this.getHibernateTemplate().update( protocol );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.protocol.Protocol)} method. This method does not instantiate
     * a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.protocol.ProtocolDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.protocol.Protocol)
     */

    protected void transformEntities( final int transform, final java.util.Collection<? extends Protocol> entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.protocol.ProtocolDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.protocol.ProtocolDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.protocol.Protocol entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}