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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import ubic.gemma.analysis.sequence.RepeatScan;
import ubic.gemma.job.TaskMethod;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * An array design repeat scan spaces task
 * 
 * @author keshav
 * @version $Id$
 */
@Service
public class ArrayDesignRepeatScanTaskImpl implements ArrayDesignRepeatScanTask {

    @Autowired
    private ArrayDesignService arrayDesignService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.grid.javaspaces.task.analysis.sequence.ArrayDesignRepeatScanTask#execute(ubic.gemma.grid.javaspaces
     * .analysis .sequence.SpacesArrayDesignRepeatScanCommand)
     */
    @Override
    @TaskMethod
    public TaskResult execute( ArrayDesignRepeatScanTaskCommand command ) {

        ArrayDesign ad = command.getArrayDesign();

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad );
        RepeatScan scanner = new RepeatScan();
        scanner.repeatScan( sequences );

        TaskResult result = new TaskResult( command, new ModelAndView( new RedirectView( "/Gemma" ) ) );

        return result;
    }

}
