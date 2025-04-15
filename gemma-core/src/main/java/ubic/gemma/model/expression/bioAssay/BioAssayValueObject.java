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

import com.fasterxml.jackson.annotation.JsonInclude;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * @author Paul
 */
@SuppressWarnings("unused") // ValueObject accessed from JS
public class BioAssayValueObject extends IdentifiableValueObject<BioAssay> {

    private static final long serialVersionUID = 9164284536309673585L;

    public static Collection<BioAssayValueObject> convert2ValueObjects( Collection<BioAssay> bioAssays ) {
        Collection<BioAssayValueObject> result = new HashSet<>();
        for ( BioAssay bioAssay : bioAssays ) {
            result.add( new BioAssayValueObject( bioAssay, false ) );
        }
        return result;
    }

    @Nullable
    private String shortName;
    private DatabaseEntryValueObject accession = null;
    private ArrayDesignValueObject arrayDesign;
    private String description = "";
    private String metadata;
    private String name = "";
    private ArrayDesignValueObject originalPlatform;
    // if it was removed as an outlier
    private Boolean outlier = false;
    // if our algorithm says it might be an outlier.
    private Boolean predictedOutlier = false;
    private Date processingDate;
    private BioMaterialValueObject sample;

    // only for RNA-Seq data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sequencePairedReads;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long sequenceReadCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer sequenceReadLength;

    // to hold state change, initialized as this.outlier
    private Boolean userFlaggedOutlier = false;

    private Long sourceBioAssayId;

    /**
     * Required when using the class as a spring bean.
     */
    public BioAssayValueObject() {
        super();
    }

    public BioAssayValueObject( BioAssay bioAssay ) {
        this( bioAssay, null, null, false, false );
    }

    public BioAssayValueObject( BioAssay bioAssay, boolean basic ) {
        this( bioAssay, null, null, basic, false );
    }

    public BioAssayValueObject( BioAssay bioAssay, boolean basic, boolean predictedOutlier ) {
        this( bioAssay, null, null, basic, false );
        this.predictedOutlier = predictedOutlier;
    }

    /**
     * @param arrayDesignValueObjectsById pre-populated array design VOs by ID, or null to ignore and the VOs will be
     *                                    initialized via {@link ArrayDesignValueObject#ArrayDesignValueObject(ArrayDesign)}
     * @param sourceBioAssay              the source {@link BioAssay} if known, this corresponds to the assay of the
     *                                    source sample, but since there might be more than one, it must be picked
     *                                    explicitly based on the context
     * @param basic                       if true, produce basic factor values in the corresponding biomaterial, see
     *                                    {@link BioMaterialValueObject#BioMaterialValueObject(BioMaterial, boolean, boolean)}
     *                                    for more details
     * @param allFactorValues             include all FVs, including those inherited from the source biomaterial in the
     *                                    corresponding biomaterial
     */
    public BioAssayValueObject( BioAssay bioAssay, @Nullable Map<Long, ArrayDesignValueObject> arrayDesignValueObjectsById, @Nullable BioAssay sourceBioAssay, boolean basic, boolean allFactorValues ) {
        super( bioAssay );
        this.shortName = bioAssay.getShortName();
        this.name = bioAssay.getName();
        this.description = bioAssay.getDescription();

        // the platform and original platform are eagerly fetched, so no need for a Hibernate.isInitialized() test:w
        ArrayDesign ad = bioAssay.getArrayDesignUsed();
        assert ad != null;
        if ( arrayDesignValueObjectsById != null && arrayDesignValueObjectsById.containsKey( ad.getId() ) ) {
            this.arrayDesign = arrayDesignValueObjectsById.get( ad.getId() );
        } else {
            this.arrayDesign = new ArrayDesignValueObject( ad );
        }

        ArrayDesign op = bioAssay.getOriginalPlatform();
        if ( op != null ) {
            if ( arrayDesignValueObjectsById != null && arrayDesignValueObjectsById.containsKey( ad.getId() ) ) {
                this.originalPlatform = arrayDesignValueObjectsById.get( ad.getId() );
            } else {
                this.originalPlatform = new ArrayDesignValueObject( op );
            }
        }

        this.processingDate = bioAssay.getProcessingDate();
        this.sequencePairedReads = bioAssay.getSequencePairedReads();
        this.sequenceReadLength = bioAssay.getSequenceReadLength();
        this.sequenceReadCount = bioAssay.getSequenceReadCount();
        this.metadata = bioAssay.getMetadata();

        if ( bioAssay.getAccession() != null ) {
            this.accession = new DatabaseEntryValueObject( bioAssay.getAccession() );
        }

        if ( bioAssay.getSampleUsed() != null ) {
            this.sample = new BioMaterialValueObject( bioAssay.getSampleUsed(), basic, allFactorValues );
            sample.getBioAssayIds().add( this.getId() );
        }

        if ( bioAssay.getIsOutlier() != null ) {
            this.outlier = bioAssay.getIsOutlier();
        }

        this.userFlaggedOutlier = this.outlier;

        if ( sourceBioAssay != null ) {
            this.sourceBioAssayId = sourceBioAssay.getId();
        }
    }

