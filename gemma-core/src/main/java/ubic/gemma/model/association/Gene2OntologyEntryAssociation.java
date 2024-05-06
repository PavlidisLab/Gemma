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

import java.io.Serializable;

public abstract class Gene2OntologyEntryAssociation implements Identifiable, Serializable {

    private static final long serialVersionUID = -6097916172357707966L;

    final private Long id = null;
    final private Gene gene = null;
    final private Characteristic ontologyEntry = null;

    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    @Override
    public Long getId() {
        return id;
    }

    public Characteristic getOntologyEntry() {
        return this.ontologyEntry;
    }

    @Override
    public String toString() {
        if ( gene == null || ontologyEntry == null ) return "?";
        return gene + " ---> " + ontologyEntry.getValue() + " [" + ontologyEntry.getValueUri() + "]";
    }

}