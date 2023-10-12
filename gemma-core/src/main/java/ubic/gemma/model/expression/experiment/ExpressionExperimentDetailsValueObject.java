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
package ubic.gemma.model.expression.experiment;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclSid;
import org.apache.commons.text.StringEscapeUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author paul
 */
@SuppressWarnings("unused") // ValueObject accessed from JS
public class ExpressionExperimentDetailsValueObject extends ExpressionExperimentValueObject {

    private static final long serialVersionUID = -1219449523930648392L;
    private static final String TROUBLE_DETAIL_PLATF = "Platform problems: ";
    private static final String TROUBLE_DETAIL_SEPARATOR = " | ";

    private Collection<ArrayDesignValueObject> arrayDesigns;
    private String batchFetchEventType;
    // Pulled from base EEVO
    private Date dateArrayDesignLastUpdated;
    private Date dateBatchFetch;
    private Date dateCached;
    private Date dateDifferentialAnalysis;
    private Date dateLinkAnalysis;
    private Date dateMissingValueAnalysis;
    private Date datePcaAnalysis;
    private Date dateProcessedDataVectorComputation;
    private Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses = new HashSet<>();
    private Collection<ExpressionExperimentSetValueObject> expressionExperimentSets;
    private boolean hasBatchInformation;
    private Boolean hasBothIntensities = false;
    private Boolean hasCoexpressionAnalysis = false;
    private Boolean hasDifferentialExpressionAnalysis = false;
    private Boolean hasEitherIntensity = false;

    private Boolean hasMultiplePreferredQuantitationTypes = false;

    private Boolean hasMultipleTechnologyTypes = false;

    private Boolean isRNASeq = false;
    private String lastArrayDesignUpdateDate;
    private String linkAnalysisEventType;
    private Double minPvalue;
    private String missingValueAnalysisEventType;
    private Long numAnnotations;
    private Long numPopulatedFactors;
    // if it was switched
    private Collection<ArrayDesignValueObject> originalPlatforms;

    // if it was split.
    private Collection<ExpressionExperimentValueObject> otherParts = new HashSet<>();

    private String pcaAnalysisEventType;

    private CitationValueObject primaryCitation;

    private String processedDataVectorComputationEventType;
    private Integer pubmedId;
    private String QChtml;
    private boolean reprocessedFromRawData;
    //   private Integer coexpressionLinkCount = null;
    private Collection<AuditEventValueObject> sampleRemovedFlags;
    private String secondaryAccession;
    private String secondaryExternalDatabase;
    private String secondaryExternalUri;

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentDetailsValueObject() {
        super();
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperiment ee ) {
        super( ee );
    }

    /**
     * {@inheritDoc}
     */
    public ExpressionExperimentDetailsValueObject( ExpressionExperiment ee, AclObjectIdentity aoi,
            AclSid sid ) {
        super( ee, aoi, sid );
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject vo ) {
        super( vo );
    }

