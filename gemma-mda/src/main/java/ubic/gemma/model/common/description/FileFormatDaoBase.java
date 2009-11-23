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
 * <code>ubic.gemma.model.common.description.FileFormat</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.FileFormat
 */
public abstract class FileFormatDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.common.description.FileFormatDao {

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FileFormat.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.common.description.FileFormat ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#create(int transform,
     *      ubic.gemma.model.common.description.FileFormat)
     */
    public Object create( final int transform, final ubic.gemma.model.common.description.FileFormat fileFormat ) {
        if ( fileFormat == null ) {
            throw new IllegalArgumentException( "FileFormat.create - 'fileFormat' can not be null" );
        }
        this.getHibernateTemplate().save( fileFormat );
        return this.transformEntity( transform, fileFormat );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#create(java.util.Collection)
     */
    
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#create(ubic.gemma.model.common.description.FileFormat)
     */
    public ubic.gemma.model.common.description.FileFormat create(
            ubic.gemma.model.common.description.FileFormat fileFormat ) {
        return ( ubic.gemma.model.common.description.FileFormat ) this.create( TRANSFORM_NONE, fileFormat );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#findByFormatIdentifier(int, java.lang.String)
     */
    
    public Object findByFormatIdentifier( final int transform, final java.lang.String formatIdentifier ) {
        return this.findByFormatIdentifier( transform,
                "from FileFormatImpl ff where ff.formatIdentifier=:formatIdentifier", formatIdentifier );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#findByFormatIdentifier(int, java.lang.String,
     *      java.lang.String)
     */
    
    public Object findByFormatIdentifier( final int transform, final java.lang.String queryString,
            final java.lang.String formatIdentifier ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( formatIdentifier );
        argNames.add( "formatIdentifier" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.FileFormat"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.FileFormat ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#findByFormatIdentifier(java.lang.String)
     */
    public ubic.gemma.model.common.description.FileFormat findByFormatIdentifier( java.lang.String formatIdentifier ) {
        return ( ubic.gemma.model.common.description.FileFormat ) this.findByFormatIdentifier( TRANSFORM_NONE,
                formatIdentifier );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#findByFormatIdentifier(java.lang.String, java.lang.String)
     */
    
    public ubic.gemma.model.common.description.FileFormat findByFormatIdentifier( final java.lang.String queryString,
            final java.lang.String formatIdentifier ) {
        return ( ubic.gemma.model.common.description.FileFormat ) this.findByFormatIdentifier( TRANSFORM_NONE,
                queryString, formatIdentifier );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FileFormat.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.FileFormatImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.FileFormat ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#load(java.lang.Long)
     */
    public ubic.gemma.model.common.description.FileFormat load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.FileFormat ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#loadAll()
     */
    
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.FileFormatImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FileFormat.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.FileFormat entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FileFormat.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#remove(ubic.gemma.model.common.description.FileFormat)
     */
    public void remove( ubic.gemma.model.common.description.FileFormat fileFormat ) {
        if ( fileFormat == null ) {
            throw new IllegalArgumentException( "FileFormat.remove - 'fileFormat' can not be null" );
        }
        this.getHibernateTemplate().delete( fileFormat );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FileFormat.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.FileFormat ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.FileFormatDao#update(ubic.gemma.model.common.description.FileFormat)
     */
    public void update( ubic.gemma.model.common.description.FileFormat fileFormat ) {
        if ( fileFormat == null ) {
            throw new IllegalArgumentException( "FileFormat.update - 'fileFormat' can not be null" );
        }
        this.getHibernateTemplate().update( fileFormat );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.FileFormat)} method. This method does not
     * instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.common.description.FileFormatDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.FileFormat)
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
     * <code>ubic.gemma.model.common.description.FileFormatDao</code>, please note that the {@link #TRANSFORM_NONE}
     * constant denotes no transformation, so the entity itself will be returned. If the integer argument value is
     * unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.common.description.FileFormatDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.common.description.FileFormat entity ) {
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