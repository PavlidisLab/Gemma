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

import java.util.HashSet;

import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.web.controller.common.auditAndSecurity.FileUpload;

/**
 * Extends a SimpleExpressionExperimentMetaData and has a FileUpload for the data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionExperimentLoadCommand extends SimpleExpressionExperimentMetaData {

    FileUpload dataFile;
    private String arrayDesignName;
    private String taxonName;
    boolean validateOnly;

    /**
     * Whether the command object should be validated or actually processed.
     * 
     * @return
     */
    protected boolean isValidateOnly() {
        return validateOnly;
    }

    protected void setValidateOnly( boolean validateOnly ) {
        this.validateOnly = validateOnly;
    }

    public SimpleExpressionExperimentLoadCommand() {
        this.dataFile = new FileUpload();
        this.setArrayDesigns( new HashSet<ArrayDesign>() );
        this.setTaxon( Taxon.Factory.newInstance() );
    }

    /**
     * @return the sequenceFile
     */
    public FileUpload getDataFile() {
        return this.dataFile;
    }

    /**
     * @param sequenceFile the sequenceFile to set
     */
    public void setDataFile( FileUpload dataFile ) {
        this.dataFile = dataFile;
    }

    public String getArrayDesignName() {
        return this.arrayDesignName;
    }

    public String getTaxonName() {
        return this.taxonName;
    }

    public void setArrayDesignName( String arrayDesign ) {
        this.arrayDesignName = arrayDesign;
    }

    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
    }

}
