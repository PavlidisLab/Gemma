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
package ubic.gemma.web.controller.analysis.expression.coexpression.links;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.analysis.coexp.LinkAnalysisTask;
import ubic.gemma.tasks.analysis.coexp.LinkAnalysisTaskCommand;
import ubic.gemma.util.ConfigUtils;

/**
 * A controller to preprocess expression data vectors.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class LinkAnalysisController extends AbstractTaskService {

    /**
     * @author keshav
     */
    private class LinkAnalysisJob extends BackgroundJob<LinkAnalysisTaskCommand> {

        public LinkAnalysisJob( LinkAnalysisTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        public TaskResult processJob() {
            return linkAnalysisTask.execute( command );
        }

    }

    /**
     * Job that loads in a javaspace.
     * 
     * @author keshav
     * @version $Id$
     */
    private class LinkAnalysisSpaceJob extends LinkAnalysisJob {

        final LinkAnalysisTask taskProxy = ( LinkAnalysisTask ) getProxy();

        public LinkAnalysisSpaceJob( LinkAnalysisTaskCommand commandObj ) {
            super( commandObj );

        }

        @Override
        public TaskResult processJob() {
            return taskProxy.execute( command );
        }

    }

    @Autowired
    LinkAnalysisTask linkAnalysisTask;

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    public LinkAnalysisController() {
        super();
        this.setBusinessInterface( LinkAnalysisTask.class );
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

        ee = expressionExperimentService.thawLite( ee );
        LinkAnalysisConfig lac = new LinkAnalysisConfig();
        FilterConfig fc = new FilterConfig();
        LinkAnalysisTaskCommand cmd = new LinkAnalysisTaskCommand( ee, lac, fc );

        return super.run( cmd );
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
    protected BackgroundJob<LinkAnalysisTaskCommand> getInProcessRunner( TaskCommand command ) {
        if ( ConfigUtils.getBoolean( "gemma.grid.gridonly.coexp" ) ) {
            return null;
        }
        return new LinkAnalysisJob( ( LinkAnalysisTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<LinkAnalysisTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new LinkAnalysisSpaceJob( ( LinkAnalysisTaskCommand ) command );
    }

}
