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
package ubic.gemma.model.genome.gene;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.gene.GeneProduct</code>.
 * </p>
 * 
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public abstract class GeneProductDaoBase extends ubic.gemma.model.genome.ChromosomeFeatureDaoImpl<GeneProduct>
        implements ubic.gemma.model.genome.gene.GeneProductDao {

    /**
     * This anonymous transformer is designed to transform entities or report query results (which result in an array of
     * objects) to {@link ubic.gemma.model.genome.gene.GeneProductValueObject} using the Jakarta Commons-Collections
     * Transformation API.
     */
    private org.apache.commons.collections.Transformer GENEPRODUCTVALUEOBJECT_TRANSFORMER = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            Object result = null;
            if ( input instanceof ubic.gemma.model.genome.gene.GeneProduct ) {
                result = toGeneProductValueObject( ( ubic.gemma.model.genome.gene.GeneProduct ) input );
            } else if ( input instanceof Object[] ) {
                result = toGeneProductValueObject( ( Object[] ) input );
            }
            return result;
        }
    };

    private final org.apache.commons.collections.Transformer GeneProductValueObjectToEntityTransformer = new org.apache.commons.collections.Transformer() {
        public Object transform( Object input ) {
            return geneProductValueObjectToEntity( ( ubic.gemma.model.genome.gene.GeneProductValueObject ) input );
        }
    };

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#countAll()
     */
    public java.lang.Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#create(int, java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform, ( ubic.gemma.model.genome.gene.GeneProduct ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#create(int transform, ubic.gemma.model.genome.gene.GeneProduct)
     */
    public Object create( final int transform, final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.create - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().save( geneProduct );
        return this.transformEntity( transform, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#create(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public GeneProduct create( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this.create( TRANSFORM_NONE, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#find(int, java.lang.String,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( geneProduct );
        argNames.add( "geneProduct" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.genome.gene.GeneProduct"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.genome.gene.GeneProduct ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#find(int, ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public Object find( final int transform, final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this
                .find(
                        transform,
                        "from ubic.gemma.model.genome.gene.GeneProduct as geneProduct where geneProduct.geneProduct = :geneProduct",
                        geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#find(java.lang.String, ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.genome.gene.GeneProduct find( final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this.find( TRANSFORM_NONE, queryString, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#find(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public ubic.gemma.model.genome.gene.GeneProduct find( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this.find( TRANSFORM_NONE, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByNcbiId(int, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByNcbiId( final int transform, final java.lang.String ncbiId ) {
        return this.findByNcbiId( transform, "from GeneImpl g where g.ncbiId = :ncbiId", ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByNcbiId(int, java.lang.String, java.lang.String)
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByNcbiId( final int transform, final java.lang.String queryString,
            final java.lang.String ncbiId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( ncbiId );
        argNames.add( "ncbiId" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByNcbiId(java.lang.String)
     */

    public java.util.Collection findByNcbiId( java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByNcbiId(java.lang.String, java.lang.String)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByNcbiId( final java.lang.String queryString, final java.lang.String ncbiId ) {
        return this.findByNcbiId( TRANSFORM_NONE, queryString, ncbiId );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByPhysicalLocation(int, java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalLocation( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( location );
        argNames.add( "location" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByPhysicalLocation(int,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalLocation( final int transform,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( transform,
                "from ubic.gemma.model.genome.gene.GeneProduct as geneProduct where geneProduct.location = :location",
                location );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByPhysicalLocation(java.lang.String,
     *      ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection findByPhysicalLocation( final java.lang.String queryString,
            final ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, queryString, location );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation)
     */

    @Override
    public java.util.Collection findByPhysicalLocation( ubic.gemma.model.genome.PhysicalLocation location ) {
        return this.findByPhysicalLocation( TRANSFORM_NONE, location );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findOrCreate(int, java.lang.String,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( geneProduct );
        argNames.add( "geneProduct" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results != null ) {
            if ( results.size() > 1 ) {
                throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                        "More than one instance of 'ubic.gemma.model.genome.gene.GeneProduct"
                                + "' was found when executing query --> '" + queryString + "'" );
            } else if ( results.size() == 1 ) {
                result = results.iterator().next();
            }
        }
        result = transformEntity( transform, ( ubic.gemma.model.genome.gene.GeneProduct ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findOrCreate(int, ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public Object findOrCreate( final int transform, final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return this
                .findOrCreate(
                        transform,
                        "from ubic.gemma.model.genome.gene.GeneProduct as geneProduct where geneProduct.geneProduct = :geneProduct",
                        geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findOrCreate(java.lang.String,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */
    @SuppressWarnings( { "unchecked" })
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( final java.lang.String queryString,
            final ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this
                .findOrCreate( TRANSFORM_NONE, queryString, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this.findOrCreate( TRANSFORM_NONE, geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#geneProductValueObjectToEntity(ubic.gemma.model.genome.gene.GeneProductValueObject,
     *      ubic.gemma.model.genome.gene.GeneProduct)
     */
    public void geneProductValueObjectToEntity( ubic.gemma.model.genome.gene.GeneProductValueObject source,
            ubic.gemma.model.genome.gene.GeneProduct target, boolean copyIfNull ) {
        // No conversion for target.type (can't convert source.getType():java.lang.String to
        // ubic.gemma.model.genome.gene.GeneProductType)
        if ( copyIfNull || source.getNcbiId() != null ) {
            target.setNcbiId( source.getNcbiId() );
        }
        if ( copyIfNull || source.getName() != null ) {
            target.setName( source.getName() );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#geneProductValueObjectToEntityCollection(java.util.Collection)
     */
    public final void geneProductValueObjectToEntityCollection( java.util.Collection instances ) {
        if ( instances != null ) {
            for ( final java.util.Iterator iterator = instances.iterator(); iterator.hasNext(); ) {
                // - remove an objects that are null or not of the correct instance
                if ( !( iterator.next() instanceof ubic.gemma.model.genome.gene.GeneProductValueObject ) ) {
                    iterator.remove();
                }
            }
            org.apache.commons.collections.CollectionUtils.transform( instances,
                    GeneProductValueObjectToEntityTransformer );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#getGenesByName(java.lang.String)
     */
    public java.util.Collection getGenesByName( final java.lang.String search ) {
        try {
            return this.handleGetGenesByName( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductDao.getGenesByName(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#getGenesByNcbiId(java.lang.String)
     */
    public java.util.Collection getGenesByNcbiId( final java.lang.String search ) {
        try {
            return this.handleGetGenesByNcbiId( search );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductDao.getGenesByNcbiId(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneProduct.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ubic.gemma.model.genome.gene.GeneProductImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.genome.gene.GeneProduct ) entity );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#load(java.lang.Long)
     */

    public GeneProduct load( java.lang.Long id ) {
        return ( ubic.gemma.model.genome.gene.GeneProduct ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#load(java.util.Collection)
     */
    public java.util.Collection load( final java.util.Collection ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.genome.gene.GeneProductDao.load(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#loadAll()
     */

    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.genome.gene.GeneProductImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'id' can not be null" );
        }
        ubic.gemma.model.genome.gene.GeneProduct entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#remove(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public void remove( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.remove - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().delete( geneProduct );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#toGeneProductValueObject(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public ubic.gemma.model.genome.gene.GeneProductValueObject toGeneProductValueObject(
            final ubic.gemma.model.genome.gene.GeneProduct entity ) {
        final ubic.gemma.model.genome.gene.GeneProductValueObject target = new ubic.gemma.model.genome.gene.GeneProductValueObject();
        this.toGeneProductValueObject( entity, target );
        return target;
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#toGeneProductValueObject(ubic.gemma.model.genome.gene.GeneProduct,
     *      ubic.gemma.model.genome.gene.GeneProductValueObject)
     */
    public void toGeneProductValueObject( ubic.gemma.model.genome.gene.GeneProduct source,
            ubic.gemma.model.genome.gene.GeneProductValueObject target ) {
        target.setId( source.getId() );
        target.setNcbiId( source.getNcbiId() );
        target.setName( source.getName() );
        // No conversion for target.type (can't convert source.getType():ubic.gemma.model.genome.gene.GeneProductType to
        // java.lang.String)
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#toGeneProductValueObjectCollection(java.util.Collection)
     */
    public final void toGeneProductValueObjectCollection( java.util.Collection entities ) {
        if ( entities != null ) {
            org.apache.commons.collections.CollectionUtils.transform( entities, GENEPRODUCTVALUEOBJECT_TRANSFORMER );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneProduct.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.genome.gene.GeneProduct ) entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.genome.gene.GeneProductDao#update(ubic.gemma.model.genome.gene.GeneProduct)
     */
    public void update( ubic.gemma.model.genome.gene.GeneProduct geneProduct ) {
        if ( geneProduct == null ) {
            throw new IllegalArgumentException( "GeneProduct.update - 'geneProduct' can not be null" );
        }
        this.getHibernateTemplate().update( geneProduct );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract java.lang.Integer handleCountAll() throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract java.util.Collection handleGetGenesByName( java.lang.String search ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getGenesByNcbiId(java.lang.String)}
     */
    protected abstract java.util.Collection handleGetGenesByNcbiId( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection handleLoad( java.util.Collection ids ) throws java.lang.Exception;

    /**
     * Default implementation for transforming the results of a report query into a value object. This implementation
     * exists for convenience reasons only. It needs only be overridden in the {@link GeneProductDaoImpl} class if you
     * intend to use reporting queries.
     * 
     * @see ubic.gemma.model.genome.gene.GeneProductDao#toGeneProductValueObject(ubic.gemma.model.genome.gene.GeneProduct)
     */
    protected ubic.gemma.model.genome.gene.GeneProductValueObject toGeneProductValueObject( Object[] row ) {
        ubic.gemma.model.genome.gene.GeneProductValueObject target = null;
        if ( row != null ) {
            final int numberOfObjects = row.length;
            for ( int ctr = 0; ctr < numberOfObjects; ctr++ ) {
                final Object object = row[ctr];
                if ( object instanceof ubic.gemma.model.genome.gene.GeneProduct ) {
                    target = this.toGeneProductValueObject( ( ubic.gemma.model.genome.gene.GeneProduct ) object );
                    break;
                }
            }
        }
        return target;
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.genome.gene.GeneProduct)} method. This method does not instantiate a
     * new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in <code>ubic.gemma.model.genome.gene.GeneProductDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.genome.gene.GeneProduct)
     */

    @Override
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case ubic.gemma.model.genome.gene.GeneProductDao.TRANSFORM_GENEPRODUCTVALUEOBJECT:
                toGeneProductValueObjectCollection( entities );
                break;
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.genome.gene.GeneProductDao</code>, please note that the {@link #TRANSFORM_NONE} constant
     * denotes no transformation, so the entity itself will be returned.
     * <p/>
     * This method will return instances of these types:
     * <ul>
     * <li>{@link ubic.gemma.model.genome.gene.GeneProduct} - {@link #TRANSFORM_NONE}</li>
     * <li>{@link ubic.gemma.model.genome.gene.GeneProductValueObject} - {@link TRANSFORM_GENEPRODUCTVALUEOBJECT}</li>
     * </ul>
     * If the integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in {@link ubic.gemma.model.genome.gene.GeneProductDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform, final ubic.gemma.model.genome.gene.GeneProduct entity ) {
        Object target = null;
        if ( entity != null ) {
            switch ( transform ) {
                case ubic.gemma.model.genome.gene.GeneProductDao.TRANSFORM_GENEPRODUCTVALUEOBJECT:
                    target = toGeneProductValueObject( entity );
                    break;
                case TRANSFORM_NONE: // fall-through
                default:
                    target = entity;
            }
        }
        return target;
    }

}