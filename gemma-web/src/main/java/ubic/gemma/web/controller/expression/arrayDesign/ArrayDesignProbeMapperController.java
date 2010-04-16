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
package ubic.gemma.web.controller.expression.arrayDesign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.job.AbstractTaskService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.tasks.analysis.sequence.ArrayDesignProbeMapTaskCommand;
import ubic.gemma.tasks.analysis.sequence.ArrayDesignProbeMapperTask;

/**
 * A controller to run array design probe mapper either locally or in a space.
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class ArrayDesignProbeMapperController extends AbstractTaskService {

    public ArrayDesignProbeMapperController() {
        super();
        this.setBusinessInterface( ArrayDesignProbeMapperTask.class );
    }

    @Autowired
    ArrayDesignProbeMapperTask arrayDesignProbeMapperTask;

    /**
     * Regular (local) job.
     */
    private class ArrayDesignProbeMapperJob extends BackgroundJob<ArrayDesignProbeMapTaskCommand> {

        /**
         * @param taskId
         * @param commandObj
         */
        public ArrayDesignProbeMapperJob( ArrayDesignProbeMapTaskCommand commandObj ) {
            super( commandObj );
        }

        @Override
        protected TaskResult processJob() {
            return arrayDesignProbeMapperTask.execute( this.command );
        }
    }

    /**
     * Job that loads in a javaspace.
     */
    private class ArrayDesignProbeMapperSpaceJob extends ArrayDesignProbeMapperJob {

        public ArrayDesignProbeMapperSpaceJob( ArrayDesignProbeMapTaskCommand commandObj ) {
            super( commandObj );
        }

        final ArrayDesignProbeMapperTask taskProxy = ( ArrayDesignProbeMapperTask ) getProxy();

        @Override
        protected TaskResult processJob() {
            return taskProxy.execute( this.command );
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
        ad = arrayDesignService.thaw( ad );

        ArrayDesignProbeMapTaskCommand cmd = new ArrayDesignProbeMapTaskCommand();
        cmd.setArrayDesign( ad );

        return super.run( cmd );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ArrayDesignProbeMapTaskCommand> getInProcessRunner( TaskCommand command ) {
        return new ArrayDesignProbeMapperJob( ( ArrayDesignProbeMapTaskCommand ) command );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.web.controller.grid.AbstractSpacesController#getSpaceRunner(java.lang.String, java.lang.Object)
     */
    @Override
    protected BackgroundJob<ArrayDesignProbeMapTaskCommand> getSpaceRunner( TaskCommand command ) {
        return new ArrayDesignProbeMapperSpaceJob( ( ArrayDesignProbeMapTaskCommand ) command );

    }

}
