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
import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

/**
 * @author Paul
 */
@Getter
@Setter
public class BioAssayValueObject extends IdentifiableValueObject<BioAssay> {

    private static final long serialVersionUID = 9164284536309673585L;

    @Nullable
    private String shortName;
    private String name;
    private String description;
    private String metadata;
    @Nullable
    private DatabaseEntryValueObject accession;
    private ArrayDesignValueObject arrayDesign;
    @Nullable
    private ArrayDesignValueObject originalPlatform;
    private Date processingDate;
    private BioMaterialValueObject sample;

    // only for RNA-Seq data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean sequencePairedReads;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long sequenceReadCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer sequenceReadLength;

    // only for single-cell data
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfCells;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfDesignElements;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer numberOfCellsByDesignElements;

    // if it was removed as an outlier
    private boolean outlier = false;
    // if our algorithm says it might be an outlier.
    private boolean predictedOutlier = false;
    // to hold state change, initialized as this.outlier
    private boolean userFlaggedOutlier = false;

    /**
     * If this BioAssay has a parent via {@link BioMaterial#getSourceBioMaterial()}, this is the ID.
     * <p>
     * This is context-dependent because the parent depends on which {@link ubic.gemma.model.expression.experiment.BioAssaySet}
     * is under consideration. For example, an experiment could have two sets of EE subsets with distinct parents.
     */
    @Nullable
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
     * @param ad2vo           pre-populated array design VOs by array design, or null to ignore and the VOs will be
     *                        initialized via {@link ArrayDesignValueObject#ArrayDesignValueObject(ArrayDesign)}
     * @param sourceBioAssay  the source {@link BioAssay} if known, this corresponds to the assay of the source sample,
     *                        but since there might be more than one, it must be picked explicitly based on the context
     * @param basic           if true, produce basic factor values in the corresponding biomaterial, see
     *                        {@link BioMaterialValueObject#BioMaterialValueObject(BioMaterial, boolean, boolean)}
     *                        for more details
     * @param allFactorValues include all FVs, including those inherited from the source biomaterial in the
     *                        corresponding biomaterial
     */
    public BioAssayValueObject( BioAssay bioAssay, @Nullable Map<ArrayDesign, ArrayDesignValueObject> ad2vo, @Nullable BioAssay sourceBioAssay, boolean basic, boolean allFactorValues ) {
        super( bioAssay );
        this.shortName = bioAssay.getShortName();
        this.name = bioAssay.getName();
        this.description = bioAssay.getDescription();

        // the platform and original platform are eagerly fetched, so no need for a Utils.isInitialized() test:w
        ArrayDesign ad = bioAssay.getArrayDesignUsed();
        assert ad != null;
        if ( ad2vo != null && ad2vo.containsKey( ad ) ) {
            this.arrayDesign = ad2vo.get( ad );
        } else {
            this.arrayDesign = new ArrayDesignValueObject( ad );
        }

        ArrayDesign op = bioAssay.getOriginalPlatform();
        if ( op != null ) {
            if ( ad2vo != null && ad2vo.containsKey( ad ) ) {
                this.originalPlatform = ad2vo.get( ad );
            } else {
                this.originalPlatform = new ArrayDesignValueObject( op );
            }
        }

        this.processingDate = bioAssay.getProcessingDate();
        this.sequencePairedReads = bioAssay.getSequencePairedReads();
        this.sequenceReadLength = bioAssay.getSequenceReadLength();
        this.sequenceReadCount = bioAssay.getSequenceReadCount();
        this.metadata = bioAssay.getMetadata();

        this.numberOfCells = bioAssay.getNumberOfCells();
        this.numberOfDesignElements = bioAssay.getNumberOfDesignElements();
        this.numberOfCellsByDesignElements = bioAssay.getNumberOfCellsByDesignElements();

        if ( bioAssay.getAccession() != null ) {
            this.accession = new DatabaseEntryValueObject( bioAssay.getAccession() );
        }

        if ( bioAssay.getSampleUsed() != null ) {
            this.sample = new BioMaterialValueObject( bioAssay.getSampleUsed(), basic, allFactorValues );
            sample.getBioAssayIds().add( this.getId() );
        }

        this.outlier = bioAssay.getIsOutlier();

        this.userFlaggedOutlier = this.outlier;

        if ( sourceBioAssay != null ) {
            this.sourceBioAssayId = sourceBioAssay.getId();
        }
    }

    public BioAssayValueObject( Long id ) {
        super( id );
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
        return super.toString() + ( name != null ? " Name=" + name : "" );
    }
}
