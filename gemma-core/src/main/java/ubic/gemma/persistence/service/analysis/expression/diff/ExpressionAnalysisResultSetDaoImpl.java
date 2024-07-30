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

import lombok.Value;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractCriteriaFilteringVoEnabledDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.GENE2CS_QUERY_SPACE;
import static ubic.gemma.persistence.util.QueryUtils.listByBatch;
import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 * @author Paul
 */
@Repository
@CommonsLog
public class ExpressionAnalysisResultSetDaoImpl extends AbstractCriteriaFilteringVoEnabledDao<ExpressionAnalysisResultSet, DifferentialExpressionAnalysisResultSetValueObject>
        implements ExpressionAnalysisResultSetDao {

    /**
     * FIXME: this projection only selects the ID of the result set, which is subsequently fetched. It would be more
     *        efficient to fetch all the necessary columns instead, but I don't know how to do that.
     */
    private final Projection rootEntityProjection;

    @Autowired
    public ExpressionAnalysisResultSetDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionAnalysisResultSet.class, sessionFactory );
        rootEntityProjection = Projections.sqlGroupProjection(
                "{alias}.ID",
                "{alias}.ID",
                new String[] { "ID" },
                new Type[] { sessionFactory.getTypeHelper().entity( ExpressionAnalysisResultSet.class ) } );
    }

    @Override
    public ExpressionAnalysisResultSet create( ExpressionAnalysisResultSet entity ) {
        throw new UnsupportedOperationException( "Individual result sets cannot be created directly, use DifferentialExpressionAnalysisDao.create() instead." );
    }

    @Override
    public void remove( ExpressionAnalysisResultSet entity ) {
        throw new UnsupportedOperationException( "Individual result sets cannot be removed directly, use DifferentialExpressionAnalysisDao.remove() instead." );
    }

    @Override
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id ) {
        StopWatch timer = StopWatch.createStarted();
        ExpressionAnalysisResultSet ears = load( id );
        if ( ears != null ) {
            //noinspection unchecked
            List<DifferentialExpressionAnalysisResult> results = ( List<DifferentialExpressionAnalysisResult> ) getSessionFactory().getCurrentSession()
                    .createQuery( "select res from DifferentialExpressionAnalysisResult res "
                            + "where res.resultSet = :ears "
                            // ascending, nulls last
                            + "order by -res.correctedPvalue desc" )
                    .setParameter( "ears", ears )
                    .list();
            // using separate loops ensure that hibernate can batch-initialize without interleaving queries
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getProbe() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getContrasts() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                for ( ContrastResult cr : r.getContrasts() ) {
                    Hibernate.initialize( cr.getFactorValue() );
                    Hibernate.initialize( cr.getSecondFactorValue() );
                }
            }
            // preserve order of results
            ears.setResults( new LinkedHashSet<>( results ) );
        }
        if ( timer.getTime() > 5000 ) {
            log.info( String.format( "Loaded [%s id=%d] with results, probes and contrasts in %d ms.",
                    elementClass.getName(), id, timer.getTime() ) );
        }
        return ears;
    }

    @Override
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id, int offset, int limit ) {
        if ( offset == 0 && id == -1 ) {
            return loadWithResultsAndContrasts( id );
        }
        StopWatch timer = StopWatch.createStarted();
        ExpressionAnalysisResultSet ears = load( id );
        if ( ears != null ) {
            //noinspection unchecked
            List<DifferentialExpressionAnalysisResult> results = ( List<DifferentialExpressionAnalysisResult> ) getSessionFactory().getCurrentSession()
                    .createQuery( "select res from DifferentialExpressionAnalysisResult res "
                            + "where res.resultSet = :ears "
                            // ascending, nulls last
                            + "order by -res.correctedPvalue desc" )
                    .setParameter( "ears", ears )
                    .setFirstResult( offset )
                    .setMaxResults( limit )
                    .list();
            // using separate loops ensure that hibernate can batch-initialize without interleaving queries
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getProbe() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getContrasts() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                for ( ContrastResult cr : r.getContrasts() ) {
                    Hibernate.initialize( cr.getFactorValue() );
                    Hibernate.initialize( cr.getSecondFactorValue() );
                }
            }
            // preserve order of results
            ears.setResults( new LinkedHashSet<>( results ) );
        }
        if ( timer.getTime() > 100 ) {
            log.info( String.format( "Loaded [%s id=%d] with results, probes and contrasts in %d ms.",
                    elementClass.getName(), id, timer.getTime() ) );
        }
        return ears;
    }

    @Override
    public ExpressionAnalysisResultSet loadWithResultsAndContrasts( Long id, double threshold, int offset, int limit ) {
        Assert.isTrue( threshold >= 0 && threshold <= 1, "Corrected P-value threshold must be in the [0, 1] interval." );
        if ( offset == 0 && id == -1 ) {
            return loadWithResultsAndContrasts( id );
        }
        StopWatch timer = StopWatch.createStarted();
        ExpressionAnalysisResultSet ears = load( id );
        if ( ears != null ) {
            //noinspection unchecked
            List<DifferentialExpressionAnalysisResult> results = ( List<DifferentialExpressionAnalysisResult> ) getSessionFactory().getCurrentSession()
                    .createQuery( "select res from DifferentialExpressionAnalysisResult res "
                            + "where res.resultSet = :ears and res.correctedPvalue <= :threshold "
                            // no need for the hack, the threshold will filter out nulls
                            + "order by res.correctedPvalue" )
                    .setParameter( "ears", ears )
                    .setParameter( "threshold", threshold )
                    .setFirstResult( offset )
                    .setMaxResults( limit )
                    .list();
            // using separate loops ensure that hibernate can batch-initialize without interleaving queries
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getProbe() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                Hibernate.initialize( r.getContrasts() );
            }
            for ( DifferentialExpressionAnalysisResult r : results ) {
                for ( ContrastResult cr : r.getContrasts() ) {
                    Hibernate.initialize( cr.getFactorValue() );
                    Hibernate.initialize( cr.getSecondFactorValue() );
                }
            }
            // preserve order of results
            ears.setResults( new LinkedHashSet<>( results ) );
        }
        if ( timer.getTime() > 100 ) {
            log.info( String.format( "Loaded [%s id=%d] with results, probes and contrasts in %d ms.",
                    elementClass.getName(), id, timer.getTime() ) );
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
    protected void postProcessValueObjects( List<DifferentialExpressionAnalysisResultSetValueObject> differentialExpressionAnalysisResultSetValueObjects ) {
        populateBaselines( differentialExpressionAnalysisResultSetValueObjects );
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

        // this needs to be initialized because it does not appear in the experimental factors
        if ( ears.getAnalysis().getSubsetFactorValue() != null ) {
            Hibernate.initialize( ears.getAnalysis().getSubsetFactorValue() );
            Hibernate.initialize( ears.getAnalysis().getSubsetFactorValue().getExperimentalFactor() );
        }
    }

    @Override
    public long countResults( ExpressionAnalysisResultSet ears ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) from ExpressionAnalysisResultSet ears join ears.results where ears = :ears" )
                .setParameter( "ears", ears )
                .uniqueResult();
    }

    @Override
    public long countResults( ExpressionAnalysisResultSet ears, double threshold ) {
        return ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(*) from ExpressionAnalysisResultSet ears join ears.results r where ears = :ears and r.correctedPvalue <= :threshold" )
                .setParameter( "ears", ears )
                .setParameter( "threshold", threshold )
                .uniqueResult();
    }

    @Override
    public Baseline getBaseline( ExpressionAnalysisResultSet ears ) {
        if ( ears.getBaselineGroup() != null ) {
            // TODO: add support for interaction baselines, see https://github.com/PavlidisLab/Gemma/issues/1122
            if ( ears.getBaselineGroup().getExperimentalFactor().getType().equals( FactorType.CATEGORICAL ) ) {
                return Baseline.categorical( ears.getBaselineGroup() );
            } else {
                // we have a few experiments with continuous factors with a baseline set in the result set, this
                // is incorrect and is being tracked in https://github.com/PavlidisLab/GemmaCuration/issues/530
                log.warn( String.format( "Unexpected factor type for baseline %s of result set with ID %d, it should be categorical.",
                        ears.getBaselineGroup(), ears.getId() ) );
                return null;
            }
        } else {
            return getBaselinesForInteractionsInternal( Collections.singleton( ears.getId() ) ).get( ears.getId() );
        }
    }

    @Override
    public Map<ExpressionAnalysisResultSet, Baseline> getBaselinesForInteractions( Collection<ExpressionAnalysisResultSet> resultSets ) {
        Map<Long, ExpressionAnalysisResultSet> idMap = EntityUtils.getIdMap( resultSets );
        return getBaselinesForInteractionsInternal( EntityUtils.getIds( resultSets ) ).entrySet().stream()
                .collect( IdentifiableUtils.toIdentifiableMap( e -> idMap.get( e.getKey() ), Map.Entry::getValue ) );
    }

    @Override
    public Map<Long, Baseline> getBaselinesForInteractionsByIds( Collection<Long> ids ) {
        return getBaselinesForInteractionsInternal( ids );
    }

    @Override
    protected Criteria getFilteringCriteria( @Nullable Filters filters ) {
        Criteria query = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionAnalysisResultSet.class )
                .setProjection( rootEntityProjection )
                // these two are necessary for ACL filtering, so we must use a (default) inner jointure
                .createAlias( "analysis", "a" )
                .createAlias( "analysis.experimentAnalyzed", "e" )
                // if this is a subset, retrieve its source experiment
                .createAlias( "analysis.experimentAnalyzed.sourceExperiment", "se", JoinType.LEFT_OUTER_JOIN )
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

        // apply the ACL on the associated EE (or source experiment for EE subset)
        // FIXME: would be nice to use COALESCE(se.id, e.id) instead
        query.add( Restrictions.or(
                AclCriteriaUtils.formAclRestrictionClause( "e.id", ExpressionExperiment.class ),
                AclCriteriaUtils.formAclRestrictionClause( "se.id", ExpressionExperiment.class ) ) );

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
        configurer.unregisterProperty( "analysis.subsetFactorValue.characteristics.migratedToStatement" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.characteristics.originalValue" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.isBaseline" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.needsAttention" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.oldStyleCharacteristics.size" );
        configurer.unregisterProperty( "analysis.subsetFactorValue.value" );

        configurer.registerAlias( "baselineGroup.characteristics.", "bc", Characteristic.class, null, 1 );
        configurer.unregisterProperty( "baselineGroup.characteristics.migratedToStatement" );
        configurer.unregisterProperty( "baselineGroup.characteristics.originalValue" );
        configurer.unregisterProperty( "baselineGroup.experimentalFactor.annotations.size" );
        configurer.unregisterProperty( "baselineGroup.experimentalFactor.factorValues.size" );
        configurer.unregisterProperty( "baselineGroup.isBaseline" );
        configurer.unregisterProperty( "baselineGroup.needsAttention" );
        configurer.unregisterProperty( "baselineGroup.oldStyleCharacteristics.size" );
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
                .addSynchronizedQuerySpace( GENE2CS_QUERY_SPACE )
                .addSynchronizedEntityClass( ArrayDesign.class )
                .addSynchronizedEntityClass( CompositeSequence.class )
                .addSynchronizedEntityClass( Gene.class )
                .addScalar( "RESULT_ID", StandardBasicTypes.LONG )
                .addEntity( "gene", Gene.class )
                .setParameter( "rsid", resultSet.getId() )
                // analysis results are immutable and the GENE2CS is generated, so flushing is pointless
                .setFlushMode( FlushMode.MANUAL )
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

    /**
     * Populate baseline groups for results sets with interactions.
     * <p>
     * Those are not being populated in the database because there is no storage for a "second" baseline group. For more
     * details, read <a href="https://github.com/PavlidisLab/Gemma/issues/1119">#1119</a>.
     */
    private void populateBaselines( List<DifferentialExpressionAnalysisResultSetValueObject> vos ) {
        Collection<DifferentialExpressionAnalysisResultSetValueObject> vosWithMissingBaselines = vos.stream()
                .filter( vo -> vo.getBaselineGroup() == null )
                .collect( Collectors.toList() );
        if ( vosWithMissingBaselines.isEmpty() ) {
            return;
        }
        // pick baseline groups from other result sets from the same analysis
        Map<Long, Baseline> baselines = getBaselinesForInteractionsInternal( EntityUtils.getIds( vosWithMissingBaselines ) );
        for ( DifferentialExpressionAnalysisResultSetValueObject vo : vos ) {
            Baseline b = baselines.get( vo.getId() );
            if ( b != null ) {
                vo.setBaselineGroup( new FactorValueBasicValueObject( b.getFactorValue() ) );
                if ( b.getSecondFactorValue() != null ) {
                    vo.setSecondBaselineGroup( new FactorValueBasicValueObject( b.getSecondFactorValue() ) );
                }
            }
        }
    }

    /**
     * Retrieve the baselines for the given result set IDs representing factor interactions.
     */
    private Map<Long, Baseline> getBaselinesForInteractionsInternal( Collection<Long> rsIds ) {
        if ( rsIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // pick baseline groups from other result sets from the same analysis
        //noinspection unchecked
        List<Object[]> otherBaselineGroups = getSessionFactory().getCurrentSession()
                .createQuery( "select rs.id, otherBg.id, otherBg.experimentalFactor.id from ExpressionAnalysisResultSet rs "
                        + "join rs.analysis a "
                        + "join a.resultSets otherRs "
                        + "join otherRs.baselineGroup otherBg "
                        + "where rs.id in :rsIds and otherRs.id not in :rsIds" )
                .setParameterList( "rsIds", optimizeParameterList( rsIds ) )
                .list();
        // maps rs ID to [fv ID, ef ID]
        Map<Long, Set<FactorValueIdAndExperimentalFactorId>> baselineMapping = otherBaselineGroups.stream()
                .collect( Collectors.groupingBy( row -> ( Long ) row[0], Collectors.mapping( row -> new FactorValueIdAndExperimentalFactorId( ( Long ) row[1], ( Long ) row[2] ), Collectors.toSet() ) ) );
        // pick one representative contrasts to order the first and second baseline group consistently
        List<Object[]> representativeContrasts = listByBatch( getSessionFactory().getCurrentSession()
                .createQuery( "select rs.id, fv.experimentalFactor.id, fv2.experimentalFactor.id from ExpressionAnalysisResultSet rs "
                        + "join rs.results r "
                        + "join r.contrasts c "
                        // A left join is critical for performance, because otherwise the database will scan every
                        // single contrast results until it finds a non-null one. We know however that they are all
                        // identical for a given result set.
                        // This is a problem with continuous factors that do not have FVs set in the CR
                        + "left join c.factorValue fv "
                        + "left join c.secondFactorValue fv2 "
                        + "where rs.id in :rsIds "
                        + "group by rs" ), "rsIds", rsIds, 128 );
        Map<Long, Long> ef1Mapping = representativeContrasts.stream()
                .filter( row -> row[1] != null )
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[1] ) );
        Map<Long, Long> ef2Mapping = representativeContrasts.stream()
                .filter( row -> row[2] != null )
                .collect( Collectors.toMap( row -> ( Long ) row[0], row -> ( Long ) row[2] ) );
        Map<Long, Baseline> results = new HashMap<>();
        for ( Long rsId : rsIds ) {
            Long ef1 = ef1Mapping.get( rsId );
            Long ef2 = ef2Mapping.get( rsId );
            if ( ef1 == null || ef2 == null ) {
                log.warn( "Could not populate baselines for " + rsId + " as its contrasts lack factor values. This is likely a continuous factor." );
                // very likely a continuous factor, it does not have a baseline
                continue;
            }
            // I don't think this is allowed
            if ( ef1.equals( ef2 ) ) {
                log.warn( "Could not populate baselines for " + rsId + ", its representative contrast uses the same experimental factor for its first and second factor value." );
                continue;
            }
            Set<FactorValueIdAndExperimentalFactorId> baselines = baselineMapping.get( rsId );
            if ( baselines == null || baselines.size() != 2 ) {
                log.warn( "Could not find two other result sets with baseline for " + rsId + " to populate its baseline groups." );
                continue;
            }
            Long firstBaselineId = null, secondBaselineId = null;
            for ( FactorValueIdAndExperimentalFactorId fv : baselines ) {
                if ( fv.getExperimentalFactorId().equals( ef1 ) ) {
                    firstBaselineId = fv.getFactorValueId();
                }
                if ( fv.getExperimentalFactorId().equals( ef2 ) ) {
                    secondBaselineId = fv.getFactorValueId();
                }
            }
            if ( firstBaselineId != null && secondBaselineId != null ) {
                results.put( rsId, Baseline.interaction(
                        ( FactorValue ) getSessionFactory().getCurrentSession().load( FactorValue.class, firstBaselineId ),
                        ( FactorValue ) getSessionFactory().getCurrentSession().load( FactorValue.class, secondBaselineId ) )
                );
            } else {
                log.warn( "Could not fill the baseline groups for " + rsId + ": one or more baselines were not found in other result sets from the same analysis." );
            }
        }
        return results;
    }

    @Value
    private static class FactorValueIdAndExperimentalFactorId {
        Long factorValueId;
        Long experimentalFactorId;
    }
}