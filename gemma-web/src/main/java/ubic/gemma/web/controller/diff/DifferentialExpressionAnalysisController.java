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
import ubic.gemma.core.analysis.expression.diff.AnalysisType;
import ubic.gemma.core.analysis.expression.diff.DiffExAnalyzerUtils;
import ubic.gemma.core.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisRemoveTaskCommand;
import ubic.gemma.core.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalDesignUtils;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * A controller to run differential expression analysis either locally or in a space.
 *
 * @author keshav
 */
@Controller
public class DifferentialExpressionAnalysisController {
    protected static final Log log = LogFactory.getLog( DifferentialExpressionAnalysisController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    @Autowired
    private ExpressionExperimentReportService experimentReportService;

    /**
     * Ajax method. Pick the analysis type when we want it to be completely automated. Does not support subset factors
     *
     * @param  id id
     * @return analysis info
     */
    public DifferentialExpressionAnalyzerInfo determineAnalysisType( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "Cannot access experiment with id=" + id );

        Collection<ExperimentalFactor> factorsWithoutBatch = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .filter( f -> !ExperimentalDesignUtils.isBatchFactor( f ) )
                .collect( Collectors.toSet() );

        AnalysisType analyzer = DiffExAnalyzerUtils.determineAnalysisType( ee, factorsWithoutBatch, null /* subset */, true /* include interactions */ );

        DifferentialExpressionAnalyzerInfo result = new DifferentialExpressionAnalyzerInfo();

        // we include all factors here, so that batch can be used for subsetting (up to client)
        for ( ExperimentalFactor factor : ee.getExperimentalDesign().getExperimentalFactors() ) {
            result.getFactors().add( new ExperimentalFactorValueObject( factor ) );
        }

        if ( analyzer == null ) {
            /*
             * Either there are no viable automatic choices, or there are no usable factors...
             */
            if ( factorsWithoutBatch.size() < 2 ) {
                throw new IllegalStateException( "This data set does not seem suitable for analysis." );
            }
            result.setType( AnalysisType.GENERICLM.toString() );
        } else {
            result.setType( analyzer.toString() );
        }
        return result;
    }

    /**
     * AJAX entry point to redo an analysis.
     *
     * @param  eeId ee id
     * @param  id   id
     * @return string
     */
    public String redo( Long eeId, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId,
                EntityNotFoundException::new, "Cannot access experiment with id=" + eeId );
        DifferentialExpressionAnalysis toRedo = differentialExpressionAnalysisService.loadOrFail( id,
                EntityNotFoundException::new, "Cannot access analysis with id=" + id );
        this.experimentReportService.evictFromCache( ee.getId() );
        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee, toRedo );
        return taskRunningService.submitTaskCommand( cmd );
    }

    public String refreshStats( Long eeId, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId,
                EntityNotFoundException::new, "Cannot access experiment with id=" + eeId );
        DifferentialExpressionAnalysis toRefresh = differentialExpressionAnalysisService.loadOrFail( id,
                EntityNotFoundException::new, "Cannot access analysis with id=" + id );
        this.experimentReportService.evictFromCache( ee.getId() );
        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee, toRefresh );
        return taskRunningService.submitTaskCommand( cmd );
    }

    /**
     * AJAX entry point to remove an analysis given by the ID
     *
     * @param  id id
     * @return string
     */
    public String remove( Long eeId, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadOrFail( eeId,
                EntityNotFoundException::new, "Cannot access experiment with id=" + eeId );
        DifferentialExpressionAnalysis toRemove = differentialExpressionAnalysisService.loadOrFail( id,
                EntityNotFoundException::new, "Cannot access analysis with id=" + id );
        DifferentialExpressionAnalysisRemoveTaskCommand cmd = new DifferentialExpressionAnalysisRemoveTaskCommand( ee,
                toRemove );
        this.experimentReportService.evictFromCache( ee.getId() );
        return taskRunningService.submitTaskCommand( cmd );
    }

    /**
     * AJAX entry point when running completely automatically.
     *
     * @param  id id
     * @return string
     */
    public String run( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "Cannot access experiment with id=" + id );
        this.experimentReportService.evictFromCache( ee.getId() );

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        boolean rnaSeq = expressionExperimentService.isRNASeq( ee );
        cmd.setUseWeights( rnaSeq );
        cmd.setFactors(
                ee.getExperimentalDesign().getExperimentalFactors().stream()
                        .filter( f -> !ExperimentalDesignUtils.isBatchFactor( f ) )
                        .collect( Collectors.toSet() ) );
        cmd.setIncludeInteractions( true ); // if possible, might get dropped.

        return taskRunningService.submitTaskCommand( cmd );
    }

    /**
     * Perform a customized DEA based on user input on web interface.
     *
     * @param  id                  of the experiment
     * @param  factorids           to include
     * @param  includeInteractions if possible
     * @param  subsetFactorId      if required
     * @return task identifier
     */
    public String runCustom( Long id, Collection<Long> factorids, boolean includeInteractions, Long subsetFactorId ) {

        if ( factorids.isEmpty() ) {
            throw new IllegalArgumentException( "You must provide at least one factor to analyze" );
        }

        ExpressionExperiment ee = expressionExperimentService.loadAndThawLiteOrFail( id,
                EntityNotFoundException::new, "Cannot access experiment with id=" + id );

        /*
         * Get the factors matching the factorids
         */
        Collection<ExperimentalFactor> factors = new HashSet<>();
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
        boolean rnaSeq = expressionExperimentService.isRNASeq( ee );
        cmd.setUseWeights( rnaSeq );
        cmd.setFactors( factors );
        cmd.setSubsetFactor( subsetFactor );

        for ( ExperimentalFactor ef : factors ) {
            if ( ExperimentalDesignUtils.isBatchFactor( ef ) ) {
                /*
                 * This is a policy and I am pretty sure it makes sense!
                 */
                DifferentialExpressionAnalysisController.log
                        .warn( "Removing interaction term because it includes 'batch'" );
                includeInteractions = false;
            }
        }

        cmd.setIncludeInteractions( includeInteractions );

        DifferentialExpressionAnalysisController.log.info( "Initializing analysis" );
        this.experimentReportService.evictFromCache( ee.getId() );
        return taskRunningService.submitTaskCommand( cmd );
    }
}
