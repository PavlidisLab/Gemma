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
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

import java.util.Collection;

/**
 * @author paul
 */
public class ExpressionExperimentDetailsValueObject extends ExpressionExperimentValueObject {

    private static final String TROUBLE_DETAIL_PLATF = "Platform problems: ";
    private static final String TROUBLE_DETAIL_SEPARATOR = " | ";
    private static final long serialVersionUID = -1219449523930648392L;

    private String description;

    private String secondaryAccession;

    private String secondaryExternalDatabase;

    private String secondaryExternalUri;

    private CitationValueObject primaryCitation;

    private Collection<ArrayDesignValueObject> arrayDesigns;

    private String parentTaxon;

    private Boolean hasMultiplePreferredQuantitationTypes = false;

    private Boolean hasMultipleTechnologyTypes = false;

    private String QChtml;

    private boolean hasBatchInformation;

    private String batchConfound;

    private String batchEffect;

    private String lastArrayDesignUpdateDate;

    private Collection<ExpressionExperimentSetValueObject> expressionExperimentSets;

    /*
     * TODO: add Geeq. Either just the scores, or a Geeq value object?
     */

    private boolean reprocessedFromRawData;

    public ExpressionExperimentDetailsValueObject() {
        super();
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject otherBean ) {
        super( otherBean );
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

    public String getDescription() {
        return this.description;
    }

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
     * <p>
     * Identifer in a second database, if available. For example, if the data are in GEO and in ArrayExpress, this might
     * be a link to the ArrayExpress version.
     * </p>
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

    public String getParentTaxon() {
        return parentTaxon;
    }

    public void setParentTaxon( String parentTaxon ) {
        this.parentTaxon = parentTaxon;
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

    public String getBatchConfound() {
        return batchConfound;
    }

    public void setBatchConfound( String batchConfound ) {
        this.batchConfound = batchConfound;
    }

    public String getBatchEffect() {
        return batchEffect;
    }

    public void setBatchEffect( String batchEffect ) {
        this.batchEffect = batchEffect;
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
        if ( !troubled ) {
            for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
                if ( ad.getTroubled() )
                    return true;
            }
        }
        return troubled;
    }

    /**
     * @return true if this EE is troubled, disregards any platform trouble that might be present.
     */
    @SuppressWarnings("unused")// Used in Curation tab, see CurationTools.js
    public Boolean getActuallyTroubled() {
        return super.getTroubled();
    }

    /**
     * @return true, if the any of the platforms of this EE is troubled. False otherwise, even if this EE itself is troubled.
     */
    @SuppressWarnings("unused")// Used in Curation tab, see CurationTools.js
    public Boolean getPlatformTroubled() {
        for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
            if ( ad.getTroubled() ) {
                return true;
            }
        }
        return false;
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
        String adTroubleDetails = null;
        String finalTroubleDetails = "";
        boolean adTroubled = false;

        if ( super.getTroubled() )
            eeTroubleDetails = super.getTroubleDetails( htmlEscape );

        for ( ArrayDesignValueObject ad : this.arrayDesigns ) {
            if ( ad.getTroubled() ) {
                adTroubled = true;
                if ( adTroubleDetails == null ) {
                    adTroubleDetails = TROUBLE_DETAIL_PLATF;
                } else {
                    adTroubleDetails += TROUBLE_DETAIL_SEPARATOR;
                }
                adTroubleDetails += ad.getTroubleDetails( false );
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
     * @return html-escaped string with trouble info.
     * @see #getTroubleDetails(boolean)
     */
    public String getTroubleDetails() {
        return this.getTroubleDetails( true );
    }
}