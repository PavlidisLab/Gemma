/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.expression.bioAssay;

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.DescribableUtils;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.util.Date;

/**
 * Represents the bringing together of a biomaterial with an assay of some sort (typically an expression assay). We
 * don't distinguish between "physical" and "computational" BioAssays, so this is a concrete class. This has several
 * slots that are used specifically to support sequence-based data, but is intended to be generic.
 */
@Indexed
public class BioAssay extends AbstractDescribable implements SecuredChild<ExpressionExperiment> {

    public static final int MAX_NAME_LENGTH = 255;

    /**
     * A unique and recognizable identifier for this assay.
     * <p>
     * This is generally the same as the accession.
     * <p>
     * It is null for now, but in the future, it will become non-null.
     */
    @Nullable
    private String shortName;

    /**
     * Platform used in this assay.
     */
    private ArrayDesign arrayDesignUsed;

    /**
     * If the assay data was switched to another platform, this is what it was originally.
     */
    @Nullable
    private ArrayDesign originalPlatform;

    /**
     * Sample used in this assay.
     */
    private BioMaterial sampleUsed;

    /**
     * Accession for this assay.
     */
    @Nullable
    private DatabaseEntry accession;

    /**
     * Indicates if the assay should be considered an outlier based on QC.
     * <p>
     * The audit trail for the owning {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} tracks when
     * this was done.
     */
    private boolean isOutlier = false;

    /**
     * Indicates the date that the assay was processed in the original study. This would correspond to "batch" in the
     * experimental design.
     */
    @Nullable
    private Date processingDate;

    /**
     * Free-form additional metadata.
     */
    @Nullable
    private String metadata;

    /**
     * For sequence-read based data, the total number of reads in the assay, computed from the data as the total of the
     * values for the elements assayed.
     */
    @Nullable
    private Long sequenceReadCount;
    /**
     * For sequencing-based data, the length of the reads. If it was paired reads, this is understood to be the length
     * for each "end". If the read length was variable (due to quality trimming, etc.) this will be treated as
     * representing the mean read length.
     */
    @Nullable
    private Integer sequenceReadLength;
    /**
     * For sequence-based data, this should be set to true if the sequencing was paired-end reads and false otherwise.
     * It should be left as null if it isn't known.
     */
    @Nullable
    private Boolean sequencePairedReads;
    /**
     * For RNA-seq representation of representative headers from the FASTQ file(s). If there is more than on FASTQ file,
     * this string will contain multiple newline-delimited headers.
     */
    @Nullable
    private String fastqHeaders;

    /**
     * Number of cells for the assay that have at least one expressed gene.
     * <p>
     * If this assay correspond to a pseudo-bulk, it is the number of such cells in the aggregate.
     * <p>
     * This applies to the preferred set of single-cell vectors.
     * <p>
     * Masked cells are not counted toward this total.
     */
    @Nullable
    private Integer numberOfCells;
    /**
     * Number of design elements in the assay with at least one cell expressing it.
     * <p>
     * If this assay correspond to a pseudo-bulk, it is the number of such design elements in the aggregate.
     * <p>
     * This applies to the preferred set of single-cell vectors.
     * <p>
     * Masked cells are not counted toward this total.
     */
    @Nullable
    private Integer numberOfDesignElements;
    /**
     * Number of cell-gene pairs with non-zero expression values.
     * <p>
     * If this assay correspond to a pseudo-bulk, it is the number of such pairs in the aggregate.
     * <p>
     * This applies to the preferred set of single-cell vectors.
     * <p>
     * Masked cells are not counted toward this total.
     */
    @Nullable
    private Integer numberOfCellsByDesignElements;

