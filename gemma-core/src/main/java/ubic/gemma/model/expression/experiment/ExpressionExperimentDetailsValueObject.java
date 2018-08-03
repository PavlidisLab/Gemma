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

import org.apache.commons.lang3.StringEscapeUtils;
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

    private static final String TROUBLE_DETAIL_PLATF = "Platform problems: ";
    private static final String TROUBLE_DETAIL_SEPARATOR = " | ";
    private static final long serialVersionUID = -1219449523930648392L;

    private boolean reprocessedFromRawData;
    private boolean hasBatchInformation;
    private String secondaryAccession;
    private String secondaryExternalDatabase;
    private String secondaryExternalUri;
    private String QChtml;
    private String lastArrayDesignUpdateDate;
    private Boolean hasMultiplePreferredQuantitationTypes = false;
    private Boolean hasMultipleTechnologyTypes = false;
    private Boolean hasCoexpressionAnalysis = false;
    private Boolean hasDifferentialExpressionAnalysis = false;
    private Boolean hasBothIntensities = false;
    private Boolean hasEitherIntensity = false;
    private CitationValueObject primaryCitation;
    private Collection<ArrayDesignValueObject> arrayDesigns;
    private Collection<ExpressionExperimentSetValueObject> expressionExperimentSets;

    // Pulled from base EEVO
    private Date dateArrayDesignLastUpdated;
    private Date dateBatchFetch;
    private Date dateCached;
    private Date dateDifferentialAnalysis;
    private Date dateLinkAnalysis;
    private Date dateMissingValueAnalysis;
    private Date datePcaAnalysis;
    private Date dateProcessedDataVectorComputation;
    private Double minPvalue;
    private String linkAnalysisEventType;
    private String missingValueAnalysisEventType;
    private String pcaAnalysisEventType;
    private String batchFetchEventType;
    private String processedDataVectorComputationEventType;
    private Integer pubmedId;
    private Integer numAnnotations;
    private Integer numPopulatedFactors;
    private Integer coexpressionLinkCount = null;
    private Collection<AuditEventValueObject> sampleRemovedFlags;
    private Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses = new HashSet<>();

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentDetailsValueObject() {
    }

    public ExpressionExperimentDetailsValueObject( Object[] row ) {
        super( row, null );
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperiment ee ) {
        super( ee );
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject vo ) {
        super( vo.getId(), vo.name, vo.description, vo.bioAssayCount, vo.getAccession(), vo.getBatchConfound(),
                vo.getBatchEffect(), vo.getExternalDatabase(), vo.getExternalUri(), vo.getMetadata(), vo.getShortName(),
                vo.getSource(), vo.getTaxon(), vo.getTechnologyType(), vo.getTaxonId(), 
                vo.getExperimentalDesign(), vo.getProcessedExpressionVectorCount(), vo.getArrayDesignCount(),
                vo.getBioMaterialCount(), vo.getCurrentUserHasWritePermission(), vo.getCurrentUserIsOwner(),
                vo.getIsPublic(), vo.getIsShared(), vo.getLastUpdated(), vo.getTroubled(), vo.getLastTroubledEvent(),
                vo.getNeedsAttention(), vo.getLastNeedsAttentionEvent(), vo.getCurationNote(),
                vo.getLastNoteUpdateEvent(), vo.getGeeq() );
    }

    public void auditEvents2SampleRemovedFlags( Collection<AuditEvent> s ) {
        Collection<AuditEventValueObject> converted = new HashSet<>();

        for ( AuditEvent ae : s ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    public String getBatchFetchEventType() {
        return batchFetchEventType;
    }

    public void setBatchFetchEventType( String batchFetchEventType ) {
        this.batchFetchEventType = batchFetchEventType;
    }

    public Integer getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    /**
     * @return The date the platform associated with the experiment was last updated. If there are multiple platforms
     *         this
     *         should be the date of the most recent modification of any of them. This is used to help flag experiments
     *         that
     *         need re-analysis due to changes in the underlying array design(s)
     */
    public Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    public void setDateArrayDesignLastUpdated( Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public Date getDateBatchFetch() {
        return dateBatchFetch;
    }

    public void setDateBatchFetch( Date dateBatchFetch ) {
        this.dateBatchFetch = dateBatchFetch;
    }

    /**
     * @return The date this object was generated.
     */
    public Date getDateCached() {
        return this.dateCached;
    }

    public void setDateCached( Date dateCached ) {
        this.dateCached = dateCached;
    }

    public Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    public void setDateDifferentialAnalysis( Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    public void setDateLinkAnalysis( Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    public void setDateMissingValueAnalysis( Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public Date getDatePcaAnalysis() {
        return datePcaAnalysis;
    }

    public void setDatePcaAnalysis( Date datePcaAnalysis ) {
        this.datePcaAnalysis = datePcaAnalysis;
    }

    public Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    public void setDateProcessedDataVectorComputation( Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    public Collection<DifferentialExpressionAnalysisValueObject> getDifferentialExpressionAnalyses() {
        return differentialExpressionAnalyses;
    }

    public void setDifferentialExpressionAnalyses(
            Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses ) {
        this.differentialExpressionAnalyses = differentialExpressionAnalyses;
    }

    public String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    public void setLinkAnalysisEventType( String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public Double getMinPvalue() {
        return this.minPvalue;
    }

    public void setMinPvalue( Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    public void setMissingValueAnalysisEventType( String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    /**
     * @return The number of terms (Characteristics) the experiment has to describe it.
     */
    public Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    public void setNumAnnotations( Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    /**
     * @return The number of experimental factors the experiment has (counting those that are populated with
     *         biomaterials)
     */
    public Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    public void setNumPopulatedFactors( Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public String getPcaAnalysisEventType() {
        return pcaAnalysisEventType;
    }

    public void setPcaAnalysisEventType( String pcaAnalysisEventType ) {
        this.pcaAnalysisEventType = pcaAnalysisEventType;
    }

    public String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    public void setProcessedDataVectorComputationEventType( String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public Integer getPubmedId() {
        return this.pubmedId;
    }

    public void setPubmedId( Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    /**
     * @return Details of samples that were removed (or marked as outliers). This can happen multiple times in the life
     *         of a
     *         data set, so this is a collection of AuditEvents.
     */
    public Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    public void setSampleRemovedFlags( Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    public Boolean getHasBothIntensities() {
        return hasBothIntensities;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    /**
     * @return true if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    public Boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    public Boolean getHasCoexpressionAnalysis() {
        return hasCoexpressionAnalysis;
    }

    public void setHasCoexpressionAnalysis( Boolean hasCoexpressionAnalysis ) {
        this.hasCoexpressionAnalysis = hasCoexpressionAnalysis;
    }

    public Boolean getHasDifferentialExpressionAnalysis() {
        return hasDifferentialExpressionAnalysis;
    }

    public void setHasDifferentialExpressionAnalysis( Boolean hasDifferentialExpressionAnalysis ) {
        this.hasDifferentialExpressionAnalysis = hasDifferentialExpressionAnalysis;
    }

    public boolean getReprocessedFromRawData() {
        return reprocessedFromRawData;
    }

    public void setReprocessedFromRawData( boolean reprocessedFromRawData ) {
        this.reprocessedFromRawData = reprocessedFromRawData;
    }

    public Boolean getHasMultipleTechnologyTypes() {
        return hasMultipleTechnologyTypes;
    }

    public void setHasMultipleTechnologyTypes( Boolean hasMultipleTechnologyTypes ) {
        this.hasMultipleTechnologyTypes = hasMultipleTechnologyTypes;
    }

    public Boolean getHasMultiplePreferredQuantitationTypes() {
        return hasMultiplePreferredQuantitationTypes;
    }

    public void setHasMultiplePreferredQuantitationTypes( Boolean hasMultiplePreferredQuantitationTypes ) {
        this.hasMultiplePreferredQuantitationTypes = hasMultiplePreferredQuantitationTypes;
    }

    public Collection<ArrayDesignValueObject> getArrayDesigns() {
        return arrayDesigns;
    }

    public void setArrayDesigns( Collection<ArrayDesignValueObject> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public void setDescription( String description ) {
        this.description = description;
    }

    public CitationValueObject getPrimaryCitation() {
        return primaryCitation;
    }

    public void setPrimaryCitation( CitationValueObject primaryCitation ) {
        this.primaryCitation = primaryCitation;
    }

    /**
     * @return Identifier in a second database, if available. For example, if the data are in GEO and in ArrayExpress,
     *         this might
     *         be a link to the ArrayExpress version.
     */
    public String getSecondaryAccession() {
        return this.secondaryAccession;
    }

    public void setSecondaryAccession( String secondaryAccession ) {
        this.secondaryAccession = secondaryAccession;
    }

    public String getSecondaryExternalDatabase() {
        return this.secondaryExternalDatabase;
    }

    public void setSecondaryExternalDatabase( String secondaryExternalDatabase ) {
        this.secondaryExternalDatabase = secondaryExternalDatabase;
    }

    public String getSecondaryExternalUri() {
        return this.secondaryExternalUri;
    }

    public void setSecondaryExternalUri( String secondaryExternalUri ) {
        this.secondaryExternalUri = secondaryExternalUri;
    }

    public String getQChtml() {
        return QChtml;
    }

    public void setQChtml( String qChtml ) {
        QChtml = qChtml;
    }

    public boolean isHasBatchInformation() {
        return hasBatchInformation;
    }

    public void setHasBatchInformation( boolean hasBatchInformation ) {
        this.hasBatchInformation = hasBatchInformation;
    }

    public String getLastArrayDesignUpdateDate() {
        return lastArrayDesignUpdateDate;
    }

    public void setLastArrayDesignUpdateDate( String lastArrayDesignUpdateDate ) {
        this.lastArrayDesignUpdateDate = lastArrayDesignUpdateDate;
    }

    /**
     * @return the expressionExperimentSets
     */
    public Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets() {
        return expressionExperimentSets;
    }

    /**
     * @param expressionExperimentSets the expressionExperimentSets to set
     */
    public void setExpressionExperimentSets( Collection<ExpressionExperimentSetValueObject> expressionExperimentSets ) {
        this.expressionExperimentSets = expressionExperimentSets;
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
     * @see    #getTroubleDetails(boolean)
     */
    @Override
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }

    /**
     * Checks trouble of this EE and all its Array Designs and returns compilation of trouble info.
     * MAKE SURE to fill the Array Design variable first!
     *
     * @param  htmlEscape whether to escape the returned string for html
     * @return            string with trouble info.
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

    /**
     * @return true if this EE is troubled, disregards any platform trouble that might be present.
     */
    @SuppressWarnings("unused") // Used in Curation tab, see CurationTools.js
    public Boolean getActuallyTroubled() {
        return super.getTroubled();
    }

    /**
     * @return true, if the any of the platforms of this EE is troubled. False otherwise, even if this EE itself is
     *         troubled.
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
}