    public BioAssayValueObject( Long id ) {
        super( id );
    }

    @Nullable
    public String getShortName() {
        return shortName;
    }

    public Long getSourceBioAssayId() {
        return sourceBioAssayId;
    }

    public DatabaseEntryValueObject getAccession() {
        return accession;
    }

    public ArrayDesignValueObject getArrayDesign() {
        return arrayDesign;
    }

    public String getDescription() {
        return description;
    }

    public String getMetadata() {
        return metadata;
    }

    public String getName() {
        return name;
    }

    public ArrayDesignValueObject getOriginalPlatform() {
        return originalPlatform;
    }

    public Boolean getPredictedOutlier() {
        return predictedOutlier;
    }

    public Date getProcessingDate() {
        return processingDate;
    }

    public BioMaterialValueObject getSample() {
        return sample;
    }

    public Boolean getSequencePairedReads() {
        return sequencePairedReads;
    }

    public Long getSequenceReadCount() {
        return sequenceReadCount;
    }

    public Integer getSequenceReadLength() {
        return sequenceReadLength;
    }

    public Boolean getUserFlaggedOutlier() {
        return userFlaggedOutlier;
    }

    public boolean isOutlier() {
        return outlier;
    }

    public void setShortName( @Nullable String shortName ) {
        this.shortName = shortName;
    }

    public void setSourceBioAssayId( Long sourceBioAssayId ) {
        this.sourceBioAssayId = sourceBioAssayId;
    }

    public void setAccession( DatabaseEntryValueObject accession ) {
        this.accession = accession;
    }

    public void setArrayDesign( ArrayDesignValueObject arrayDesign ) {
        this.arrayDesign = arrayDesign;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setMetadata( String metadata ) {
        this.metadata = metadata;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setOriginalPlatform( ArrayDesignValueObject originalPlatform ) {
        this.originalPlatform = originalPlatform;
    }

    public void setOutlier( boolean outlier ) {
        this.outlier = outlier;
    }

    public void setPredictedOutlier( Boolean predictedOutlier ) {
        this.predictedOutlier = predictedOutlier;
    }

    public void setProcessingDate( Date processingDate ) {
        this.processingDate = processingDate;
    }

    public void setSample( BioMaterialValueObject sample ) {
        this.sample = sample;
    }

    public void setSequencePairedReads( Boolean sequencePairedReads ) {
        this.sequencePairedReads = sequencePairedReads;
    }

    public void setSequenceReadCount( Long sequenceReadCount ) {
        this.sequenceReadCount = sequenceReadCount;
    }

    public void setSequenceReadLength( Integer sequenceReadLength ) {
        this.sequenceReadLength = sequenceReadLength;
    }

    public void setUserFlaggedOutlier( Boolean userFlaggedOutlier ) {
        this.userFlaggedOutlier = userFlaggedOutlier;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        BioAssayValueObject other = ( BioAssayValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null )
                return false;
        } else if ( !id.equals( other.id ) )
            return false;

        if ( name == null ) {
            return other.name == null;
        }
        return name.equals( other.name );
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

    @Override
    public String toString() {
        return "BioAssayVO [" + ( id != null ? "id=" + id + ", " : "" ) + ( name != null ? "name=" + name + ", " : "" )
                + ( description != null ? "description=" + description : "" ) + "]";
    }
}
