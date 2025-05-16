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
package ubic.gemma.core.tasks.analysis.sequence;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A command object to be used by spaces.
 *
 * @author keshav
 */
@SuppressWarnings("unused") // Possible external use
public class ArrayDesignProbeMapTaskCommand extends TaskCommand {

    private static final long serialVersionUID = 1L;

    private boolean forceAnalysis = false;

    private ArrayDesign arrayDesign = null;

    public ArrayDesignProbeMapTaskCommand() {
        super();
    }

    /*
     * NOTE: we can't pass in a we command as they are defined in the web module, which messes up the configuration.
     *
     */
    public ArrayDesignProbeMapTaskCommand( String taskId, boolean forceAnalysis, ArrayDesign arrayDesign ) {
        super();
        this.forceAnalysis = forceAnalysis;
        this.arrayDesign = arrayDesign;
    }

    public ArrayDesign getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    @Override
    public Class<ArrayDesignProbeMapperTask> getTaskClass() {
        return ArrayDesignProbeMapperTask.class;
    }

    public boolean isForceAnalysis() {
        return forceAnalysis;
    }

    public void setForceAnalysis( boolean forceAnalysis ) {
        this.forceAnalysis = forceAnalysis;
    }

}
