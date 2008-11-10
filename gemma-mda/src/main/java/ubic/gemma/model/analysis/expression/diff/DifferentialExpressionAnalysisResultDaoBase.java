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
package ubic.gemma.model.analysis.expression.diff;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public abstract class DifferentialExpressionAnalysisResultDaoBase extends
        ubic.gemma.model.analysis.AnalysisResultDaoImpl implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao {

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysisResult.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultImpl.class, id );
        return transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) entity );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.analysis.AnalysisResult load( java.lang.Long id ) {
        return ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) this.load(
                TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#loadAll()
     */
    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public ubic.gemma.model.analysis.AnalysisResult create(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        return ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) this.create(
                TRANSFORM_NONE, differentialExpressionAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public Object create(
            final int transform,
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        if ( differentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.create - 'differentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().save( differentialExpressionAnalysisResult );
        return this.transformEntity( transform, differentialExpressionAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#create(java.util.Collection)
     */
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#create(int,
     *      java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create(
                                    transform,
                                    ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) entityIterator
                                            .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#update(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public void update(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        if ( differentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.update - 'differentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().update( differentialExpressionAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#remove(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public void remove(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        if ( differentialExpressionAnalysisResult == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.remove - 'differentialExpressionAnalysisResult' can not be null" );
        }
        this.getHibernateTemplate().delete( differentialExpressionAnalysisResult );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "DifferentialExpressionAnalysisResult.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult entity = ( ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException(
                    "DifferentialExpressionAnalysisResult.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public java.util.Collection getExperimentalFactors(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResult );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao.getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for
     * {@link #getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)}
     */
    protected abstract java.util.Collection handleGetExperimentalFactors(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult )
            throws java.lang.Exception;

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao#getExperimentalFactors(java.util.Collection)
     */
    public java.util.Map getExperimentalFactors( final java.util.Collection differentialExpressionAnalysisResults ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResults );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao.getExperimentalFactors(java.util.Collection differentialExpressionAnalysisResults)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetExperimentalFactors(
            java.util.Collection differentialExpressionAnalysisResults ) throws java.lang.Exception;

    /**
     * Allows transformation of entities into value objects (or something else for that matter), when the
     * <code>transform</code> flag is set to one of the constants defined in
     * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao</code>, please note that
     * the {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the
     * integer argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult entity ) {
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

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)}
     * method. This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected void transformEntities( final int transform, final java.util.Collection entities ) {
        switch ( transform ) {
            case TRANSFORM_NONE: // fall-through
            default:
                // do nothing;
        }
    }

}