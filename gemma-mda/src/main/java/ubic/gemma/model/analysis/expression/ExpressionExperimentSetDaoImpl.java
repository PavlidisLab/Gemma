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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.ExpressionExperimentSet
 * @version $Id$
 * @author paul
 */
@Repository
public class ExpressionExperimentSetDaoImpl extends HibernateDaoSupport implements ExpressionExperimentSetDao {

    @Autowired
    public ExpressionExperimentSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#find(ubic.gemma.model.expression.experiment.
     * BioAssaySet)
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees from ExpressionExperimentSetImpl ees inner join ees.experiments e where e = :ee", "ee",
                bioAssaySet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#getExperimentsInSet(java.lang.Long)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees.experiments from ExpressionExperimentSetImpl ees where ees.id = :id", "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllMultiExperimentSets()
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where size(ees.experiments) > 1" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllExperimentSetsWithTaxon()
     */
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where ees.taxon is not null" );
    }

    @Override
    public Taxon getTaxon( Long id) {
        
        // using Query because I want to be able to limit the number of row returned to one
        
        Query q = this.getSession().createQuery( 
                "select ees.id, ees.taxon from ExpressionExperimentSetImpl ees where ees.id = :id" );
        q.setParameter( "id", id );
        q.setMaxResults( 1 );
        
        List<?> list = q.list();
        for(Object obj : list){
            Object[] oa = ( Object[] ) obj;
            return ( ( Taxon ) oa[1] );
        }

        return null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#thaw(ubic.gemma.model.analysis.expression.
     * ExpressionExperimentSet)
     */
    @Override
    public void thaw( final ExpressionExperimentSet expressionExperimentSet ) {

        this.getHibernateTemplate().execute( new HibernateCallback<Object>() {

            @Override
            public Object doInHibernate( Session session ) throws HibernateException, SQLException {
                session.buildLockRequest( LockOptions.NONE ).lock( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet.getTaxon() );
                Hibernate.initialize( expressionExperimentSet.getExperiments() );
                return null;
            }
        } );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleFindByName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getHibernateTemplate()
                   .findByNamedParam( "from ExpressionExperimentSetImpl where name=:query", "query", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleGetAnalyses(ubic.gemma.model.analysis
     * .expression.ExpressionExperimentSet)
     */
    @SuppressWarnings("unchecked")
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select a from ExpressionAnalysisImpl a inner join a.expressionExperimentSetAnalyzed ees where ees = :eeset ",
                        "eeset", expressionExperimentSet );
    }

    public int getExperimentCount( Long id ) {

        List<?> o = this.getHibernateTemplate().findByNamedParam(
                "select e.id, count(i) from ExpressionExperimentSetImpl e join e.experiments i where e.id in (:ids)",
                "ids", id );

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            return ( ( Long ) oa[1] ).intValue();
        }

        return 0;

    }

   
    /**
     * @see ExpressionExperimentSetDao#create(int, Collection)
     */
    public Collection<? extends ExpressionExperimentSet> create(
            final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ExpressionExperimentSet> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see ExpressionExperimentSetDao#create(int transform, ExpressionExperimentSet)
     */
    public ExpressionExperimentSet create( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.create - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSet );
        return expressionExperimentSet;
    }

    /**
     * @see ExpressionExperimentSetDao#findByName(java.lang.String)
     */
    public Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.findByName(java.lang.String name)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentSetDao#getAnalyses(ExpressionExperimentSet)
     */
    public Collection<ExpressionAnalysis> getAnalyses( final ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleGetAnalyses( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.getAnalyses(ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }
    
    public Collection<? extends ExpressionExperimentSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ExpressionExperimentSetDao#load(int, java.lang.Long)
     */

    public ExpressionExperimentSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ExpressionExperimentSetImpl.class, id );
        return ( ExpressionExperimentSet ) entity;
    }

    /**
     * @see ExpressionExperimentSetDao#loadAll(int)
     */

    public Collection<? extends ExpressionExperimentSet> loadAll() {
        final Collection<? extends ExpressionExperimentSet> results = this.getHibernateTemplate().loadAll(
                ExpressionExperimentSetImpl.class );
        return results;
    }

    /**
     * @see ExpressionExperimentSetDao#remove(java.lang.Long)
     */

    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'id' can not be null" );
        }
        ExpressionExperimentSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    public void remove( Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionExperimentSetDao#remove(ExpressionExperimentSet)
     */
    public void remove( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.remove - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSet );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    public void update( final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( Iterator<? extends ExpressionExperimentSet> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see ExpressionExperimentSetDao#update(ExpressionExperimentSet)
     */
    public void update( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.update - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSet );
    }

    
    @Override
    public Collection<Long> getExperimentIds( Long id ) {

        List<?> o = this.getHibernateTemplate().findByNamedParam(
                "select e.id, i.id from ExpressionExperimentSetImpl e join e.experiments i where e.id = :id", "id", id );
        Collection<Long> results = new ArrayList<Long>();

        for ( Object object : o ) {
            Object[] oa = ( Object[] ) object;
            results.add( ( Long ) oa[1] );
        }

        return results;

    }

}