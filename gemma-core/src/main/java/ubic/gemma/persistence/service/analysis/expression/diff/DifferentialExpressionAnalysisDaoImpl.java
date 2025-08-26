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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.jdbc.spi.SqlStatementLogger;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.IdentifierGeneratorHelper;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Expectations;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.hibernate.HibernateUtils;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * @author paul
 * @see    DifferentialExpressionAnalysis
 */
@Repository
class DifferentialExpressionAnalysisDaoImpl extends AbstractDao<DifferentialExpressionAnalysis>
        implements DifferentialExpressionAnalysisDao {

    /**
     * Logger for manual SQL statements so they appear alongside Hibernate's.
     */
    private static final SqlStatementLogger statementLogger = new SqlStatementLogger();

    private static final String
            INSERT_RESULT_SQL = "insert into DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT (ID, PVALUE, CORRECTED_PVALUE, `RANK`, CORRECTED_P_VALUE_BIN, PROBE_FK, RESULT_SET_FK) values (?, ?, ?, ?, ?, ?, ?)",
            INSERT_CONTRAST_SQL = "insert into CONTRAST_RESULT (ID, PVALUE, TSTAT, FACTOR_VALUE_FK, DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK, COEFFICIENT, LOG_FOLD_CHANGE, SECOND_FACTOR_VALUE_FK) values (?, ?, ?, ?, ?, ?, ?, ?)",
            DELETE_RESULT_SQL = "delete from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT where ID = ?",
            DELETE_CONTRAST_SQL = "delete from CONTRAST_RESULT where ID = ?";

    private final EntityPersister resultPersister, contrastPersister;

    private final int bioAssaySetBatchSize;

    @Autowired
    public DifferentialExpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( DifferentialExpressionAnalysis.class, sessionFactory );
        resultPersister = ( ( SessionFactoryImpl ) sessionFactory )
                .getEntityPersister( DifferentialExpressionAnalysisResult.class.getName() );
        contrastPersister = ( ( SessionFactoryImpl ) sessionFactory )
                .getEntityPersister( ContrastResult.class.getName() );
        bioAssaySetBatchSize = HibernateUtils.getBatchSize( sessionFactory.getClassMetadata( BioAssaySet.class ), sessionFactory );
    }

    /**
     * Creating a full analysis with a single persist() is not efficient because Hibernate does not order inserts with
     * MySQL 5.7 dialect. However, inserting in order and using 'rewriteBatchedStatements=true' will cause batch inserts
     * to be performed.
     * <p>
     * FIXME: remove this method when <a href="https://github.com/PavlidisLab/Gemma/issues/825">#825</a> is resolve and
     *        persist by cascade
     * <p>
     * To avoid updates of the DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK in the CONTRAST_RESULT table, we manually
     * insert results and contrasts with the generated IDs.
     */
    @Override
    public DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis entity ) {
        for ( ExpressionAnalysisResultSet rs : entity.getResultSets() ) {
            Set<CompositeSequence> expectedProbes = rs.getResults().stream()
                    .map( DifferentialExpressionAnalysisResult::getProbe )
                    .collect( Collectors.toSet() );
            // collect all the pairs of FVs that are used in the contrasts of the result set
            // Gemma might not retain insignificant contrasts, so we hope that all the contrasts defined in the design
            // appear at least once
            Set<Pair<FactorValue, FactorValue>> expectedContrasts = rs.getResults().stream()
                    .flatMap( r -> r.getContrasts().stream() )
                    .map( cr2 -> Pair.of( cr2.getFactorValue(), cr2.getSecondFactorValue() ) )
                    .collect( Collectors.toSet() );
            if ( rs.getAnalysis() != entity ) {
                throw new IllegalArgumentException( "The result set is not associated to its analysis." );
            }
            if ( rs.getPvalueDistribution() == null ) {
                throw new IllegalArgumentException( "The result set must have a P-value distribution." );
            }
            if ( rs.getResults().size() != expectedProbes.size() ) {
                throw new IllegalArgumentException();
            }
            for ( CompositeSequence cs : expectedProbes ) {
                boolean found = false;
                for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                    if ( result.getProbe() == cs ) {
                        found = true;
                        break;
                    }
                }
                if ( !found ) {
                    throw new IllegalArgumentException( String.format( "The expected probe %s was not found in %s.", cs, rs ) );
                }
            }
            for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                if ( result.getResultSet() != rs ) {
                    throw new IllegalArgumentException( String.format( "%s is not associated to its result set.", result ) );
                }
                for ( ContrastResult cr : result.getContrasts() ) {
                    boolean found = false;
                    for ( Pair<FactorValue, FactorValue> ef : expectedContrasts ) {
                        if ( cr.getFactorValue() == ef.getLeft() && cr.getSecondFactorValue() == ef.getRight() ) {
                            found = true;
                            break;
                        }
                    }
                    if ( !found ) {
                        throw new IllegalArgumentException( String.format( "%s has unexpected contrast %s: it does not share its FVs with other contrasts of the result set.", result, cr ) );
                    }
                }
            }
        }

        // create the analysis, result sets, pvalue distributions, etc.
        DifferentialExpressionAnalysis finalEntity = super.create( entity );

        Session session = getSessionFactory().getCurrentSession();

        session.doWork( work -> {
            PreparedStatement insertResultStmt = work.prepareStatement( INSERT_RESULT_SQL, PreparedStatement.RETURN_GENERATED_KEYS );
            PreparedStatement insertContrastStmt = work.prepareStatement( INSERT_CONTRAST_SQL, PreparedStatement.RETURN_GENERATED_KEYS );
            List<DifferentialExpressionAnalysisResult> results = new ArrayList<>();
            List<ContrastResult> contrasts = new ArrayList<>();
            for ( ExpressionAnalysisResultSet rs : finalEntity.getResultSets() ) {
                for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                    insertResultStmt.setNull( 1, Types.BIGINT );
                    insertResultStmt.setObject( 2, result.getPvalue(), Types.DOUBLE );
                    insertResultStmt.setObject( 3, result.getCorrectedPvalue(), Types.DOUBLE );
                    insertResultStmt.setObject( 4, result.getRank(), Types.DOUBLE );
                    insertResultStmt.setObject( 5, result.getCorrectedPValueBin(), Types.INTEGER );
                    insertResultStmt.setLong( 6, result.getProbe().getId() );
                    insertResultStmt.setLong( 7, rs.getId() );
                    insertResultStmt.addBatch();
                    results.add( result );
                }
            }

            insertRowsAndAssignGeneratedKeys( INSERT_RESULT_SQL, insertResultStmt, results, resultPersister, ( SessionImplementor ) session );

            for ( ExpressionAnalysisResultSet rs : finalEntity.getResultSets() ) {
                for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                    for ( ContrastResult cr : result.getContrasts() ) {
                        insertContrastStmt.setNull( 1, Types.BIGINT );
                        insertContrastStmt.setObject( 2, cr.getPvalue(), Types.DOUBLE );
                        insertContrastStmt.setObject( 3, cr.getTstat(), Types.DOUBLE );
                        if ( cr.getFactorValue() != null ) {
                            insertContrastStmt.setLong( 4, cr.getFactorValue().getId() );
                        } else {
                            insertContrastStmt.setNull( 4, Types.BIGINT );
                        }
                        insertContrastStmt.setLong( 5, result.getId() );
                        insertContrastStmt.setObject( 6, cr.getCoefficient(), Types.DOUBLE );
                        insertContrastStmt.setObject( 7, cr.getLogFoldChange(), Types.DOUBLE );
                        if ( cr.getSecondFactorValue() != null ) {
                            insertContrastStmt.setLong( 8, cr.getSecondFactorValue().getId() );
                        } else {
                            insertContrastStmt.setNull( 8, Types.BIGINT );
                        }
                        insertContrastStmt.addBatch();
                        contrasts.add( cr );
                    }
                }
            }

            insertRowsAndAssignGeneratedKeys( INSERT_CONTRAST_SQL, insertContrastStmt, contrasts, contrastPersister, ( SessionImplementor ) session );
        } );

        getSessionFactory().getCurrentSession().flush();

        return finalEntity;
    }

    private void insertRowsAndAssignGeneratedKeys( String insertSql, PreparedStatement insertStmt, List<?> objects, EntityPersister persister, SessionImplementor session ) throws SQLException {
        statementLogger.logStatement( insertSql + String.format( " [repeated %d times]", objects.size() ) );
        ensureExpectedRowsAreInserted( insertStmt, insertStmt.executeBatch() );
        ResultSet rs = insertStmt.getGeneratedKeys();
        String idProp = persister.getIdentifierPropertyName();
        Type idType = persister.getIdentifierType();
        for ( Object object : objects ) {
            Serializable id = IdentifierGeneratorHelper.getGeneratedIdentity( rs, idProp, idType );
            persister.setIdentifier( object, id, session );
        }
    }

    private void ensureExpectedRowsAreInserted( PreparedStatement statement, int[] batchStatus ) throws
            HibernateException, SQLException {
        int i = 0;
        for ( int bs : batchStatus ) {
            Expectations.BASIC.verifyOutcome( bs, statement, i++ );
        }
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return findByProperty( "name", name );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        Set<DifferentialExpressionAnalysis> results = new HashSet<>();

        // subset factorValues factors.
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from DifferentialExpressionAnalysis a "
                        + "join a.subsetFactorValue ssf "
                        + "where ssf.experimentalFactor = :ef "
                        + "group by a" )
                .setParameter( "ef", ef )
                .list() );

        // factors used in the analysis.
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from DifferentialExpressionAnalysis a "
                        + "join a.resultSets rs "
                        + "join rs.experimentalFactors efa "
                        + "where efa = :ef "
                        + "group by a" )
                .setParameter( "ef", ef )
                .list() );

        return results;
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactors( Collection<ExperimentalFactor> experimentalFactors ) {
        if ( experimentalFactors.isEmpty() ) {
            return Collections.emptySet();
        }

        Set<DifferentialExpressionAnalysis> results = new HashSet<>();
        // subset factorValues factors.
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from DifferentialExpressionAnalysis a "
                        + "join a.subsetFactorValue ssf "
                        + "where ssf.experimentalFactor in :efs "
                        + "group by a" )
                .setParameterList( "efs", optimizeIdentifiableParameterList( experimentalFactors ) )
                .list() );

        // factors used in the analysis
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select a from DifferentialExpressionAnalysis a "
                        + "join a.resultSets rs "
                        + "join rs.experimentalFactors efa "
                        + "where efa in :efs "
                        + "group by a" )
                .setParameterList( "efs", optimizeIdentifiableParameterList( experimentalFactors ) )
                .list() );

        return results;
    }

    @Override
    public DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment experimentAnalyzed, Long analysisId, boolean includeSubSets ) {
        DifferentialExpressionAnalysis result = ( DifferentialExpressionAnalysis ) getSessionFactory().getCurrentSession()
                .createQuery( "select a from DifferentialExpressionAnalysis a "
                        + "where a.experimentAnalyzed = :experimentAnalyzed and a.id = :id" )
                .setParameter( "experimentAnalyzed", experimentAnalyzed )
                .setParameter( "id", analysisId )
                .uniqueResult();

        if ( result != null ) {
            return result;
        }

        // try to lookup subsets if we haven't found the analysis directly
        if ( includeSubSets ) {
            return ( DifferentialExpressionAnalysis ) getSessionFactory().getCurrentSession()
                    .createQuery( "select a from DifferentialExpressionAnalysis a, ExpressionExperimentSubSet subset "
                            + "where subset.sourceExperiment = :experimentAnalyzed and a.experimentAnalyzed = subset and a.id = :id" )
                    .setParameter( "experimentAnalyzed", experimentAnalyzed )
                    .setParameter( "id", analysisId )
                    .uniqueResult();
        }

        return result;
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentAnalyzedId(
            Collection<Long> experimentAnalyzedId ) {

        Map<Long, Collection<DifferentialExpressionAnalysis>> results = new HashMap<>();
        //noinspection unchecked
        List<Object[]> qresult = this.getSessionFactory().getCurrentSession()
                .createQuery( "select e, a from DifferentialExpressionAnalysis a "
                        + "join a.experimentAnalyzed e "
                        + "where e.id in (:eeIds) "
                        + "group by e, a" )
                .setParameterList( "eeIds", optimizeParameterList( experimentAnalyzedId ) )
                .list();
        for ( Object[] o : qresult ) {
            BioAssaySet bas = ( BioAssaySet ) o[0];
            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) o[1];
            results.computeIfAbsent( bas.getId(), k -> new HashSet<>() ).add( dea );
        }
        return results;
    }

    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<CompositeSequence> probes = CommonQueries
                .getCompositeSequences( gene, this.getSessionFactory().getCurrentSession() );
        Collection<BioAssaySet> result = new HashSet<>();
        if ( probes.isEmpty() ) {
            return result;
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Find probes: " + timer.getTime() + " ms" );
        }
        timer.reset();
        timer.start();

        // Note: this query misses ExpressionExperimentSubSets. The native query was implemented because HQL was always
        // constructing a constraint on SubSets. See bug 2173.
        // final String queryToUse = "select e.ID from ANALYSIS a inner join INVESTIGATION e ON a.EXPERIMENT_ANALYZED_FK = e.ID "
        //         + "inner join BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK=e.ID "
        //         + " inner join BIO_MATERIAL bm ON bm.ID=ba.SAMPLE_USED_FK inner join TAXON t ON bm.SOURCE_TAXON_FK=t.ID "
        //         + " inner join COMPOSITE_SEQUENCE cs ON ba.ARRAY_DESIGN_USED_FK =cs.ARRAY_DESIGN_FK where cs.ID in "
        //         + " (:probes) and t.ID = :taxon";

        Taxon taxon = gene.getTaxon();

        Set<Long> ids = new HashSet<>();
        for ( Collection<Long> batch : batchParameterList( IdentifiableUtils.getIds( probes ), 1024 ) ) {
            //noinspection unchecked
            ids.addAll( this.getSessionFactory().getCurrentSession()
                    .createSQLQuery( "select a.EXPERIMENT_ANALYZED_FK from ANALYSIS a "
                            + "join BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK = a.EXPERIMENT_ANALYZED_FK "
                            + "join BIO_MATERIAL bm ON bm.ID = ba.SAMPLE_USED_FK "
                            + "join TAXON t ON bm.SOURCE_TAXON_FK = t.ID "
                            + "join COMPOSITE_SEQUENCE cs ON ba.ARRAY_DESIGN_USED_FK = cs.ARRAY_DESIGN_FK "
                            + "where cs.ID in (:probes) and t.ID = :taxon" )
                    .addScalar( "ID", StandardBasicTypes.LONG )
                    .setParameterList( "probes", batch )
                    .setParameter( "taxon", taxon )
                    .list() );
        }

        for ( Collection<Long> batch : batchParameterList( ids, bioAssaySetBatchSize ) ) {
            //noinspection unchecked
            result.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "from BioAssaySet ba where ba.id in (:ids)" )
                    .setParameterList( "ids", batch )
                    .list() );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Find experiments: " + timer.getTime() + " ms" );
        }

        return result;
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> eeIds, boolean includeSubSets ) {
        //noinspection unchecked
        Set<Long> result = new HashSet<>( ( List<Long> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select a.experimentAnalyzed.id from DifferentialExpressionAnalysis a"
                        + " where a.experimentAnalyzed.id in (:eeIds) "
                        + "group by a.experimentAnalyzed" )
                .setParameterList( "eeIds", optimizeParameterList( eeIds ) )
                .list() );
        if ( includeSubSets ) {
            //noinspection unchecked
            result.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "select e.id from DifferentialExpressionAnalysis a, ExpressionExperimentSubSet eess "
                            + "join eess.sourceExperiment e "
                            + "where a.experimentAnalyzed = eess and e.id in (:eeIds) "
                            + "group by e" )
                    .setParameterList( "eeIds", optimizeParameterList( eeIds ) )
                    .list() );
        }
        return result;
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select ee.id from DifferentialExpressionAnalysis as doa "
                        + "join doa.experimentAnalyzed as ee "
                        + "join ee.bioAssays as ba "
                        + "join ba.sampleUsed as sample "
                        + "where sample.sourceTaxon = :taxon "
                        + "group by ee" )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public Map<Long, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> experimentAnalyzedIds, int offset, int limit, boolean includeSubSets ) {

        /*
         * There are three cases to consider: the ids are experiments; the ids are experiment subsets; the ids are
         * experiments that have subsets.
         */
        Map<Long, List<DifferentialExpressionAnalysisValueObject>> r = new HashMap<>();

        Map<Long, Collection<Long>> arrayDesignsUsed = CommonQueries
                .getArrayDesignsUsedEEMap( experimentAnalyzedIds, this.getSessionFactory().getCurrentSession() );

        /*
         * Fetch analyses of experiments or subsets.
         */
        //noinspection unchecked
        Collection<DifferentialExpressionAnalysis> hits = this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from DifferentialExpressionAnalysis a "
                                + "join fetch a.experimentAnalyzed e "
                                + "where e.id in (:eeIds)" )
                .setParameterList( "eeIds", optimizeParameterList( experimentAnalyzedIds ) )
                .setFirstResult( offset )
                .setMaxResults( limit )
                .list();

        // initialize result sets and hit list sizes
        // this is necessary because the DEA VO constructor will ignore uninitialized associations
        for ( DifferentialExpressionAnalysis hit : hits ) {
            Hibernate.initialize( hit.getResultSets() );
            for ( ExpressionAnalysisResultSet rs : hit.getResultSets() ) {
                Hibernate.initialize( rs.getHitListSizes() );
            }
        }

        Map<Long, Collection<FactorValue>> ee2fv = new HashMap<>();
        List<Object[]> fvs;

        if ( !hits.isEmpty() ) {
            // factor values for the experiments.
            //noinspection unchecked
            fvs = this.getSessionFactory().getCurrentSession()
                    .createQuery( "select ee.id, fv from ExpressionExperiment ee "
                            + "join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv "
                            + "where ee.id in (:ees) "
                            + "group by ee, fv" )
                    .setParameterList( "ees", optimizeParameterList( experimentAnalyzedIds ) ).list();
            this.addFactorValues( ee2fv, fvs );

            // also get factor values for subsets - those not found yet.
            Collection<Long> used = new HashSet<>();
            for ( DifferentialExpressionAnalysis a : hits ) {
                used.add( a.getExperimentAnalyzed().getId() );
            }

            // drop all the EE IDs, we should be left only with subset IDs
            List<Long> probableSubSetIds = ListUtils.removeAll( used, ee2fv.keySet() );
            if ( !probableSubSetIds.isEmpty() ) {
                //noinspection unchecked
                fvs = this.getSessionFactory().getCurrentSession()
                        .createQuery( "select eess.id, fv from ExpressionExperimentSubSet eess "
                                + "join eess.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv "
                                + "where eess.id in (:ees) "
                                + "group by eess, fv" )
                        .setParameterList( "ees", optimizeParameterList( probableSubSetIds ) ).list();
                this.addFactorValues( ee2fv, fvs );
            }
        }

        if ( includeSubSets ) {
            /*
             * Subsets of those same experiments (there might not be any)
             */
            // EE and subsets share the same ID space, so it's not a problem to use them for querying
            //noinspection unchecked
            List<DifferentialExpressionAnalysis> analysesOfSubsets = this.getSessionFactory().getCurrentSession()
                    .createQuery( "select distinct a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                            + "join eess.sourceExperiment ee "
                            + "join fetch a.experimentAnalyzed eeanalyzed "
                            + "where ee.id in (:eeids) and eess=eeanalyzed" )
                    .setParameterList( "eeids", optimizeParameterList( experimentAnalyzedIds ) ).list();

            if ( !analysesOfSubsets.isEmpty() ) {
                hits.addAll( analysesOfSubsets );

                Collection<Long> experimentSubsetIds = new HashSet<>();
                for ( DifferentialExpressionAnalysis a : analysesOfSubsets ) {
                    ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) a.getExperimentAnalyzed();
                    experimentSubsetIds.add( subset.getId() );
                }

                // factor value information for the subset. The key output is the ID of the subset, not of the source
                // experiment.
                //noinspection unchecked
                fvs = this.getSessionFactory().getCurrentSession()
                        .createQuery( "select ee.id, fv from ExpressionExperimentSubSet ee "
                                + "join ee.bioAssays ba "
                                + "join ba.sampleUsed bm "
                                + "join bm.factorValues fv "
                                + "where ee.id in (:ees) "
                                + "group by ee, fv" )
                        .setParameterList( "ees", optimizeParameterList( experimentSubsetIds ) ).list();
                this.addFactorValues( ee2fv, fvs );
            }
        }

        // postprocesss...
        if ( hits.isEmpty() ) {
            return r;
        }
        Collection<DifferentialExpressionAnalysisValueObject> summaries = this
                .convertToValueObjects( hits, arrayDesignsUsed, ee2fv );

        for ( DifferentialExpressionAnalysisValueObject an : summaries ) {
            Long bioAssaySetId;
            if ( an.getSourceExperimentId() != null ) {
                bioAssaySetId = an.getSourceExperimentId();
            } else {
                bioAssaySetId = an.getExperimentAnalyzedId();
            }
            r.computeIfAbsent( bioAssaySetId, k -> new ArrayList<>() ).add( an );
        }

        return r;
    }

    private void addFactorValues( Map<Long, Collection<FactorValue>> ee2fv, List<Object[]> fvs ) {
        for ( Object[] oa : fvs ) {
            Long eeId = ( Long ) oa[0];
            FactorValue fv = ( FactorValue ) oa[1];
            ee2fv.computeIfAbsent( eeId, k -> new HashSet<>() ).add( fv );
        }
    }

    @Override
    public void remove( DifferentialExpressionAnalysis analysis ) {
        log.info( "Removing " + analysis + "..." );
        List<Long> resultSetIds = IdentifiableUtils.getIds( analysis.getResultSets() );
        if ( !resultSetIds.isEmpty() ) {
            int removedContrasts = getSessionFactory().getCurrentSession()
                    .createSQLQuery( "delete cr from CONTRAST_RESULT cr where cr.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK in (select dear.ID from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear where dear.RESULT_SET_FK in (:resultSetIds))" )
                    .setParameterList( "resultSetIds", resultSetIds )
                    .executeUpdate();
            int removedResults = getSessionFactory().getCurrentSession()
                    .createSQLQuery( "delete dear from DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT dear where dear.RESULT_SET_FK in (:resultSetIds)" )
                    .setParameterList( "resultSetIds", resultSetIds )
                    .executeUpdate();
            log.info( String.format( "Removed %d results and %d contrasts from %s.", removedResults, removedContrasts, analysis ) );
        }
        super.remove( analysis );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByExperimentAnalyzed( ExpressionExperiment experiment, boolean includeSubSets ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>( findByExperimentAnalyzed( experiment ) );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        if ( includeSubSets ) {
            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "select a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                            + "join eess.sourceExperiment see "
                            + "join a.experimentAnalyzed eeanalyzed "
                            + "where see = :ee and eess = eeanalyzed "
                            + "group by a" )
                    .setParameter( "ee", experiment )
                    .list() );
        }

        return results;
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByExperimentAnalyzed( BioAssaySet experimentAnalyzed ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select a from DifferentialExpressionAnalysis a "
                                + "where a.experimentAnalyzed = :ee" )
                .setParameter( "ee", experimentAnalyzed )
                .list();
    }

    @Override
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> findByExperimentsAnalyzed( Collection<ExpressionExperiment> experiments, boolean includeSubSets ) {
        //noinspection unchecked
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>(
                ( List<DifferentialExpressionAnalysis> ) this.getSessionFactory().getCurrentSession()
                        .createQuery( "select a from DifferentialExpressionAnalysis a "
                                + "where a.experimentAnalyzed in :ees group by a" )
                        .setParameterList( "ees", optimizeIdentifiableParameterList( experiments ) )
                        .list() );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        if ( includeSubSets ) {
            Collection<ExpressionExperiment> sourceExperiments = getExpressionExperiments( experiments );
            if ( !sourceExperiments.isEmpty() ) {
                //noinspection unchecked
                results.addAll( this.getSessionFactory().getCurrentSession()
                        .createQuery( "select a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                                + "join eess.sourceExperiment see "
                                + "join a.experimentAnalyzed eeanalyzed "
                                + "where see in :ees and eess=eeanalyzed "
                                + "group by eess, a" )
                        .setParameterList( "ees", optimizeIdentifiableParameterList( sourceExperiments ) )
                        .list() );
            }
        }

        return results.stream()
                .collect( Collectors.groupingBy( DifferentialExpressionAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
    }

    /**
     * Select actual {@link ExpressionExperiment} from a collection of experiments analyzed.
     */
    private Collection<ExpressionExperiment> getExpressionExperiments( Collection<? extends BioAssaySet> bioAssaySets ) {
        return bioAssaySets.stream()
                .filter( bas -> bas instanceof ExpressionExperiment )
                .map( bas -> ( ( ExpressionExperiment ) bas ) )
                .collect( IdentifiableUtils.toIdentifiableSet() );
    }

    private Collection<DifferentialExpressionAnalysisValueObject> convertToValueObjects(
            Collection<DifferentialExpressionAnalysis> analyses, Map<Long, Collection<Long>> arrayDesignsUsed,
            Map<Long, Collection<FactorValue>> ee2fv ) {
        Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<>();

        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

            DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

            BioAssaySet bioAssaySet = analysis.getExperimentAnalyzed();

            avo.setExperimentAnalyzedId( bioAssaySet.getId() ); // might be a subset.

            if ( analysis.getSubsetFactorValue() != null ) {
                avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );
                avo.setSubsetFactor(
                        new ExperimentalFactorValueObject( analysis.getSubsetFactorValue().getExperimentalFactor() ) );
                assert bioAssaySet instanceof ExpressionExperimentSubSet;
                avo.setSourceExperimentId( ( ( ExpressionExperimentSubSet ) bioAssaySet ).getSourceExperiment().getId() );
                if ( arrayDesignsUsed.containsKey( bioAssaySet.getId() ) ) {
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( bioAssaySet.getId() ) );
                } else {
                    assert arrayDesignsUsed.containsKey( avo.getSourceExperimentId() );
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( avo.getSourceExperimentId() ) );
                }
            } else {
                Collection<Long> adids = arrayDesignsUsed.get( bioAssaySet.getId() );
                avo.setArrayDesignsUsed( adids );
            }

            for ( ExpressionAnalysisResultSet resultSet : results ) {
                DiffExResultSetSummaryValueObject desvo = new DiffExResultSetSummaryValueObject( resultSet );
                desvo.setArrayDesignsUsed( avo.getArrayDesignsUsed() );
                desvo.setBioAssaySetAnalyzedId( bioAssaySet.getId() ); // might be a subset.
                desvo.setAnalysisId( analysis.getId() );
                avo.getResultSets().add( desvo );
                assert ee2fv.containsKey( bioAssaySet.getId() );
                this.populateWhichFactorValuesUsed( avo, ee2fv.get( bioAssaySet.getId() ) );
            }

            summaries.add( avo );
        }
        return summaries;
    }

    /**
     * Figure out which factorValues were used for each of the experimental factors (excluding the subset factor)
     */
    private void populateWhichFactorValuesUsed( DifferentialExpressionAnalysisValueObject avo, @Nullable Collection<FactorValue> fvs ) {
        if ( fvs == null || fvs.isEmpty() ) {
            return;
        }
        ExperimentalFactorValueObject subsetFactor = avo.getSubsetFactor();

        for ( FactorValue fv : fvs ) {

            Long experimentalFactorId = fv.getExperimentalFactor().getId();

            if ( subsetFactor != null && experimentalFactorId.equals( subsetFactor.getId() ) ) {
                continue;
            }

            avo.getFactorValuesUsedByExperimentalFactorId()
                    .computeIfAbsent( experimentalFactorId, k -> new HashSet<>() )
                    .add( new FactorValueValueObject( fv ) );
        }
    }
}