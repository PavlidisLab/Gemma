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
package ubic.gemma.persistence.service.analysis.expression;

import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSetImpl;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author paul
 */
@Repository
public class ExpressionExperimentSetDaoImpl extends HibernateDaoSupport implements ExpressionExperimentSetDao {

    private static Logger log = LoggerFactory.getLogger( ExpressionExperimentSetDaoImpl.class );

    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    public ExpressionExperimentSetDaoImpl( SessionFactory sessionFactory,
            ExpressionExperimentDao expressionExperimentDao ) {
        super.setSessionFactory( sessionFactory );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    @Override
    public Collection<? extends ExpressionExperimentSet> create(
            final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.create - 'entities' can not be null" );
        }

        for ( ExpressionExperimentSet entity : entities ) {
            create( entity );
        }

        return entities;
    }

    @Override
    public ExpressionExperimentSet create( final ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.create - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperimentSet );
        return expressionExperimentSet;
    }

    @Override
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select ees from ExpressionExperimentSetImpl ees inner join ees.experiments e where e = :ee", "ee",
                bioAssaySet );
    }

    @Override
    public Collection<ExpressionExperimentSet> findByName( final java.lang.String name ) {
        try {
            return this.handleFindByName( name );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'ExpressionExperimentSetDao.findByName(java.lang.String name)' --> " + th, th );
        }
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "select ees.experiments from ExpressionExperimentSetImpl ees where ees.id = :id",
                        "id", id );
    }

    @Override
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id ) {
        return expressionExperimentDao.loadValueObjects( this.getHibernateTemplate().findByNamedParam(
                "select i.id from ExpressionExperimentSetImpl eset join eset.experiments i where eset.id = :id", "id",
                id ), false );
    }

    @Override
    public Collection<? extends ExpressionExperimentSet> load( Collection<Long> ids ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "from ExpressionExperimentSetImpl where id in (:ids)", "ids", ids );
    }

    @Override
    public ExpressionExperimentSet load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ExpressionExperimentSetImpl.class, id );
        return ( ExpressionExperimentSet ) entity;
    }

    @Override
    public Collection<? extends ExpressionExperimentSet> loadAll() {
        return this.getHibernateTemplate().loadAll( ExpressionExperimentSetImpl.class );
    }

    @Override
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        return this.getHibernateTemplate()
                .find( "select ees from ExpressionExperimentSetImpl ees where ees.taxon is not null" );
    }

    @Override
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        return this.getHibernateTemplate()
                .find( "select ees from ExpressionExperimentSetImpl ees where size(ees.experiments) > 1" );
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadAllValueObjects( boolean loadEEIds ) {
        return fetchValueObjects( null, loadEEIds );
    }

    @Override
    public ExpressionExperimentSetValueObject loadValueObject( Long id, boolean loadEEIds ) {
        Collection<Long> setIds = new HashSet<>();
        setIds.add( id );

        Collection<ExpressionExperimentSetValueObject> vos = this.loadValueObjects( setIds, loadEEIds );
        if ( vos.isEmpty() ) {
            return null;
        }
        return vos.iterator().next();
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadValueObjects( Collection<Long> eeSetIds,
            boolean loadEEIds ) {
        return fetchValueObjects( eeSetIds, loadEEIds );
    }

    @Override
    public void remove( Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    @Override
    public void remove( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.remove - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperimentSet );
    }

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

    @Override
    public void thaw( final ExpressionExperimentSet expressionExperimentSet ) {
        Session sess = this.getSessionFactory().getCurrentSession();
        sess.buildLockRequest( LockOptions.NONE ).lock( expressionExperimentSet );
        Hibernate.initialize( expressionExperimentSet );
        Hibernate.initialize( expressionExperimentSet.getTaxon() );
        Hibernate.initialize( expressionExperimentSet.getExperiments() );
    }

    @Override
    public void update( final Collection<? extends ExpressionExperimentSet> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperimentSet.update - 'entities' can not be null" );
        }

        for ( ExpressionExperimentSet entity : entities ) {
            update( entity );
        }
    }

    @Override
    public void update( ExpressionExperimentSet expressionExperimentSet ) {
        if ( expressionExperimentSet == null ) {
            throw new IllegalArgumentException(
                    "ExpressionExperimentSet.update - 'expressionExperimentSet' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperimentSet );
    }

    protected Collection<ExpressionExperimentSet> handleFindByName( String name ) throws Exception {
        return this.getHibernateTemplate()
                .findByNamedParam( "from ExpressionExperimentSetImpl where name=:query", "query", name );
    }

    /**
     * @param ids,      if null fetch all.
     * @param loadEEIds whether the returned value object should have the ExpressionExperimentIds collection populated.
     *                  This might be a useful information, but loading the IDs takes slightly longer, so for larger amount of
     *                  EESets this might want to be avoided.
     */
    private Collection<ExpressionExperimentSetValueObject> fetchValueObjects( Collection<Long> ids,
            boolean loadEEIds ) {
        Map<Long, ExpressionExperimentSetValueObject> vo = new LinkedHashMap<>();
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

            /*
             * FIXME this is not adequate because these are not security filtered, so the count could be too high for
             * the current user. We can avoid a lot of problems by not putting private data sets in public EE sets.
             */
            v.setSize( ( ( Long ) res[5] ).intValue() );

            // Add experiment ids
            if ( loadEEIds ) {
                v.setExpressionExperimentIds( this.getExperimentIdsInSet( eeId ) );
            }

            vo.put( eeId, v );

        }

        Collection<ExpressionExperimentSetValueObject> result = vo.values();
        populateAnalysisInformation( result );
        return result;
    }

    private Collection<Long> getExperimentIdsInSet( Long setId ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select i.id from ExpressionExperimentSetImpl eset join eset.experiments i where eset.id = :id", "id",
                setId );
    }

    private void populateAnalysisInformation( Collection<ExpressionExperimentSetValueObject> vo ) {
        if ( vo.isEmpty() ) {
            return;
        }

        Map<Long, ExpressionExperimentSetValueObject> idMap = EntityUtils.getIdMap( vo );

        StopWatch timer = new StopWatch();
        timer.start();
        //noinspection unchecked
        List<Object[]> withCoex = this.getSessionFactory().getCurrentSession().createQuery(
                "select e.id, count(an) from ExpressionExperimentSetImpl e, CoexpressionAnalysisImpl an join e.experiments ea "
                        + "where an.experimentAnalyzed = ea and e.id in (:ids) group by e.id" )
                .setParameterList( "ids", idMap.keySet() ).list();

        for ( Object[] oa : withCoex ) {
            Long id = ( Long ) oa[0];
            Integer c = ( ( Long ) oa[1] ).intValue();
            idMap.get( id ).setNumWithCoexpressionAnalysis( c );
        }

        /*
         * We're counting the number of data sets that have analyses, not the number of analyses (since a data set can
         * have more than one)
         */
        //noinspection unchecked
        List<Object[]> withDiffEx = this.getSessionFactory().getCurrentSession().createQuery(
                "select e.id, count(distinct an.experimentAnalyzed) "
                        + "from ExpressionExperimentSetImpl e, DifferentialExpressionAnalysisImpl an join e.experiments ea "
                        + "where an.experimentAnalyzed = ea and e.id in (:ids) group by e.id" )
                .setParameterList( "ids", idMap.keySet() ).list();

        for ( Object[] oa : withDiffEx ) {
            Long id = ( Long ) oa[0];
            Integer c = ( ( Long ) oa[1] ).intValue();
            assert c <= idMap.get( id ).getSize();
            idMap.get( id ).setNumWithDifferentialExpressionAnalysis( c );
        }

        if ( timer.getTime() > 200 ) {
            log.info( "Fetch analysis counts for " + vo.size() + " ee sets: " + timer.getTime() + "ms" );
        }
    }

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

        Query queryObject = super.getSessionFactory().getCurrentSession().createQuery( queryString );
        if ( ids != null )
            queryObject.setParameterList( "ids", ids );
        return queryObject;
    }

}