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

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * @version $Id$
 * @author paul
 */
public class ExpressionExperimentDetailsValueObject extends
        ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1219449523930648392L;

    private java.lang.String description;

    private java.lang.String secondaryAccession;

    private java.lang.String secondaryExternalDatabase;

    private java.lang.String secondaryExternalUri;

    private String primaryCitation;

    private Collection<ArrayDesignValueObject> arrayDesigns;

    public Collection<ArrayDesignValueObject> getArrayDesigns() {
        return arrayDesigns;
    }

    public void setArrayDesigns( Collection<ArrayDesignValueObject> arrayDesigns ) {
        this.arrayDesigns = arrayDesigns;
    }

    public String getPrimaryCitation() {
        return primaryCitation;
    }

    public void setPrimaryCitation( String primaryCitation ) {
        this.primaryCitation = primaryCitation;
    }

    public ExpressionExperimentDetailsValueObject() {
        super();
    }

    public ExpressionExperimentDetailsValueObject( ExpressionExperimentValueObject otherBean ) {
        super( otherBean );
    }

    /**
     * 
     */
    public java.lang.String getDescription() {
        return this.description;
    }

    /**
     * <p>
     * Identifer in a second database, if available. For example, if the data are in GEO and in ArrayExpress, this might
     * be a link to the ArrayExpress version.
     * </p>
     */
    public java.lang.String getSecondaryAccession() {
        return this.secondaryAccession;
    }

    /**
     * 
     */
    public java.lang.String getSecondaryExternalDatabase() {
        return this.secondaryExternalDatabase;
    }

    /**
     * 
     */
    public java.lang.String getSecondaryExternalUri() {
        return this.secondaryExternalUri;
    }

    public void setDescription( java.lang.String description ) {
        this.description = description;
    }

    public void setSecondaryAccession( java.lang.String secondaryAccession ) {
        this.secondaryAccession = secondaryAccession;
    }

    public void setSecondaryExternalDatabase( java.lang.String secondaryExternalDatabase ) {
        this.secondaryExternalDatabase = secondaryExternalDatabase;
    }

    public void setSecondaryExternalUri( java.lang.String secondaryExternalUri ) {
        this.secondaryExternalUri = secondaryExternalUri;
    }

}