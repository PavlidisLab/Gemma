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

import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Status;

/**
 * Instances of this are used to describe other entities. This base class is just a characteristic that is simply a
 * 'tag' of free text.
 */
public abstract class Characteristic extends Auditable {

    /**
     * 
     */
    private static final long serialVersionUID = -7242166109264718620L;

    /**
     * Constructs new instances of {@link Characteristic}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link Characteristic}.
         */
        public static Characteristic newInstance() {
            return new CharacteristicImpl();
        }

        /**
         * Constructs a new instance of {@link Characteristic}, taking all possible properties (except the
         * identifier(s))as arguments.
         */
        public static Characteristic newInstance( String name, String description, AuditTrail auditTrail, Status status,
                String value, String category, String categoryUri, GOEvidenceCode evidenceCode ) {
            final Characteristic entity = new CharacteristicImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setAuditTrail( auditTrail );
            entity.setStatus( status );
            entity.setValue( value );
            entity.setCategory( category );
            entity.setCategoryUri( categoryUri );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }
    }

    private String category;

    private String categoryUri;

    private GOEvidenceCode evidenceCode;

    private String value;

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
     * The URI of the class that this is an instance of. Will only be different from the termUri when the class is
     * effectively abstract, and this is a concrete instance. By putting the abstract class URI in the object we can
     * more readily group together Characteristics that are instances of the same class. For example: If the classUri is
     * "Sex", then the termUri might be "male" or "female" for various instances. Otherwise, the classUri and the
     * termUri can be the same; for example, for "Age", if the "Age" is defined through its properties declared as
     * associations with this.
     */
    public String getCategoryUri() {
        return this.categoryUri;
    }

    /**
     * 
     */
    public GOEvidenceCode getEvidenceCode() {
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

    public void setCategoryUri( String categoryUri ) {
        this.categoryUri = categoryUri;
    }

    public void setEvidenceCode( GOEvidenceCode evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public void setValue( String value ) {
        this.value = value;
    }

}