    @Nullable
    private ExpressionExperiment securityOwner;

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioAssay ) )
            return false;
        final BioAssay that = ( BioAssay ) object;
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        }
        return DescribableUtils.equalsByName( this, that );
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Nullable
    @Field
    public String getShortName() {
        return shortName;
    }

    public void setShortName( @Nullable String shortName ) {
        this.shortName = shortName;
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    @Override
    @Field(store = Store.YES)
    public String getDescription() {
        return super.getDescription();
    }

    @Nullable
    @IndexedEmbedded
    public DatabaseEntry getAccession() {
        return this.accession;
    }

    public void setAccession( @Nullable DatabaseEntry accession ) {
        this.accession = accession;
    }

    public ArrayDesign getArrayDesignUsed() {
        return this.arrayDesignUsed;
    }

    public void setArrayDesignUsed( ArrayDesign arrayDesignUsed ) {
        this.arrayDesignUsed = arrayDesignUsed;
    }

    public boolean getIsOutlier() {
        return this.isOutlier;
    }

    public void setIsOutlier( boolean isOutlier ) {
        this.isOutlier = isOutlier;
    }

    @Nullable
    public Date getProcessingDate() {
        return this.processingDate;
    }

    public void setProcessingDate( @Nullable Date processingDate ) {
        this.processingDate = processingDate;
    }

    @IndexedEmbedded
    public BioMaterial getSampleUsed() {
        return this.sampleUsed;
    }

    public void setSampleUsed( BioMaterial sampleUsed ) {
        this.sampleUsed = sampleUsed;
    }

    @Nullable
    @Transient
    @Override
    public ExpressionExperiment getSecurityOwner() {
        return this.securityOwner;
    }

    @SuppressWarnings("unused")
    public void setSecurityOwner( @Nullable ExpressionExperiment securable ) {
        this.securityOwner = securable;
    }

    @Nullable
    public Boolean getSequencePairedReads() {
        return this.sequencePairedReads;
    }

    public void setSequencePairedReads( @Nullable Boolean sequencePairedReads ) {
        this.sequencePairedReads = sequencePairedReads;
    }

    @Nullable
    public Long getSequenceReadCount() {
        return this.sequenceReadCount;
    }

    public void setSequenceReadCount( @Nullable Long sequenceReadCount ) {
        this.sequenceReadCount = sequenceReadCount;
    }

    @Nullable
    public Integer getSequenceReadLength() {
        return this.sequenceReadLength;
    }

    public void setSequenceReadLength( @Nullable Integer sequenceReadLength ) {
        this.sequenceReadLength = sequenceReadLength;
    }

    @Nullable
    public String getMetadata() {
        return metadata;
    }

    public void setMetadata( @Nullable String metadata ) {
        this.metadata = metadata;
    }

    @Nullable
    public ArrayDesign getOriginalPlatform() {
        return originalPlatform;
    }

    public void setOriginalPlatform( @Nullable ArrayDesign originalPlatform ) {
        this.originalPlatform = originalPlatform;
    }

    @Nullable
    public String getFastqHeaders() {
        return fastqHeaders;
    }

    public void setFastqHeaders( @Nullable String fastqHeaders ) {
        this.fastqHeaders = fastqHeaders;
    }

    @Nullable
    public Integer getNumberOfCells() {
        return numberOfCells;
    }

    public void setNumberOfCells( @Nullable Integer numberOfCells ) {
        this.numberOfCells = numberOfCells;
    }

    @Nullable
    public Integer getNumberOfDesignElements() {
        return numberOfDesignElements;
    }

    public void setNumberOfDesignElements( @Nullable Integer numberOfDesignElements ) {
        this.numberOfDesignElements = numberOfDesignElements;
    }

    @Nullable
    public Integer getNumberOfCellsByDesignElements() {
        return numberOfCellsByDesignElements;
    }

    public void setNumberOfCellsByDesignElements( @Nullable Integer numberOfCellsByDesignElements ) {
        this.numberOfCellsByDesignElements = numberOfCellsByDesignElements;
    }

    public static final class Factory {

        public static BioAssay newInstance() {
            return new BioAssay();
        }

        public static BioAssay newInstance( String name ) {
            BioAssay ba = new BioAssay();
            ba.setName( name );
            return ba;
        }

        public static BioAssay newInstance( String name, ArrayDesign arrayDesignUsed, BioMaterial sampleUsed ) {
            BioAssay ba = new BioAssay();
            ba.setName( name );
            ba.setArrayDesignUsed( arrayDesignUsed );
            ba.setSampleUsed( sampleUsed );
            return ba;
        }
    }

}