    public void auditEvents2SampleRemovedFlags( Collection<AuditEvent> s ) {
        Collection<AuditEventValueObject> converted = new HashSet<>();

        for ( AuditEvent ae : s ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    /**
     * @return true if this EE is troubled, disregards any platform trouble that might be present.
     */
    @SuppressWarnings("unused") // Used in Curation tab, see CurationTools.js
    public Boolean getActuallyTroubled() {
        return super.getTroubled();
    }

    public Collection<ArrayDesignValueObject> getArrayDesigns() {
        return arrayDesigns;
    }

    public String getBatchFetchEventType() {
        return batchFetchEventType;
    }

    /**
     * @return The date the platform associated with the experiment was last updated. If there are multiple platforms
     * this should be the date of the most recent modification of any of them. This is used to help flag experiments
     * that need re-analysis due to changes in the underlying array design(s)
     */
    public Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    public Date getDateBatchFetch() {
        return dateBatchFetch;
    }

    /**
     * @return The date this object was generated.
     */
    public Date getDateCached() {
        return this.dateCached;
    }

    public Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    //    public Integer getCoexpressionLinkCount() {
    //        return this.coexpressionLinkCount;
    //    }
    //
    //    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
    //        this.coexpressionLinkCount = coexpressionLinkCount;
    //    }

    public Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    public Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    public Date getDatePcaAnalysis() {
        return datePcaAnalysis;
    }

    public Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    public Collection<DifferentialExpressionAnalysisValueObject> getDifferentialExpressionAnalyses() {
        return differentialExpressionAnalyses;
    }

    /**
     * @return the expressionExperimentSets
     */
    public Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets() {
        return expressionExperimentSets;
    }

    public Boolean getHasBothIntensities() {
        return hasBothIntensities;
    }

    public Boolean getHasCoexpressionAnalysis() {
        return hasCoexpressionAnalysis;
    }

    public Boolean getHasDifferentialExpressionAnalysis() {
        return hasDifferentialExpressionAnalysis;
    }

    /**
     * @return true if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    public Boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    public Boolean getHasMultiplePreferredQuantitationTypes() {
        return hasMultiplePreferredQuantitationTypes;
    }

    public Boolean getHasMultipleTechnologyTypes() {
        return hasMultipleTechnologyTypes;
    }

    public Boolean getIsRNASeq() {
        return isRNASeq;
    }

    public String getLastArrayDesignUpdateDate() {
        return lastArrayDesignUpdateDate;
    }

    public String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    public Double getMinPvalue() {
        return this.minPvalue;
    }

    public String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    /**
     * @return The number of terms (Characteristics) the experiment has to describe it.
     */
    public Long getNumAnnotations() {
        return this.numAnnotations;
    }

    /**
     * @return The number of experimental factors the experiment has (counting those that are populated with
     * biomaterials)
     */
    public Long getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    public Collection<ArrayDesignValueObject> getOriginalPlatforms() {
        return originalPlatforms;
    }

    /**
     * @return IDs of experiments that are related to this one via the splitting of a source experiment.
     */
    public Collection<ExpressionExperimentValueObject> getOtherParts() {
        return otherParts;
    }

    public void setOtherParts( Collection<ExpressionExperimentValueObject> otherParts ) {
        this.otherParts = otherParts;
    }

    public String getPcaAnalysisEventType() {
        return pcaAnalysisEventType;
    }

    /**
     * @return true, if the any of the platforms of this EE is troubled. False otherwise, even if this EE itself is
     * troubled.
     */
    @SuppressWarnings("unused") // Used in Curation tab, see CurationTools.js
    public Boolean getPlatformTroubled() {
        if ( this.arrayDesigns == null ) {
            return null; // Just because dwr accesses this even when arrayDesigns is not set.
        }
        for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
            if ( ad.getTroubled() ) {
                return true;
            }
        }
        return false;
    }

    public CitationValueObject getPrimaryCitation() {
        return primaryCitation;
    }

    public String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    public Integer getPubmedId() {
        return this.pubmedId;
    }

    public String getQChtml() {
        return QChtml;
    }

    public boolean getReprocessedFromRawData() {
        return reprocessedFromRawData;
    }

    /**
     * @return Details of samples that were removed (or marked as outliers). This can happen multiple times in the life
     * of data set, so this is a collection of AuditEvents.
     */
    public Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    /**
     * @return Identifier in a second database, if available. For example, if the data are in GEO and in ArrayExpress,
     * this might be a link to the ArrayExpress version.
     */
    public String getSecondaryAccession() {
        return this.secondaryAccession;
    }

    public String getSecondaryExternalDatabase() {
        return this.secondaryExternalDatabase;
    }

    public String getSecondaryExternalUri() {
        return this.secondaryExternalUri;
    }

    /**
     * @return true if the EE, or any of its Array Designs is troubled.
     */
    @Override
    public Boolean getTroubled() {
        Boolean troubled = super.getTroubled();
        if ( !troubled && this.arrayDesigns != null ) {
            for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
                if ( ad.getTroubled() )
                    return true;
            }
        }
        return troubled;
    }

