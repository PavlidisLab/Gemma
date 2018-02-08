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

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.NativeQueryUtils;

import java.math.BigInteger;
import java.util.*;

/**
 * @author paul
 * @see DifferentialExpressionAnalysis
 */
@Repository
public class DifferentialExpressionAnalysisDaoImpl extends DifferentialExpressionAnalysisDaoBase {

    @Autowired
    public DifferentialExpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r "
                + "join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat < 0";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { par, threshold };

        List<?> qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

        if ( qresult.isEmpty() ) {
            log.warn( "No count returned" );
            return 0;
        }
        Long count = ( Long ) qresult.iterator().next();

        log.debug( "Found " + count + " downregulated genes in result set (" + par.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {

        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r where rs = :rs and r.correctedPvalue < :threshold";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { ears, threshold };

        List<?> qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

        if ( qresult.isEmpty() ) {
            log.warn( "No count returned" );
            return 0;
        }
        Long count = ( Long ) qresult.iterator().next();

        log.debug( "Found " + count + " differentially expressed genes in result set (" + ears.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSet rs inner join rs.results r"
                + " join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat > 0";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { par, threshold };

        List<?> qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

        if ( qresult.isEmpty() ) {
            log.warn( "No count returned" );
            return 0;
        }
        Long count = ( Long ) qresult.iterator().next();

        log.debug( "Found " + count + " upregulated genes in result set (" + par.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {

        // subset factorvalues factors.
        Collection<DifferentialExpressionAnalysis> result = this.getHibernateTemplate().findByNamedParam(
                "select distinct a from DifferentialExpressionAnalysis a join a.subsetFactorValue ssf"
                        + " join ssf.experimentalFactor efa where efa = :ef ", "ef", ef );

        // factors used in the analysis.
        result.addAll( this.getHibernateTemplate().findByNamedParam(
                "select distinct a from DifferentialExpressionAnalysis a join a.resultSets rs"
                        + " left join rs.baselineGroup bg join rs.experimentalFactors efa where efa = :ef ", "ef",
                ef ) );

        return result;
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.getHibernateTemplate()
                .findByNamedParam( "select a from DifferentialExpressionAnalysis as a where a.name = :name", "name",
                        name );
    }

    @Override
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> experiments ) {
        Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> result = new HashMap<>();

        StopWatch timer = new StopWatch();
        timer.start();
        final String query =
                "select distinct a from DifferentialExpressionAnalysis a inner join fetch a.resultSets res "
                        + " inner join fetch res.baselineGroup"
                        + " inner join fetch res.experimentalFactors facs inner join fetch facs.factorValues "
                        + " inner join fetch res.hitListSizes where a.experimentAnalyzed.id in (:ees) ";

        List<DifferentialExpressionAnalysis> r1 = this.getHibernateTemplate()
                .findByNamedParam( query, "ees", EntityUtils.getIds( experiments ) );
        int count = 0;
        for ( DifferentialExpressionAnalysis a : r1 ) {
            if ( !result.containsKey( a.getExperimentAnalyzed() ) ) {
                result.put( ( ExpressionExperiment ) a.getExperimentAnalyzed(),
                        new HashSet<DifferentialExpressionAnalysis>() );
            }
            result.get( a.getExperimentAnalyzed() ).add( a );
            count++;
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Fetch " + count + " analyses for " + result.size() + " experiments: " + timer.getTime()
                    + "ms; Query was:\n" + NativeQueryUtils.toSql( this.getHibernateTemplate(), query ) );
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
                + " join eess.sourceExperiment see join a.experimentAnalyzed ee  where eess=ee and see.id in (:ees) ";
        List<DifferentialExpressionAnalysis> r2 = this.getHibernateTemplate()
                .findByNamedParam( q2, "ees", EntityUtils.getIds( experiments ) );

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
                log.info( "Fetch " + count + " subset analyses for " + result.size() + " experiment subsets: " + timer
                        .getTime() + "ms" );
                log.debug( "Query for subsets was: " + NativeQueryUtils.toSql( this.getHibernateTemplate(), q2 ) );
            }
        }

        return result;

    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds ) {
        return this.getAnalysesByExperimentIds( expressionExperimentIds, 0, -1 );
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysesByExperimentIds(
            Collection<Long> expressionExperimentIds, int offset, int limit ) {

        /*
         * There are three cases to consider: the ids are experiments; the ids are experimentsubsets; the ids are
         * experiments that have subsets.
         */
        Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> r = new HashMap<>();

        Map<Long, Collection<Long>> arrayDesignsUsed = CommonQueries
                .getArrayDesignsUsedEEMap( expressionExperimentIds, this.getSessionFactory().getCurrentSession() );

        /*
         * Fetch analyses of experiments or subsets.
         */
        //noinspection unchecked
        Collection<DifferentialExpressionAnalysis> hits = this.getSessionFactory().getCurrentSession().createQuery(
                "select distinct a from DifferentialExpressionAnalysis a join fetch a.experimentAnalyzed e join"
                        + " fetch a.resultSets rs join fetch rs.hitListSizes where e.id in (:eeids)" )
                .setParameterList( "eeids", expressionExperimentIds )
                .setFirstResult( offset )
                .setMaxResults( limit > 0 ? limit : -1 )
                .list();

        Map<Long, Collection<FactorValue>> ee2fv = new HashMap<>();
        List<Object[]> fvs;

        if ( !hits.isEmpty() ) {
            // factor values for the experiments.
            //noinspection unchecked
            fvs = this.getSessionFactory().getCurrentSession().createQuery(
                    "select distinct ee.id, fv from " + "ExpressionExperiment"
                            + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                    .setParameterList( "ees", expressionExperimentIds ).list();
            for ( Object[] oa : fvs ) {
                if ( !ee2fv.containsKey( oa[0] ) ) {
                    ee2fv.put( ( Long ) oa[0], new HashSet<FactorValue>() );
                }
                ee2fv.get( oa[0] ).add( ( FactorValue ) oa[1] );
            }

            // also get factor values for subsets - those not found yet.
            Collection<Long> used = new HashSet<>();
            for ( DifferentialExpressionAnalysis a : hits ) {
                used.add( a.getExperimentAnalyzed().getId() );
            }
            Collection<Long> probableSubSetIds = ListUtils.removeAll( used, ee2fv.keySet() );
            if ( !probableSubSetIds.isEmpty() ) {
                //noinspection unchecked
                fvs = this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct ee.id, fv from " + "ExpressionExperimentSubSet"
                                + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                        .setParameterList( "ees", probableSubSetIds ).list();

                for ( Object[] oa : fvs ) {
                    if ( !ee2fv.containsKey( oa[0] ) ) {
                        ee2fv.put( ( Long ) oa[0], new HashSet<FactorValue>() );
                    }
                    ee2fv.get( oa[0] ).add( ( FactorValue ) oa[1] );
                }
            }

        }

        /*
         * Subsets of those same experiments (there might not be any)
         */
        //noinspection unchecked
        List<DifferentialExpressionAnalysis> analysesofSubsets = this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct a from " + "ExpressionExperimentSubSet"
                        + " ee, DifferentialExpressionAnalysis a" + " join ee.sourceExperiment see "
                        + " join fetch a.experimentAnalyzed eeanalyzed where see.id in (:eeids) and ee=eeanalyzed" )
                .setParameterList( "eeids", expressionExperimentIds ).list();

        if ( !analysesofSubsets.isEmpty() ) {
            hits.addAll( analysesofSubsets );

            Collection<Long> experimentSubsetIds = new HashSet<>();
            for ( DifferentialExpressionAnalysis a : analysesofSubsets ) {
                ExpressionExperimentSubSet subset = ( ExpressionExperimentSubSet ) a.getExperimentAnalyzed();
                experimentSubsetIds.add( subset.getId() );
            }

            // factor value information for the subset. The key output is the ID of the subset, not of the source
            // experiment.
            //noinspection unchecked
            fvs = this.getSessionFactory().getCurrentSession().createQuery(
                    "select distinct ee.id, fv from " + "ExpressionExperimentSubSet"
                            + " ee join ee.bioAssays ba join ba.sampleUsed bm join bm.factorValues fv where ee.id in (:ees)" )
                    .setParameterList( "ees", experimentSubsetIds ).list();
            for ( Object[] oa : fvs ) {
                if ( !ee2fv.containsKey( oa[0] ) ) {
                    Long subsetId = ( Long ) oa[0];
                    ee2fv.put( subsetId, new HashSet<FactorValue>() );
                }
                ee2fv.get( oa[0] ).add( ( FactorValue ) oa[1] );
            }
        }

        // postprocesss...
        if ( hits.isEmpty() ) {
            return r;
        }
        Collection<DifferentialExpressionAnalysisValueObject> summaries = convertToValueObjects( hits, arrayDesignsUsed,
                ee2fv );

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
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        final String queryString = "select distinct e.id from DifferentialExpressionAnalysis a"
                + " inner join a.experimentAnalyzed e where e.id in (:eeIds)";
        return this.getHibernateTemplate().findByNamedParam( queryString, "eeIds", idsToFilter );
    }

    @Override
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        final String queryString = "select distinct ee.id from DifferentialExpressionAnalysis"
                + " as doa inner join doa.experimentAnalyzed as ee " + "inner join ee.bioAssays as ba "
                + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    public void remove( DifferentialExpressionAnalysis analysis ) {
        if ( analysis == null ) {
            throw new IllegalArgumentException( "analysis cannot be null" );
        }

        Session session = this.getSessionFactory().getCurrentSession(); // hopefully okay.
        session.flush();
        session.clear();

        session.buildLockRequest( LockOptions.NONE ).lock( analysis );
        int contrastsDone = 0;
        int resultsDone = 0;

        StopWatch timer = new StopWatch();
        timer.start();

        for ( ExpressionAnalysisResultSet rs : analysis.getResultSets() ) {

            // Delete contrasts
            final String nativeDeleteContrastsQuery =
                    "DELETE c FROM CONTRAST_RESULT c, DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d"
                            + " WHERE d.RESULT_SET_FK = :rsid AND d.ID = c.DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT_FK";
            SQLQuery q = session.createSQLQuery( nativeDeleteContrastsQuery );
            q.setParameter( "rsid", rs.getId() );
            contrastsDone += q.executeUpdate(); // cannot use the limit clause for this multi-table remove.

            // will happen by cascade.
            // // remove HIT_LISTS
            // String nativeDeleteHLQuery = "DELETE h from HIT_LIST_SIZE h"
            // + " where h.RESULT_SET_FK = :rsid  ";
            // q = session.createSQLQuery( nativeDeleteHLQuery );
            // q.setParameter( "rsid", rs.getId() );
            // resultsDone += q.executeUpdate();
            //
            // // remove P_VALUE_DISTRIBUTION
            // String nativeDeletePVDQuery = "DELETE p from ANALYSIS_RESULT_SET ars, PVALUE_DISTRIBUTION p"
            // + " where ars.ID=:rsid AND ars.PVALUE_DISTRIBUTION_FK = p.ID";
            // q = session.createSQLQuery( nativeDeletePVDQuery );
            // q.setParameter( "rsid", rs.getId() );
            // resultsDone += q.executeUpdate();

            // Delete AnalysisResults
            String nativeDeleteARQuery =
                    "DELETE d FROM DIFFERENTIAL_EXPRESSION_ANALYSIS_RESULT d" + " WHERE d.RESULT_SET_FK = :rsid  ";
            q = session.createSQLQuery( nativeDeleteARQuery );
            q.setParameter( "rsid", rs.getId() );
            resultsDone += q.executeUpdate();
            // could do in a loop with limit , might be faster.

            session.flush();
            session.clear();
        }
        log.info( "Deleted " + contrastsDone + " contrasts, " + resultsDone + " results in " + timer.getTime() + "ms" );

        analysis = ( DifferentialExpressionAnalysis ) session
                .load( DifferentialExpressionAnalysis.class, analysis.getId() );

        session.delete( analysis );

        session.flush();
        session.clear();

    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFind( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysis a"
                + "   inner join a.experimentAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet"
                + " and r.correctedPvalue < :threshold";

        String[] paramNames = { "gene", "resultSet", "threshold" };
        Object[] objectValues = { gene, resultSet, threshold };

        return this.getHibernateTemplate().findByNamedParam( findByResultSet, paramNames, objectValues );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByInvestigation( Investigation investigation ) {
        return getAnalyses( investigation );
    }

    @Override
    protected Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            Collection<Long> investigationIds ) {

        Map<Long, Collection<DifferentialExpressionAnalysis>> results = new HashMap<Long, Collection<DifferentialExpressionAnalysis>>();
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysis a"
                + "   inner join a.experimentAnalyzed e where e.id in (:eeIds)";
        List<?> qresult = this.getHibernateTemplate().findByNamedParam( queryString, "eeIds", investigationIds );
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
    protected Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigations ) {

        Map<Investigation, Collection<DifferentialExpressionAnalysis>> results = new HashMap<>();

        for ( Investigation i : investigations ) {
            results.put( i, this.getAnalyses( i ) );
        }

        return results;
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        final String queryString =
                "select distinct doa from DifferentialExpressionAnalysis as doa inner join doa.experimentAnalyzed as ee "
                        + "inner join ee.bioAssays as ba " + "inner join ba.sampleUsed as sample "
                        + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString =
                "select distinct doa from DifferentialExpressionAnalysis as doa inner join doa.experimentAnalyzed as ee "
                        + "inner join ee.bioAssays as ba "
                        + "inner join ba.sampleUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    @Override
    protected Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<CompositeSequence> probes = CommonQueries
                .getCompositeSequences( gene, this.getSessionFactory().getCurrentSession() );
        Collection<BioAssaySet> result = new HashSet<BioAssaySet>();
        if ( probes.size() == 0 ) {
            return result;
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Find probes: " + timer.getTime() + " ms" );
        }
        timer.reset();
        timer.start();

        /*
         * Note: this query misses ExpressionExperimentSubSets. The native query was implemented because HQL was always
         * constructing a constraint on SubSets. See bug 2173.
         */
        final String nativeQuery =
                "select e.ID from ANALYSIS a inner join INVESTIGATION e ON a.EXPERIMENT_ANALYZED_FK = e.ID "
                        + "inner join BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK=e.ID "
                        + " inner join BIO_MATERIAL bm ON bm.ID=ba.SAMPLE_USED_FK inner join TAXON t ON bm.SOURCE_TAXON_FK=t.ID "
                        + " inner join COMPOSITE_SEQUENCE cs ON ba.ARRAY_DESIGN_USED_FK =cs.ARRAY_DESIGN_FK where cs.ID in "
                        + " (:probes) ";

        final String speciesConstraint = " and t.ID = :taxon";
        final String parentTaxonConstraint = " and t.PARENT_TAXON_FK = :taxon";

        Taxon taxon = gene.getTaxon();
        String taxonConstraint = taxon.getIsSpecies() ? speciesConstraint : parentTaxonConstraint;

        String queryToUse = nativeQuery + taxonConstraint;

        int batchSize = 1000;
        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        for ( CompositeSequence probe : probes ) {
            batch.add( probe );

            if ( batch.size() == batchSize ) {
                fetchExperimentsTestingGeneNativeQuery( batch, result, queryToUse, taxon );
                batch.clear();
            }
        }

        if ( !batch.isEmpty() ) {
            fetchExperimentsTestingGeneNativeQuery( batch, result, queryToUse, taxon );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Find experiments: " + timer.getTime() + " ms" );
        }

        return result;
    }

    @Override
    protected void handleThaw( final Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            handleThaw( ea );
        }
    }

    @Override
    protected void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();
        Session session = this.getSessionFactory().getCurrentSession();
        session.buildLockRequest( LockOptions.NONE ).lock( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );
        session.buildLockRequest( LockOptions.NONE ).lock( differentialExpressionAnalysis.getExperimentAnalyzed() );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() );

        Hibernate.initialize( differentialExpressionAnalysis.getProtocol() );

        if ( differentialExpressionAnalysis.getSubsetFactorValue() != null ) {
            Hibernate.initialize( differentialExpressionAnalysis.getSubsetFactorValue() );
        }

        Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
        Hibernate.initialize( ears );
        for ( ExpressionAnalysisResultSet ear : ears ) {
            session.buildLockRequest( LockOptions.NONE ).lock( ear );
            Hibernate.initialize( ear );
            Hibernate.initialize( ( ( FactorAssociatedAnalysisResultSet ) ear ).getExperimentalFactors() );

        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw: " + timer.getTime() + "ms" );
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

                DiffExResultSetSummaryValueObject desvo = new DiffExResultSetSummaryValueObject();
                desvo.setThreshold( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD );
                for ( ExperimentalFactor ef : resultSet.getExperimentalFactors() ) {
                    desvo.getExperimentalFactors().add( new ExperimentalFactorValueObject( ef ) );
                }
                desvo.setArrayDesignsUsed( avo.getArrayDesignsUsed() );
                desvo.setBioAssaySetAnalyzedId( bioAssaySet.getId() ); // might be a subset.
                desvo.setResultSetId( resultSet.getId() );
                desvo.setAnalysisId( analysis.getId() );
                desvo.setFactorIds( EntityUtils.getIds( resultSet.getExperimentalFactors() ) );
                desvo.setNumberOfGenesAnalyzed( resultSet.getNumberOfGenesTested() );
                desvo.setNumberOfProbesAnalyzed( resultSet.getNumberOfProbesTested() );

                for ( HitListSize hitList : resultSet.getHitListSizes() ) {
                    if ( hitList.getThresholdQvalue()
                            .equals( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD ) ) {
                        if ( hitList.getDirection().equals( Direction.UP ) ) {
                            desvo.setUpregulatedCount( hitList.getNumberOfProbes() );
                        } else if ( hitList.getDirection().equals( Direction.DOWN ) ) {
                            desvo.setDownregulatedCount( hitList.getNumberOfProbes() );
                        } else if ( hitList.getDirection().equals( Direction.EITHER ) ) {
                            desvo.setNumberOfDiffExpressedProbes( hitList.getNumberOfProbes() );
                        }

                    }
                }

                if ( resultSet.getBaselineGroup() != null ) {
                    desvo.setBaselineGroup( new FactorValueValueObject( resultSet.getBaselineGroup() ) );
                }

                avo.getResultSets().add( desvo );

                assert ee2fv.containsKey( bioAssaySet.getId() );
                populateWhichFactorValuesUsed( avo, ee2fv.get( bioAssaySet.getId() ) );

            }

            summaries.add( avo );
        }
        return summaries;
    }

    private void fetchExperimentsTestingGeneNativeQuery( Collection<CompositeSequence> probes,
            Collection<BioAssaySet> result, final String nativeQuery, Taxon taxon ) {

        if ( probes.isEmpty() )
            return;

        SQLQuery nativeQ = this.getSessionFactory().getCurrentSession().createSQLQuery( nativeQuery );
        nativeQ.setParameterList( "probes", EntityUtils.getIds( probes ) );
        nativeQ.setParameter( "taxon", taxon );
        List<?> list = nativeQ.list();
        Set<Long> ids = new HashSet<Long>();
        for ( Object o : list ) {
            ids.add( ( ( BigInteger ) o ).longValue() );
        }
        if ( !ids.isEmpty() ) {
            result.addAll( this.getHibernateTemplate()
                    .findByNamedParam( "from ExpressionExperiment e where e.id in (:ids)", "ids", ids ) );
        }
    }

    private Collection<DifferentialExpressionAnalysis> getAnalyses( Investigation investigation ) {
        if ( investigation == null )
            throw new IllegalArgumentException( "Investigation must not be null" );
        Long id = investigation.getId();

        return getAnalysesForExperiment( id );

    }

    private Collection<DifferentialExpressionAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        final String query = "select distinct a from DifferentialExpressionAnalysis a where a.experimentAnalyzed.id=:eeid ";
        results.addAll( this.getHibernateTemplate().findByNamedParam( query, "eeid", id ) );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        results.addAll( this.getHibernateTemplate().findByNamedParam(
                "select distinct a from ExpressionExperimentSubSet eess, DifferentialExpressionAnalysis a"
                        + " join eess.sourceExperiment see "
                        + " join a.experimentAnalyzed eeanalyzed where see.id=:ee and eess=eeanalyzed", "ee", id ) );

        return results;
    }

    /**
     * Figure out which factorValues were used for each of the experimental factors (excluding the subsetfactor)
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

            if ( !avo.getFactorValuesUsed().containsKey( experimentalFactorId ) ) {
                avo.getFactorValuesUsed().put( experimentalFactorId, new HashSet<FactorValueValueObject>() );
            }

            avo.getFactorValuesUsed().get( experimentalFactorId ).add( new FactorValueValueObject( fv ) );

        }
    }

}