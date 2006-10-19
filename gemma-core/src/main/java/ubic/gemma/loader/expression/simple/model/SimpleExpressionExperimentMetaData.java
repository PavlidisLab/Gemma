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

import org.biomage.AuditAndSecurity.Contact;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Taxon;

/**
 * Represents the basic data to enter about an expression experiment when starting from a delimited file of data
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SimpleExpressionExperimentMetaData {

    private String name;

    private String description;

    /**
     * The person who loaded this data.
     */
    private Contact user;

    /**
     * Only handle a single array design
     */
    private String arrayDesignName;

    private String arrayDesignDescription;

    private String quantitationTypeName;

    private String quantitationTypeDescription;

    private ScaleType scale;

    private GeneralType generalType;

    private StandardQuantitationType type;

    /**
     * 
     */
    private String taxonName;

    private DatabaseEntry externalReference;

    /**
     * Publication reference.
     */
    private int pubMedId;

    /**
     * @return the arrayDesign
     */
    public String getArrayDesignName() {
        return this.arrayDesignName;
    }

    /**
     * @param arrayDesign the arrayDesign to set
     */
    public void setArrayDesignName( String arrayDesign ) {
        this.arrayDesignName = arrayDesign;
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
    public void setPubMedId( int pubMedId ) {
        this.pubMedId = pubMedId;
    }

    /**
     * @return the taxon
     */
    public String getTaxonName() {
        return this.taxonName;
    }

    /**
     * @param taxon the taxon to set
     */
    public void setTaxonName( String taxonName ) {
        this.taxonName = taxonName;
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
     * @return the arrayDesignDescription
     */
    public String getArrayDesignDescription() {
        return this.arrayDesignDescription;
    }

    /**
     * @param arrayDesignDescription the arrayDesignDescription to set
     */
    public void setArrayDesignDescription( String arrayDesignDescription ) {
        this.arrayDesignDescription = arrayDesignDescription;
    }

}
