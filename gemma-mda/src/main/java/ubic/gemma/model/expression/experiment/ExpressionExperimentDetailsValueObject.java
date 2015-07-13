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

import java.util.Collection;

import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * @version $Id$
 * @author paul
 */
public class ExpressionExperimentDetailsValueObject extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject {

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

    public boolean getReprocessedFromRawData() {
        return reprocessedFromRawData;
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

    public ExpressionExperimentDetailsValueObject() {
        super();
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject otherBean ) {
        super( otherBean );
    }

    public Collection<ArrayDesignValueObject> getArrayDesigns() {
        return arrayDesigns;
    }

    /**
     * 
     */
    public String getDescription() {
        return this.description;
    }

    public CitationValueObject getPrimaryCitation() {
        return primaryCitation;
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

    /**
     * 
     */
    public String getSecondaryExternalDatabase() {
        return this.secondaryExternalDatabase;
    }

    /**
     * 
     */
    public String getSecondaryExternalUri() {
        return this.secondaryExternalUri;
    }

    public void setArrayDesigns( Collection<ArrayDesignValueObject> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setPrimaryCitation( CitationValueObject primaryCitation ) {
        this.primaryCitation = primaryCitation;
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

    public void setParentTaxon( String parentTaxon ) {
        this.parentTaxon = parentTaxon;
    }

    public String getParentTaxon() {
        return parentTaxon;
    }

    public void setQChtml( String qChtml ) {
        QChtml = qChtml;
    }

    public String getQChtml() {
        return QChtml;
    }

    public boolean isHasBatchInformation() {
        return hasBatchInformation;
    }

    public void setHasBatchInformation( boolean hasBatchInformation ) {
        this.hasBatchInformation = hasBatchInformation;
    }

    public void setReprocessedFromRawData( boolean reprocessedFromRawData ) {
        this.reprocessedFromRawData = reprocessedFromRawData;
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

    public void setLastArrayDesignUpdateDate( String lastArrayDesignUpdateDate ) {
        this.lastArrayDesignUpdateDate = lastArrayDesignUpdateDate;
    }

    public String getLastArrayDesignUpdateDate() {
        return lastArrayDesignUpdateDate;
    }

    /**
     * @param expressionExperimentSets the expressionExperimentSets to set
     */
    public void setExpressionExperimentSets( Collection<ExpressionExperimentSetValueObject> expressionExperimentSets ) {
        this.expressionExperimentSets = expressionExperimentSets;
    }

    /**
     * @return the expressionExperimentSets
     */
    public Collection<ExpressionExperimentSetValueObject> getExpressionExperimentSets() {
        return expressionExperimentSets;
    }

}