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
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @author paul
 */
@Repository
public class ExpressionExperimentSetDaoImpl
        extends VoEnabledDao<ExpressionExperimentSet, ExpressionExperimentSetValueObject>
        implements ExpressionExperimentSetDao {

    private final ExpressionExperimentDao expressionExperimentDao;

    /* ********************************
     * Constructors
     * ********************************/

    @Autowired
    public ExpressionExperimentSetDaoImpl( SessionFactory sessionFactory,
            ExpressionExperimentDao expressionExperimentDao ) {
        super( ExpressionExperimentSet.class, sessionFactory );
        this.expressionExperimentDao = expressionExperimentDao;
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    public Collection<ExpressionExperimentSet> find( BioAssaySet bioAssaySet ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( "select ees from ExpressionExperimentSet ees inner join ees.experiments e where e = :ee" )
                .setParameter( "ee", bioAssaySet ).list();
    }

    @Override
    public Collection<ExpressionExperimentSet> findByName( final String name ) {
        return this.findByProperty( "name", name );
    }

    @Override
    public Collection<ExpressionExperiment> getExperimentsInSet( Long id ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ees.experiments from ExpressionExperimentSet ees where ees.id = :id" )
                .setParameter( "id", id ).list();
    }

    @Override
    public Collection<ExpressionExperimentValueObject> getExperimentValueObjectsInSet( Long id ) {
        //noinspection unchecked
        return expressionExperimentDao.loadValueObjects( this.getSession().createQuery(
                "select i.id from ExpressionExperimentSet eset join eset.experiments i where eset.id = :id" )
                .setParameter( "id", id ).list(), false );
    }

    @Override
    public Collection<ExpressionExperimentSet> loadAllExperimentSetsWithTaxon() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperimentSet.class )
                .add( Restrictions.isNotNull( "taxon" ) ).list();
    }

    @Override
    public Collection<ExpressionExperimentSet> loadAllMultiExperimentSets() {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( ExpressionExperimentSet.class )
                .add( Restrictions.sizeGt( "experiments", 1 ) ).list();
    }

    @Override
    public void thaw( ExpressionExperimentSet expressionExperimentSet ) {
        Session session = this.getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( expressionExperimentSet );
        Hibernate.initialize( expressionExperimentSet );
        Hibernate.initialize( expressionExperimentSet.getTaxon() );
        Hibernate.initialize( expressionExperimentSet.getExperiments() );
    }

    @Override
    public ExpressionExperimentSetValueObject loadValueObject( ExpressionExperimentSet entity ) {
        return this.loadValueObject( entity.getId(), false );
    }

    @Override
    public Collection<ExpressionExperimentSetValueObject> loadAllValueObjects( boolean loadEEIds ) {
        return fetchValueObjects( null, loadEEIds );
    }

    @Override
    public ExpressionExperimentSetValueObject loadValueObject( Long id, boolean loadEEIds ) {
        Collection<ExpressionExperimentSetValueObject> vos = this
                .loadValueObjects( Collections.singleton( id ), loadEEIds );
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

    /**
     * @deprecated extremely inefficient for EESets, use {@link #loadValueObjects(Collection, boolean)} if possible.
     */
    @Deprecated
    @Override
    public Collection<ExpressionExperimentSetValueObject> loadValueObjects(
            Collection<ExpressionExperimentSet> entities ) {
        return this.loadValueObjects( EntityUtils.getIds( entities ), false );
    }

    /* ********************************
     * Private methods
     * ********************************/

    private Collection<Long> getExperimentIdsInSet( Long setId ) {
        //noinspection unchecked
        return this.getHibernateTemplate().findByNamedParam(
                "select i.id from ExpressionExperimentSet eset join eset.experiments i where eset.id = :id", "id",
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
        List<Object[]> withCoexp = this.getSessionFactory().getCurrentSession().createQuery(
                "select e.id, count(an) from ExpressionExperimentSet e, CoexpressionAnalysis an join e.experiments ea "
                        + "where an.experimentAnalyzed = ea and e.id in (:ids) group by e.id" )
                .setParameterList( "ids", idMap.keySet() ).list();

        for ( Object[] oa : withCoexp ) {
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
                        + "from ExpressionExperimentSet e, DifferentialExpressionAnalysis an join e.experiments ea "
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
                + " from ExpressionExperimentSet as eeset inner join eeset.taxon taxon inner join eeset.experiments ees "
                + idClause + " group by eeset.id ";

        Query queryObject = this.getSessionFactory().getCurrentSession().createQuery( queryString );
        if ( ids != null )
            queryObject.setParameterList( "ids", ids );
        return queryObject;
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
                v = new ExpressionExperimentSetValueObject( eeId );
                vo.put( eeId, v );
            }
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
}