    /**
     * @return html-escaped string with trouble info.
     * @see #getTroubleDetails(boolean)
     */
    @Override
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }

    /**
     * Checks trouble of this EE and all its Array Designs and returns compilation of trouble info.
     * MAKE SURE to fill the Array Design variable first!
     *
     * @param htmlEscape whether to escape the returned string for html
     * @return string with trouble info.
     */
    @Override
    public String getTroubleDetails( boolean htmlEscape ) {
        String eeTroubleDetails = null;
        StringBuilder adTroubleDetails = null;
        String finalTroubleDetails = "";
        boolean adTroubled = false;

        if ( super.getTroubled() )
            eeTroubleDetails = super.getTroubleDetails( htmlEscape );

        if ( this.arrayDesigns != null ) { // Just because dwr accesses this even when arrayDesigns is not set.
            for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
                if ( ad.getTroubled() ) {
                    adTroubled = true;
                    if ( adTroubleDetails == null ) {
                        adTroubleDetails = new StringBuilder(
                                ExpressionExperimentDetailsValueObject.TROUBLE_DETAIL_PLATF );
                    } else {
                        adTroubleDetails.append( ExpressionExperimentDetailsValueObject.TROUBLE_DETAIL_SEPARATOR );
                    }
                    adTroubleDetails.append( ad.getTroubleDetails( false ) );
                }
            }
        }

        if ( super.getTroubled() ) {
            finalTroubleDetails += eeTroubleDetails;
        } else if ( adTroubled ) {
            finalTroubleDetails += adTroubleDetails;
        }

        return htmlEscape ? StringEscapeUtils.escapeHtml4( finalTroubleDetails ) : finalTroubleDetails;
    }

    public boolean isHasBatchInformation() {
        return hasBatchInformation;
    }

    /**
     * As a side effect, sets the technology type and taxon of this based on the first arrayDesign.
     *
     * @param arrayDesigns arrayDesign value objects to associate
     */
    public void setArrayDesigns( Collection<ArrayDesignValueObject> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
        ArrayDesignValueObject ad = arrayDesigns.iterator().next();
        setArrayDesignCount( ( long ) arrayDesigns.size() );
        this.setTechnologyType( ad.getTechnologyType() );
        this.setTaxonObject( ad.getTaxonObject() ); // FIXME still need the ID of the taxon, don't we?
    }

    public void setBatchFetchEventType( String batchFetchEventType ) {
        this.batchFetchEventType = batchFetchEventType;
    }

    public void setDateArrayDesignLastUpdated( Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public void setDateBatchFetch( Date dateBatchFetch ) {
        this.dateBatchFetch = dateBatchFetch;
    }

    public void setDateCached( Date dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDateDifferentialAnalysis( Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public void setDateLinkAnalysis( Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public void setDateMissingValueAnalysis( Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public void setDatePcaAnalysis( Date datePcaAnalysis ) {
        this.datePcaAnalysis = datePcaAnalysis;
    }

    public void setDateProcessedDataVectorComputation( Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    @Override
    public void setDescription( String description ) {
        this.description = description;
    }

    public void setDifferentialExpressionAnalyses(
            Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses ) {
        this.differentialExpressionAnalyses = differentialExpressionAnalyses;
    }

    /**
     * @param expressionExperimentSets the expressionExperimentSets to set
     */
    public void setExpressionExperimentSets( Collection<ExpressionExperimentSetValueObject> expressionExperimentSets ) {
        this.expressionExperimentSets = expressionExperimentSets;
    }

    public void setHasBatchInformation( boolean hasBatchInformation ) {
        this.hasBatchInformation = hasBatchInformation;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public void setHasCoexpressionAnalysis( Boolean hasCoexpressionAnalysis ) {
        this.hasCoexpressionAnalysis = hasCoexpressionAnalysis;
    }

    public void setHasDifferentialExpressionAnalysis( Boolean hasDifferentialExpressionAnalysis ) {
        this.hasDifferentialExpressionAnalysis = hasDifferentialExpressionAnalysis;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    public void setHasMultiplePreferredQuantitationTypes( Boolean hasMultiplePreferredQuantitationTypes ) {
        this.hasMultiplePreferredQuantitationTypes = hasMultiplePreferredQuantitationTypes;
    }

    public void setHasMultipleTechnologyTypes( Boolean hasMultipleTechnologyTypes ) {
        this.hasMultipleTechnologyTypes = hasMultipleTechnologyTypes;
    }

    public void setIsRNASeq( Boolean isRNASeq ) {
        this.isRNASeq = isRNASeq;
    }

    public void setLastArrayDesignUpdateDate( String lastArrayDesignUpdateDate ) {
        this.lastArrayDesignUpdateDate = lastArrayDesignUpdateDate;
    }

    public void setLinkAnalysisEventType( String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public void setMinPvalue( Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public void setMissingValueAnalysisEventType( String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    public void setNumAnnotations( Long numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    public void setNumPopulatedFactors( Long numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public void setOriginalPlatforms( Collection<ArrayDesignValueObject> originalPlatforms ) {
        this.originalPlatforms = originalPlatforms;
    }

    public void setPcaAnalysisEventType( String pcaAnalysisEventType ) {
        this.pcaAnalysisEventType = pcaAnalysisEventType;
    }

    public void setPrimaryCitation( CitationValueObject primaryCitation ) {
        this.primaryCitation = primaryCitation;
    }

    public void setProcessedDataVectorComputationEventType( String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public void setPubmedId( Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    public void setQChtml( String qChtml ) {
        QChtml = qChtml;
    }

    public void setReprocessedFromRawData( boolean reprocessedFromRawData ) {
        this.reprocessedFromRawData = reprocessedFromRawData;
    }

    public void setSampleRemovedFlags( Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    public void setSecondaryAccession( String secondaryAccession ) {
        this.secondaryAccession = secondaryAccession;
    }

    public void setSecondaryExternalDatabase( String secondaryExternalDatabase ) {
        this.secondaryExternalDatabase = secondaryExternalDatabase;
    }

    public void setSecondaryExternalUri( String secondaryExternalUri ) {
        this.secondaryExternalUri = secondaryExternalUri;
    }
}
