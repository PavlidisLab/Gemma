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
package ubic.gemma.model.association;

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;

import ubic.gemma.core.lang.Nullable;
import java.io.Serializable;

@SuppressWarnings("unused") // fields are assigned by Hibernate
public class Gene2GOAssociation implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -710930089869830248L;

    private Long id;
    private Gene gene;
    private Characteristic ontologyEntry;
    @Nullable
    private GOEvidenceCode evidenceCode;

    @Override
    public Long getId() {
        return id;
    }

    public Gene getGene() {
        return this.gene;
    }

    public Characteristic getOntologyEntry() {
        return this.ontologyEntry;
    }

    @Nullable
    public GOEvidenceCode getEvidenceCode() {
        return this.evidenceCode;
    }

    @Override
    public String toString() {
        if ( gene == null || ontologyEntry == null ) return "?";
        return gene + " ---> " + ontologyEntry.getValue() + " [" + ontologyEntry.getValueUri() + "]";
    }

    public static final class Factory {
        public static Gene2GOAssociation newInstance( Gene gene, Characteristic ontologyEntry,
                GOEvidenceCode evidenceCode ) {
            final Gene2GOAssociation entity = new Gene2GOAssociation();
            entity.gene = gene;
            entity.ontologyEntry = ontologyEntry;
            entity.evidenceCode = evidenceCode;
            return entity;
        }
    }
}