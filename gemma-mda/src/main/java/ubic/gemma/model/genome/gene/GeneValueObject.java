/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

package ubic.gemma.model.genome.gene;

/**
 * Took out of the model, can edit by hand
 * 
 * @author kelsey
 * @version
 */
public class GeneValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7098036090107647318L;

    private java.lang.Long id;

    private java.lang.String name;

    private java.lang.String ncbiId;

    private java.lang.String officialSymbol;

    private java.lang.String officialName;

    private java.lang.String description;

    public GeneValueObject() {
    }

    /**
     * Copies constructor from other GeneValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws java.lang.NullPointerException if the argument is <code>null</code>
     */
    public GeneValueObject( GeneValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), otherBean.getNcbiId(), otherBean.getOfficialSymbol(), otherBean
                .getOfficialName(), otherBean.getDescription() );
    }

    public GeneValueObject( java.lang.Long id, java.lang.String name, java.lang.String ncbiId,
            java.lang.String officialSymbol, java.lang.String officialName, java.lang.String description ) {
        this.id = id;
        this.name = name;
        this.ncbiId = ncbiId;
        this.officialSymbol = officialSymbol;
        this.officialName = officialName;
        this.description = description;
    }

    /**
     * Copies all properties from the argument value object into this value object.
     */
    public void copy( GeneValueObject otherBean ) {
        if ( otherBean != null ) {
            this.setId( otherBean.getId() );
            this.setName( otherBean.getName() );
            this.setNcbiId( otherBean.getNcbiId() );
            this.setOfficialSymbol( otherBean.getOfficialSymbol() );
            this.setOfficialName( otherBean.getOfficialName() );
            this.setDescription( otherBean.getDescription() );
        }
    }

    /**
     * 
     */
    public java.lang.String getDescription() {
        return this.description;
    }

    /**
     * 
     */
    public java.lang.Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public java.lang.String getName() {
        return this.name;
    }

    /**
     * 
     */
    public java.lang.String getNcbiId() {
        return this.ncbiId;
    }

    /**
     * 
     */
    public java.lang.String getOfficialName() {
        return this.officialName;
    }

    /**
     * 
     */
    public java.lang.String getOfficialSymbol() {
        return this.officialSymbol;
    }

    public void setDescription( java.lang.String description ) {
        this.description = description;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    public void setName( java.lang.String name ) {
        this.name = name;
    }

    public void setNcbiId( java.lang.String ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public void setOfficialName( java.lang.String officialName ) {
        this.officialName = officialName;
    }

    public void setOfficialSymbol( java.lang.String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    // ubic.gemma.model.genome.gene.GeneValueObject value-object java merge-point
}