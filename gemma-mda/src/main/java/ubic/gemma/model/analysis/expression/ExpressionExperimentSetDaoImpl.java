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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.util.EntityUtils;

/**
 * @see ubic.gemma.model.analysis.ExpressionExperimentSet
 * @version $Id$
 * @author paul
 */
@Repository
public class ExpressionExperimentSetDaoImpl extends HibernateDaoSupport implements ExpressionExperimentSetDao {

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    public ExpressionExperimentSetDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.util.Collection)
     */
    @Override
    public Collection<? extends ExpressionExperimentSet> create(
            final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    @Override
    public ExpressionExperimentSet create( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.create - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSet );
        return expressionExperimentSet;
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#find(ubic.gemma.model.expression.experiment.
     * BioAssaySet)
     */
    @Override
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees from ExpressionExperimentSetImpl ees inner join ees.experiments e where e = :ee", "ee",
                bioAssaySet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#findByName(java.lang.String)
     */
    @Override
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
    @Override
    public Collection<ExpressionAnalysis> getAnalyses( final ExpressionExperimentSet expressionExperimentSet ) {
        try {
            return this.handleGetAnalyses( expressionExperimentSet );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.getAnalyses(ExpressionExperimentSet expressionExperimentSet)' --> "
                            + th, th );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#getExperimentsInSet(java.lang.Long)
     */
    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees.experiments from ExpressionExperimentSetImpl ees where ees.id = :id", "id", id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#getExperimentValueObjectsInSet(java.lang.Long)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id ) {
        return expressionExperimentDao
                .loadValueObjects(
                        this.getHibernateTemplate()
                                .findByNamedParam(
                                        "select i.id from ExpressionExperimentSetImpl eset join eset.experiments i where eset.id = :id",
                                        "id", id ), false );
    }

    @Override
    public Collection<? extends ExpressionExperimentSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where id in (:ids)",
                "ids", ids );
    }

    /**
     * @see ExpressionExperimentSetDao#load(int, java.lang.Long)
     */

    @Override
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

    @Override
    public Collection<? extends ExpressionExperimentSet> loadAll() {
        final Collection<? extends ExpressionExperimentSet> results = this.getHibernateTemplate().loadAll(
                ExpressionExperimentSetImpl.class );
        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllExperimentSetsWithTaxon()
     */
    @Override
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where ees.taxon is not null" );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDao#loadAllMultiExperimentSets()
     */
    @Override
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getHibernateTemplate().find(
                "select ees from ExpressionExperimentSetImpl ees where size(ees.experiments) > 1" );
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadAllValueObjects() {

        return fetchValueObjects( null );
    }

    @Override
    public ExpressionExperimentSetValueObject loadValueObject( Long id ) {
        Collection<Long> setIds = new HashSet<Long>();
        setIds.add( id );
        Collection<ExpressionExperimentSetValueObject> vos = this.loadValueObjects( setIds );
        if ( vos.isEmpty() ) {
            return null;
        }
        return vos.iterator().next();
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadValueObjects( Collection<Long> eeSetIds ) {

        return fetchValueObjects( eeSetIds );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    @Override
    public void remove( Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionExperimentSetDao#remove(ExpressionExperimentSet)
     */
    @Override
    public void remove( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.remove - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSet );
    }

    /**
     * @see ExpressionExperimentSetDao#remove(java.lang.Long)
     */

    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'id' can not be null" );
        }
        ExpressionExperimentSet entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
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
            public Object doInHibernate( Session session ) throws HibernateException {
                session.buildLockRequest( LockOptions.NONE ).lock( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet );
                Hibernate.initialize( expressionExperimentSet.getTaxon() );
                Hibernate.initialize( expressionExperimentSet.getExperiments() );
                return null;
            }
        } );

    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
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
    @Override
    public void update( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.update - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSet );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleFindByName(java.lang.String)
     */
    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getHibernateTemplate().findByNamedParam( "from ExpressionExperimentSetImpl where name=:query",
                "query", name );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.ExpressionExperimentSetDaoBase#handleGetAnalyses(ubic.gemma.model.analysis
     * .expression.ExpressionExperimentSet)
     */
    protected Collection<ExpressionAnalysis> handleGetAnalyses( ExpressionExperimentSet expressionExperimentSet )
            throws Exception {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select a from ExpressionAnalysisImpl a inner join a.expressionExperimentSetAnalyzed ees where ees = :eeset ",
                        "eeset", expressionExperimentSet );
    }

    /**
     * @param ids, if null fetch all.
     * @return
     */
    private Collection<ExpressionExperimentSetValueObject> fetchValueObjects( Collection<Long> ids ) {
        Map<Long, ExpressionExperimentSetValueObject> vo = new LinkedHashMap<Long, ExpressionExperimentSetValueObject>();
        Query queryObject = this.getLoadValueObjectsQueryString( ids );
        List<?> list = queryObject.list();
        for ( Object object : list ) {

            Object[] res = ( Object[] ) object;

            Long eeId = ( Long ) res[0];

            assert eeId != null;

            ExpressionExperimentSetValueObject v;
            if ( vo.containsKey( eeId ) ) {
                v = vo.get( eeId );
            } else {
                v = new ExpressionExperimentSetValueObject();
                v.setId( eeId );
                vo.put( eeId, v );
            }

            v.setId( eeId );
            v.setName( ( String ) res[1] );
            v.setDescription( ( String ) res[2] );

            v.setTaxonName( ( String ) res[3] );
            v.setTaxonId( ( Long ) res[4] );
            v.setNumExperiments( ( ( Long ) res[5] ).intValue() );
            vo.put( eeId, v );

        }

        Collection<ExpressionExperimentSetValueObject> result = vo.values();

        /*
         * populate 'modifiable' - if it has any analysis attached to it, it is not. Might want to make this optional.
         */
        if ( !result.isEmpty() ) {
            populateModifiable( result );
        }
        return result;
    }

    private void populateModifiable( Collection<ExpressionExperimentSetValueObject> eeSets ) {

        /*
         * This can be sped up by checking if any are master sets.
         */

        Map<Long, ExpressionExperimentSetValueObject> idMap = EntityUtils.getIdMap( eeSets );

        Set<Long> ids = idMap.keySet();

        /*
         * Currently the only type of analysis that ties up EEsets is the GenecoexpressionAnalysis.
         */
        List<Long> unmodifiable = this.getHibernateTemplate().findByNamedParam(
                "select es.id from GeneCoexpressionAnalysisImpl m "
                        + "join m.expressionExperimentSetAnalyzed es where es.id in (:ids)", "ids", ids );

        for ( Long id : unmodifiable ) {
            idMap.get( id ).setModifiable( false );
        }

    }

    /**
     * @param ids
     * @return
     */
    private Query getLoadValueObjectsQueryString( Collection<Long> ids ) {

        String idClause = "";
        if ( ids != null ) {
            if ( ids.isEmpty() ) {
                throw new IllegalArgumentException( "If provided ids cannot be empty" );
            }
            idClause = " where eeset.id in (:ids)";
        }

        String queryString = "select eeset.id , " // 0
                + "eeset.name, " // 1
                + "eeset.description, " // 2
                + "taxon.commonName," // 3
                + "taxon.id," // 4
                + " count(ees) " // 5
                + " from ExpressionExperimentSetImpl as eeset inner join eeset.taxon taxon inner join eeset.experiments ees "
                + idClause + " group by eeset.id ";

        Query queryObject = super.getSession().createQuery( queryString );
        if ( ids != null ) queryObject.setParameterList( "ids", ids );
        return queryObject;
    }

}