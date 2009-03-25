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
package ubic.gemma.grid.javaspaces.expression.arrayDesign;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * A probe mapper spaces task that can be passed into a space and executed by a worker.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignProbeMapperTaskImpl extends BaseSpacesTask implements ArrayDesignProbeMapperTask {

    private Log log = LogFactory.getLog( ArrayDesignProbeMapperTaskImpl.class );

    private ArrayDesignProbeMapperService arrayDesignProbeMapperService = null;

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.grid.javaspaces.expression.arrayDesign.ArrayDesignProbeMapperTask#execute(ubic.gemma.grid.javaspaces
     * .expression.arrayDesign.SpacesProbeMapperCommand)
     */
    public TaskResult execute( ArrayDesignProbeMapTaskCommand command ) {

        SpacesProgressAppender spacesProgressAppender = super.initProgressAppender( this.getClass() );

        ArrayDesign ad = command.getArrayDesign();

        arrayDesignProbeMapperService.processArrayDesign( ad );

        TaskResult result = new TaskResult();

        result.setAnswer( ad.getName() );

        result.setTaskID( super.taskId );
        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );

        super.tidyProgress( spacesProgressAppender );

        return result;
    }

    public void setArrayDesignProbeMapperService( ArrayDesignProbeMapperService arrayDesignProbeMapperService ) {
        this.arrayDesignProbeMapperService = arrayDesignProbeMapperService;
    }

}
