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

@Deprecated
public class PhenotypeAssociationPublication implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4156623260205197700L;

    private Long id;
    private String type;
    private BibliographicReference citation;

    @SuppressWarnings("WeakerAccess") // Required by Spring
    public PhenotypeAssociationPublication() {
    }

    public BibliographicReference getCitation() {
        return this.citation;
    }

    public void setCitation( BibliographicReference citation ) {
        this.citation = citation;
    }

    public String getType() {
        return this.type;
    }

    public void setType( String type ) {
        this.type = type;
    }

    public Long getId() {
        return this.id;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void setId( Long id ) {
        this.id = id;
    }

    public static final class Factory {

        public static PhenotypeAssociationPublication newInstance() {
            return new PhenotypeAssociationPublication();
        }

    }

}
