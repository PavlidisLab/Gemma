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
package ubic.gemma.model.analysis.expression.diff;

import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockOptions;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Direction;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.CommonQueries;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysis
 * @version $Id$
 * @author paul
 */
@Repository
public class DifferentialExpressionAnalysisDaoImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase {

    private Log log = LogFactory.getLog( this.getClass() );

    @Autowired
    public DifferentialExpressionAnalysisDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#countProbesMeetingThreshold(ubic.
     * gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {

        // int up = this.countUpregulated( ears, threshold );
        // int down = this.countDownregulated( ears, threshold );
        // int result = up + down;
        //
        // if ( result > 0 ) {
        // return result;
        // }

        /*
         * Otherwise, perhaps we have not filled in the contrast information. Try alternative instead. This is basically
         * a backwards compatibility fix, it can be removed once all data sets are analyzed with the new method.
         */

        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r where rs = :rs and r.correctedPvalue < :threshold";

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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#findByFactor(ubic.gemma.model.expression
     * .experiment.ExperimentalFactor)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select distinct a from DifferentialExpressionAnalysisImpl a join a.resultSets rs"
                        + " left join rs.baselineGroup bg join rs.experimentalFactors efa where efa = :ef ", "ef", ef );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDao#findByName(java.lang.String)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from DifferentialExpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    /**
     * @param investigation
     * @return
     */
    private Collection<DifferentialExpressionAnalysis> getAnalyses( Investigation investigation ) {
        if ( investigation == null ) throw new IllegalArgumentException( "Investigation must not be null" );
        Long id = investigation.getId();

        return getAnalysesForExperiment( id );

    }

    private Collection<DifferentialExpressionAnalysis> getAnalysesForExperiment( Long id ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        final String query = "select distinct a from DifferentialExpressionAnalysisImpl a where a.experimentAnalyzed.id=:eeid ";
        results.addAll( this.getHibernateTemplate().findByNamedParam( query, "eeid", id ) );

        /*
         * Deal with the analyses of subsets of the investigation. User has to know this is possible.
         */
        results.addAll( this.getHibernateTemplate().findByNamedParam(
                "select distinct a from ExpressionExperimentSubSetImpl eess, DifferentialExpressionAnalysisImpl a"
                        + " join eess.sourceExperiment see "
                        + " join a.experimentAnalyzed eeanalyzed where see.id=:ee and eess=eeanalyzed", "ee", id ) );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#getAnalyses(java.util.Collection)
     */
    @Override
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> experiments ) {
        Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> result = new HashMap<BioAssaySet, Collection<DifferentialExpressionAnalysis>>();

        /*
         * In these queries, we used left joins for things that were added to the system 'recently' and might have been
         * null, namely baselineGroup and hitListSizes. But this should no longer be the case.
         */

        StopWatch timer = new StopWatch();
        timer.start();
        final String query = "select distinct a from DifferentialExpressionAnalysisImpl a inner join fetch a.resultSets res "
                + "inner join fetch res.baselineGroup"
                + " inner join fetch res.experimentalFactors facs inner join fetch facs.factorValues "
                + "inner join fetch res.hitListSizes where a.experimentAnalyzed.id in (:ees) ";

        List<DifferentialExpressionAnalysis> r1 = this.getHibernateTemplate().findByNamedParam( query, "ees",
                EntityUtils.getIds( experiments ) );
        int count = 0;
        for ( DifferentialExpressionAnalysis a : r1 ) {
            if ( !result.containsKey( a.getExperimentAnalyzed() ) ) {
                result.put( a.getExperimentAnalyzed(), new HashSet<DifferentialExpressionAnalysis>() );
            }
            result.get( a.getExperimentAnalyzed() ).add( a );
            count++;
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Fetch " + count + " analyses for " + result.size() + " experiments: " + timer.getTime() + "ms" );
            log.info( "Query was: " + NativeQueryUtils.toSql( this.getHibernateTemplate(), query ) );
        }
        timer.reset();
        timer.start();

        /*
         * Deal with the analyses of subsets of the experiments given being analyzed; but we keep things organized by
         * the source experiment. Maybe that is confusing.
         */
        String q2 = "select distinct a from ExpressionExperimentSubSetImpl eess, DifferentialExpressionAnalysisImpl a "
                + " inner join fetch a.resultSets res inner join fetch res.baselineGroup "
                + " inner join fetch res.experimentalFactors facs inner join fetch facs.factorValues"
                + " inner join fetch res.hitListSizes  "
                + " join eess.sourceExperiment see join a.experimentAnalyzed ee  where eess=ee and see.id in (:ees) ";
        List<DifferentialExpressionAnalysis> r2 = this.getHibernateTemplate().findByNamedParam( q2, "ees",
                EntityUtils.getIds( experiments ) );

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
                log.info( "Fetch " + count + " subset analyses for " + result.size() + " experiment subsets: "
                        + timer.getTime() + "ms" );
                log.info( "Query for subsets was: " + NativeQueryUtils.toSql( this.getHibernateTemplate(), q2 ) );
            }
        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    protected void handleThaw( final Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            DifferentialExpressionAnalysis dea = ea;
            doThaw( dea );
        }
    }

    @Override
    protected void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.doThaw( differentialExpressionAnalysis );
    }

    /**
     * @param differentialExpressionAnalysis
     * @param deep
     */
    protected void doThaw( final DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        Session session = this.getSession();
        session.buildLockRequest( LockOptions.NONE ).lock( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis );

        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );

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

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome
     * .Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFind( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysisImpl a"
                + "   inner join a.experimentAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet"
                + " and r.correctedPvalue < :threshold";

        String[] paramNames = { "gene", "resultSet", "threshold" };
        Object[] objectValues = { gene, resultSet, threshold };

        return this.getHibernateTemplate().findByNamedParam( findByResultSet, paramNames, objectValues );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByInvestigation(ubic.gemma.model.analysis.Investigation)
     */
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByInvestigation( Investigation investigation ) {
        return getAnalyses( investigation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFindByInvestigationIds(
     * java.util.Collection)
     */
    @Override
    protected Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            Collection<Long> investigationIds ) {

        Map<Long, Collection<DifferentialExpressionAnalysis>> results = new HashMap<Long, Collection<DifferentialExpressionAnalysis>>();
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByInvestigations(java.util.Collection)
     */
    @Override
    protected Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            Collection<Investigation> investigations ) {

        Map<Investigation, Collection<DifferentialExpressionAnalysis>> results = new HashMap<Investigation, Collection<DifferentialExpressionAnalysis>>();

        for ( Investigation i : investigations ) {
            results.put( i, this.getAnalyses( i ) );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        final String queryString = "select distinct doa from DifferentialExpressionAnalysisImpl as doa inner join doa.experimentAnalyzed as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample "
                + "inner join sample.sourceTaxon as childtaxon where childtaxon.parentTaxon  = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisDaoBase#handleFindByTaxon(ubic.gemma.model.genome.Taxon)
     */
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        final String queryString = "select distinct doa from DifferentialExpressionAnalysisImpl as doa inner join doa.experimentAnalyzed as ee "
                + "inner join ee.bioAssays as ba "
                + "inner join ba.samplesUsed as sample where sample.sourceTaxon = :taxon ";
        return this.getHibernateTemplate().findByNamedParam( queryString, "taxon", taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFindExperimentsWithAnalyses
     * (ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Collection<CompositeSequence> probes = CommonQueries.getCompositeSequences( gene, this.getSession() );
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
        final String nativeQuery = "select e.ID from ANALYSIS a inner join INVESTIGATION e ON a.EXPERIMENT_ANALYZED_FK = e.ID "
                + "inner join BIO_ASSAY ba ON ba.EXPRESSION_EXPERIMENT_FK=e.ID inner join SAMPLES_USED sa ON ba.ID=sa.BIO_ASSAYS_USED_IN_FK "
                + " inner join BIO_MATERIAL bm ON bm.ID=sa.SAMPLES_USED_FK inner join TAXON t ON bm.SOURCE_TAXON_FK=t.ID "
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

    private void fetchExperimentsTestingGeneNativeQuery( Collection<CompositeSequence> probes,
            Collection<BioAssaySet> result, final String nativeQuery, Taxon taxon ) {

        if ( probes.isEmpty() ) return;

        SQLQuery nativeQ = this.getSession().createSQLQuery( nativeQuery );
        nativeQ.setParameterList( "probes", EntityUtils.getIds( probes ) );
        nativeQ.setParameter( "taxon", taxon );
        List<?> list = nativeQ.list();
        Set<Long> ids = new HashSet<Long>();
        for ( Object o : list ) {
            ids.add( ( ( BigInteger ) o ).longValue() );
        }
        if ( !ids.isEmpty() ) {
            result.addAll( this.getHibernateTemplate().findByNamedParam(
                    "from ExpressionExperimentImpl e where e.id in (:ids)", "ids", ids ) );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#countDownregulated(ubic.gemma.model
     * .analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r "
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao#countUpregulated(ubic.gemma.model
     * .analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r"
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
    public Collection<DifferentialExpressionAnalysisValueObject> getAnalysisValueObjects( Long experimentId ) {
        Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<DifferentialExpressionAnalysisValueObject>();
        Collection<DifferentialExpressionAnalysis> analyses = getAnalysesForExperiment( experimentId );

        // FIXME this can be made much faster with some query help.
        for ( DifferentialExpressionAnalysis analysis : analyses ) {

            Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

            DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

            if ( analysis.getSubsetFactorValue() != null ) {
                avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );
                avo.setSubsetFactor( new ExperimentalFactorValueObject( analysis.getSubsetFactorValue()
                        .getExperimentalFactor() ) );
            }

            for ( ExpressionAnalysisResultSet par : results ) {

                DifferentialExpressionSummaryValueObject desvo = new DifferentialExpressionSummaryValueObject();
                desvo.setThreshold( DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD );
                desvo.setExperimentalFactors( par.getExperimentalFactors() );
                desvo.setResultSetId( par.getId() );
                desvo.setAnalysisId( analysis.getId() );
                desvo.setFactorIds( EntityUtils.getIds( par.getExperimentalFactors() ) );

                for ( HitListSize hitList : par.getHitListSizes() ) {
                    if ( hitList.getThresholdQvalue().equals(
                            DifferentialExpressionAnalysisValueObject.DEFAULT_THRESHOLD ) ) {

                        if ( hitList.getDirection().equals( Direction.UP ) ) {
                            desvo.setUpregulatedCount( hitList.getNumberOfProbes() );
                        } else if ( hitList.getDirection().equals( Direction.DOWN ) ) {
                            desvo.setDownregulatedCount( hitList.getNumberOfProbes() );
                        } else if ( hitList.getDirection().equals( Direction.EITHER ) ) {
                            desvo.setNumberOfDiffExpressedProbes( hitList.getNumberOfProbes() );
                        }

                    }
                }

                if ( par.getBaselineGroup() != null ) {
                    desvo.setBaselineGroup( new FactorValueValueObject( par.getBaselineGroup() ) );
                }

                avo.getResultSets().add( desvo );

            }

            summaries.add( avo );
        }
        return summaries;
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysisValueObjects(
            Collection<Long> expressionExperimentIds ) {
        Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> result = new HashMap<Long, Collection<DifferentialExpressionAnalysisValueObject>>();
        for ( Long id : expressionExperimentIds ) {

            Collection<DifferentialExpressionAnalysisValueObject> analyses = this.getAnalysisValueObjects( id );
            if ( analyses.isEmpty() ) continue;
            result.put( id, analyses );
        }
        return result;
    }

}