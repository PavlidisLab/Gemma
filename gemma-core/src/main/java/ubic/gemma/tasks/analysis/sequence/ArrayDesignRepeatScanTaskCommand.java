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
package ubic.gemma.tasks.analysis.sequence;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.tasks.Task;

/**
 * A command object to be used by spaces.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignRepeatScanTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private ArrayDesign arrayDesign = null;

    /**
     * @return
     */
    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    /**
     * @param arrayDesign
     */
    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    /**
     * NOTE: we can't pass in a we command as they are defined in the web module, which messes up the configuration.
     * 
     * @param taskId
     * @param arrayDesign
     */
    public ArrayDesignRepeatScanTaskCommand( String taskId, ArrayDesign arrayDesign ) {
        super();
        this.setTaskId( taskId );
        this.arrayDesign = arrayDesign;
    }

    public ArrayDesignRepeatScanTaskCommand( ArrayDesign ad ) {
        super();
        this.arrayDesign = ad;
    }

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return ArrayDesignRepeatScanTask.class;
    }
}
