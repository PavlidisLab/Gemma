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

import ubic.gemma.job.TaskCommand;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * Form information for associating sequences with an (existing) array design.
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated not used
 */
@Deprecated
public class ArrayDesignSequenceAddCommand extends TaskCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    ArrayDesign arrayDesign;

    FileUpload sequenceFile;

    SequenceType sequenceType;

    /**
     * 
     */
    public ArrayDesignSequenceAddCommand() {
        this.sequenceFile = new FileUpload();
        this.arrayDesign = ArrayDesign.Factory.newInstance();
    }

    /**
     * @return the arrayDesign
     */
    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    /**
     * @return the sequenceFile
     */
    public FileUpload getSequenceFile() {
        return this.sequenceFile;
    }

    /**
     * @return the sequenceType
     */
    public SequenceType getSequenceType() {
        return this.sequenceType;
    }

    /**
     * @param arrayDesign the arrayDesign to set
     */
    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    /**
     * @param sequenceFile the sequenceFile to set
     */
    public void setSequenceFile( FileUpload sequenceFile ) {
        this.sequenceFile = sequenceFile;
    }

    /**
     * @param sequenceType the sequenceType to set
     */
    public void setSequenceType( SequenceType sequenceType ) {
        this.sequenceType = sequenceType;
    }

}
