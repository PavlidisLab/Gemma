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
package ubic.gemma.web.controller.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;

/**
 * Handles loading of Expression data into the system when the source is GEO or ArrayExpress, via Spring MVC or AJAX.
 *
 * @author pavlidis
 * @author keshav
 * @see ubic.gemma.web.controller.expression.experiment.ExpressionDataFileUploadController for how flat-file data is
 * loaded.
 */
@Controller
public class ExpressionExperimentLoadController {

    @Autowired
    private TaskRunningService taskRunningService;

    public ExpressionExperimentLoadController() {
        super();
    }

    public String load( ExpressionExperimentLoadTaskCommand command ) {
        // remove stray whitespace.
        command.setAccession( StringUtils.strip( command.getAccession() ) );

        if ( StringUtils.isBlank( command.getAccession() ) ) {
            throw new IllegalArgumentException( "Must provide an accession" );
        }

        return taskRunningService.submitTaskCommand( command );
    }

    @RequestMapping("/admin/loadExpressionExperiment.html")
    public ModelAndView show() {
        return new ModelAndView( "/admin/loadExpressionExperimentForm" );
    }
}
