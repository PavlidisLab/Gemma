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

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.BibliographicReference</code>.
 * </p>
 * 
 * @see ubic.gemma.model.common.description.BibliographicReference
 */
public abstract class BibliographicReferenceDaoBase extends HibernateDaoSupport implements
        ubic.gemma.model.common.description.BibliographicReferenceDao {

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#create(int, java.util.Collection)
     */
    public java.util.Collection<BibliographicReference> create( final int transform,
            final java.util.Collection<BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create( transform,
                                    ( ubic.gemma.model.common.description.BibliographicReference ) entityIterator
                                            .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#create(int transform,
     *      ubic.gemma.model.common.description.BibliographicReference)
     */
    public BibliographicReference create( final int transform,
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.create - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().save( bibliographicReference );
        return ( BibliographicReference ) this.transformEntity( transform, bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#create(ubic.gemma.model.common.description.BibliographicReference)
     */
    public BibliographicReference create(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        return this.create( TRANSFORM_NONE, bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(int, java.lang.String,
     *      java.lang.String)
     */
    public Object findByExternalId( final int transform, final java.lang.String id, final java.lang.String databaseName ) {
        return this
                .findByExternalId(
                        transform,
                        "from BibliographicReferenceImpl b where b.pubAccession.accession=:id AND b.pubAccession.externalDatabase.name=:databaseName",
                        id, databaseName );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(int, java.lang.String,
     *      java.lang.String, java.lang.String)
     */

    public Object findByExternalId( final int transform, final java.lang.String queryString, final java.lang.String id,
            final java.lang.String databaseName ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( id );
        argNames.add( "id" );
        args.add( databaseName );
        argNames.add( "databaseName" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.BibliographicReference"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.BibliographicReference ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(int, java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */

    public Object findByExternalId( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.description.DatabaseEntry externalId ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( externalId );
        argNames.add( "externalId" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.BibliographicReference"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.BibliographicReference ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(int,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    public Object findByExternalId( final int transform,
            final ubic.gemma.model.common.description.DatabaseEntry externalId ) {
        return this.findByExternalId( transform, "from BibliographicReferenceImpl b where b.pubAccession=:externalId",
                externalId );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(java.lang.String,
     *      java.lang.String)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( java.lang.String id,
            java.lang.String databaseName ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.findByExternalId( TRANSFORM_NONE,
                id, databaseName );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(java.lang.String,
     *      java.lang.String, java.lang.String)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId(
            final java.lang.String queryString, final java.lang.String id, final java.lang.String databaseName ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.findByExternalId( TRANSFORM_NONE,
                queryString, id, databaseName );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(java.lang.String,
     *      ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId(
            final java.lang.String queryString, final ubic.gemma.model.common.description.DatabaseEntry externalId ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.findByExternalId( TRANSFORM_NONE,
                queryString, externalId );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByExternalId(ubic.gemma.model.common.description.DatabaseEntry)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId(
            ubic.gemma.model.common.description.DatabaseEntry externalId ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.findByExternalId( TRANSFORM_NONE,
                externalId );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByTitle(int, java.lang.String)
     */
    public Object findByTitle( final int transform, final java.lang.String title ) {
        return this
                .findByTitle(
                        transform,
                        "from ubic.gemma.model.common.description.BibliographicReference as bibliographicReference where bibliographicReference.title = :title",
                        title );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByTitle(int, java.lang.String,
     *      java.lang.String)
     */

    public BibliographicReference findByTitle( final int transform, final java.lang.String queryString,
            final java.lang.String title ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( title );
        argNames.add( "title" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;
        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ubic.gemma.model.common.description.BibliographicReference"
                            + "' was found when executing query --> '" + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform, ( ubic.gemma.model.common.description.BibliographicReference ) result );
        return ( BibliographicReference ) result;
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByTitle(java.lang.String)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByTitle( java.lang.String title ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.findByTitle( TRANSFORM_NONE, title );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#findByTitle(java.lang.String,
     *      java.lang.String)
     */
    public ubic.gemma.model.common.description.BibliographicReference findByTitle( final java.lang.String queryString,
            final java.lang.String title ) {
        return this.findByTitle( TRANSFORM_NONE, queryString, title );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#getAllExperimentLinkedReferences()
     */
    public java.util.Map getAllExperimentLinkedReferences() {
        try {
            return this.handleGetAllExperimentLinkedReferences();
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceDao.getAllExperimentLinkedReferences()' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference)
     */
    public java.util.Collection getRelatedExperiments(
            final ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        try {
            return this.handleGetRelatedExperiments( bibliographicReference );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceDao.getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference bibliographicReference)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#load(int, java.lang.Long)
     */

    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibliographicReference.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.common.description.BibliographicReferenceImpl.class, id );
        return transformEntity( transform, ( ubic.gemma.model.common.description.BibliographicReference ) entity );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#load(java.lang.Long)
     */

    public BibliographicReference load( java.lang.Long id ) {
        return ( ubic.gemma.model.common.description.BibliographicReference ) this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#loadMultiple(java.util.Collection)
     */
    public java.util.Collection<BibliographicReference> load( final java.util.Collection ids ) {
        try {
            return this.handleLoadMultiple( ids );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.common.description.BibliographicReferenceDao.loadMultiple(java.util.Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#loadAll(int)
     */

    public java.util.Collection<BibliographicReference> loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.common.description.BibliographicReferenceImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "BibliographicReference.remove - 'id' can not be null" );
        }
        ubic.gemma.model.common.description.BibliographicReference entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    public void remove( java.util.Collection<? extends BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#remove(ubic.gemma.model.common.description.BibliographicReference)
     */
    public void remove( ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.remove - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().delete( bibliographicReference );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    public void update( final java.util.Collection<? extends BibliographicReference> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "BibliographicReference.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.common.description.BibliographicReference ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.common.description.BibliographicReferenceDao#update(ubic.gemma.model.common.description.BibliographicReference)
     */
    public void update( ubic.gemma.model.common.description.BibliographicReference bibliographicReference ) {
        if ( bibliographicReference == null ) {
            throw new IllegalArgumentException(
                    "BibliographicReference.update - 'bibliographicReference' can not be null" );
        }
        this.getHibernateTemplate().update( bibliographicReference );
    }

    /**
     * Performs the core logic for {@link #getAllExperimentLinkedReferences()}
     */
    protected abstract java.util.Map<ExpressionExperiment, BibliographicReference> handleGetAllExperimentLinkedReferences()
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getRelatedExperiments(ubic.gemma.model.common.description.BibliographicReference)}
     */
    protected abstract java.util.Collection handleGetRelatedExperiments(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #loadMultiple(java.util.Collection)}
     */
    protected abstract java.util.Collection<BibliographicReference> handleLoadMultiple( java.util.Collection ids )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.common.description.BibliographicReference)} method. This method does
     * not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.common.description.BibliographicReferenceDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.common.description.BibliographicReference)
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
     * <code>ubic.gemma.model.common.description.BibliographicReferenceDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.common.description.BibliographicReferenceDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.common.description.BibliographicReference entity ) {
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