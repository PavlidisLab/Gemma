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

/**
 * 
 */
public abstract class Gene2OntologyEntryAssociation extends ubic.gemma.model.association.Relationship {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7343879715589606942L;

    private ubic.gemma.model.genome.Gene gene;
    private ubic.gemma.model.common.description.VocabCharacteristic ontologyEntry;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Gene2OntologyEntryAssociation() {
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    /**
     * 
     */
    public ubic.gemma.model.common.description.VocabCharacteristic getOntologyEntry() {
        return this.ontologyEntry;
    }

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    public void setOntologyEntry( ubic.gemma.model.common.description.VocabCharacteristic ontologyEntry ) {
        this.ontologyEntry = ontologyEntry;
    }

}