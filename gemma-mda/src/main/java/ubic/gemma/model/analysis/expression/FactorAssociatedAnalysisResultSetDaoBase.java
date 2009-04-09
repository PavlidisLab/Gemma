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
package ubic.gemma.model.analysis.expression;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet
 */
public abstract class FactorAssociatedAnalysisResultSetDaoBase extends
        ubic.gemma.model.analysis.AnalysisResultSetDaoImpl implements
        ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao {

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#load(int, java.lang.Long)
     */
    @Override
    public Object load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FactorAssociatedAnalysisResultSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetImpl.class, id );
        return transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet ) entity );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.analysis.AnalysisResultSet load( java.lang.Long id ) {
        return ( ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet ) this
                .load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#loadAll()
     */
    @Override
    @SuppressWarnings( { "unchecked" })
    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#loadAll(int)
     */
    @Override
    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "FactorAssociatedAnalysisResultSet.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet entity = ( ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet ) this
                .load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorAssociatedAnalysisResultSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#remove(ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet)
     */
    public void remove(
            ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet factorAssociatedAnalysisResultSet ) {
        if ( factorAssociatedAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "FactorAssociatedAnalysisResultSet.remove - 'factorAssociatedAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().delete( factorAssociatedAnalysisResultSet );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisResultSetDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "FactorAssociatedAnalysisResultSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao#update(ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet)
     */
    public void update(
            ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet factorAssociatedAnalysisResultSet ) {
        if ( factorAssociatedAnalysisResultSet == null ) {
            throw new IllegalArgumentException(
                    "FactorAssociatedAnalysisResultSet.update - 'factorAssociatedAnalysisResultSet' can not be null" );
        }
        this.getHibernateTemplate().update( factorAssociatedAnalysisResultSet );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet)} method. This
     * method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet)
     */
    @Override
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
     * <code>ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSetDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet entity ) {
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