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
package ubic.gemma.core.loader.expression.simple.model;

import ubic.gemma.core.job.TaskCommand;
import ubic.gemma.core.job.Task;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;
import java.util.HashSet;

/**
 * Represents the basic data to enter about an expression experiment when starting from a delimited file of data
 *
 * @author pavlidis
 */
@SuppressWarnings("unused") // Frontend use
public class SimpleExpressionExperimentMetaData extends TaskCommand {

    private static final long serialVersionUID = 1L;

    // for Ajax
    private Collection<Long> arrayDesignIds;

    private Collection<ArrayDesign> arrayDesigns;

    private Taxon taxon;

    private Long taxonId;

    private String description;

    private String experimentalDesignDescription = "No information available";

    private String experimentalDesignName = "Unknown";

    private DatabaseEntry externalReference;

    private GeneralType generalType;

    private Boolean isBatchCorrected = Boolean.FALSE;

    private Boolean isMaskedPreferred = Boolean.FALSE;

    private Boolean isRatio = Boolean.FALSE;

    private String name;

    private boolean probeIdsAreImageClones;

    /**
     * Publication reference.
     */
    private Integer pubMedId = null;

    private String quantitationTypeDescription;

    private String quantitationTypeName;

    private ScaleType scale;

    private String shortName;

    private String sourceUrl;

    private TechnologyType technologyType;

    private StandardQuantitationType type;

    /**
     * The person who loaded this data.
     */
    private Contact user;

    public SimpleExpressionExperimentMetaData() {
        super();
        this.arrayDesigns = new HashSet<>();
        this.arrayDesignIds = new HashSet<>();
    }

    public Collection<Long> getArrayDesignIds() {
        return arrayDesignIds;
    }

    public void setArrayDesignIds( Collection<Long> arrayDesignIds ) {
        this.arrayDesignIds = arrayDesignIds;
    }

    public Collection<ArrayDesign> getArrayDesigns() {
        return this.arrayDesigns;
    }

    public void setArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    public String getExperimentalDesignDescription() {
        return experimentalDesignDescription;
    }

    public void setExperimentalDesignDescription( String experimentalDesignDescription ) {
        this.experimentalDesignDescription = experimentalDesignDescription;
    }

    public String getExperimentalDesignName() {
        return experimentalDesignName;
    }

    public void setExperimentalDesignName( String experimentalDesignName ) {
        this.experimentalDesignName = experimentalDesignName;
    }

    /**
     * @return the externalReference
     */
    public DatabaseEntry getExternalReference() {
        return this.externalReference;
    }

    /**
     * @param externalReference the externalReference to set
     */
    public void setExternalReference( DatabaseEntry externalReference ) {
        this.externalReference = externalReference;
    }

    /**
     * @return the generalType
     */
    public GeneralType getGeneralType() {
        return this.generalType;
    }

    /**
     * @param generalType the generalType to set
     */
    public void setGeneralType( GeneralType generalType ) {
        this.generalType = generalType;
    }

    /**
     * @return the isBatchCorrected
     */
    public Boolean getIsBatchCorrected() {
        return isBatchCorrected;
    }

    /**
     * @param isBatchCorrected the isBatchCorrected to set
     */
    public void setIsBatchCorrected( Boolean isBatchCorrected ) {
        this.isBatchCorrected = isBatchCorrected;
    }

    public Boolean getIsMaskedPreferred() {
        return isMaskedPreferred;
    }

    public void setIsMaskedPreferred( Boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public Boolean getIsRatio() {
        return isRatio;
    }

    public void setIsRatio( Boolean isRatio ) {
        this.isRatio = isRatio;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return the pubMedId
     */
    public Integer getPubMedId() {
        return this.pubMedId;
    }

    /**
     * @param pubMedId the pubMedId to set
     */
    public void setPubMedId( Integer pubMedId ) {
        this.pubMedId = pubMedId;
    }

    /**
     * @return the quantitationTypeDescription
     */
    public String getQuantitationTypeDescription() {
        return this.quantitationTypeDescription;
    }

    /**
     * @param quantitationTypeDescription the quantitationTypeDescription to set
     */
    public void setQuantitationTypeDescription( String quantitationTypeDescription ) {
        this.quantitationTypeDescription = quantitationTypeDescription;
    }

    /**
     * @return the quantitationTypeName
     */
    public String getQuantitationTypeName() {
        return this.quantitationTypeName;
    }

    /**
     * @param quantitationTypeName the quantitationTypeName to set
     */
    public void setQuantitationTypeName( String quantitationTypeName ) {
        this.quantitationTypeName = quantitationTypeName;
    }

    /**
     * @return the scale
     */
    public ScaleType getScale() {
        return this.scale;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale( ScaleType scale ) {
        this.scale = scale;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl( String sourceUrl ) {
        this.sourceUrl = sourceUrl;
    }

    @Override
    public Class<? extends Task<? extends TaskCommand>> getTaskClass() {
        return null;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public TechnologyType getTechnologyType() {
        return technologyType;
    }

    public void setTechnologyType( TechnologyType technologyType ) {
        this.technologyType = technologyType;
    }

    /**
     * @return the type
     */
    public StandardQuantitationType getType() {
        return this.type;
    }

    /**
     * @param type the type to set
     */
    public void setType( StandardQuantitationType type ) {
        this.type = type;
    }

    /**
     * @return the user
     */
    public Contact getUser() {
        return this.user;
    }

    /**
     * @param user the user to set
     */
    public void setUser( Contact user ) {
        this.user = user;
    }

    public boolean isProbeIdsAreImageClones() {
        return probeIdsAreImageClones;
    }

    public void setProbeIdsAreImageClones( boolean probeIdsAreImageClones ) {
        this.probeIdsAreImageClones = probeIdsAreImageClones;
    }

}
