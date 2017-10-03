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

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * @author pavlidis
 */
public class ArrayDesignAddCommand {
    ArrayDesign arrayDesign;
    FileUpload file;
    Taxon taxon;

    public ArrayDesignAddCommand() {
        this.file = new FileUpload();
        this.arrayDesign = ArrayDesign.Factory.newInstance();
        this.taxon = Taxon.Factory.newInstance();
    }

    /**
     * @return the arrayDesign
     */
    public ArrayDesign getArrayDesign() {
        return this.arrayDesign;
    }

    /**
     * @param arrayDesign the arrayDesign to set
     */
    public void setArrayDesign( ArrayDesign arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    /**
     * @return the sequenceFile
     */
    public FileUpload getFile() {
        return this.file;
    }

    /**
     * @param file the sequenceFile to set
     */
    public void setFile( FileUpload file ) {
        this.file = file;
    }

    /**
     * @return the taxon
     */
    public Taxon getTaxon() {
        return this.taxon;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

}
