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

import java.util.Collection;
import java.util.HashSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.analysis.expression.diff.AnalysisSelectionAndExecutionService;
import ubic.gemma.analysis.expression.diff.DiffExAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerServiceImpl.AnalysisType;
import ubic.gemma.analysis.preprocess.batcheffects.BatchInfoPopulationServiceImpl;
import ubic.gemma.analysis.util.ExperimentalDesignUtils;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisTask;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.util.ConfigUtils;

/**
 * A controller to run differential expression analysis either locally or in a space.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class DifferentialExpressionAnalysisController extends AbstractTaskService {

    public DifferentialExpressionAnalysisController() {
        super();
        this.setBusinessInterface( DifferentialExpressionAnalysisTask.class );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class DiffAnalysisSpaceJob extends BackgroundJob<DifferentialExpressionAnalysisTaskCommand> {

        final DifferentialExpressionAnalysisTask taskProxy = ( DifferentialExpressionAnalysisTask ) getProxy();

        public DiffAnalysisSpaceJob( DifferentialExpressionAnalysisTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    /**
     * Local task.
     */
    private class DiffAnalysisJob extends BackgroundJob<DifferentialExpressionAnalysisTaskCommand> {
        public DiffAnalysisJob( DifferentialExpressionAnalysisTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return differentialExpressionAnalysisTask.execute( command );
        }

    }

    @Autowired
    private DifferentialExpressionAnalysisTask differentialExpressionAnalysisTask;

    @Autowired
    private AnalysisSelectionAndExecutionService analysisSelectionAndExecutionService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

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
                null );

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
     * AJAX entry point when running completely automatically.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        /* this 'run' method is exported in the spring-beans.xml */

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }
        ee = expressionExperimentService.thawLite( ee );

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setFactors( ExperimentalDesignUtils.factorsWithoutBatch( ee.getExperimentalDesign()
                .getExperimentalFactors() ) );

        return super.run( cmd );
    }

    /**
     * AJAX entry point for 'customized' analysis.
     * 
     * @param id
     * @param factorIds
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
        return super.run( cmd );
    }

    public void setDifferentialExpressionAnalyzer( AnalysisSelectionAndExecutionService differentialExpressionAnalyzer ) {
        this.analysisSelectionAndExecutionService = differentialExpressionAnalyzer;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<DifferentialExpressionAnalysisTaskCommand> getInProcessRunner( TaskCommand command ) {
        if ( ConfigUtils.getBoolean( "gemma.grid.gridonly.diff" ) ) {
            return null;
        }
        return new DiffAnalysisJob( ( DifferentialExpressionAnalysisTaskCommand ) command );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<DifferentialExpressionAnalysisTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new DiffAnalysisSpaceJob( ( DifferentialExpressionAnalysisTaskCommand ) command );

    }

}
