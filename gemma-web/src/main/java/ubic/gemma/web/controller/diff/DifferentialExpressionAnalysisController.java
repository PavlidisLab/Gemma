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

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.context.SecurityContextHolder;

import ubic.gemma.analysis.expression.diff.AbstractDifferentialExpressionAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService;
import ubic.gemma.analysis.expression.diff.OneWayAnovaAnalyzer;
import ubic.gemma.analysis.expression.diff.TTestAnalyzer;
import ubic.gemma.analysis.expression.diff.TwoWayAnovaWithInteractionsAnalyzer;
import ubic.gemma.analysis.expression.diff.TwoWayAnovaWithoutInteractionsAnalyzer;
import ubic.gemma.analysis.expression.diff.DifferentialExpressionAnalyzerService.AnalysisType;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTask;
import ubic.gemma.grid.javaspaces.diff.DifferentialExpressionAnalysisTaskCommand;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.controller.BackgroundControllerJob;
import ubic.gemma.web.controller.BaseControllerJob;
import ubic.gemma.web.controller.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * A controller to run differential expression analysis either locally or in a space.
 * 
 * @spring.bean id="differentialExpressionAnalysisController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name="differentialExpressionAnalyzerService" ref="differentialExpressionAnalyzerService"
 * @spring.property name="differentialExpressionAnalyzer" ref="differentialExpressionAnalyzer"
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisController extends AbstractSpacesController<DifferentialExpressionAnalysis> {

    /**
     * Regular (local) job.
     */
    private class DiffAnalysisJob extends BaseControllerJob<DifferentialExpressionAnalysis> {

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        public DifferentialExpressionAnalysis call() throws Exception {
            SecurityContextHolder.setContext( securityContext );

            DifferentialExpressionAnalysisTaskCommand diffAnalysisCommand = ( ( DifferentialExpressionAnalysisTaskCommand ) command );

            ProgressManager.createProgressJob( this.getTaskId(), securityContext.getAuthentication().getName(),
                    "Loading " + diffAnalysisCommand.getExpressionExperiment().getShortName() );

            return processJob( diffAnalysisCommand );
        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected DifferentialExpressionAnalysis processJob( TaskCommand c ) {
            DifferentialExpressionAnalysisTaskCommand dc = ( DifferentialExpressionAnalysisTaskCommand ) c;

            AnalysisType analysisType = dc.getAnalysisType();
            ExpressionExperiment ee = dc.getExpressionExperiment();
            expressionExperimentService.thawLite( ee );
            DifferentialExpressionAnalysis results;
            if ( analysisType != null ) {
                assert dc.getFactors() != null;
                results = differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee, dc.getFactors(),
                        analysisType );
            } else {
                results = differentialExpressionAnalyzerService.runDifferentialExpressionAnalyses( ee );
            }
            return results;
        }
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class DiffAnalysisSpaceJob extends DiffAnalysisJob {

        final DifferentialExpressionAnalysisTask taskProxy = ( DifferentialExpressionAnalysisTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public DiffAnalysisSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );

        }

        /*
         * (non-Javadoc)
         * @see
         * ubic.gemma.web.controller.diff.DifferentialExpressionAnalysisController.DiffAnalysisJob#processJob(ubic.gemma
         * .grid.javaspaces.diff.DifferentialExpressionAnalysisCommand)
         */
        @Override
        protected DifferentialExpressionAnalysis processJob( TaskCommand baseCommand ) {
            baseCommand.setTaskId( this.taskId );
            return ( DifferentialExpressionAnalysis ) process(
                    ( DifferentialExpressionAnalysisTaskCommand ) baseCommand ).getAnswer();
        }

        /**
         * @param diffCommand
         * @return
         */
        private TaskResult process( DifferentialExpressionAnalysisTaskCommand diffCommand ) {
            expressionExperimentService.thawLite( diffCommand.getExpressionExperiment() );
            TaskResult result = taskProxy.execute( diffCommand );
            return result;
        }

    }

    private DifferentialExpressionAnalyzer differentialExpressionAnalyzer;

    private DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService;

    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * FIXME: used?
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
            throw new UnsupportedOperationException( "Don't know how to handle analyzer of class: "
                    + analyzer.getClass().getSimpleName() );
        }
        return result;
    }

    /**
     * @param id
     * @return
     */
    public DifferentialExpressionAnalyzerInfo determineAnalysisType( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Cannot access experiment with id=" + id );
        }
        AbstractDifferentialExpressionAnalyzer analyzer = this.differentialExpressionAnalyzer.determineAnalysis( ee );

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
            throw new UnsupportedOperationException( "Don't know how to handle analyzer of class: "
                    + analyzer.getClass().getSimpleName() );
        }
        return result;
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

        AnalysisType type = null;
        if ( factors.size() == 2 ) {
            if ( includeInteractions ) {
                type = AnalysisType.TWIA;
            } else {
                type = AnalysisType.TWA;
            }
        } else if ( factors.size() == 1 ) {

            int numValues = factors.iterator().next().getFactorValues().size();
            if ( numValues < 2 ) {
                throw new IllegalArgumentException( "There must be at least two factor values" );
            }
            if ( numValues == 2 ) {
                type = AnalysisType.TTEST;
            } else if ( numValues > 2 ) {
                type = AnalysisType.OWA;
            }
        } else {
            throw new IllegalArgumentException( "You must choose at most 2 factors" );
        }

        AbstractDifferentialExpressionAnalyzer analyzer = this.differentialExpressionAnalyzer.determineAnalysis( ee,
                factors, type );

        if ( analyzer == null ) {
            throw new IllegalArgumentException( "Your settings were not valid. Please check them and try again." );
        }

        DifferentialExpressionAnalysisTaskCommand cmd = new DifferentialExpressionAnalysisTaskCommand( ee );
        cmd.setAnalysisType( type );
        cmd.setFactors( factors );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), DifferentialExpressionAnalysisTask.class
                .getName(), true );
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

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), DifferentialExpressionAnalysisTask.class
                .getName(), true );
    }

    public void setDifferentialExpressionAnalyzer( DifferentialExpressionAnalyzer differentialExpressionAnalyzer ) {
        this.differentialExpressionAnalyzer = differentialExpressionAnalyzer;
    }

    public void setDifferentialExpressionAnalyzerService(
            DifferentialExpressionAnalyzerService differentialExpressionAnalyzerService ) {
        this.differentialExpressionAnalyzerService = differentialExpressionAnalyzerService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<DifferentialExpressionAnalysis> getRunner( String jobId, Object command ) {
        return new DiffAnalysisJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<DifferentialExpressionAnalysis> getSpaceRunner( String jobId, Object command ) {
        return new DiffAnalysisSpaceJob( jobId, command );

    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return "differentialExpressionAnalysis";
    }
}
