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
package ubic.gemma.model.association.phenotype;

import ubic.gemma.model.common.description.BibliographicReference;

public abstract class PhenotypeAssociationPublication implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4156623260205197700L;

    private Long id;
    private String type;
    private BibliographicReference citation;

    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.description.ExternalDatabase}.
         */
        public static PhenotypeAssociationPublication newInstance() {
            return new PhenotypeAssociationPublicationImpl();
        }

    }

    public BibliographicReference getCitation() {
        return this.citation;
    }

    public void setCitation( BibliographicReference citation ) {
        this.citation = citation;
    }

    public PhenotypeAssociationPublication() {
    }

    public String getType() {
        return this.type;
    }

    public Long getId() {
        return this.id;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public void setId( Long id ) {
        this.id = id;
    }

}
