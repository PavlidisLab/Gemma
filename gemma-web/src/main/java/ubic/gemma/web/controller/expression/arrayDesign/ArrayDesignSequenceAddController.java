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
package ubic.gemma.web.controller.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;

import ubic.basecode.util.FileTools;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.BackgroundJob;
import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceProcessingService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * Controller for associating sequences with an existing arrayDesign.
 * 
 * @author pavlidis
 * @version $Id$
 */
// FIXME: dead code?
@Controller
public class ArrayDesignSequenceAddController {

    @Autowired TaxonService taxonService;
    @Autowired ArrayDesignService arrayDesignService;
    @Autowired ArrayDesignSequenceProcessingService arrayDesignSequenceProcessingService;

    protected BackgroundJob<ArrayDesignSequenceAddCommand, TaskResult> getInProcessRunner( TaskCommand command ) {

        BackgroundJob<ArrayDesignSequenceAddCommand, TaskResult> r = new BackgroundJob<ArrayDesignSequenceAddCommand, TaskResult>(
                ( ArrayDesignSequenceAddCommand ) command ) {
            @Override
            public TaskResult processJob() {

                FileUpload fileUpload = command.getSequenceFile();
                ArrayDesign arrayDesign = command.getArrayDesign();
                SequenceType sequenceType = command.getSequenceType();

                //ProgressManager.setForwardingURL( taskId, "/Gemma/arrayDesign/associateSequences.html" );

                String filePath = fileUpload.getLocalPath();

                assert filePath != null;

                try {
                    InputStream stream = FileTools.getInputStreamFromPlainOrCompressedFile( filePath );

                    Collection<BioSequence> bioSequences = arrayDesignSequenceProcessingService.processArrayDesign(
                            arrayDesign, stream, sequenceType );

                    stream.close();

                    return new TaskResult( command, new ModelAndView( "view" ).addObject( "message",
                            "Successfully loaded " + bioSequences.size() + " sequences for " + arrayDesign ) );
                } catch ( Exception e ) {
                    throw new RuntimeException( e );
                }

            }
        };
        return r;
    }

}
