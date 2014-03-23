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
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Status;

/**
 * <p>
 * A Characteristic that uses terms from ontologies or controlled vocabularies. These Characteristics can be chained
 * together in complex ways.
 * </p>
 * <p>
 * A Characteristic can form an RDF-style triple, with a Term (the subject) a CharacteristicProperty (the predicate) and
 * an object (either another Characteristic or a DataProperty to hold a literal value).
 * </p>
 */
public abstract class VocabCharacteristic extends CharacteristicImpl {

    /**
     * Constructs new instances of {@link VocabCharacteristic}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link VocabCharacteristic}.
         */
        public static VocabCharacteristic newInstance() {
            return new VocabCharacteristicImpl();
        }

        public static VocabCharacteristic newInstance( String name, String description, AuditTrail auditTrail,
                Status status, String value, String valueUri, String category, String categoryUri,
                GOEvidenceCode evidenceCode ) {
            final VocabCharacteristic entity = new VocabCharacteristicImpl();
            entity.setName( name );
            entity.setDescription( description );
            entity.setAuditTrail( auditTrail );
            entity.setStatus( status );
            entity.setCategoryUri( categoryUri );
            entity.setValueUri( valueUri );
            entity.setValue( value );
            entity.setCategory( category );
            entity.setEvidenceCode( evidenceCode );
            return entity;
        }

    }

    private String valueUri;

    /**
     * This can be a URI to any resources that describes the characteristic. Often it might be a URI to an OWL ontology
     * term. If the URI is an instance of an abstract class, the classUri should be filled in with the URI for the
     * abstract class.
     */
    public String getValueUri() {
        return this.valueUri;
    }

    public void setValueUri( String valueUri ) {
        this.valueUri = valueUri;
    }

}