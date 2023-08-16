/*
 * The Gemma project
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
package ubic.gemma.web.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.tasks.AbstractTask;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller that does nothing except wait a while. Used for tests.
 *
 * @author pavlidis
 */
@Controller
public class MockLongJobControllerImpl implements MockLongJobController {

    @Autowired
    private TaskRunningService taskRunningService;

    @Override
    public String runJob( TaskCommand command ) {
        return taskRunningService.submitTask( new WasteOfTime( command ) );
    }

    static class WasteOfTime extends AbstractTask<TaskCommand> {
        protected Log log = LogFactory.getLog( this.getClass().getName() );

        public WasteOfTime( TaskCommand command ) {
            super( command );
        }

        @Override
        public TaskResult call() {

            long millis = System.currentTimeMillis();
            while ( System.currentTimeMillis() - millis < MockLongJobController.JOB_LENGTH ) {
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException e ) {
                }
                // we're using this as a test to pass in a 'die' signal, abuse of api
                if ( !this.taskCommand.getPersistJobDetails() ) {
                    throw new RuntimeException( "Exception thrown on purpose." );
                }
            }

            log.info( "Done doin sumpin'" );

            Map<String, Object> model = new HashMap<>();
            model.put( "answer", "42" );
            return new TaskResult( taskCommand, new ModelAndView( "view", model ) );
        }
    }

}
