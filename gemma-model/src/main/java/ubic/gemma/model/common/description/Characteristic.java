/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

package ubic.gemma.model.common.description;

/**
 * Instances of this are used to describe other entities. This base class is just a characteristic that is simply a
 * 'tag' of free text.
 */
public abstract class Characteristic extends ubic.gemma.model.common.Auditable {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.description.Characteristic}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.Characteristic}.
         */
        public static ubic.gemma.model.common.description.Characteristic newInstance() {
            return new ubic.gemma.model.common.description.CharacteristicImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.Characteristic}, taking all possible
         * properties (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.common.description.Characteristic newInstance( String name, String description,
                ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail,
                ubic.gemma.model.common.auditAndSecurity.Status status, String value, String category,
                ubic.gemma.model.association.GOEvidenceCode evidenceCode ) {
            final ubic.gemma.model.common.description.Characteristic entity = new ubic.gemma.model.common.description.CharacteristicImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setAuditTrail( auditTrail );
            entity.setStatus( status );
            entity.setValue( value );
            entity.setCategory( category );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4317228624899157443L;
    private String value;

    private String category;

    private ubic.gemma.model.association.GOEvidenceCode evidenceCode;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Characteristic() {
    }

    /**
     * either the human readable form of the classUri or a free text version if no classUri exists
     */
    public String getCategory() {
        return this.category;
    }

    /**
     * 
     */
    public ubic.gemma.model.association.GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    /**
     * The human-readable term (e.g., "OrganismPart"; "kinase")
     */
    public String getValue() {
        return this.value;
    }

    public void setCategory( String category ) {
        this.category = category;
    }

    public void setEvidenceCode( ubic.gemma.model.association.GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setValue( String value ) {
        this.value = value;
    }

}