/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;

import java.util.Collection;
import java.util.HashSet;

/**
 * A controller to run differential expression analysis either locally or in a space.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class DifferentialExpressionAnalysisController {
    protected static Log log = LogFactory.getLog( DifferentialExpressionAnalysisController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentReportService experimentReportService;

    /**
     * Ajax method. Pick the analysis type when we want it to be completely automated.
     * 
     * @param id
     * @return
     */
    public DifferentialExpressionAnalyzerInfo determineAnalysisType( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<ExperimentalFactor> factorsWithoutBatch = ExperimentalDesignUtils.factorsWithoutBatch( ee
                .getExperimentalDesign().getExperimentalFactors() );

        AnalysisType analyzer = this.analysisSelectionAndExecutionService.determineAnalysis( ee, factorsWithoutBatch,
                null, true );

        DifferentialExpressionAnalyzerInfo result = new DifferentialExpressionAnalyzerInfo();

        for ( ExperimentalFactor factor : factorsWithoutBatch ) {
            result.getFactors().add( new ExperimentalFactorValueObject( factor ) );
        }

        if ( analyzer == null ) {
            /*
             * Either there are no viable automatic choices, or there are no usable factors...
             */
            if ( factorsWithoutBatch.size() < 2 ) {
                throw new IllegalStateException( "This data set does not seem suitable for analysis." );
            }
        }

        result.setType( analyzer );
        return result;
    }

    /**
     * AJAX entry point to remove an analysis given by the ID
     * 
     * @param id
     * @return
     * @throws Exception
     */
    public String remove( Long eeId, Long id ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );
        }

        DifferentialExpressionAnalysis toRemove = differentialExpressionAnalysisService.load( id );
        if ( toRemove == null ) {
            throw new IllegalArgumentException( "Cannot access analysis with id=" + id );
        }
        DifferentialExpressionAnalysisRemoveTaskCommand cmd = new DifferentialExpressionAnalysisRemoveTaskCommand( ee,
                toRemove );
        this.experimentReportService.evictFromCache( ee.getId() );
        return taskRunningService.submitRemoteTask( cmd );
    }

    /**
     * AJAX entry point to redo an analysis.
     * 
     * @param eeId
     * @param id
     * @return
     * @throws Exception
     */
    public String redo( Long eeId, Long id ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );
        }

        DifferentialExpressionAnalysis toRedo = differentialExpressionAnalysisService.load( id );
        if ( toRedo == null ) {
            throw new IllegalArgumentException( "Cannot access analysis with id=" + id );
        }
        this.experimentReportService.evictFromCache( ee.getId() );
        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee, toRedo, true );
        return taskRunningService.submitRemoteTask( cmd );
    }

    /**
     * @param eeId
     * @param id
     * @return
     * @throws Exception
     */
    public String refreshStats( Long eeId, Long id ) throws Exception {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + eeId );
        }

        DifferentialExpressionAnalysis toRefresh = differentialExpressionAnalysisService.load( id );
        if ( toRefresh == null ) {
            throw new IllegalArgumentException( "Cannot access analysis with id=" + id );
        }
        this.experimentReportService.evictFromCache( ee.getId() );
        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee, toRefresh,
                false );
        return taskRunningService.submitLocalTask( cmd );
    }

    /**
     * AJAX entry point when running completely automatically.
     * 
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }
        this.experimentReportService.evictFromCache( ee.getId() );
        ee = expressionExperimentService.thawLite( ee );

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setFactors( ExperimentalDesignUtils.factorsWithoutBatch( ee.getExperimentalDesign()
                .getExperimentalFactors() ) );

        return taskRunningService.submitRemoteTask( cmd );
    }

    /**
     * AJAX entry point for 'customized' analysis.
     * 
     * @param id
     * @param includeInteractions
     * @param subsetFactorId optional
     * @return
     * @throws Exception
     */
    public String runCustom( Long id, Collection<Long> factorids, boolean includeInteractions, Long subsetFactorId )
            throws Exception {

        if ( factorids.isEmpty() ) {
            throw new IllegalArgumentException( "You must provide at least one factor to analyze" );
        }

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }

        ee = expressionExperimentService.thawLite( ee );

        /*
         * Get the factors matching the factorids
         */
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( factorids.contains( ef.getId() ) ) {
                factors.add( ef );
            }
        }

        if ( factors.size() != factorids.size() ) {
            throw new IllegalArgumentException( "Unknown factors?" );
        }

        ExperimentalFactor subsetFactor = null;
        if ( subsetFactorId != null ) {
            for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
                if ( subsetFactorId.equals( ef.getId() ) ) {
                    subsetFactor = ef;
                    break;
                }
            }
            if ( subsetFactor == null ) {
                throw new IllegalArgumentException( "Unknown subset factor?" );
            }

            if ( factors.contains( subsetFactor ) ) {
                throw new IllegalArgumentException( "Subset factor must not be one of the factors used in the analysis" );
            }
        }

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setFactors( factors );
        cmd.setSubsetFactor( subsetFactor );

        for ( ExperimentalFactor ef : factors ) {
            if ( BatchInfoPopulationServiceImpl.isBatchFactor( ef ) ) {
                /*
                 * This is a policy and I am pretty sure it makes sense!
                 */
                log.warn( "Removing interaction term because it includes 'batch'" );
                includeInteractions = false;
            }
        }

        cmd.setIncludeInteractions( includeInteractions );

        log.info( "Initializing analysis" );
        this.experimentReportService.evictFromCache( ee.getId() );
        return taskRunningService.submitRemoteTask( cmd );
    }
}
