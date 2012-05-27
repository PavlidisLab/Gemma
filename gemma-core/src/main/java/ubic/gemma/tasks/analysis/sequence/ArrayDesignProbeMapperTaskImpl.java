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
package ubic.gemma.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A probe mapper spaces task .
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class ArrayDesignProbeMapperTaskImpl implements ArrayDesignProbeMapperTask {

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService = null;

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.grid.javaspaces.task.expression.arrayDesign.ArrayDesignProbeMapperTask#execute(ubic.gemma.grid.javaspaces
     * .expression.arrayDesign.SpacesProbeMapperCommand)
     */
    @Override
    @TaskMethod
    public TaskResult execute( ArrayDesignProbeMapTaskCommand command ) {

        ArrayDesign ad = command.getArrayDesign();

        arrayDesignProbeMapperService.processArrayDesign( ad );

        /*
         * FIXME get rid of web dependency
         */
        TaskResult result = new TaskResult( command, new ModelAndView( new RedirectView( "/Gemma" ) ) );

        return result;
    }

}
