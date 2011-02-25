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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.FactorAssociatedAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.CommonQueries;

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
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {

        int up = this.countUpregulated( ears, threshold );
        int down = this.countDownregulated( ears, threshold );
        int result = up + down;

        if ( result > 0 ) {
            return result;
        }

        /*
         * Otherwise, perhaps we have not filled in the contrast information. Try alternative instead. This is basically
         * a backwards compatibility fix, it can be removed once all data sets are analyzed with the new method.
         */

        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r where rs = :rs and r.correctedPvalue < :threshold";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { ears, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

        if ( qresult.isEmpty() ) {
            log.warn( "No count returned" );
            return 0;
        }
        Long count = ( Long ) qresult.iterator().next();

        log.debug( "Found " + count + " differentially expressed genes in result set (" + ears.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this
                .getHibernateTemplate()
                .findByNamedParam(
                        "select distinct a from DifferentialExpressionAnalysisImpl a join a.resultSets rs left join rs.baselineGroup bg join rs.experimentalFactors efa where efa = :ef ",
                        "ef", ef );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select a from DifferentialExpressionAnalysisImpl as a where a.name = :name", "name", name );
    }

    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysis> getAnalyses( Investigation investigation ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();
        final String query = "select distinct a from DifferentialExpressionAnalysisImpl a where a.experimentAnalyzed=:expressionExperiment ";
        results.addAll( this.getHibernateTemplate().findByNamedParam( query, "expressionExperiment", investigation ) );

        /*
         * Deal with the analyses of subsets.
         */
        results
                .addAll( this
                        .getHibernateTemplate()
                        .findByNamedParam(
                                "select distinct a from ExpressionExperimentSubSetImpl eess, DifferentialExpressionAnalysisImpl a join eess.sourceExperiment see "
                                        + " join a.experimentAnalyzed ee where see=:expressionExperiment and eess=ee",
                                "expressionExperiment", investigation ) );

        return results;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisDaoBase#handleThaw(java.util.Collection)
     */
    @Override
    public void handleThaw( final Collection<DifferentialExpressionAnalysis> expressionAnalyses ) throws Exception {
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            DifferentialExpressionAnalysis dea = ea;
            doThaw( dea, false );
        }
    }

    @Override
    public void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.doThaw( differentialExpressionAnalysis, false );
    }

    protected void doThaw( final DifferentialExpressionAnalysis differentialExpressionAnalysis, final boolean deep ) {
        HibernateTemplate templ = this.getHibernateTemplate();

        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {

            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( differentialExpressionAnalysis, LockMode.NONE );
                Hibernate.initialize( differentialExpressionAnalysis );

                Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );

                if ( differentialExpressionAnalysis.getSubsetFactorValue() != null ) {
                    Hibernate.initialize( differentialExpressionAnalysis.getSubsetFactorValue() );
                }

                Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
                Hibernate.initialize( ears );
                for ( ExpressionAnalysisResultSet ear : ears ) {
                    session.lock( ear, LockMode.NONE );
                    Hibernate.initialize( ear );

                    if ( deep ) { // not used.
                        Collection<DifferentialExpressionAnalysisResult> ders = ear.getResults();
                        Hibernate.initialize( ders );
                        for ( DifferentialExpressionAnalysisResult der : ders ) {
                            session.lock( der, LockMode.NONE );
                            Hibernate.initialize( der );
                            if ( der instanceof ProbeAnalysisResult ) {
                                ProbeAnalysisResult par = ( ProbeAnalysisResult ) der;
                                CompositeSequence cs = par.getProbe();
                                Hibernate.initialize( cs );
                            }
                        }
                    }

                    Hibernate.initialize( ( ( FactorAssociatedAnalysisResultSet ) ear ).getExperimentalFactors() );

                }
                return null;
            }
        } );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFind(ubic.gemma.model.genome
     * .Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFind( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) throws Exception {
        final String findByResultSet = "select distinct r from DifferentialExpressionAnalysisImpl a"
                + "   inner join a.experimentAnalyzed e inner join e.bioAssays ba inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs inner join cs.biologicalCharacteristic bs inner join "
                + "bs.bioSequence2GeneProduct bs2gp inner join bs2gp.geneProduct gp inner join gp.gene g"
                + " inner join a.resultSets rs inner join rs.results r where r.probe=cs and g=:gene and rs=:resultSet and r.correctedPvalue < :threshold";

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
    protected Collection<DifferentialExpressionAnalysis> handleFindByInvestigation( Investigation investigation )
            throws Exception {
        return getAnalyses( investigation );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleFindByInvestigationIds(
     * java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map<Long, DifferentialExpressionAnalysis> handleFindByInvestigationIds( Collection<Long> investigationIds )
            throws Exception {
        /*
         * FIXME deal with subsets.
         */
        Map<Long, DifferentialExpressionAnalysis> results = new HashMap<Long, DifferentialExpressionAnalysis>();
        final String queryString = "select distinct e, a from DifferentialExpressionAnalysisImpl a"
                + "   inner join a.experimentAnalyzed e where e.id in (:eeIds)";
        List qresult = this.getHibernateTemplate().findByNamedParam( queryString, "eeIds", investigationIds );
        for ( Object o : qresult ) {
            Object[] oa = ( Object[] ) o;
            BioAssaySet bas = ( BioAssaySet ) oa[0];
            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) oa[1];
            results.put( bas.getId(), dea );
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
            Collection<Investigation> investigations ) throws Exception {

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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene ) throws Exception {

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
         * The constraint on taxon is required because of the potential for array designs that use sequences from the
         * "wrong" taxon, like GPL560. This way we ensure that we only get expression experiments for the same taxon as
         * the gene.
         */
        final String queryString = "select distinct e from DifferentialExpressionAnalysisImpl a "
                + " join a.experimentAnalyzed e inner join e.bioAssays ba"
                + " inner join ba.samplesUsed sa inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs where cs in (:probes) and sa.sourceTaxon = :taxon";

        // if parent taxon make sure get children - the conditional logic for species should be moved to calling class
        final String queryStringParentTaxon = "select distinct e from DifferentialExpressionAnalysisImpl a "
                + " join a.experimentAnalyzed e inner join e.bioAssays ba"
                + " inner join ba.samplesUsed sa inner join ba.arrayDesignUsed ad"
                + " inner join ad.compositeSequences cs"
                + " inner join sa.sourceTaxon childtaxon where cs in (:probes) and childtaxon.parentTaxon in (:parentTaxon) ";

        int batchSize = 1000;

        /*
         * If 'probes' is too large, query will fail so we have to batch. Yes, it can happen!
         */

        Collection<CompositeSequence> batch = new HashSet<CompositeSequence>();
        Taxon taxon = gene.getTaxon();
        String[] paramNames = { "probes", "parentTaxon" };

        for ( CompositeSequence probe : probes ) {
            batch.add( probe );

            if ( batch.size() == batchSize ) {

                if ( !taxon.getIsSpecies() ) {
                    log.debug( "Finding children taxa experiments" );
                    Object[] values = { batch, taxon };
                    result.addAll( this.getHibernateTemplate().findByNamedParam( queryStringParentTaxon, paramNames,
                            values ) );
                } else {
                    // most common case.
                    result.addAll( this.getHibernateTemplate().findByNamedParam( queryString,
                            new String[] { "probes", "taxon" }, new Object[] { batch, taxon } ) );
                }
                batch.clear();
            }

        }

        if ( !batch.isEmpty() ) {
            if ( !taxon.getIsSpecies() ) {
                log.debug( "Finding children taxa experiments" );
                Object[] values = { batch, taxon };
                result.addAll( this.getHibernateTemplate()
                        .findByNamedParam( queryStringParentTaxon, paramNames, values ) );
            } else {
                // most common case.
                result.addAll( this.getHibernateTemplate().findByNamedParam( queryString,
                        new String[] { "probes", "taxon" }, new Object[] { batch, gene.getTaxon() } ) );
            }
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Find experiments: " + timer.getTime() + " ms" );
        }
        timer.reset();
        timer.start();

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDaoBase#handleGetResultSets(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ExpressionAnalysisResultSet> handleGetResultSets( ExpressionExperiment expressionExperiment )
            throws Exception {
        /*
         * FIXME this has to be changed to handle the case of EESubSets.
         */
        final String query = "select distinct r from ExpressionAnalysisResultSetImpl r inner join r.analysis a"
                + " join a.experimentAnalyzed ee where ee=:expressionExperiment ";
        return this.getHibernateTemplate().findByNamedParam( query, "expressionExperiment", expressionExperiment );
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
        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat < 0";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { par, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

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
        String query = "select count(distinct r) from ExpressionAnalysisResultSetImpl rs inner join rs.results r join r.contrasts c where rs = :rs and r.correctedPvalue < :threshold and c.tstat > 0";

        String[] paramNames = { "rs", "threshold" };
        Object[] objectValues = { par, threshold };

        List qresult = this.getHibernateTemplate().findByNamedParam( query, paramNames, objectValues );

        if ( qresult.isEmpty() ) {
            log.warn( "No count returned" );
            return 0;
        }
        Long count = ( Long ) qresult.iterator().next();

        log.debug( "Found " + count + " upregulated genes in result set (" + par.getId()
                + ") at a corrected pvalue threshold of " + threshold );

        return count.intValue();
    }

}