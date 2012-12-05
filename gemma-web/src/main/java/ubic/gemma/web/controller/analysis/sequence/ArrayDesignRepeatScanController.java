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
package ubic.gemma.web.controller.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.tasks.analysis.sequence.ArrayDesignRepeatScanTask;
import ubic.gemma.tasks.analysis.sequence.ArrayDesignRepeatScanTaskCommand;

/**
 * A controller to run array design repeat scan either locally or in a space.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class ArrayDesignRepeatScanController extends AbstractTaskService {

    /**
     * Job that loads in a javaspace.
     */
    private class ArrayDesignRepeatScanSpaceJob extends BackgroundJob<ArrayDesignRepeatScanTaskCommand> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignRepeatScanSpaceJob( ArrayDesignRepeatScanTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            ArrayDesignRepeatScanTask taskProxy = ( ArrayDesignRepeatScanTask ) getProxy();
            TaskResult result = taskProxy.execute( command );
            return result;
        }

    }

    @Autowired
    private ArrayDesignService arrayDesignService = null;

    /**
     * AJAX entry point.
     * 
     * @param cmd
     * @return
     * @throws Exception
     */
    public String run( Long id ) throws Exception {
        ArrayDesign ad = arrayDesignService.load( id );

        ad = arrayDesignService.thawLite( ad );
        ArrayDesignRepeatScanTaskCommand cmd = new ArrayDesignRepeatScanTaskCommand( ad );

        return super.run( cmd );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ArrayDesignRepeatScanTaskCommand> getInProcessRunner( TaskCommand command ) {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ArrayDesignRepeatScanTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new ArrayDesignRepeatScanSpaceJob( ( ArrayDesignRepeatScanTaskCommand ) command );

    }

}
