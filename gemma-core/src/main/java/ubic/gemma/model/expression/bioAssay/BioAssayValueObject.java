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

import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author Paul
 */
public class BioAssayValueObject extends IdentifiableValueObject<BioAssay> implements Serializable {

    private static final long serialVersionUID = 9164284536309673585L;
    private DatabaseEntryValueObject accession = null;
    private ArrayDesignValueObject arrayDesign;
    private String description = "";
    private String name = "";
    // if it was removed as an outlier
    private Boolean outlier = false;
    // if our algorithm says it might be an outlier.
    private Boolean predictedOutlier = false;
    private Date processingDate;
    private BioMaterialValueObject sample;
    private Boolean sequencePairedReads;
    private Integer sequenceReadCount;
    private Integer sequenceReadLength;
    // to hold state change, initialized as this.outlier
    private Boolean userFlaggedOutlier = false;

    public BioAssayValueObject( Long id ) {
        super( id );
    }

    /**
     * FIXME This method is a bit dangerous because we can get lazy-load errors.
     */
    public BioAssayValueObject( BioAssay bioAssay ) {
        super( bioAssay.getId() );
        this.name = bioAssay.getName();
        this.description = bioAssay.getDescription();

        ArrayDesign ad = bioAssay.getArrayDesignUsed();
        assert ad != null;
        this.arrayDesign = new ArrayDesignValueObject(ad.getId());
        arrayDesign.setShortName( ad.getShortName() );
        arrayDesign.setName( ad.getName() );

        this.processingDate = bioAssay.getProcessingDate();
        this.sequencePairedReads = bioAssay.getSequencePairedReads();
        this.sequenceReadLength = bioAssay.getSequenceReadLength();
        this.sequenceReadCount = bioAssay.getSequenceReadCount();

        if ( bioAssay.getAccession() != null ) {
            this.accession = new DatabaseEntryValueObject( bioAssay.getAccession() );
        }

        if ( bioAssay.getSampleUsed() != null ) {
            this.sample = new BioMaterialValueObject( bioAssay.getSampleUsed() );
            sample.getBioAssays().add( this );
        }

        if ( bioAssay.getIsOutlier() != null ) {
            this.outlier = bioAssay.getIsOutlier();
        }

        this.userFlaggedOutlier = this.outlier;
    }

    public static Collection<BioAssayValueObject> convert2ValueObjects( Collection<BioAssay> bioAssays ) {
        Collection<BioAssayValueObject> result = new HashSet<>();
        for ( BioAssay bioAssay : bioAssays ) {
            result.add( new BioAssayValueObject( bioAssay ) );
        }
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        BioAssayValueObject other = ( BioAssayValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;

        if ( name == null ) {
            if ( other.name != null )
                return false;
        } else if ( !name.equals( other.name ) )
            return false;
        return true;
    }

    public DatabaseEntryValueObject getAccession() {
        return accession;
    }

    public void setAccession( DatabaseEntryValueObject accession ) {
        this.accession = accession;
    }

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Boolean getPredictedOutlier() {
        return predictedOutlier;
    }

    public void setPredictedOutlier( Boolean predictedOutlier ) {
        this.predictedOutlier = predictedOutlier;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate( Date processingDate ) {
        this.processingDate = processingDate;
    }

    public BioMaterialValueObject getSample() {
        return sample;
    }

    public void setSample( BioMaterialValueObject sample ) {
        this.sample = sample;
    }

    public Boolean getSequencePairedReads() {
        return sequencePairedReads;
    }

    public void setSequencePairedReads( Boolean sequencePairedReads ) {
        this.sequencePairedReads = sequencePairedReads;
    }

    public Integer getSequenceReadCount() {
        return sequenceReadCount;
    }

    public void setSequenceReadCount( Integer sequenceReadCount ) {
        this.sequenceReadCount = sequenceReadCount;
    }

    public Integer getSequenceReadLength() {
        return sequenceReadLength;
    }

    public void setSequenceReadLength( Integer sequenceReadLength ) {
        this.sequenceReadLength = sequenceReadLength;
    }

    public Boolean getUserFlaggedOutlier() {
        return userFlaggedOutlier;
    }

    public void setUserFlaggedOutlier( Boolean userFlaggedOutlier ) {
        this.userFlaggedOutlier = userFlaggedOutlier;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        if ( id == null ) {
            result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        }
        return result;
    }

    public boolean isOutlier() {
        return outlier;
    }

    public void setOutlier( boolean outlier ) {
        this.outlier = outlier;
    }

    @Override
    public String toString() {
        return "BioAssayVO [" + ( id != null ? "id=" + id + ", " : "" ) + ( name != null ? "name=" + name + ", " : "" )
                + ( description != null ? "description=" + description : "" ) + "]";
    }

}
