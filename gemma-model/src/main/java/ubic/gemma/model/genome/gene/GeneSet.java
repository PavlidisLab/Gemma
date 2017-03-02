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

package ubic.gemma.model.genome.gene;

import gemma.gsec.model.SecuredNotChild;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * A grouping of genes that share a common relationship
 */
public abstract class GeneSet extends Auditable implements SecuredNotChild {

    /**
     * 
     */
    private static final long serialVersionUID = 4357218100681569138L;

    /**
     * Constructs new instances of {@link GeneSet}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link GeneSet}.
         */
        public static GeneSet newInstance() {
            return new GeneSetImpl();
        }

    }

    private Collection<Characteristic> characteristics = new HashSet<>();

    private DatabaseEntry sourceAccession;

    private Collection<BibliographicReference> literatureSources = new HashSet<>();

    private Collection<GeneSetMember> members = new java.util.HashSet<>();

    /**
     * 
     */
    public Collection<Characteristic> getCharacteristics() {
        return this.characteristics;
    }

    /**
     * 
     */
    public Collection<BibliographicReference> getLiteratureSources() {
        return this.literatureSources;
    }

    /**
     * 
     */
    public Collection<GeneSetMember> getMembers() {
        return this.members;
    }

    /**
     * 
     */
    public DatabaseEntry getSourceAccession() {
        return this.sourceAccession;
    }

    public void setCharacteristics( Collection<Characteristic> characteristics ) {
        this.characteristics = characteristics;
    }

    public void setLiteratureSources( Collection<BibliographicReference> literatureSources ) {
        this.literatureSources = literatureSources;
    }

    public void setMembers( Collection<GeneSetMember> members ) {
        this.members = members;
    }

    public void setSourceAccession( DatabaseEntry sourceAccession ) {
        this.sourceAccession = sourceAccession;
    }

}