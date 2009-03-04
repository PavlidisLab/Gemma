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
package ubic.gemma.grid.javaspaces.analysis.sequence;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.RepeatScan;
import ubic.gemma.grid.javaspaces.BaseSpacesTask;
import ubic.gemma.grid.javaspaces.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * An array design repeat scan spaces task that can be passed into a space and executed by a worker.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignRepeatScanTaskImpl extends BaseSpacesTask implements ArrayDesignRepeatScanTask {

    private Log log = LogFactory.getLog( ArrayDesignRepeatScanTaskImpl.class );

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.grid.javaspaces.analysis.sequence.ArrayDesignRepeatScanTask#execute(ubic.gemma.grid.javaspaces.analysis
     * .sequence.SpacesArrayDesignRepeatScanCommand)
     */
    public TaskResult execute( ArrayDesignRepeatScanTaskCommand command ) {

        super.initProgressAppender( this.getClass() );

        TaskResult result = new TaskResult();

        ArrayDesign ad = command.getArrayDesign();

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentService.getSequences( ad );
        RepeatScan scanner = new RepeatScan();
        Collection<BioSequence> altered = scanner.repeatScan( sequences );

        result.setAnswer( altered.size() );

        result.setTaskID( super.taskId );
        log.info( "Task execution complete ... returning result for task with id " + result.getTaskID() );
        return result;
    }

}
