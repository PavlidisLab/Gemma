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
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.protocol.Protocol;
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
        StopWatch stopWatch = StopWatch.createStarted();
        ExpressionAnalysisResultSet ears = load( id );
        if ( ears == null ) {
            return null;
        }
        StopWatch resultTimer = StopWatch.createStarted();
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResult> results = getSessionFactory().getCurrentSession()
                .createQuery( "select res from DifferentialExpressionAnalysisResult res left join fetch res.contrasts c where res.resultSet = :rs" )
                .setParameter( "rs", ears )
                .list();
        ears.setResults( new HashSet<>( results ) );
        resultTimer.stop();
        StopWatch probeTimer = StopWatch.createStarted();
        // probes and genes are almost certainly cached
        for ( DifferentialExpressionAnalysisResult r : ears.getResults() ) {
            Hibernate.initialize( r.getProbe() );
        }
        probeTimer.stop();
        if ( stopWatch.getTime( TimeUnit.SECONDS ) > 1 ) {
            log.info( String.format( "Loaded [%s id=%d] with results and contrasts in %d ms (results and contrasts: %d ms, probes: %d ms).",
                    elementClass.getName(), id, stopWatch.getTime(), resultTimer.getTime(), probeTimer.getTime() ) );
        }
        return ears;
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
        // flush any pending changes before running a bulk query
        Session session = this.getSessionFactory().getCurrentSession();
        session.flush();

        // Remove results - Not using DifferentialExpressionResultDaoImpl.remove() for speed
        {
            int contrastsDone = 0;
            int resultsDone = 0;

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
        }

        // clear manually removed result sets
        resultSet.setResults( new HashSet<>() );

        // Remove result set
        AbstractDao.log.info( "Removing result set " + resultSet.getId() );
        super.remove( resultSet );
        flush();
    }

    @Override
    public Slice<DifferentialExpressionAnalysisResultSetValueObject> findByBioAssaySetInAndDatabaseEntryInLimit( @Nullable Collection<BioAssaySet> bioAssaySets, @Nullable Collection<DatabaseEntry> databaseEntries, @Nullable Filters filters, int offset, int limit, @Nullable Sort sort ) {
        Criteria query = getFilteringCriteria( filters );
        Criteria totalElementsQuery = getFilteringCriteria( filters );

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
    public void thaw( ExpressionAnalysisResultSet ears ) {
        // this drastically reduces the number of columns fetched which would anyway be repeated
        Hibernate.initialize( ears.getAnalysis() );
        Hibernate.initialize( ears.getAnalysis().getExperimentAnalyzed() );
        // it is faster to query those separately because there's a large number of rows fetched via the results &
        // contrasts and only a handful of factors
        Hibernate.initialize( ears.getExperimentalFactors() );
        // factor values are always eagerly fetched (see ExperimentalFactor.hbm.xml), so we don't need to initialize.
        // I still think it's neat to use stream API for that though in case we ever make them lazy:
        // resultSet.getExperimentalFactors().stream().forEach( Hibernate::initialize );
    }

    @Override
    protected Criteria getFilteringCriteria( @Nullable Filters filters ) {
        Criteria query = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionAnalysisResultSet.class )
                // these two are necessary for ACL filtering, so we must use a (default) inner jointure
                .createAlias( "analysis", "a" )
                .createAlias( "analysis.experimentAnalyzed", "e" )
                // we need a left outer jointure so that we do not miss any result set that lacks one of these associations
                // these aliases are necessary to resolve filterable properties
                .createAlias( "analysis.experimentAnalyzed.accession", "ea", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "analysis.protocol", "p", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "analysis.subsetFactorValue", "sfv", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "analysis.subsetFactorValue.characteristics", "sfvc", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "baselineGroup", "b", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "baselineGroup.characteristics", "bc", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "baselineGroup.experimentalFactor", "bef", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "baselineGroup.measurement", "bm", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "pvalueDistribution", "pvd", JoinType.LEFT_OUTER_JOIN )
                // these are used for filtering
                .createAlias( "experimentalFactors", "ef", JoinType.LEFT_OUTER_JOIN )
                .createAlias( "ef.factorValues", "fv", JoinType.LEFT_OUTER_JOIN );

        // apply filtering
        query.add( FilterCriteriaUtils.formRestrictionClause( filters ) );

        // apply the ACL on the associated EE (or EE subset)
        query.add( Restrictions.or(
                AclCriteriaUtils.formAclRestrictionClause( "e.id", ExpressionExperiment.class ),
                AclCriteriaUtils.formAclRestrictionClause( "e.id", ExpressionExperimentSubSet.class ) ) );

        return query;
    }

    @Override
    protected void configureFilterableProperties( FilterablePropertiesConfigurer configurer ) {
        super.configureFilterableProperties( configurer );

        // this column is mostly null
        configurer.unregisterProperty( "analysis.name" );

        // this is useless
        configurer.unregisterEntity( "analysis.protocol.", Protocol.class );

        // use the characteristics instead
        configurer.registerAlias( "analysis.subsetFactorValue.characteristics.", "sfvc", Characteristic.class, null, 1 );
        configurer.unregisterProperty( "analysis.subsetFactorValue.characteristics.originalValue" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.value" );

        // baseline is always baseline
        configurer.unregisterProperty( "baselineGroup.isBaseline" );
        configurer.registerAlias( "baselineGroup.characteristics.", "bc", Characteristic.class, null, 1 );
        configurer.unregisterProperty( "baselineGroup.characteristics.originalValue" );
        configurer.unregisterProperty( "baselineGroup.value" );

        // not relevant
        configurer.unregisterProperty( "hitListSizes.size" );
        configurer.unregisterEntity( "pvalueDistribution.", PvalueDistribution.class );

        // FIXME: these cause a org.hibernate.MappingException: Unknown collection role exception (see https://github.com/PavlidisLab/Gemma/issues/518)
        configurer.unregisterProperty( "analysis.experimentAnalyzed.characteristics.size" );
        configurer.unregisterProperty( "analysis.experimentAnalyzed.otherRelevantPublications.size" );
        configurer.unregisterProperty( "experimentalFactors.size" );
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
    public Map<Long, List<Gene>> loadResultToGenesMap( ExpressionAnalysisResultSet resultSet ) {
        Query query = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select result.ID as RESULT_ID, {gene.*} from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT result "
                        + "join GENE2CS on GENE2CS.CS = result.PROBE_FK "
                        + "join CHROMOSOME_FEATURE as {gene} on {gene}.ID = GENE2CS.GENE "
                        + "where result.RESULT_SET_FK = :rsid" )
                .addScalar( "RESULT_ID", StandardBasicTypes.LONG )
                .addEntity( "gene", Gene.class )
                .setParameter( "rsid", resultSet.getId() )
                .setCacheable( true );

        //noinspection unchecked
        List<Object[]> list = query.list();

        // YAY! my brain was almost fried writing that collector
        return list.stream()
                .collect( Collectors.groupingBy(
                        l -> ( Long ) l[0],
                        Collectors.collectingAndThen( Collectors.toList(),
                                elem -> elem.stream()
                                        .map( l -> ( Gene ) l[1] )
                                        .collect( Collectors.toList() ) ) ) );
    }
}