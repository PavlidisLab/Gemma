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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedDifferentialExpressionAnalysisEvent;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

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

    private static Log log = LogFactory.getLog( DifferentialExpressionAnalyzerServiceImpl.class );

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

    @Autowired
    private AuditTrailService auditTrailService = null;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;

    @Autowired
    private DifferentialExpressionAnalysisHelperService helperService;

    private Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors ) {
        return analysisSelectionAndExecutionService.analyze( expressionExperiment, factors );
    }

    private Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type ) {
        return analysisSelectionAndExecutionService.analyze( expressionExperiment, factors, type );
    }

    private Collection<DifferentialExpressionAnalysis> doDifferentialExpressionAnalysis(
            ExpressionExperiment expressionExperiment, DifferentialExpressionAnalysisConfig config ) {
        return analysisSelectionAndExecutionService.analyze( expressionExperiment, config );
    }

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

            return helperService.persistAnalyses( expressionExperiment, diffExpressionAnalyses, factors );
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
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#runDifferentialExpressionAnalyses(ubic
     * .gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection,
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> runDifferentialExpressionAnalyses(
            ExpressionExperiment expressionExperiment, Collection<ExperimentalFactor> factors, AnalysisType type ) {

        try {
            Collection<DifferentialExpressionAnalysis> diffExpressionAnalyses = doDifferentialExpressionAnalysis(
                    expressionExperiment, factors, type );

            Collection<DifferentialExpressionAnalysis> results = helperService.persistAnalyses( expressionExperiment,
                    diffExpressionAnalyses, factors );
            return results;
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

            return helperService.persistAnalyses( expressionExperiment, diffExpressionAnalyses,
                    config.getFactorsToInclude() );
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#wasDifferentialAnalysisRun(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment)
     */
    @Override
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        return !expressionAnalyses.isEmpty();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService#wasDifferentialAnalysisRun(ubic.gemma
     * .model.expression.experiment.ExpressionExperiment, java.util.Collection)
     */
    @Override
    public boolean wasDifferentialAnalysisRun( ExpressionExperiment ee, Collection<ExperimentalFactor> factors ) {

        Collection<DifferentialExpressionAnalysis> expressionAnalyses = differentialExpressionAnalysisService
                .findByInvestigation( ee );

        for ( DifferentialExpressionAnalysis de : expressionAnalyses ) {
            Collection<ExperimentalFactor> factorsInAnalysis = new HashSet<ExperimentalFactor>();
            for ( ExpressionAnalysisResultSet resultSet : de.getResultSets() ) {
                factorsInAnalysis.addAll( resultSet.getExperimentalFactors() );
            }

            if ( factorsInAnalysis.size() == factors.size() && factorsInAnalysis.containsAll( factors ) ) {
                return true;
            }

        }
        return false;
    }

}
