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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.job.*;

/**
 * Controller that does nothing except wait a while. Used for tests.
 * 
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @author pavlidis
 * @version $Id$
 */
@Controller
public class MockLongJobControllerImpl implements MockLongJobController {

    @Autowired private TaskRunningService taskRunningService;
    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MockLongJobController#runJob(ubic.gemma.job.TaskCommand)
     */
    @Override
    public String runJob( TaskCommand command ) {
        return taskRunningService.submitLocalJob( new WasteOfTime( command ) );
    }

    static class WasteOfTime extends BackgroundJob<TaskCommand, TaskResult> {

        public WasteOfTime( TaskCommand command ) {
            super( command );
            this.command = command;
        }

        @Override
        public TaskResult processJob() {

            long millis = System.currentTimeMillis();
            while ( System.currentTimeMillis() - millis < JOB_LENGTH ) {
                try {
                    Thread.sleep( 500 );
                } catch ( InterruptedException e ) {
                }
                // we're using this as a test to pass in a 'die' signal, abuse of api
                if ( !this.command.getPersistJobDetails() ) {
                    throw new RuntimeException( "Exception thrown on purpose." );
                }
            }

            log.info( "Done doin sumpin'" );

            Map<String, Object> model = new HashMap<String, Object>();
            model.put( "answer", "42" );
            return new TaskResult( command, new ModelAndView( "view", model ) );
        }

    }

}
