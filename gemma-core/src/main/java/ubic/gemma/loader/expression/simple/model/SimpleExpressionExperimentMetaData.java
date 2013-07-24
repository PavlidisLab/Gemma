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
package ubic.gemma.loader.expression.simple.model;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.tasks.Task;

/**
 * Represents the basic data to enter about an expression experiment when starting from a delimited file of data
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionExperimentMetaData extends TaskCommand {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String name;

    private String shortName;

    private String description;

    /**
     * The person who loaded this data.
     */
    private Contact user;

    private String quantitationTypeName;

    private String quantitationTypeDescription;

    private String experimentalDesignName = "Unknown";

    private String experimentalDesignDescription = "No information available";

    private ScaleType scale;

    private GeneralType generalType;

    private StandardQuantitationType type;

    private Boolean isRatio = Boolean.FALSE;

    private Boolean isMaskedPreferred = Boolean.FALSE;

    private DatabaseEntry externalReference;

    private String sourceUrl;

    private boolean probeIdsAreImageClones;

    private TechnologyType technologyType;

    /**
     * Publication reference.
     */
    private Integer pubMedId = null;

    Collection<ArrayDesign> arrayDesigns;

    // for Ajax
    Collection<Long> arrayDesignIds;

    Taxon taxon;

    Long taxonId;

    @Override
    public Class<? extends Task<TaskResult, ? extends TaskCommand>> getTaskClass() {
        return null;
    }

    public SimpleExpressionExperimentMetaData() {
        super();
        this.arrayDesigns = new HashSet<ArrayDesign>();
        this.arrayDesignIds = new HashSet<Long>();
    }

    public Collection<Long> getArrayDesignIds() {
        return arrayDesignIds;
    }

    public Collection<ArrayDesign> getArrayDesigns() {
        return this.arrayDesigns;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return this.description;
    }

    public String getExperimentalDesignDescription() {
        return experimentalDesignDescription;
    }

    public String getExperimentalDesignName() {
        return experimentalDesignName;
    }

    /**
     * @return the externalReference
     */
    public DatabaseEntry getExternalReference() {
        return this.externalReference;
    }

    /**
     * @return the generalType
     */
    public GeneralType getGeneralType() {
        return this.generalType;
    }

    public Boolean getIsMaskedPreferred() {
        return isMaskedPreferred;
    }

    public Boolean getIsRatio() {
        return isRatio;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the pubMedId
     */
    public Integer getPubMedId() {
        return this.pubMedId;
    }

    /**
     * @return the quantitationTypeDescription
     */
    public String getQuantitationTypeDescription() {
        return this.quantitationTypeDescription;
    }

    /**
     * @return the quantitationTypeName
     */
    public String getQuantitationTypeName() {
        return this.quantitationTypeName;
    }

    /**
     * @return the scale
     */
    public ScaleType getScale() {
        return this.scale;
    }

    public String getShortName() {
        return shortName;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public TechnologyType getTechnologyType() {
        return technologyType;
    }

    /**
     * @return the type
     */
    public StandardQuantitationType getType() {
        return this.type;
    }

    /**
     * @return the user
     */
    public Contact getUser() {
        return this.user;
    }

    public boolean isProbeIdsAreImageClones() {
        return probeIdsAreImageClones;
    }

    public void setArrayDesignIds( Collection<Long> arrayDesignIds ) {
        this.arrayDesignIds = arrayDesignIds;
    }

    public void setArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    /**
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    public void setExperimentalDesignDescription( String experimentalDesignDescription ) {
        this.experimentalDesignDescription = experimentalDesignDescription;
    }

    public void setExperimentalDesignName( String experimentalDesignName ) {
        this.experimentalDesignName = experimentalDesignName;
    }

    /**
     * @param externalReference the externalReference to set
     */
    public void setExternalReference( DatabaseEntry externalReference ) {
        this.externalReference = externalReference;
    }

    /**
     * @param generalType the generalType to set
     */
    public void setGeneralType( GeneralType generalType ) {
        this.generalType = generalType;
    }

    public void setIsMaskedPreferred( Boolean isMaskedPreferred ) {
        this.isMaskedPreferred = isMaskedPreferred;
    }

    public void setIsRatio( Boolean isRatio ) {
        this.isRatio = isRatio;
    }

    /**
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    public void setProbeIdsAreImageClones( boolean probeIdsAreImageClones ) {
        this.probeIdsAreImageClones = probeIdsAreImageClones;
    }

    /**
     * @param pubMedId the pubMedId to set
     */
    public void setPubMedId( Integer pubMedId ) {
        this.pubMedId = pubMedId;
    }

    /**
     * @param quantitationTypeDescription the quantitationTypeDescription to set
     */
    public void setQuantitationTypeDescription( String quantitationTypeDescription ) {
        this.quantitationTypeDescription = quantitationTypeDescription;
    }

    /**
     * @param quantitationTypeName the quantitationTypeName to set
     */
    public void setQuantitationTypeName( String quantitationTypeName ) {
        this.quantitationTypeName = quantitationTypeName;
    }

    /**
     * @param scale the scale to set
     */
    public void setScale( ScaleType scale ) {
        this.scale = scale;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSourceUrl( String sourceUrl ) {
        this.sourceUrl = sourceUrl;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTechnologyType( TechnologyType technologyType ) {
        this.technologyType = technologyType;
    }

    /**
     * @param type the type to set
     */
    public void setType( StandardQuantitationType type ) {
        this.type = type;
    }

    /**
     * @param user the user to set
     */
    public void setUser( Contact user ) {
        this.user = user;
    }

}
