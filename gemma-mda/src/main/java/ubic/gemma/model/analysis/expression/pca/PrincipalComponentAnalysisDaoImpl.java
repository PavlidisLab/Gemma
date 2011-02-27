/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.analysis.expression.pca;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
@Repository
public class PrincipalComponentAnalysisDaoImpl extends HibernateDaoSupport implements PrincipalComponentAnalysisDao {

    @Autowired
    public PrincipalComponentAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    @Override
    public Collection<? extends PrincipalComponentAnalysis> create(
            Collection<? extends PrincipalComponentAnalysis> entities ) {
        this.getHibernateTemplate().saveOrUpdateAll( entities );
        return entities;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public PrincipalComponentAnalysis create( PrincipalComponentAnalysis entity ) {
        this.getHibernateTemplate().save( entity );
        return entity;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.PrincipalComponentAnalysisDao#findByExperiment(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public PrincipalComponentAnalysis findByExperiment( ExpressionExperiment ee ) {
        if ( ee == null || ee.getId() == null ) return null;
        List<?> fetched = this.getHibernateTemplate().findByNamedParam(
                "select p from PrincipalComponentAnalysisImpl as p where p.experimentAnalyzed = :ee", "ee", ee );
        if ( fetched.isEmpty() ) return null;

        if ( fetched.size() > 1 ) {
            // throw new IllegalStateException( "There are multiple PCAs for experiment: " + ee );
            // return fetch.get(0);
        }

        return ( PrincipalComponentAnalysis ) fetched.get( 0 );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.PrincipalComponentAnalysisDao#getTopLoadedProbes(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment, int, int)
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        if ( ee == null || ee.getId() == null ) return new ArrayList<ProbeLoading>();
        HibernateTemplate t = new HibernateTemplate( getSessionFactory() );
        t.setMaxResults( count );
        return t.findByNamedParam( "select pr from PrincipalComponentAnalysisImpl p join p.probeLoadings pr"
                + " where p.experimentAnalyzed = :ee and pr.componentNumber = :cmp order by pr.loadingRank ",
                new String[] { "ee", "cmp" }, new Object[] { ee, component } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.util.Collection)
     */
    @Override
    public Collection<? extends PrincipalComponentAnalysis> load( Collection<Long> ids ) {
        Collection<PrincipalComponentAnalysis> result = new HashSet<PrincipalComponentAnalysis>();
        for ( Long id : ids ) {
            Object loaded = this.load( id );
            if ( loaded != null ) {
                result.add( ( PrincipalComponentAnalysis ) loaded );
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#load(java.lang.Long)
     */
    @Override
    public PrincipalComponentAnalysis load( Long id ) {
        return this.getHibernateTemplate().load( PrincipalComponentAnalysisImpl.class, id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#loadAll()
     */
    @Override
    public Collection<? extends PrincipalComponentAnalysis> loadAll() {
        return this.getHibernateTemplate().loadAll( PrincipalComponentAnalysisImpl.class );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    @Override
    public void remove( Collection<? extends PrincipalComponentAnalysis> entities ) {
        this.getHibernateTemplate().deleteAll( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
    @Override
    public void remove( Long id ) {
        this.getHibernateTemplate().delete( this.load( id ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Object)
     */
    @Override
    public void remove( PrincipalComponentAnalysis entity ) {
        if ( entity == null ) return;
        this.getHibernateTemplate().delete( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.util.Collection)
     */
    @Override
    public void update( Collection<? extends PrincipalComponentAnalysis> entities ) {
        for ( PrincipalComponentAnalysis principalComponentAnalysis : entities ) {
            this.update( principalComponentAnalysis );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#update(java.lang.Object)
     */
    @Override
    public void update( PrincipalComponentAnalysis entity ) {
        this.getHibernateTemplate().update( entity );

    }

}
