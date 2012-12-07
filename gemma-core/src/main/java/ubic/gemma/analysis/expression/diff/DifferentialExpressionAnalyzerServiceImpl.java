/*
 og* The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.HitListSize;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Gene;

/**
 * Differential expression service to run the differential expression analysis (and persist the results using the
 * appropriate data access objects).
 * <p>
 * Note that there is also a DifferentialExpressionAnalysisService (which handled CRUD for analyses). In contrast this
 * _does_ the analysis.
 * 
 * @author keshav
 * @version $Id$
 */
@Component
public class DifferentialExpressionAnalyzerServiceImpl implements DifferentialExpressionAnalyzerService {

    /**
     * Defines the different types of analyses our linear modeling framework supports:
     * <ul>
     * <li>GENERICLM - genric linear regression (interactions are omitted, but this could change)
     * <li>OSTTEST - one sample t-test
     * <li>OWA - one-way anova
     * <li>TTEST - two sample t-test
     * <li>TWA - two way anova with interaction
     * <li>TWANI - two-way anova with no interaction
     * </ul>
     * 
     * @author Paul
     * @version $Id$
     */
    public enum AnalysisType {
        GENERICLM, OSTTEST /* one-sample */, OWA /* one-way anova */, TTEST, TWA /* with interactions */, TWANI /*
                                                                                                                 * no
                                                                                                                 * interactions
                                                                                                                 */
    }

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalyzerServiceImpl.class );

    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionAnalysisHelperService helperService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#getAnalyses(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment ) {
        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .getAnalyses( expressionExperiment );
        differentialExpressionAnalysisService.thaw( expressionAnalyses );
        return expressionAnalyses;
    }

    /**
     * @param ee
     * @param copyMe
     * @return
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> redoAnalysis( ExpressionExperiment ee,
            DifferentialExpressionAnalysis copyMe ) {
        Collection<DifferentialExpressionAnalysis> results = new HashSet<DifferentialExpressionAnalysis>();

        if ( !differentialExpressionAnalysisService.canDelete( copyMe ) ) {
            throw new IllegalArgumentException(
                    "Cannot redo the analysis because it is included in a meta-analysis (or something). "
                            + "Delete the constraining entity first." );
        }

        differentialExpressionAnalysisService.thaw( copyMe );

        log.info( "Will base analysis on old one: " + copyMe );
        DifferentialExpressionAnalysisConfig config = new DifferentialExpressionAnalysisConfig();

        if ( copyMe.getSubsetFactorValue() != null ) {
            config.setSubsetFactor( copyMe.getSubsetFactorValue().getExperimentalFactor() );
        }

        Collection<ExpressionAnalysisResultSet> resultSets = copyMe.getResultSets();
        Collection<ExperimentalFactor> factorsFromOldExp = new HashSet<ExperimentalFactor>();
        for ( ExpressionAnalysisResultSet rs : resultSets ) {
            Collection<ExperimentalFactor> oldfactors = rs.getExperimentalFactors();
            factorsFromOldExp.addAll( oldfactors );

            /*
             * If we included the interaction before, include it again.
             */
            if ( oldfactors.size() == 2 ) {
                log.info( "Including interaction term" );
                config.getInteractionsToInclude().add( oldfactors );
            }

        }

        if ( factorsFromOldExp.isEmpty() ) {
            throw new IllegalStateException( "Old analysis didn't have any factors" );
        }

        config.getFactorsToInclude().addAll( factorsFromOldExp );

        BioAssaySet experimentAnalyzed = copyMe.getExperimentAnalyzed();
        assert experimentAnalyzed != null;

        if ( experimentAnalyzed.equals( ee ) ) {
            results = this.runDifferentialExpressionAnalyses( ee, config );
        } else if ( experimentAnalyzed instanceof ExpressionExperimentSubSet
                && ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().equals( ee ) ) {
            DifferentialExpressionAnalysis subsetAnalysis = this.runDifferentialExpressionAnalysis(
                    ( ExpressionExperimentSubSet ) experimentAnalyzed, config );

            results.add( subsetAnalysis );
        } else {
            throw new IllegalStateException(
                    "Cannot redo an analysis for one experiment if the analysis is for another (" + ee
                            + " is the proposed target, but analysis is from " + experimentAnalyzed );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#runDifferentialExpressionAnalyses(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors );

            diffExpressionAnalyses = helperService.persistAnalyses( expressionExperiment, diffExpressionAnalyses,
                    factors );
            /*
             * Save histograms . Do this here, outside of the other transaction .
             */
            for ( DifferentialExpressionAnalysis a : diffExpressionAnalyses ) {
                helperService.writeDistributions( expressionExperiment, a );
            }
            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment,
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    @Override
    public DifferentialExpressionAnalysis runDifferentialExpressionAnalysis( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {
        try {
            DifferentialExpressionAnalysis a = doDifferentialExpressionAnalysis( subset, config );

            a = helperService.persistAnalysis( subset.getSourceExperiment(), a, config.getFactorsToInclude() );

            /*
             * Save histograms . Do this here, outside of the other transaction .
             */

            helperService.writeDistributions( subset.getSourceExperiment(), a );

            return a;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( subset.getSourceExperiment(),
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    "Failed to run analysis on subset: " + subset, ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#runDifferentialExpressionAnalyses(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalysisConfig)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, config );

            diffExpressionAnalyses = helperService.persistAnalyses( expressionExperiment, diffExpressionAnalyses,
                    config.getFactorsToInclude() );

            /*
             * Save histograms . Do this here, outside of the other transaction .
             */
            for ( DifferentialExpressionAnalysis a : diffExpressionAnalyses ) {
                helperService.writeDistributions( expressionExperiment, a );
            }
            return diffExpressionAnalyses;
        } catch ( Exception e ) {
            auditTrailService.addUpdateEvent( expressionExperiment,
                    FailedDifferentialExpressionAnalysisEvent.Factory.newInstance(),
                    ExceptionUtils.getFullStackTrace( e ) );
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#updateScoreDistributionFiles(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public void updateScoreDistributionFiles( ExpressionExperiment ee ) {

        Collection<DifferentialExpressionAnalysis> analyses = this.getAnalyses( ee );

        if ( analyses.size() == 0 ) {
            log.info( "No  analyses for experiment " + ee.getShortName()
                    + ".  The differential expression analysis may not have been run on this experiment yet." );
            return;
        }

        for ( DifferentialExpressionAnalysis differentialExpressionAnalysis : analyses ) {
            helperService.writeDistributions( ee, differentialExpressionAnalysis );
        }

    }

    private Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        return analysisSelectionAndExecutionService.analyze( expressionExperiment, factors );
    }

    private DifferentialExpressionAnalysis doDifferentialExpressionAnalysis( ExpressionExperimentSubSet subset,
            DifferentialExpressionAnalysisConfig config ) {
        return analysisSelectionAndExecutionService.analyze( subset, config );
    }

    private Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        return analysisSelectionAndExecutionService.analyze( expressionExperiment, config );
    }

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private CompositeSequenceService compositeSequenceService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#updateSummaries(ubic.gemma.model.analysis
     * .expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void updateSummaries( DifferentialExpressionAnalysis analysis ) {

        DiffExAnalyzer lma = analysisSelectionAndExecutionService.getAnalyzer();
        Map<CompositeSequence, Collection<Gene>> probe2GeneMap = new HashMap<CompositeSequence, Collection<Gene>>();

        log.info( "Reading the analysis ..." );
        differentialExpressionAnalysisService.thaw( analysis );
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            differentialExpressionResultService.thaw( resultSet );
            List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                    resultSet.getResults() );
            for ( DifferentialExpressionAnalysisResult d : results ) {
                CompositeSequence probe = d.getProbe();
                probe2GeneMap.put( probe, new HashSet<Gene>() );
            }
        }

        log.info( "Retrieving gene-element information ..." );
        probe2GeneMap = compositeSequenceService.getGenes( probe2GeneMap.keySet() );

        if ( probe2GeneMap.isEmpty() ) throw new IllegalStateException( "The probes do not map to genes" );

        int i = 1;
        for ( ExpressionAnalysisResultSet resultSet : analysis.getResultSets() ) {
            log.info( "Updating stats for " + i + "/" + analysis.getResultSets().size()
                    + " resultsets for the analysis" );
            List<DifferentialExpressionAnalysisResult> results = new ArrayList<DifferentialExpressionAnalysisResult>(
                    resultSet.getResults() );
            Collection<HitListSize> hitlists = lma.computeHitListSizes( results, probe2GeneMap );
            resultSet.getHitListSizes().clear();
            resultSet.getHitListSizes().addAll( hitlists );
            resultSet.setNumberOfGenesTested( lma.getNumberOfGenesTested( results, probe2GeneMap ) );
            resultSet.setNumberOfProbesTested( results.size() );
            differentialExpressionResultService.update( resultSet );
            i++;
        }

        log.info( "Writing distributions" );
        helperService.writeDistributions( analysis.getExperimentAnalyzed(), analysis );
        log.info( "Done" );

    }

}
