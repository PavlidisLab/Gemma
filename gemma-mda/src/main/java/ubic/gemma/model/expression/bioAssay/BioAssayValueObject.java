/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.expression.bioAssay;

import java.io.Serializable;

import ubic.gemma.model.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

/**
 * @author Paul
 * @version $Id$
 */
public class BioAssayValueObject implements Serializable {

    private BioMaterialValueObject sample;

    private DatabaseEntryValueObject accession = null;

    public DatabaseEntryValueObject getAccession() {
        return accession;
    }

    public void setAccession( DatabaseEntryValueObject accession ) {
        this.accession = accession;
    }

    public BioMaterialValueObject getSample() {
        return sample;
    }

    public void setSample( BioMaterialValueObject sample ) {
        this.sample = sample;
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public boolean isOutlier() {
        return outlier;
    }

    public void setOutlier( boolean outlier ) {
        this.outlier = outlier;
    }

    private Long id = null;
    private String name = "";
    private String description = "";
    private boolean outlier = false;
    private ArrayDesignValueObject arrayDesign;

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public BioAssayValueObject() {

    }

    public BioAssayValueObject( BioAssay bioAssay ) {
        this.id = bioAssay.getId();
        this.name = bioAssay.getName();
        this.description = bioAssay.getDescription();

        ArrayDesign ad = bioAssay.getArrayDesignUsed();
        this.arrayDesign = new ArrayDesignValueObject();
        arrayDesign.setId( ad.getId() );
        arrayDesign.setShortName( ad.getShortName() );
        arrayDesign.setName( ad.getName() );

        if ( bioAssay.getAccession() != null ) {
            this.accession = new DatabaseEntryValueObject( bioAssay.getAccession() );
        }

        if ( !bioAssay.getSamplesUsed().isEmpty() ) {
            this.sample = new BioMaterialValueObject( bioAssay.getSamplesUsed().iterator().next(), bioAssay );
        }

    }

}
