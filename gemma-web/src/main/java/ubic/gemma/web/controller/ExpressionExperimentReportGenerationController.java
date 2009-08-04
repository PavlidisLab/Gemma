/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.controller;

import javax.servlet.http.HttpServletRequest;

import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.grid.javaspaces.TaskCommand;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTask;
import ubic.gemma.grid.javaspaces.expression.experiment.ExpressionExperimentReportTaskCommand;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.grid.javaspaces.SpacesEnum;
import ubic.gemma.web.controller.grid.AbstractSpacesController;

/**
 * @author klc
 * @version $Id$ *
 * @spring.bean name="expressionExperimentReportGenerationService"
 * @spring.property name="expressionExperimentReportService" ref="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * 
 * 
 */
public class ExpressionExperimentReportGenerationController extends AbstractSpacesController<Boolean> {

    private ExpressionExperimentReportService expressionExperimentReportService = null;
    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * @param expressionExperimentReportService
     */
    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }
    
    public void setExpressionExperimentService(ExpressionExperimentService ees){
        this.expressionExperimentService = ees;
    }
    
    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {

        ExpressionExperiment ee = expressionExperimentService.load( id );

        ExpressionExperimentReportTaskCommand cmd = new ExpressionExperimentReportTaskCommand( ee );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentReportTask.class.getName(), true );
    }
    
    public String runAll() throws Exception {
        
        ExpressionExperimentReportTaskCommand cmd = new ExpressionExperimentReportTaskCommand( true );

        return super.run( cmd, SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentReportTask.class.getName(), true );
        
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<Boolean> getRunner( String jobId, Object command ) {
        return new ExpressionExperimentReportJob( jobId, command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundControllerJob<Boolean> getSpaceRunner( String jobId, Object command ) {
        return new ExpressionExperimentReportSpaceJob( jobId, command );
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ExpressionExperimentReportSpaceJob extends ExpressionExperimentReportJob {
      

        final ExpressionExperimentReportTask taskProxy = ( ExpressionExperimentReportTask ) updatedContext
                .getBean( "proxy" );

        /**
         * @param taskId
         * @param commandObj
         */
        public ExpressionExperimentReportSpaceJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        /*
         * (non-Javadoc)
         * @seeubic.gemma.web.controller.analysis.preprocess.ProcessedExpressionDataVectorCreateController.
         * ProcessedExpressionDataVectorCreateJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected Boolean processJob( TaskCommand baseCommand ) {
            baseCommand.setTaskId( this.taskId );
            ExpressionExperimentReportTaskCommand vectorCommand = ( ExpressionExperimentReportTaskCommand ) baseCommand;
            process( vectorCommand );
            return true;
        }

        /**
         * @param command
         * @return
         */
        private TaskResult process( ExpressionExperimentReportTaskCommand command ) {
            ExpressionExperimentReportTaskCommand jsCommand = createCommandObject( command );
            TaskResult result = taskProxy.execute( jsCommand );
            return result;
        }

        /**
         * @param command
         * @return
         */
        protected ExpressionExperimentReportTaskCommand createCommandObject(
                ExpressionExperimentReportTaskCommand command ) {
            return new ExpressionExperimentReportTaskCommand( taskId, command.getExpressionExperiment() );
        }

    }


    /**
     * @author klc
     */
    private class ExpressionExperimentReportJob extends BaseControllerJob<Boolean> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ExpressionExperimentReportJob( String taskId, Object commandObj ) {
            super( taskId, commandObj );
        }

        /*
         * (non-Javadoc)
         * @see java.util.concurrent.Callable#call()
         */
        public Boolean call() throws Exception {

            ExpressionExperimentReportTaskCommand vectorCommand = ( ExpressionExperimentReportTaskCommand ) command; 

            super.initializeProgressJob( vectorCommand.getExpressionExperiment().getShortName() );

            return processJob( vectorCommand );
        }

        /*
         * (non-Javadoc)
         * @see ubic.gemma.web.controller.BaseControllerJob#processJob(ubic.gemma.web.controller.BaseCommand)
         */
        @Override
        protected Boolean processJob( TaskCommand c ) {
            ExpressionExperimentReportTaskCommand vectorCommand = ( ExpressionExperimentReportTaskCommand ) c; 


            ExpressionExperiment ee = vectorCommand.getExpressionExperiment();
            expressionExperimentReportService.generateSummaryObject( ee.getId() );
            // expressionExperimentService.thawLite( ee );

         return true;
        }
    }

    /*
     * (non-Javadoc)
     * @seeorg.springframework.web.servlet.mvc.AbstractUrlViewController#getViewNameForRequest(javax.servlet.http.
     * HttpServletRequest)
     */
    @Override
    protected String getViewNameForRequest( HttpServletRequest arg0 ) {
        return null;
    }

    //-------------------------------------
    
//    public void generateSummaryObjects() {
//        startJob( SpacesEnum.DEFAULT_SPACE.getSpaceUrl(), ExpressionExperimentReportTask.class.getName(), true );
//    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesService#runLocally(java.lang.String)
     */
//    @Override
//    public void runLocally( String taskId, Object command ) {
//        expressionExperimentReportService.generateSummaryObjects();
//    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.grid.javaspaces.AbstractSpacesService#runRemotely(java.lang.String)
     */
//    @Override
//    public void runRemotely( String taskId, Object command ) {
        // ExpressionExperimentReportTask reportProxy = (
        // ExpressionExperimentReportTask ) updatedContext
        // .getBean( "expressionExperimentReportTask" );
        // reportProxy.execute();

//        ExpressionExperimentReportTask reportProxy = ( ExpressionExperimentReportTask ) updatedContext
//                .getBean( "proxy" );
//        TaskCommand spacesCommand = new TaskCommand();
//        spacesCommand.setTaskId( taskId );
//        reportProxy.execute( spacesCommand );
//    }

  

}
