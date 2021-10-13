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
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openjena.atlas.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledDao;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.ObjectFilterCriteriaUtils;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Paul
 */
@Repository
public class ExpressionAnalysisResultSetDaoImpl extends AbstractFilteringVoEnabledDao<ExpressionAnalysisResultSet, ExpressionAnalysisResultSetValueObject>
        implements ExpressionAnalysisResultSetDao {

    @Autowired
    public ExpressionAnalysisResultSetDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionAnalysisResultSet.class, sessionFactory );
    }

    @Override
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id ) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //noinspection unchecked
        ExpressionAnalysisResultSet ears = ( ExpressionAnalysisResultSet ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select r from ExpressionAnalysisResultSet r "
                                + "left join fetch r.results res "
                                + "left join fetch res.probe p "
                                + "left join fetch res.contrasts c "
                                + "left join fetch c.factorValue "
                                + "left join fetch c.secondFactorValue "
                                + "where r.id = :rs " )
                .setParameter( "rs", id )
                .setReadOnly( true )
                .uniqueResult();
        // this drastically reduces the number of columns fetched which would anyway be repeated
        Hibernate.initialize( ears.getAnalysis() );
        Hibernate.initialize( ears.getAnalysis().getExperimentAnalyzed() );
        // it is faster to query those separately because there's a large number of rows fetched via the results &
        // contrasts and only a handful of factors
        Hibernate.initialize( ears.getExperimentalFactors() );
        for ( ExperimentalFactor ef : ears.getExperimentalFactors() ) {
            Hibernate.initialize( ef.getFactorValues() );
        }
        if ( stopWatch.getTime( TimeUnit.SECONDS ) > 10 ) {
            log.info( "Loaded [" + elementClass.getName() + " id=" + ears.getId() + "] with results and contrasts in " + stopWatch.getTime() + "ms." );
        }
        return ears;
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thaw(ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet thaw( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> res = this.getSessionFactory().getCurrentSession().createQuery(
                "select r from ExpressionAnalysisResultSet r left join fetch r.results res "
                        + "left join fetch res.probe left join fetch res.contrasts "
                        + "left join fetch r.experimentalFactors ef left join fetch ef.factorValues "
                        + "where r = :rs " ).setParameter( "rs", resultSet ).list();

        // FIXME: this check should be unnecessary since we're using outer jointures, unless the result set was
        //  nonexistent in the first place
        assert !res.isEmpty();

        if ( timer.getTime() > 1000 ) {
            Log.info( this.getClass(), "Thaw resultSet " + res.get( 0 ).getId() + " took " + timer.getTime() + "ms" );
        }

        return res.get( 0 );
    }

    @Override
    public void thawLite( final ExpressionAnalysisResultSet resultSet ) {
        Session session = this.getSessionFactory().getCurrentSession();

        session.buildLockRequest( LockOptions.NONE ).lock( resultSet );
        for ( ExperimentalFactor factor : resultSet.getExperimentalFactors() ) {
            Hibernate.initialize( factor );
        }

        Hibernate.initialize( resultSet.getAnalysis() );
        Hibernate.initialize( resultSet.getAnalysis().getExperimentAnalyzed() );
    }

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select a from GeneDifferentialExpressionMetaAnalysis a"
                                + "  inner join a.resultSetsIncluded rs where rs.analysis=:an" )
                .setParameter( "an", differentialExpressionAnalysis ).list().isEmpty();
    }

    @Override
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ( DifferentialExpressionAnalysis ) this.getSessionFactory().getCurrentSession()
                .load( DifferentialExpressionAnalysis.class, differentialExpressionAnalysis.getId() );
        Collection<ExpressionAnalysisResultSet> thawed = new HashSet<>();
        Collection<ExpressionAnalysisResultSet> rss = differentialExpressionAnalysis.getResultSets();
        int size = rss.size();
        int cnt = 0;
        for ( ExpressionAnalysisResultSet rs : rss ) {
            thawed.add( this.thaw( rs ) );
            cnt++;
            Log.info( this.getClass(), "Thawed " + cnt + "/" + size + " resultSets" );
        }
        boolean changed = differentialExpressionAnalysis.getResultSets().addAll( thawed );
        assert !changed; // they are the same objects, just updated.
        return differentialExpressionAnalysis;
    }

    /**
     * @see ExpressionAnalysisResultSetDao#thawWithoutContrasts(ExpressionAnalysisResultSet)
     */
    @Override
    public ExpressionAnalysisResultSet thawWithoutContrasts( final ExpressionAnalysisResultSet resultSet ) {
        StopWatch timer = new StopWatch();
        timer.start();
        this.thawLite( resultSet );

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> res = this.getSessionFactory().getCurrentSession().createQuery(
                "select r from ExpressionAnalysisResultSet r left join fetch r.results res "
                        + "left join fetch res.probe p left join fetch p.biologicalCharacteristic bc "
                        + "left join fetch bc.sequenceDatabaseEntry "
                        + "left join fetch r.experimentalFactors ef left join fetch ef.factorValues "
                        + "where r = :rs " ).setParameter( "rs", resultSet ).list();

        if ( timer.getTime() > 1000 ) {
            Log.info( this.getClass(), "Thaw resultset: " + timer.getTime() + "ms" );
        }

        assert !res.isEmpty();

        return res.get( 0 );

    }

    @Override
    public void remove( ExpressionAnalysisResultSet resultSet ) {

        // Wipe references
        resultSet.setResults( new HashSet<DifferentialExpressionAnalysisResult>() );
        this.update( resultSet );

        // Clear session
        Session session = this.getSessionFactory().getCurrentSession();
        session.flush();
        session.clear();
        session.buildLockRequest( LockOptions.NONE ).lock( resultSet );
        int contrastsDone = 0;
        int resultsDone = 0;

        // Remove results - Not using DifferentialExpressionResultDaoImpl.remove() for speed
        {
            AbstractDao.log.info( "Bulk removing dea results..." );

            // Delete contrasts
            //language=MySQL
            final String nativeDeleteContrastsQuery =
                    "DELETE c FROM CONTRAST_RESULT c, DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d"
                            + " WHERE d.RESULT_SET_FK = :rsid AND d.ID = c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK";
            SQLQuery q = session.createSQLQuery( nativeDeleteContrastsQuery );
            q.setParameter( "rsid", resultSet.getId() );
            contrastsDone += q.executeUpdate(); // cannot use the limit clause for this multi-table remove.

            // Delete AnalysisResults
            //language=MySQL
            String nativeDeleteARQuery = "DELETE d FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d WHERE d.RESULT_SET_FK = :rsid  ";
            q = session.createSQLQuery( nativeDeleteARQuery );
            q.setParameter( "rsid", resultSet.getId() );
            resultsDone += q.executeUpdate();

            AbstractDao.log.info( "Deleted " + contrastsDone + " contrasts, " + resultsDone + " results. Flushing..." );
            session.flush();
            session.clear();
        }

        // Remove result set
        AbstractDao.log.info( "Removing result set " + resultSet.getId() );
        super.remove( resultSet );
        this.getSessionFactory().getCurrentSession().flush();
    }

    @Override
    public Slice<ExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> databaseEntries, List<ObjectFilter[]> objectFilters, int offset, int limit, Sort sort ) {
        Criteria query = getLoadValueObjectsCriteria( bioAssaySets, databaseEntries, objectFilters, sort );
        Criteria totalElementsQuery = getLoadValueObjectsCriteria( bioAssaySets, databaseEntries, objectFilters, sort );

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .setCacheable( true )
                .list();

        Long totalElements = ( Long ) totalElementsQuery
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();

        //noinspection unchecked
        return new Slice<>( super.loadValueObjects( data ), sort, offset, limit, totalElements );
    }

    private Criteria getLoadValueObjectsCriteria( Collection<BioAssaySet> bioAssaySets, Collection<DatabaseEntry> databaseEntries, List<ObjectFilter[]> objectFilters, Sort sort ) {
        Criteria query = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionAnalysisResultSet.class )
                .createAlias( "analysis", "a" )
                .createAlias( "analysis.experimentAnalyzed", "e" );

        if ( bioAssaySets != null ) {
            query.add( Restrictions.in( "a.experimentAnalyzed", bioAssaySets ) );
        }

        if ( databaseEntries != null ) {
            query.add( Restrictions.in( "e.accession", databaseEntries ) );
        }

        if ( objectFilters != null && objectFilters.size() > 0 ) {
            query.add( ObjectFilterCriteriaUtils.formRestrictionClause( objectFilters ) );
        }

        // apply the ACL on the associated EE
        query.add( ObjectFilterCriteriaUtils.formAclRestrictionClause( "e", "ubic.gemma.model.expression.experiment.ExpressionExperiment" ) );

        if ( sort != null ) {
            if ( sort.getDirection() == Sort.Direction.ASC ) {
                query.addOrder( Order.asc( sort.getOrderBy() ) );
            } else if ( sort.getDirection() == Sort.Direction.DESC ) {
                query.addOrder( Order.desc( sort.getOrderBy() ) );
            } else {
                // defaulting to ASC
                query.addOrder( Order.asc( sort.getOrderBy() ) );
            }
        }

        return query;
    }

    @Override
    protected Query getLoadValueObjectsQuery( List<ObjectFilter[]> filters, Sort sort ) {
        throw new NotImplementedException( "This is not supported yet." );
    }

    @Override
    protected Query getCountValueObjectsQuery( List<ObjectFilter[]> filters ) {
        throw new NotImplementedException( "This is not supported yet." );
    }

    @Override
    public ExpressionAnalysisResultSetValueObject loadValueObject( ExpressionAnalysisResultSet entity ) {
        return new ExpressionAnalysisResultSetValueObject( entity );
    }

    @Override
    public ExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet entity ) {
        return new ExpressionAnalysisResultSetValueObject( entity, loadResultToGenesMap( entity ) );
    }

    @Override
    public Map<DifferentialExpressionAnalysisResult, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet ) {
        Query query = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select {result.*}, {gene.*} from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT {result} "
                        + "join GENE2CS on GENE2CS.CS = {result}.PROBE_FK "
                        + "join CHROMOSOME_FEATURE as {gene} on {gene}.ID = GENE2CS.GENE "
                        + "where {result}.RESULT_SET_FK = :rsid" )
                .addEntity( "result", DifferentialExpressionAnalysisResult.class )
                .addEntity( "gene", Gene.class )
                .setParameter( "rsid", resultSet.getId() );

        //noinspection unchecked
        List<Object[]> list = query.list();

        // YAY! my brain was almost fried writing that collector
        return list.stream()
                .collect( Collectors.groupingBy(
                        l -> ( DifferentialExpressionAnalysisResult ) l[0],
                        Collectors.collectingAndThen( Collectors.toList(),
                                elem -> elem.stream()
                                        .map( l -> ( Gene ) l[1] )
                                        .collect( Collectors.toList() ) ) ) );
    }

    @Override
    public String getObjectAlias() {
        return null;
    }
}