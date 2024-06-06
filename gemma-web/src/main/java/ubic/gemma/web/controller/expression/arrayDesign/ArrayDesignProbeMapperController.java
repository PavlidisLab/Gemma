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
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.sequence.ArrayDesignProbeMapTaskCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * A controller to run array design probe mapper either locally or in a space.
 *
 * @author keshav
 */
@Controller
public class ArrayDesignProbeMapperController {

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    /**
     * AJAX entry point.
     */
    public String run( Long id ) {

        ArrayDesign arrayDesign = arrayDesignService.loadOrFail( id );
        arrayDesign = arrayDesignService.thaw( arrayDesign );

        ArrayDesignProbeMapTaskCommand cmd = new ArrayDesignProbeMapTaskCommand();
        cmd.setArrayDesign( arrayDesign );

        return taskRunningService.submitTaskCommand( cmd );
    }
}
