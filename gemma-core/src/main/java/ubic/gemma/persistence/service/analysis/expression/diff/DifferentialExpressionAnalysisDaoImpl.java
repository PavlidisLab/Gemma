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
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.analysis.SingleExperimentAnalysisDaoBase;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.hibernate.HibernateUtils;

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
class DifferentialExpressionAnalysisDaoImpl extends SingleExperimentAnalysisDaoBase<DifferentialExpressionAnalysis>
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
        bioAssaySetBatchSize = HibernateUtils.getBatchSize( sessionFactory, sessionFactory.getClassMetadata( BioAssaySet.class ) );
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
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r "
                + "join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat < 0";

        Long count = ( Long ) this.getSessionFactory().getCurrentSession().createQuery( query )
                .setParameter( "rs", par )
                .setParameter( "threshold", threshold )
                .uniqueResult();

        AbstractDao.log.debug( "Found " + count + " downregulated genes in result set (" + par.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {

        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r where rs = :rs and r.correctedPvalue < :threshold";

        Long count = ( Long ) this.getSessionFactory().getCurrentSession().createQuery( query )
                .setParameter( "rs", ears )
                .setParameter( "threshold", threshold )
                .uniqueResult();

        AbstractDao.log.debug( "Found " + count + " differentially expressed genes in result set (" + ears.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r"
                + " join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat > 0";

        Long count = ( Long ) this.getSessionFactory().getCurrentSession().createQuery( query )
                .setParameter( "rs", par )
                .setParameter( "threshold", threshold )
                .uniqueResult();

        AbstractDao.log.debug( "Found " + count + " upregulated genes in result set (" + par.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysis a"
                + "   inner join a.experimentAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet"
                + " and r.correctedPvalue < :threshold";

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( findByResultSet )
                .setParameter( "gene", gene )
                .setParameter( "resultSet", resultSet )
                .setParameter( "threshold", threshold )
                .list();
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        Set<DifferentialExpressionAnalysis> results = new HashSet<>();

        // subset factorValues factors.
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from DifferentialExpressionAnalysis a "
                        + "join a.subsetFactorValue ssf "
                        + "where ssf.experimentalFactor = :ef" )
                .setParameter( "ef", ef )
                .list() );

        // factors used in the analysis.
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from DifferentialExpressionAnalysis a "
                        + "join a.resultSets rs "
                        + "join rs.experimentalFactors efa "
                        + "where efa = :ef" )
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
                .createQuery( "select distinct a from DifferentialExpressionAnalysis a "
                        + "join a.subsetFactorValue ssf "
                        + "where ssf.experimentalFactor in :efs" )
                .setParameterList( "efs", optimizeIdentifiableParameterList( experimentalFactors ) )
                .list() );

        // factors used in the analysis
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from DifferentialExpressionAnalysis a "
                        + "join a.resultSets rs "
                        + "join rs.experimentalFactors efa "
                        + "where efa in :efs" )
                .setParameterList( "efs", optimizeIdentifiableParameterList( experimentalFactors ) )
                .list() );

        return results;
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByExperimentIds(
            Collection<Long> experimentIds ) {

        Map<Long, Collection<DifferentialExpressionAnalysis>> results = new HashMap<>();
        //language=HQL
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysis a"
                + "   inner join a.experimentAnalyzed e where e.id in (:eeIds)";
        List<?> qresult = this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "eeIds", optimizeParameterList( experimentIds ) )
                .list();
        for ( Object o : qresult ) {
            Object[] oa = ( Object[] ) o;
            BioAssaySet bas = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) oa[1];
            Long id = bas.getId();
            if ( !results.containsKey( id ) ) {
                results.put( id, new HashSet<DifferentialExpressionAnalysis>() );
            }
            results.get( id ).add( dea );
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
            AbstractDao.log.info( "Find probes: " + timer.getTime() + " ms" );
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
        for ( Collection<Long> batch : batchParameterList( EntityUtils.getIds( probes ), 1024 ) ) {
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
            AbstractDao.log.info( "Find experiments: " + timer.getTime() + " ms" );
        }

        return result;
    }

    @Override
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> experiments ) {
        Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> result = new HashMap<>();

        StopWatch timer = new StopWatch();
        timer.start();
        final String query = "select distinct a from DifferentialExpressionAnalysis a inner join fetch a.resultSets res "
                + " inner join fetch res.baselineGroup"
                + " inner join fetch res.experimentalFactors facs inner join fetch facs.factorValues "
                + " inner join fetch res.hitListSizes where a.experimentAnalyzed in (:ees) ";

        //noinspection unchecked
        List<DifferentialExpressionAnalysis> r1 = this.getSessionFactory().getCurrentSession()
                .createQuery( query )
                .setParameterList( "ees", optimizeIdentifiableParameterList( experiments ) )
                .list();
        int count = 0;
        for ( DifferentialExpressionAnalysis a : r1 ) {
            //noinspection SuspiciousMethodCalls // Ignoring subsets
            if ( !result.containsKey( a.getExperimentAnalyzed() ) ) {
                result.put( ( ExpressionExperiment ) a.getExperimentAnalyzed(),
                        new HashSet<DifferentialExpressionAnalysis>() );
            }
            //noinspection SuspiciousMethodCalls // Ignoring subsets
            result.get( a.getExperimentAnalyzed() ).add( a );
            count++;
        }
        if ( timer.getTime() > 1000 ) {
            AbstractDao.log
                    .info( "Fetch " + count + " analyses for " + result.size() + " experiments: " + timer.getTime()
                            + "ms; Query was:\n" + query );
        }
        timer.reset();
        timer.start();

        /*
         * Deal with the analyses of subsets of the experiments given being analyzed; but we keep things organized by
         * the source experiment. Maybe that is confusing.
         */
        String q2 = "select distinct a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                + " inner join fetch a.resultSets res inner join fetch res.baselineGroup "
                + " inner join fetch res.experimentalFactors facs inner join fetch facs.factorValues"
                + " inner join fetch res.hitListSizes  "
                + " join eess.sourceExperiment see join a.experimentAnalyzed ee  where eess=ee and see in (:ees) ";
        //noinspection unchecked
        List<DifferentialExpressionAnalysis> r2 = this.getSessionFactory().getCurrentSession()
                .createQuery( q2 )
                .setParameterList( "ees", optimizeIdentifiableParameterList( experiments ) )
                .list();

        if ( !r2.isEmpty() ) {
            count = 0;
            for ( DifferentialExpressionAnalysis a : r2 ) {
                BioAssaySet experimentAnalyzed = a.getExperimentAnalyzed();

                assert experimentAnalyzed instanceof ExpressionExperimentSubSet;

                ExpressionExperiment sourceExperiment = ( ( ExpressionExperimentSubSet ) experimentAnalyzed )
                        .getSourceExperiment();

                if ( !result.containsKey( sourceExperiment ) ) {
                    result.put( sourceExperiment, new HashSet<DifferentialExpressionAnalysis>() );
                }

                result.get( sourceExperiment ).add( a );
                count++;
            }
            if ( timer.getTime() > 1000 ) {
                AbstractDao.log
                        .info( "Fetch " + count + " subset analyses for " + result.size() + " experiment subsets: "
                                + timer.getTime() + "ms" );
                AbstractDao.log.debug( "Query for subsets was: " + q2 );
            }
        }

        return result;

    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        //language=HQL
        final String queryString = "select distinct e.id from DifferentialExpressionAnalysis a"
                + " inner join a.experimentAnalyzed e where e.id in (:eeIds)";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameterList( "eeIds", optimizeParameterList( idsToFilter ) )
                .list();
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        //language=HQL
        final String queryString = "select distinct ee.id from DifferentialExpressionAnalysis"
                + " as doa inner join doa.experimentAnalyzed as ee " + "inner join ee.bioAssays as ba "
                + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon ";
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "taxon", taxon )
                .list();
    }

    @Override
    public Map<Long, List<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds, int offset, int limit ) {

        /*
         * There are three cases to consider: the ids are experiments; the ids are experiment subsets; the ids are
         * experiments that have subsets.
         */
        Map<Long, List<DifferentialExpressionAnalysisValueObject>> r = new HashMap<>();

        Map<Long, Collection<Long>> arrayDesignsUsed = CommonQueries
                .getArrayDesignsUsedEEMap( expressionExperimentIds, this.getSessionFactory().getCurrentSession() );

        /*
         * Fetch analyses of experiments or subsets.
         */
        //noinspection unchecked
        Collection<DifferentialExpressionAnalysis> hits = this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from DifferentialExpressionAnalysis a "
                                + "join fetch a.experimentAnalyzed e "
                                + "where e.id in (:eeIds)" )
                .setParameterList( "eeIds", optimizeParameterList( expressionExperimentIds ) )
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
            fvs = this.getSessionFactory().getCurrentSession().createQuery(
                            "select distinct ee.id, fv from " + "ExpressionExperiment"
                                    + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                    .setParameterList( "ees", optimizeParameterList( expressionExperimentIds ) ).list();
            this.addFactorValues( ee2fv, fvs );

            // also get factor values for subsets - those not found yet.
            Collection<Long> used = new HashSet<>();
            for ( DifferentialExpressionAnalysis a : hits ) {
                used.add( a.getExperimentAnalyzed().getId() );
            }

            List<Long> probableSubSetIds = ListUtils.removeAll( used, ee2fv.keySet() );
            if ( !probableSubSetIds.isEmpty() ) {
                //noinspection unchecked
                fvs = this.getSessionFactory().getCurrentSession().createQuery(
                                "select distinct ee.id, fv from " + "ExpressionExperimentSubSet"
                                        + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                        .setParameterList( "ees", optimizeParameterList( probableSubSetIds ) ).list();
                this.addFactorValues( ee2fv, fvs );
            }

        }

        /*
         * Subsets of those same experiments (there might not be any)
         */
        //noinspection unchecked
        List<DifferentialExpressionAnalysis> analysesOfSubsets = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from " + "ExpressionExperimentSubSet"
                        + " ee, DifferentialExpressionAnalysis a" + " join ee.sourceExperiment see "
                        + " join fetch a.experimentAnalyzed eeanalyzed where see.id in (:eeids) and ee=eeanalyzed" )
                .setParameterList( "eeids", optimizeParameterList( expressionExperimentIds ) ).list();

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
            fvs = this.getSessionFactory().getCurrentSession().createQuery(
                            "select distinct ee.id, fv from " + "ExpressionExperimentSubSet"
                                    + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                    .setParameterList( "ees", optimizeParameterList( experimentSubsetIds ) ).list();
            this.addFactorValues( ee2fv, fvs );
        }

        // postprocesss...
        if ( hits.isEmpty() ) {
            return r;
        }
        Collection<DifferentialExpressionAnalysisValueObject> summaries = this
                .convertToValueObjects( hits, arrayDesignsUsed, ee2fv );

        for ( DifferentialExpressionAnalysisValueObject an : summaries ) {

            Long bioAssaySetId;
            if ( an.getSourceExperiment() != null ) {
                bioAssaySetId = an.getSourceExperiment();
            } else {
                bioAssaySetId = an.getBioAssaySetId();
            }
            if ( !r.containsKey( bioAssaySetId ) ) {
                r.put( bioAssaySetId, new ArrayList<DifferentialExpressionAnalysisValueObject>() );
            }
            r.get( bioAssaySetId ).add( an );
        }

        return r;

    }

    @Override
    public void remove( DifferentialExpressionAnalysis analysis ) {
        this.getSessionFactory().getCurrentSession().doWork( work -> {
            PreparedStatement deleteContrast = work.prepareStatement( DELETE_CONTRAST_SQL );
            PreparedStatement deleteResult = work.prepareStatement( DELETE_RESULT_SQL );
            int numResults = 0;
            int numContrasts = 0;
            for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {
                for ( DifferentialExpressionAnalysisResult result : rs.getResults() ) {
                    deleteResult.setLong( 1, result.getId() );
                    deleteResult.addBatch();
                    numResults++;
                    for ( ContrastResult cr : result.getContrasts() ) {
                        deleteContrast.setLong( 1, cr.getId() );
                        deleteContrast.addBatch();
                        numContrasts++;
                    }
                }
            }
            statementLogger.logStatement( String.format( "%s [repeated %d times]", DELETE_CONTRAST_SQL, numContrasts ) );
            ensureExpectedRowsAreInserted( deleteContrast, deleteContrast.executeBatch() );
            statementLogger.logStatement( String.format( "%s [repeated %d times]", DELETE_RESULT_SQL, numResults ) );
            ensureExpectedRowsAreInserted( deleteResult, deleteResult.executeBatch() );
        } );
        super.remove( analysis );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByExperiment( BioAssaySet experiment ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();

        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from DifferentialExpressionAnalysis a "
                                + "where a.experimentAnalyzed = :ee" )
                .setParameter( "ee", experiment ).list() );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                                + "join eess.sourceExperiment see "
                                + "join a.experimentAnalyzed eeanalyzed where see = :ee and eess = eeanalyzed" )
                .setParameter( "ee", experiment ).list() );

        return results;
    }

    @Override
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> findByExperiments( Collection<? extends
            BioAssaySet> experiments ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<>();

        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from DifferentialExpressionAnalysis a "
                                + "where a.experimentAnalyzed in :ees" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( experiments ) ).list() );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        //noinspection unchecked
        results.addAll( this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a "
                                + "join eess.sourceExperiment see "
                                + "join a.experimentAnalyzed eeanalyzed where see in :ees and eess=eeanalyzed" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( experiments ) ).list() );

        return results.stream()
                .collect( Collectors.groupingBy( DifferentialExpressionAnalysis::getExperimentAnalyzed, Collectors.toCollection( ArrayList::new ) ) );
    }

    private void addFactorValues( Map<Long, Collection<FactorValue>> ee2fv, List<Object[]> fvs ) {
        for ( Object[] oa : fvs ) {
            Long eeId = ( Long ) oa[0];
            if ( !ee2fv.containsKey( eeId ) ) {
                ee2fv.put( eeId, new HashSet<FactorValue>() );
            }
            ee2fv.get( eeId ).add( ( FactorValue ) oa[1] );
        }
    }

    private Collection<DifferentialExpressionAnalysisValueObject> convertToValueObjects(
            Collection<DifferentialExpressionAnalysis> analyses, Map<Long, Collection<Long>> arrayDesignsUsed,
            Map<Long, Collection<FactorValue>> ee2fv ) {
        Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<>();

        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

            DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

            BioAssaySet bioAssaySet = analysis.getExperimentAnalyzed();

            avo.setBioAssaySetId( bioAssaySet.getId() ); // might be a subset.

            if ( analysis.getSubsetFactorValue() != null ) {
                avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );
                avo.setSubsetFactor(
                        new ExperimentalFactorValueObject( analysis.getSubsetFactorValue().getExperimentalFactor() ) );
                assert bioAssaySet instanceof ExpressionExperimentSubSet;
                avo.setSourceExperiment( ( ( ExpressionExperimentSubSet ) bioAssaySet ).getSourceExperiment().getId() );
                if ( arrayDesignsUsed.containsKey( bioAssaySet.getId() ) ) {
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( bioAssaySet.getId() ) );
                } else {
                    assert arrayDesignsUsed.containsKey( avo.getSourceExperiment() );
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( avo.getSourceExperiment() ) );
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
    private void populateWhichFactorValuesUsed( DifferentialExpressionAnalysisValueObject avo,
            Collection<FactorValue> fvs ) {
        if ( fvs == null || fvs.isEmpty() ) {
            return;
        }
        ExperimentalFactorValueObject subsetFactor = avo.getSubsetFactor();

        for ( FactorValue fv : fvs ) {

            Long experimentalFactorId = fv.getExperimentalFactor().getId();

            if ( subsetFactor != null && experimentalFactorId.equals( subsetFactor.getId() ) ) {
                continue;
            }

            if ( !avo.getFactorValuesUsedByExperimentalFactorId().containsKey( experimentalFactorId ) ) {
                avo.getFactorValuesUsedByExperimentalFactorId().put( experimentalFactorId, new HashSet<FactorValueValueObject>() );
            }

            avo.getFactorValuesUsedByExperimentalFactorId().get( experimentalFactorId ).add( new FactorValueValueObject( fv ) );

        }
    }
}