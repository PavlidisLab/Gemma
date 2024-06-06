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
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.web.tasks.analysis.sequence.ArrayDesignRepeatScanTaskCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

/**
 * A controller to run array design repeat scan either locally or in a space.
 *
 * @author keshav
 */
@Controller
public class ArrayDesignRepeatScanController {

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private ArrayDesignService arrayDesignService;

    /**
     * AJAX entry point.
     */
    public String run( Long id ) {
        ArrayDesign ad = arrayDesignService.loadOrFail( id );

        ad = arrayDesignService.thawLite( ad );
        ArrayDesignRepeatScanTaskCommand cmd = new ArrayDesignRepeatScanTaskCommand( ad );

        return taskRunningService.submitTaskCommand( cmd );
    }

}
