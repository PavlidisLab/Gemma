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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractCriteriaFilteringVoEnabledDao;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Paul
 */
@Repository
@CommonsLog
public class ExpressionAnalysisResultSetDaoImpl extends AbstractCriteriaFilteringVoEnabledDao<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject>
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
                .uniqueResult();
        if ( ears == null ) {
            return null;
        }
        this.thaw( ears );
        if ( stopWatch.getTime( TimeUnit.SECONDS ) > 1 ) {
            log.info( "Loaded [" + elementClass.getName() + " id=" + id + "] with results and contrasts in " + stopWatch.getTime() + "ms." );
        }
        return ears;
    }

    @Override
    public void thaw( final ExpressionAnalysisResultSet resultSet ) {
        // this drastically reduces the number of columns fetched which would anyway be repeated
        Hibernate.initialize( resultSet.getAnalysis() );
        Hibernate.initialize( resultSet.getAnalysis().getExperimentAnalyzed() );
        // it is faster to query those separately because there's a large number of rows fetched via the results &
        // contrasts and only a handful of factors
        Hibernate.initialize( resultSet.getExperimentalFactors() );
        // factor values are always eagerly fetched (see ExperimentalFactor.hbm.xml), so we don't need to initialize.
        // I still think it's neat to use stream API for that though in case we ever make them lazy:
        // resultSet.getExperimentalFactors().stream().forEach( Hibernate::initialize );
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
            thawed.add( loadWithResultsAndContrasts( rs.getId() ) );
            cnt++;
            log.info( "Thawed " + cnt + "/" + size + " resultSets" );
        }
        boolean changed = differentialExpressionAnalysis.getResultSets().addAll( thawed );
        assert !changed; // they are the same objects, just updated.
        return differentialExpressionAnalysis;
    }

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select a from GeneDifferentialExpressionMetaAnalysis a"
                                + "  inner join a.resultSetsIncluded rs where rs.analysis=:an" )
                .setParameter( "an", differentialExpressionAnalysis ).list().isEmpty();
    }

    @Override
    public void remove( ExpressionAnalysisResultSet resultSet ) {

        // Wipe references
        resultSet.setResults( new HashSet<>() );
        this.update( resultSet );

        // Clear session
        Session session = this.getSessionFactory().getCurrentSession();
        session.flush();
        session.clear();
        reattach( resultSet );
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
        flush();
    }

    @Override
    public Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> databaseEntries, @Nullable Filters objectFilters, int offset, int limit, @Nullable Sort sort ) {
        Criteria query = getLoadValueObjectsCriteria( objectFilters );
        Criteria totalElementsQuery = getLoadValueObjectsCriteria( objectFilters );

        if ( bioAssaySets != null ) {
            query.add( Restrictions.in( "a.experimentAnalyzed", bioAssaySets ) );
            totalElementsQuery.add( Restrictions.in( "a.experimentAnalyzed", bioAssaySets ) );
        }

        if ( databaseEntries != null ) {
            query.add( Restrictions.in( "e.accession", databaseEntries ) );
            totalElementsQuery.add( Restrictions.in( "e.accession", databaseEntries ) );
        }

        //noinspection unchecked
        List<ExpressionAnalysisResultSet> data = query.setResultTransformer( Criteria.DISTINCT_ROOT_ENTITY )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();

        Long totalElements = ( Long ) totalElementsQuery
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();

        // initialize subset factor values
        for ( ExpressionAnalysisResultSet d : data ) {
            Hibernate.initialize( d.getAnalysis().getSubsetFactorValue() );
        }

        return new Slice<>( super.loadValueObjects( data ), sort, offset, limit, totalElements );
    }

    @Override
    protected Criteria getLoadValueObjectsCriteria( @Nullable Filters filters ) {
        Criteria query = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionAnalysisResultSet.class )
                // these two are necessary for ACL filtering, so we must use a (default) inner jointure
                .createAlias( "analysis", "a" )
                .createAlias( "analysis.experimentAnalyzed", "e" )
                .createAlias( "experimentalFactors", "ef", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "ef.factorValues", "fv", JoinType.LEFT_OUTER_JOIN );

        // apply filtering
        query.add( ObjectFilterCriteriaUtils.formRestrictionClause( filters ) );

        // apply the ACL on the associated EE (or EE subset)
        query.add( Restrictions.or(
                AclCriteriaUtils.formAclRestrictionClause( "e", ExpressionExperiment.class ),
                AclCriteriaUtils.formAclRestrictionClause( "e", ExpressionExperimentSubSet.class ) ) );

        return query;
    }

    @Override
    protected DifferentialExpressionAnalysisResultSetValueObject doLoadValueObject( ExpressionAnalysisResultSet entity ) {
        return new DifferentialExpressionAnalysisResultSetValueObject( entity );
    }

    @Override
    public DifferentialExpressionAnalysisResultSetValueObject loadValueObjectWithResults( ExpressionAnalysisResultSet entity ) {
        return new DifferentialExpressionAnalysisResultSetValueObject( entity, loadResultToGenesMap( entity ) );
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
                .setParameter( "rsid", resultSet.getId() )
                .setCacheable( true );

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
}