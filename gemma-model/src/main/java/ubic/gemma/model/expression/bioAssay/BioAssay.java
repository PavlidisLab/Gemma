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

import gemma.gsec.model.Securable;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;

/**
 * Represents the bringing together of a biomaterial with an assay of some sort (typically an expression assay). We
 * don't distinguish between "physical" and "computational" BioAssays, so this is a concrete class. This has several
 * slots that are used specifically to support sequence-based data, but is intended to be generic.
 */
public abstract class BioAssay extends Auditable implements gemma.gsec.model.SecuredChild {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.bioAssay.BioAssay}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.bioAssay.BioAssay}.
         */
        public static BioAssay newInstance() {
            return new BioAssayImpl();
        }

    }

    private Date processingDate;

    private Integer sequenceReadCount;

    private Integer sequenceReadLength;

    private Boolean sequencePairedReads = Boolean.valueOf( false );

    private Boolean isOutlier = Boolean.valueOf( false );

    private ArrayDesign arrayDesignUsed;

    private DatabaseEntry accession;

    private LocalFile rawDataFile;

    private Collection<LocalFile> derivedDataFiles = new HashSet<LocalFile>();

    private BioMaterial sampleUsed;

    /**
     * 
     */
    public DatabaseEntry getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.arrayDesign.ArrayDesign getArrayDesignUsed() {
        return this.arrayDesignUsed;
    }

    /**
     * Files containing derived data, from our own or someone else's analysis.
     */
    public Collection<LocalFile> getDerivedDataFiles() {
        return this.derivedDataFiles;
    }

    /**
     * Used to indicate if the sample should be considered an outlier based on QC. The audit trail for the entity tracks
     * when this was done.
     */
    public Boolean getIsOutlier() {
        return this.isOutlier;
    }

    /**
     * Indicates the date that the assay was processed in the original study. This would correspond to "batch"
     * information and will often be a "scan date" or similar information extracted from the raw data files.
     */
    public Date getProcessingDate() {
        return this.processingDate;
    }

    /**
     * The data 'as we got it', such as CEL files or raw files from the SMD site, or GEO soft files.
     */
    public LocalFile getRawDataFile() {
        return this.rawDataFile;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.biomaterial.BioMaterial getSampleUsed() {
        return this.sampleUsed;
    }

    @Override
    public Securable getSecurityOwner() {
        return null;
    }

    /**
     * For sequence-based data, this should be set to true if the sequencing was paired-end reads and false otherwise.
     * It should be left as null if it isn't known.
     */
    public Boolean getSequencePairedReads() {
        return this.sequencePairedReads;
    }

    /**
     * For sequence-read based data, the total number of reads in the sample, computed from the data as the total of the
     * values for the elements assayed.
     */
    public Integer getSequenceReadCount() {
        return this.sequenceReadCount;
    }

    /**
     * For sequencing-based data, the length of the reads. If it was paired reads, this is understood to be the length
     * for each "end". If the read length was variable (due to quality trimming, etc.) this will be treated as
     * representing the mean read length.
     */
    public Integer getSequenceReadLength() {
        return this.sequenceReadLength;
    }

    public void setAccession( DatabaseEntry accession ) {
        this.accession = accession;
    }

    public void setArrayDesignUsed( ArrayDesign arrayDesignUsed ) {
        this.arrayDesignUsed = arrayDesignUsed;
    }

    public void setDerivedDataFiles( Collection<LocalFile> derivedDataFiles ) {
        this.derivedDataFiles = derivedDataFiles;
    }

    public void setIsOutlier( Boolean isOutlier ) {
        this.isOutlier = isOutlier;
    }

    public void setProcessingDate( Date processingDate ) {
        this.processingDate = processingDate;
    }

    public void setRawDataFile( LocalFile rawDataFile ) {
        this.rawDataFile = rawDataFile;
    }

    public void setSampleUsed( BioMaterial sampleUsed ) {
        this.sampleUsed = sampleUsed;
    }

    public void setSequencePairedReads( Boolean sequencePairedReads ) {
        this.sequencePairedReads = sequencePairedReads;
    }

    public void setSequenceReadCount( Integer sequenceReadCount ) {
        this.sequenceReadCount = sequenceReadCount;
    }

    public void setSequenceReadLength( Integer sequenceReadLength ) {
        this.sequenceReadLength = sequenceReadLength;
    }

}