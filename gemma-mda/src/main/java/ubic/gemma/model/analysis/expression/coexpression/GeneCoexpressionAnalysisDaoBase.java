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

import ubic.gemma.model.analysis.AnalysisDaoImpl;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis
 */
public abstract class GeneCoexpressionAnalysisDaoBase extends AnalysisDaoImpl<GeneCoexpressionAnalysis> implements
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao {

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#create(int,
     *      java.util.Collection)
     */
    public java.util.Collection create( final int transform, final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create(
                                    transform,
                                    ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) entityIterator
                                            .next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public Object create( final int transform,
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        if ( geneCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "GeneCoexpressionAnalysis.create - 'geneCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( geneCoexpressionAnalysis );
        return this.transformEntity( transform, geneCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#create(java.util.Collection)
     */

    public java.util.Collection create( final java.util.Collection entities ) {
        return create( TRANSFORM_NONE, entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#create(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public GeneCoexpressionAnalysis create(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        return ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) this.create(
                TRANSFORM_NONE, geneCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#findByName(int,
     *      java.lang.String)
     */

    @Override
    public java.util.Collection findByName( final int transform, final java.lang.String name ) {
        return this.findByName( transform, "select a from AnalysisImpl as a where a.name like :name", name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#findByName(int,
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
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#findByName(java.lang.String)
     */

    @Override
    public java.util.Collection findByName( java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#findByName(java.lang.String,
     *      java.lang.String)
     */

    @Override
    public java.util.Collection findByName( final java.lang.String queryString, final java.lang.String name ) {
        return this.findByName( TRANSFORM_NONE, queryString, name );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public java.util.Collection getDatasetsAnalyzed(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis ) {
        try {
            return this.handleGetDatasetsAnalyzed( analysis );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao.getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMask(int, java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public Object getMask( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( securable );
        argNames.add( "securable" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'java.lang.Integer" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMask(int,
     *      ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public Object getMask( final int transform, final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return this
                .getMask(
                        transform,
                        "from ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis as geneCoexpressionAnalysis where geneCoexpressionAnalysis.securable = :securable",
                        securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMask(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public java.lang.Integer getMask( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return ( java.lang.Integer ) this.getMask( TRANSFORM_NONE, queryString, securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMask(ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public java.lang.Integer getMask( ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return ( java.lang.Integer ) this.getMask( TRANSFORM_NONE, securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMasks(int,
     *      java.lang.String, java.util.Collection)
     */

    public Object getMasks( final int transform, final java.lang.String queryString,
            final java.util.Collection securables ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( securables );
        argNames.add( "securables" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'java.util.Map" + "' was found when executing query --> '" + queryString
                            + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMasks(int,
     *      java.util.Collection)
     */

    public Object getMasks( final int transform, final java.util.Collection securables ) {
        return this
                .getMasks(
                        transform,
                        "from ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis as geneCoexpressionAnalysis where geneCoexpressionAnalysis.securables = :securables",
                        securables );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMasks(java.lang.String,
     *      java.util.Collection)
     */

    public java.util.Map getMasks( final java.lang.String queryString, final java.util.Collection securables ) {
        return ( java.util.Map ) this.getMasks( TRANSFORM_NONE, queryString, securables );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getMasks(java.util.Collection)
     */

    public java.util.Map getMasks( java.util.Collection securables ) {
        return ( java.util.Map ) this.getMasks( TRANSFORM_NONE, securables );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public int getNumDatasetsAnalyzed(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis ) {
        try {
            return this.handleGetNumDatasetsAnalyzed( analysis );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao.getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getObjectIdentityId(int,
     *      java.lang.String, ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public Object getObjectIdentityId( final int transform, final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( securable );
        argNames.add( "securable" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'java.lang.Long" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getObjectIdentityId(int,
     *      ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    public Object getObjectIdentityId( final int transform,
            final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return this
                .getObjectIdentityId(
                        transform,
                        "from ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis as geneCoexpressionAnalysis where geneCoexpressionAnalysis.securable = :securable",
                        securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getObjectIdentityId(java.lang.String,
     *      ubic.gemma.model.common.auditAndSecurity.Securable)
     */
    public java.lang.Long getObjectIdentityId( final java.lang.String queryString,
            final ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return ( java.lang.Long ) this.getObjectIdentityId( TRANSFORM_NONE, queryString, securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getObjectIdentityId(ubic.gemma.model.common.auditAndSecurity.Securable)
     */

    public java.lang.Long getObjectIdentityId( ubic.gemma.model.common.auditAndSecurity.Securable securable ) {
        return ( java.lang.Long ) this.getObjectIdentityId( TRANSFORM_NONE, securable );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getRecipient(int,
     *      java.lang.Long)
     */
    public Object getRecipient( final int transform, final java.lang.Long id ) {
        return this
                .getRecipient(
                        transform,
                        "from ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis as geneCoexpressionAnalysis where geneCoexpressionAnalysis.id = :id",
                        id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getRecipient(int,
     *      java.lang.String, java.lang.Long)
     */

    public Object getRecipient( final int transform, final java.lang.String queryString, final java.lang.Long id ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( id );
        argNames.add( "id" );
        java.util.Set results = new java.util.LinkedHashSet( this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        Object result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'java.lang.String" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        result = transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) result );
        return result;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getRecipient(java.lang.Long)
     */

    public java.lang.String getRecipient( java.lang.Long id ) {
        return ( java.lang.String ) this.getRecipient( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#getRecipient(java.lang.String,
     *      java.lang.Long)
     */
    public java.lang.String getRecipient( final java.lang.String queryString, final java.lang.Long id ) {
        return ( java.lang.String ) this.getRecipient( TRANSFORM_NONE, queryString, id );
    }

    public Collection<? extends GeneCoexpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from GeneCoexpressionAnalysisImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#load(int, java.lang.Long)
     */
    public GeneCoexpressionAnalysis load( final int transform, final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisImpl.class, id );
        return transformEntity( transform,
                ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) entity );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#load(java.lang.Long)
     */

    public GeneCoexpressionAnalysis load( java.lang.Long id ) {
        return this.load( TRANSFORM_NONE, id );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#loadAll()
     */

    public java.util.Collection loadAll() {
        return this.loadAll( TRANSFORM_NONE );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#loadAll(int)
     */

    public java.util.Collection loadAll( final int transform ) {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisImpl.class );
        this.transformEntities( transform, results );
        return results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis entity = this.load( id );
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
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#remove(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public void remove(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        if ( geneCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "GeneCoexpressionAnalysis.remove - 'geneCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().delete( geneCoexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public void thaw(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        try {
            this.handleThaw( geneCoexpressionAnalysis );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao.thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            update( ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) entityIterator
                                    .next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#update(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    public void update(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        if ( geneCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "GeneCoexpressionAnalysis.update - 'geneCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().update( geneCoexpressionAnalysis );
    }

    /**
     * Performs the core logic for
     * {@link #getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract java.util.Collection handleGetDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract int handleGetNumDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis )
            throws java.lang.Exception;

    /**
     * Transforms a collection of entities using the
     * {@link #transformEntity(int,ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)} method.
     * This method does not instantiate a new collection.
     * <p/>
     * This method is to be used internally only.
     * 
     * @param transform one of the constants declared in
     *        <code>ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao</code>
     * @param entities the collection of entities to transform
     * @return the same collection as the argument, but this time containing the transformed entities
     * @see #transformEntity(int,ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
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
     * <code>ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao</code>, please note that the
     * {@link #TRANSFORM_NONE} constant denotes no transformation, so the entity itself will be returned. If the integer
     * argument value is unknown {@link #TRANSFORM_NONE} is assumed.
     * 
     * @param transform one of the constants declared in
     *        {@link ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao}
     * @param entity an entity that was found
     * @return the transformed entity (i.e. new value object, etc)
     * @see #transformEntities(int,java.util.Collection)
     */
    protected GeneCoexpressionAnalysis transformEntity( final int transform,
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis entity ) {
        GeneCoexpressionAnalysis target = null;
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