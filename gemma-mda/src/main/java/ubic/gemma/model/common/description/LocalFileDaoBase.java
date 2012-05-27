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
package ubic.gemma.model.common.description;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.LocalFile</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.LocalFile
 */
public abstract class LocalFileDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        ubic.gemma.model.common.description.LocalFileDao {

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "LocalFile.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.description.LocalFile ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#create(int transform,
     *      ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public Object create( final int transform, final ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null ) {
            throw new IllegalArgumentException( "LocalFile.create - 'localFile' can not be null" );
        }
        this.getHibernateTemplate().save( localFile );
        return this.transformEntity( transform, localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#create(java.util.Collection)
     */
    
    @Override
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#create(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile create( ubic.gemma.model.common.description.LocalFile localFile ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.create( TRANSFORM_NONE, localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#find(int, java.lang.String,
     *      ubic.gemma.model.common.description.LocalFile)
     */
    
    @Override
    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( localFile );
        argNames.add( "localFile" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.LocalFile ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#find(int, ubic.gemma.model.common.description.LocalFile)
     */ 
    @Override
    public Object find( final int transform, final ubic.gemma.model.common.description.LocalFile localFile ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.common.description.LocalFile as localFile where localFile.localFile = :localFile",
                        localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#find(java.lang.String,
     *      ubic.gemma.model.common.description.LocalFile)
     */ 
    @Override
    public ubic.gemma.model.common.description.LocalFile find( final java.lang.String queryString,
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.find( TRANSFORM_NONE, queryString, localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#find(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile find( ubic.gemma.model.common.description.LocalFile localFile ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.find( TRANSFORM_NONE, localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(int, java.lang.String, java.net.URL,
     *      java.lang.Long)
     */
    
    @Override
    public Object findByLocalURL( final int transform, final java.lang.String queryString, final java.net.URL url,
            final java.lang.Long size ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( url );
        argNames.add( "url" );
        args.add( size );
        argNames.add( "size" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null; 
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            } 
        result = transformEntity( transform, ( ubic.gemma.model.common.description.LocalFile ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(int, java.net.URL, java.lang.Long)
     */ 
    @Override
    public Object findByLocalURL( final int transform, final java.net.URL url, final java.lang.Long size ) {
        return this
                .findByLocalURL(
                        transform,
                        "from ubic.gemma.model.common.description.LocalFile as localFile where localFile.url = :url and localFile.size = :size",
                        url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(java.lang.String, java.net.URL,
     *      java.lang.Long)
     */ 
    @Override
    public ubic.gemma.model.common.description.LocalFile findByLocalURL( final java.lang.String queryString,
            final java.net.URL url, final java.lang.Long size ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findByLocalURL( TRANSFORM_NONE, queryString, url,
                size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByLocalURL(java.net.URL, java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findByLocalURL( java.net.URL url, java.lang.Long size ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findByLocalURL( TRANSFORM_NONE, url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(int, java.lang.String, java.net.URL,
     *      java.lang.Long)
     */
    
    @Override
    public Object findByRemoteURL( final int transform, final java.lang.String queryString, final java.net.URL url,
            final java.lang.Long size ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( url );
        argNames.add( "url" );
        args.add( size );
        argNames.add( "size" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null; 
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            } 
        result = transformEntity( transform, ( ubic.gemma.model.common.description.LocalFile ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(int, java.net.URL, java.lang.Long)
     */ 
    @Override
    public Object findByRemoteURL( final int transform, final java.net.URL url, final java.lang.Long size ) {
        return this
                .findByRemoteURL(
                        transform,
                        "from ubic.gemma.model.common.description.LocalFile as localFile where localFile.url = :url and localFile.size = :size",
                        url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(java.lang.String, java.net.URL,
     *      java.lang.Long)
     */ 
    @Override
    public ubic.gemma.model.common.description.LocalFile findByRemoteURL( final java.lang.String queryString,
            final java.net.URL url, final java.lang.Long size ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findByRemoteURL( TRANSFORM_NONE, queryString,
                url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findByRemoteURL(java.net.URL, java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findByRemoteURL( java.net.URL url, java.lang.Long size ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findByRemoteURL( TRANSFORM_NONE, url, size );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.common.description.LocalFile)
     */
    
    @Override
    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( localFile );
        argNames.add( "localFile" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null; 
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.common.description.LocalFile"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            } 
        result = transformEntity( transform, ( ubic.gemma.model.common.description.LocalFile ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findOrCreate(int,
     *      ubic.gemma.model.common.description.LocalFile)
     */ 
    @Override
    public Object findOrCreate( final int transform, final ubic.gemma.model.common.description.LocalFile localFile ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.common.description.LocalFile as localFile where localFile.localFile = :localFile",
                        localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.common.description.LocalFile)
     */ 
    @Override
    public ubic.gemma.model.common.description.LocalFile findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.common.description.LocalFile localFile ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findOrCreate( TRANSFORM_NONE, queryString,
                localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#findOrCreate(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile findOrCreate(
            ubic.gemma.model.common.description.LocalFile localFile ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.findOrCreate( TRANSFORM_NONE, localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "LocalFile.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.common.description.LocalFileImpl.class,
                id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.LocalFile ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.LocalFile load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.LocalFile ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#loadAll()
     */
    
    @Override
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.LocalFileImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "LocalFile.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.LocalFile entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "LocalFile.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#remove(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public void remove( ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null ) {
            throw new IllegalArgumentException( "LocalFile.remove - 'localFile' can not be null" );
        }
        this.getHibernateTemplate().delete( localFile );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "LocalFile.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.LocalFile ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.LocalFileDao#update(ubic.gemma.model.common.description.LocalFile)
     */
    @Override
    public void update( ubic.gemma.model.common.description.LocalFile localFile ) {
        if ( localFile == null ) {
            throw new IllegalArgumentException( "LocalFile.update - 'localFile' can not be null" );
        }
        this.getHibernateTemplate().update( localFile );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.LocalFile)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.description.LocalFileDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.LocalFile)
     */
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.common.description.LocalFileDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.description.LocalFileDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.description.LocalFile entity ) {
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