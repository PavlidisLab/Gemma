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
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

import javax.annotation.Nullable;
import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Date;

/**
 * Represents the bringing together of a biomaterial with an assay of some sort (typically an expression assay). We
 * don't distinguish between "physical" and "computational" BioAssays, so this is a concrete class. This has several
 * slots that are used specifically to support sequence-based data, but is intended to be generic.
 */
@Indexed
public class BioAssay extends AbstractDescribable implements SecuredChild, Serializable {

    private static final long serialVersionUID = -7868768731812964045L;

    private Boolean isOutlier = false;
    private Date processingDate;
    @Nullable
    private Long sequenceReadCount;
    @Nullable
    private Integer sequenceReadLength;
    @Nullable
    private Boolean sequencePairedReads;

    private ArrayDesign arrayDesignUsed;

    /**
     * If the sample data was switched to another platform, this is what it was originally.
     */
    private ArrayDesign originalPlatform;

    private BioMaterial sampleUsed;

    /**
     * Accession for this assay.
     */
    @Nullable
    private DatabaseEntry accession;
    private String metadata;

    /**
     * For RNA-seq representation of representative headers from the FASTQ file(s). If there is more than on FASTQ file,
     * this string will contain multiple newline-delimited headers.
     */
    private String fastqHeaders;

    @Override
    public int hashCode() {
        int hashCode;

        if ( this.getId() != null ) {
            return 29 * this.getId().hashCode();
        }
        int nameHash = this.getName() == null ? 0 : this.getName().hashCode();

        int descHash = this.getDescription() == null ? 0 : this.getDescription().hashCode();
        hashCode = 29 * nameHash + descHash;

        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {

        if ( !( object instanceof BioAssay ) ) {
            return false;
        }
        final BioAssay that = ( BioAssay ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );

        //noinspection SimplifiableIfStatement // Better readability
        if ( this.getName() != null && that.getName() != null && !this.getName().equals( that.getName() ) )
            return false;

        return this.getDescription() == null || that.getDescription() == null || this.getDescription()
                .equals( that.getDescription() );
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
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

    /**
     * @return Used to indicate if the sample should be considered an outlier based on QC. The audit trail for the
     *         entity tracks
     *         when this was done.
     */
    public Boolean getIsOutlier() {
        return this.isOutlier;
    }

    public void setIsOutlier( Boolean isOutlier ) {
        this.isOutlier = isOutlier;
    }

    /**
     * @return Indicates the date that the assay was processed in the original study. This would correspond to "batch"
     *         information and will often be a "scan date" or similar information extracted from the raw data files.
     */
    public Date getProcessingDate() {
        return this.processingDate;
    }

    public void setProcessingDate( Date processingDate ) {
        this.processingDate = processingDate;
    }

    @IndexedEmbedded
    public BioMaterial getSampleUsed() {
        return this.sampleUsed;
    }

    public void setSampleUsed( BioMaterial sampleUsed ) {
        this.sampleUsed = sampleUsed;
    }

    @Transient
    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    /**
     * @return For sequence-based data, this should be set to true if the sequencing was paired-end reads and false
     *         otherwise.
     *         It should be left as null if it isn't known.
     */
    @Nullable
    public Boolean getSequencePairedReads() {
        return this.sequencePairedReads;
    }

    public void setSequencePairedReads( @Nullable Boolean sequencePairedReads ) {
        this.sequencePairedReads = sequencePairedReads;
    }

    /**
     * @return For sequence-read based data, the total number of reads in the sample, computed from the data as the
     *         total of the
     *         values for the elements assayed.
     */
    @Nullable
    public Long getSequenceReadCount() {
        return this.sequenceReadCount;
    }

    public void setSequenceReadCount( @Nullable Long sequenceReadCount ) {
        this.sequenceReadCount = sequenceReadCount;
    }

    /**
     * @return For sequencing-based data, the length of the reads. If it was paired reads, this is understood to be the
     *         length
     *         for each "end". If the read length was variable (due to quality trimming, etc.) this will be treated as
     *         representing the mean read length.
     */
    @Nullable
    public Integer getSequenceReadLength() {
        return this.sequenceReadLength;
    }

    public void setSequenceReadLength( @Nullable Integer sequenceReadLength ) {
        this.sequenceReadLength = sequenceReadLength;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata( String metadata ) {
        this.metadata = metadata;
    }

    public ArrayDesign getOriginalPlatform() {
        return originalPlatform;
    }

    public void setOriginalPlatform( ArrayDesign originalPlatform ) {
        if ( this.originalPlatform != null && !( this.originalPlatform.equals( originalPlatform ) ) ) {
            System.err.println( "Warning: setting 'original platform' to a different value?" );
        }
        this.originalPlatform = originalPlatform;
    }

    public String getFastqHeaders() {
        return fastqHeaders;
    }

    public void setFastqHeaders( String fastqHeaders ) {
        this.fastqHeaders = fastqHeaders;
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
    }

}
