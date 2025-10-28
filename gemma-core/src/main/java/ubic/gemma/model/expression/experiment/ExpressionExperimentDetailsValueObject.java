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
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringEscapeUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;

/**
 * @author paul
 */
@SuppressWarnings("unused") // ValueObject accessed from JS
@Getter
@Setter
public class ExpressionExperimentDetailsValueObject extends ExpressionExperimentValueObject {

    private static final long serialVersionUID = -1219449523930648392L;
    private static final String TROUBLE_DETAIL_PLATF = "Platform problems: ";
    private static final String TROUBLE_DETAIL_SEPARATOR = " | ";

    @Nullable
    private Collection<ArrayDesignValueObject> arrayDesigns;
    /**
     * The date the platform associated with the experiment was last updated.
     * <p>
     * If there are multiple platforms this should be the date of the most recent modification of them. This is
     * used to help flag experiments that need re-analysis due to changes in the underlying array design(s).
     */
    private Date dateArrayDesignLastUpdated;
    private Date dateBatchFetch;
    /**
     * The date this object was generated.
     */
    private Date dateCached;
    private Date dateDifferentialAnalysis;
    private Date dateLinkAnalysis;
    private Date dateMissingValueAnalysis;
    private Date datePcaAnalysis;
    private Date dateProcessedDataVectorComputation;
    private Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses = new HashSet<>();
    /**
     * EE sets this experiment is part of.
     */
    @Nullable
    private Collection<ExpressionExperimentSetValueObject> expressionExperimentSets;
    /**
     * FIXME: rename this to hasUsableBatchInformation
     */
    private boolean hasBatchInformation;
    private boolean hasBothIntensities = false;
    private boolean hasCoexpressionAnalysis = false;
    private boolean hasDifferentialExpressionAnalysis = false;
    /**
     * Indicate if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    private boolean hasEitherIntensity = false;
    private boolean hasMultiplePreferredQuantitationTypes = false;
    private boolean hasMultipleTechnologyTypes = false;
    private boolean isRNASeq = false;
    private boolean isReprocessedFromRawData;

    // audit events
    private String batchFetchEventType;
    private String pcaAnalysisEventType;
    private String processedDataVectorComputationEventType;
    private String lastArrayDesignUpdateDate;
    private String linkAnalysisEventType;
    private String missingValueAnalysisEventType;
    /**
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life
     * of data set, so this is a collection of AuditEvents.
     */
    private Collection<AuditEventValueObject> sampleRemovedFlags;

    /**
     * The number of terms (Characteristics) the experiment has to describe it.
     */
    private Long numAnnotations;
    /**
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials).
     */
    private Long numPopulatedFactors;

    // if it was switched
    private Collection<ArrayDesignValueObject> originalPlatforms;

    // if it was split.
    /**
     * Experiments that are related to this one via the splitting of a source experiment.
     */
    private Collection<ExpressionExperimentValueObject> otherParts = new HashSet<>();

    private CitationValueObject primaryCitation;
    @Nullable
    private Integer pubmedId;
    /**
     * Identifier in a second database, if available. For example, if the data are in GEO and in ArrayExpress,
     * this might be a link to the ArrayExpress version.
     */
    @Nullable
    private String secondaryAccession;
    @Nullable
    private String secondaryExternalDatabase;
    @Nullable
    private String secondaryExternalUri;

    private String QChtml;

    /**
     * Indicate if this experiment is a single-cell experiment.
     */
    private boolean isSingleCell;
    /**
     * The number of cells this experiment has.
     * @see ExpressionExperiment#getNumberOfCells()
     */
    @Nullable
    private Integer numberOfCells;
    /**
     * The number of cell IDs that the preferred single-cell dimension has.
     * @see SingleCellDimension#getNumberOfCellIds()
     */
    private Integer numberOfCellIds;
    /**
     * Indicate if this experiment has a Cell Browser associated with it.
     */
    private boolean hasCellBrowser;
    /**
     * URL for the Cell Browser, if available.
     */
    @Nullable
    private String cellBrowserUrl;
    private String cellBrowserDatasetName;

    /**
     * Font to use when rendering diagnostic plots, etc.
     */
    @Nullable
    private String font;

    /**
     * Required when using the class as a spring bean.
     */
    public ExpressionExperimentDetailsValueObject() {
        super();
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperiment ee ) {
        super( ee );
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperiment ee, AclObjectIdentity aoi,
            AclSid sid ) {
        super( ee, aoi, sid );
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject vo ) {
        super( vo );
    }

    /**
     * @return true if this EE is troubled, disregards any platform trouble that might be present.
     */
    public boolean getActuallyTroubled() {
        return super.getTroubled();
    }

    public boolean getHasBatchInformation() {
        return hasBatchInformation;
    }

    public boolean getHasBothIntensities() {
        return hasBothIntensities;
    }

    public boolean getHasCoexpressionAnalysis() {
        return hasCoexpressionAnalysis;
    }

    public boolean getHasDifferentialExpressionAnalysis() {
        return hasDifferentialExpressionAnalysis;
    }

    public boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    public boolean getHasMultiplePreferredQuantitationTypes() {
        return hasMultiplePreferredQuantitationTypes;
    }

    public boolean getHasMultipleTechnologyTypes() {
        return hasMultipleTechnologyTypes;
    }

    public boolean getIsSingleCell() {
        return isSingleCell;
    }

    public void setIsSingleCell( boolean isSingleCell ) {
        this.isSingleCell = isSingleCell;
    }

    public boolean getIsRNASeq() {
        return isRNASeq;
    }

    public void setIsRNASeq( boolean isRNASeq ) {
        this.isRNASeq = isRNASeq;
    }

    /**
     * @return true, if the any of the platforms of this EE is troubled. False otherwise, even if this EE itself is
     * troubled. May return null if the arrayDesigns are not set.
     */
    @Nullable
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

    public boolean getReprocessedFromRawData() {
        return isReprocessedFromRawData;
    }

    /**
     * @return true if the EE, or any of its Array Designs is troubled.
     */
    @Override
    public boolean getTroubled() {
        boolean troubled = super.getTroubled();
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

    /**
     * As a side effect, sets the technology type and taxon of this based on the first arrayDesign.
     *
     * @param arrayDesigns arrayDesign value objects to associate
     */
    public void setArrayDesigns( Collection<ArrayDesignValueObject> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
        if ( arrayDesigns.isEmpty() ) {
            // this seems to occur in one situation I'm aware of, sorting the
            // legacy experiment browser (expressionExperiment/showAllExpressionExperiments.html)
            // by Assay count. Other columns are okay.
            return;
        }
        ArrayDesignValueObject ad = arrayDesigns.iterator().next();
        setArrayDesignCount( ( long ) arrayDesigns.size() );
        this.setTechnologyType( ad.getTechnologyType() );
        this.setTaxonObject( ad.getTaxonObject() ); // FIXME still need the ID of the taxon, don't we?
    }
}
