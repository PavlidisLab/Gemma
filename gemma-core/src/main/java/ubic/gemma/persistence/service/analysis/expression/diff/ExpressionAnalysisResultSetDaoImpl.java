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
import org.hibernate.criterion.*;
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
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractFilteringVoEnabledDao;
import ubic.gemma.persistence.util.*;

import java.util.*;

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
        Criteria query = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionAnalysisResultSet.class )
                .setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
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

        query.setFirstResult( offset );
        query.setMaxResults( limit );
        query.setCacheable( true );

        //noinspection unchecked
        return new Slice<>( super.loadValueObjects( ( List<ExpressionAnalysisResultSet> ) query.list() ), sort, offset, limit, null );
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
    public String getObjectAlias() {
        return "resultSet";
    }
}