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

import ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzer;
import ubic.gemma.analysis.expression.diff.OneWayAnovaAnalyzer;
import ubic.gemma.analysis.expression.diff.TTestAnalyzer;
import ubic.gemma.analysis.expression.diff.TwoWayAnovaWithInteractionsAnalyzer;
import ubic.gemma.analysis.expression.diff.TwoWayAnovaWithoutInteractionsAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisTask;
import ubic.gemma.tasks.analysis.diffex.DifferentialExpressionAnalysisTaskCommand;

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

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisSpaceJob( DifferentialExpressionAnalysisTaskCommand commandObj ) {
            super( commandObj );

        }

        /**
         * @return
         */
        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    @Autowired
    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer;

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
        AbstractDifferentialExpressionAnalyzer analyzer = this.differentialExpressionAnalyzer.determineAnalysis( ee,
                null );

        DifferentialExpressionAnalyzerInfo result = new DifferentialExpressionAnalyzerInfo();

        for ( ExperimentalFactor factor : ee.getExperimentalDesign().getExperimentalFactors() ) {
            result.getFactors().add( new ExperimentalFactorValueObject( factor ) );
        }

        if ( analyzer == null ) {
            /*
             * Either there are no viable automatic choices, or there are no factors...
             */

        } else if ( analyzer instanceof TTestAnalyzer ) {
            if ( ee.getExperimentalDesign().getExperimentalFactors().iterator().next().getFactorValues().size() == 1 ) {
                result.setType( AnalysisType.OSTTEST );
            } else {
                result.setType( AnalysisType.TTEST );
            }
        } else if ( analyzer instanceof OneWayAnovaAnalyzer ) {
            result.setType( AnalysisType.OWA );
        } else if ( analyzer instanceof TwoWayAnovaWithInteractionsAnalyzer ) {
            result.setType( AnalysisType.TWIA );
        } else if ( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer ) {
            result.setType( AnalysisType.TWA );
        } else {
            result.setType( AnalysisType.GENERICLM );
        }
        return result;
    }

    /**
     * Not used?
     * 
     * @param id
     * @param factorids
     * @param type
     * @return
     */
    public DifferentialExpressionAnalyzerInfo determineAnalysisType( Long id, Collection<Long> factorids,
            AnalysisType type ) {

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }

        /*
         * Get the factors matching the factorids
         */
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( factorids.contains( ef.getId() ) ) {
                factors.add( ef );
            }
        }

        AbstractDifferentialExpressionAnalyzer analyzer = this.differentialExpressionAnalyzer.determineAnalysis( ee,
                factors, type );

        DifferentialExpressionAnalyzerInfo result = new DifferentialExpressionAnalyzerInfo();

        for ( ExperimentalFactor factor : ee.getExperimentalDesign().getExperimentalFactors() ) {
            result.getFactors().add( new ExperimentalFactorValueObject( factor ) );
        }

        if ( analyzer == null ) {
            /*
             * Either there are no viable automatic choices, or there are no factors...
             */

        } else if ( analyzer instanceof TTestAnalyzer ) {
            result.setType( AnalysisType.TTEST );
        } else if ( analyzer instanceof OneWayAnovaAnalyzer ) {
            result.setType( AnalysisType.OWA );
        } else if ( analyzer instanceof TwoWayAnovaWithInteractionsAnalyzer ) {
            result.setType( AnalysisType.TWIA );
        } else if ( analyzer instanceof TwoWayAnovaWithoutInteractionsAnalyzer ) {
            result.setType( AnalysisType.TWA );
        } else {
            result.setType( AnalysisType.GENERICLM );
        }
        return result;
    }

    /**
     * AJAX entry point.
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

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );

        return super.run( cmd );
    }

    /**
     * AJAX entry point for 'customized' analysis.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String runCustom( Long id, Collection<Long> factorids, boolean includeInteractions ) throws Exception {
        /* this 'run' method is exported in the spring-beans.xml */

        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }

        /*
         * Get the factors matching the factorids
         */
        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( ExperimentalFactor ef : ee.getExperimentalDesign().getExperimentalFactors() ) {
            if ( factorids.contains( ef.getId() ) ) {
                factors.add( ef );
            }
        }

        /*
         * Note that the setup gets checked again later, so if the choice is not valid it's not the end of the world.
         */
        AnalysisType type = null;
        if ( factors.size() == 2 ) {
            if ( includeInteractions ) {
                type = AnalysisType.TWIA;
            } else {
                type = AnalysisType.TWA;
            }
        } else if ( factors.size() == 1 ) {

            int numValues = factors.iterator().next().getFactorValues().size();
            if ( numValues == 0 ) {
                throw new IllegalArgumentException( "Factor must have at least one value" );
            } else if ( numValues == 1 ) {
                type = AnalysisType.OSTTEST;
            }
            if ( numValues == 2 ) {
                type = AnalysisType.TTEST;
            } else if ( numValues > 2 ) {
                type = AnalysisType.OWA;
            }
        } else {
            type = AnalysisType.GENERICLM;
        }

        log.info( "Determining analysis type" );
        AbstractDifferentialExpressionAnalyzer analyzer = this.differentialExpressionAnalyzer.determineAnalysis( ee,
                factors, type );

        if ( analyzer == null ) {
            throw new IllegalArgumentException( "Your settings were not valid. Please check them and try again." );
        }

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setAnalysisType( type );
        cmd.setFactors( factors );

        log.info( "Initializing analysis" );
        return super.run( cmd );
    }

    public void setDifferentialExpressionAnalyzer( DifferentialExpressionAnalyzer differentialExpressionAnalyzer ) {
        this.differentialExpressionAnalyzer = differentialExpressionAnalyzer;
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
        return null;
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
