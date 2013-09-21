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

import java.io.Serializable;

import ubic.gemma.model.genome.Gene;

/**
 * Entity representing a relationship between two genes. This abstract class is immutable, and it is recommended that
 * subclasses be immutable as well.
 */
public abstract class Gene2GeneAssociation implements Serializable {

    final private Gene firstGene = null;

    final private Long id = null;

    final private Gene secondGene = null;

    /**
     * 
     */
    public Gene getFirstGene() {
        return this.firstGene;
    }

    public Long getId() {
        return id;
    }

    /**
     * 
     */
    public Gene getSecondGene() {
        return this.secondGene;
    }

}