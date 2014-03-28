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
 * <code>ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis</code>.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis
 */
public abstract class CoexpressionAnalysisDaoBase extends AnalysisDaoImpl<CoexpressionAnalysis> implements
        CoexpressionAnalysisDao {

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends CoexpressionAnalysis> create(
            final java.util.Collection<? extends CoexpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CoexpressionAnalysis> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#create(int transform,
     *      ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis)
     */
    @Override
    public CoexpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis coexpressionAnalysis ) {
        if ( coexpressionAnalysis == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.create - 'coexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().save( coexpressionAnalysis );
        return coexpressionAnalysis;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#findByName(int, java.lang.String)
     */

    @Override
    public java.util.Collection<CoexpressionAnalysis> findByName( final java.lang.String name ) {
        return this.findByName( "select a from CoexpressionAnalysisImpl as a where a.name = :name", name );
    }

    @Override
    public Collection<? extends CoexpressionAnalysis> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CoexpressionAnalysisImpl where id in (:ids)", "ids",
                ids );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#load(int, java.lang.Long)
     */

    @Override
    public CoexpressionAnalysis load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get(
                ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisImpl.class, id );
        return ( ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis ) entity;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    @Override
    public java.util.Collection<? extends CoexpressionAnalysis> loadAll() {
        final java.util.Collection<?> results = this.getHibernateTemplate().loadAll(
                ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisImpl.class );
        return ( Collection<? extends CoexpressionAnalysis> ) results;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.remove - 'id' can not be null" );
        }
        ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(java.util.Collection)
     */

    @Override
    public void remove( java.util.Collection<? extends CoexpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#remove(ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis)
     */
    @Override
    public void remove( ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis coexpressionAnalysis ) {
        if ( coexpressionAnalysis == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.remove - 'coexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().delete( coexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(java.util.Collection)
     */

    @Override
    public void update( final Collection<? extends CoexpressionAnalysis> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends CoexpressionAnalysis> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#update(ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis)
     */
    @Override
    public void update( CoexpressionAnalysis coexpressionAnalysis ) {
        if ( coexpressionAnalysis == null ) {
            throw new IllegalArgumentException( "CoexpressionAnalysis.update - 'coexpressionAnalysis' can not be null" );
        }
        this.getHibernateTemplate().update( coexpressionAnalysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisDao#findByName(int, java.lang.String,
     *      java.lang.String)
     */
    @SuppressWarnings("unchecked")
    private java.util.Collection<CoexpressionAnalysis> findByName( final java.lang.String queryString,
            final java.lang.String name ) {
        java.util.List<String> argNames = new java.util.ArrayList<String>();
        java.util.List<Object> args = new java.util.ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        java.util.List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );
        return ( Collection<CoexpressionAnalysis> ) results;
    }

}