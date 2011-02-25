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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis
 */
public abstract class ProbeCoexpressionAnalysisDaoBase extends
        ubic.gemma.model.analysis.AnalysisDaoImpl<ProbeCoexpressionAnalysis> implements
        ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao {

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#create(int,
     *      java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeCoexpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create(
                                    transform,
                                    ( ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis ) entityIterator
                                            .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
     */
    public Object create( final int transform,
            final ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        if ( probeCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "ProbeCoexpressionAnalysis.create - 'probeCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( probeCoexpressionAnalysis );
        return this.transformEntity( transform, probeCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#create(ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
     */
    public ProbeCoexpressionAnalysis create(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        return ( ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis ) this.create(
                TRANSFORM_NONE, probeCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#findByName(int,
     *      java.lang.String)
     */

    @Override
    public java.util.Collection findByName( final int transform, final java.lang.String name ) {
        return this.findByName( transform, "select a from AnalysisImpl as a where a.name like :name", name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#findByName(int,
     *      java.lang.String, java.lang.String)
     */

    @Override
    public java.util.Collection findByName( final int transform, final java.lang.String queryString,
            final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#findByName(java.lang.String)
     */

    @Override
    public java.util.Collection findByName( java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#findByName(java.lang.String,
     *      java.lang.String)
     */

    @Override
    public java.util.Collection findByName( final java.lang.String queryString, final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    public Collection<? extends ProbeCoexpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ProbeCoexpressionAnalysisImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#load(int, java.lang.Long)
     */

    public ProbeCoexpressionAnalysis load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeCoexpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl.class, id );
        return ( ProbeCoexpressionAnalysis ) transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis ) entity );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#load(java.lang.Long)
     */

    public ProbeCoexpressionAnalysis load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ProbeCoexpressionAnalysis.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeCoexpressionAnalysis.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#remove(ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
     */
    public void remove(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        if ( probeCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "ProbeCoexpressionAnalysis.remove - 'probeCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().delete( probeCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ProbeCoexpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao#update(ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
     */
    public void update(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        if ( probeCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "ProbeCoexpressionAnalysis.update - 'probeCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().update( probeCoexpressionAnalysis );
    }

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)} method.
     * This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis)
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
     * <code>ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected Object transformEntity( final int transform,
            final ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis entity ) {
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