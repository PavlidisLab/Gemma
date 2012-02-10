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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.maintenance.ExpressionExperimentReportTask;
import ubic.gemma.tasks.maintenance.ExpressionExperimentReportTaskCommand;

/**
 * @author klc
 * @version $Id$ *
 */
@Controller
public class ExpressionExperimentReportGenerationController extends AbstractTaskService {

    public ExpressionExperimentReportGenerationController() {
        super();
        this.setBusinessInterface( ExpressionExperimentReportTask.class );
    }

    /**
     * @author klc
     */
    private class ExpressionExperimentReportJob extends BackgroundJob<ExpressionExperimentReportTaskCommand> {

        public ExpressionExperimentReportJob( ExpressionExperimentReportTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            return expressionExperimentReportTask.execute( this.command );
        }
    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class ExpressionExperimentReportSpaceJob extends BackgroundJob<ExpressionExperimentReportTaskCommand> {

        public ExpressionExperimentReportSpaceJob( ExpressionExperimentReportTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            ExpressionExperimentReportTask taskProxy = ( ExpressionExperimentReportTask ) getProxy();
            return taskProxy.execute( command );
        }

    }

    @Autowired
    private ExpressionExperimentReportTask expressionExperimentReportTask;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "Could not access experiment with id=" + id );
        }

        ExpressionExperimentReportTaskCommand cmd = new ExpressionExperimentReportTaskCommand( ee );

        return super.run( cmd );
    }

    /**
     * AJAX entry point
     * 
     * @return
     * @throws Exception
     */
    public String runAll() throws Exception {

        ExpressionExperimentReportTaskCommand cmd = new ExpressionExperimentReportTaskCommand( true );

        return super.run( cmd );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ExpressionExperimentReportTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new ExpressionExperimentReportJob( ( ExpressionExperimentReportTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ExpressionExperimentReportTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new ExpressionExperimentReportSpaceJob( ( ExpressionExperimentReportTaskCommand ) command );
    }

}
