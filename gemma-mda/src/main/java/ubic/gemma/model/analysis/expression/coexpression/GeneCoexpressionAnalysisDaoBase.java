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
    public java.util.Collection create( final java.util.Collection entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
                            create(

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
    public GeneCoexpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        if ( geneCoexpressionAnalysis == null ) {
            throw new IllegalArgumentException(
                    "GeneCoexpressionAnalysis.create - 'geneCoexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( geneCoexpressionAnalysis );
        return geneCoexpressionAnalysis;
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
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#load(int, java.lang.Long)
     */
    public GeneCoexpressionAnalysis load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "GeneCoexpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisImpl.class, id );
        return ( ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis ) entity;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao#loadAll(int)
     */

    public java.util.Collection loadAll() {
        final java.util.Collection results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisImpl.class );
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
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis );

}