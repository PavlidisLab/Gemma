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
package ubic.gemma.model.genome;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Chromosome</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.Chromosome
 */
public abstract class ChromosomeDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport
        implements ubic.gemma.model.genome.ChromosomeDao {

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.Chromosome ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#create(int transform, ubic.gemma.model.genome.Chromosome)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.create - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().save( chromosome );
        return this.transformEntity( transform, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#create(ubic.gemma.model.genome.Chromosome)
     */
    public ubic.gemma.model.genome.Chromosome create( ubic.gemma.model.genome.Chromosome chromosome ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.create( TRANSFORM_NONE, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#find(int, java.lang.String, ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Chromosome chromosome ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( chromosome );
        argNames.add( "chromosome" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.genome.Chromosome"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.genome.Chromosome ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#find(int, ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final ubic.gemma.model.genome.Chromosome chromosome ) {
        return this.find( transform,
                "from ubic.gemma.model.genome.Chromosome as chromosome where chromosome.chromosome = :chromosome",
                chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#find(java.lang.String, ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.genome.Chromosome find( final java.lang.String queryString,
            final ubic.gemma.model.genome.Chromosome chromosome ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.find( TRANSFORM_NONE, queryString, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#find(ubic.gemma.model.genome.Chromosome)
     */
    public ubic.gemma.model.genome.Chromosome find( ubic.gemma.model.genome.Chromosome chromosome ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.find( TRANSFORM_NONE, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.Chromosome chromosome ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( chromosome );
        argNames.add( "chromosome" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.genome.Chromosome"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.genome.Chromosome ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#findOrCreate(int, ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform, final ubic.gemma.model.genome.Chromosome chromosome ) {
        return this.findOrCreate( transform,
                "from ubic.gemma.model.genome.Chromosome as chromosome where chromosome.chromosome = :chromosome",
                chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#findOrCreate(java.lang.String, ubic.gemma.model.genome.Chromosome)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.genome.Chromosome findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.Chromosome chromosome ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.findOrCreate( TRANSFORM_NONE, queryString, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#findOrCreate(ubic.gemma.model.genome.Chromosome)
     */
    public ubic.gemma.model.genome.Chromosome findOrCreate( ubic.gemma.model.genome.Chromosome chromosome ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.findOrCreate( TRANSFORM_NONE, chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#load(int, java.lang.Long)
     */
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Chromosome.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.ChromosomeImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.Chromosome ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#load(java.lang.Long)
     */
    public ubic.gemma.model.genome.Chromosome load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.Chromosome ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#loadAll()
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#loadAll(int)
     */
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.ChromosomeImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#remove(java.lang.Long)
     */
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.Chromosome entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#remove(java.util.Collection)
     */
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#remove(ubic.gemma.model.genome.Chromosome)
     */
    public void remove( ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.remove - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().delete( chromosome );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#update(java.util.Collection)
     */
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Chromosome.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.Chromosome ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.ChromosomeDao#update(ubic.gemma.model.genome.Chromosome)
     */
    public void update( ubic.gemma.model.genome.Chromosome chromosome ) {
        if ( chromosome == null ) {
            throw new IllegalArgumentException( "Chromosome.update - 'chromosome' can not be null" );
        }
        this.getHibernateTemplate().update( chromosome );
    }

    /**
     * Transforms a collection of entities using the {@link #transformEntity(int,ubic.gemma.model.genome.Chromosome)}
     * method. This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.ChromosomeDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.Chromosome)
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
     * <code>ubic.gemma.model.genome.ChromosomeDao</code>, please note that the {@link #TRANSFORM_NONE} constant denotes
     * no transformation, so the entity itself will be returned. If the integer argument value is unknown
     * {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.ChromosomeDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.Chromosome entity ) {
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