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
public abstract class Gene2GeneAssociation extends ubic.gemma.model.association.Relationship {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4065373891743627781L;

    private ubic.gemma.model.genome.Gene secondGene;
    private ubic.gemma.model.genome.Gene firstGene;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Gene2GeneAssociation() {
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getFirstGene() {
        return this.firstGene;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getSecondGene() {
        return this.secondGene;
    }

    public void setFirstGene( ubic.gemma.model.genome.Gene firstGene ) {
        this.firstGene = firstGene;
    }

    public void setSecondGene( ubic.gemma.model.genome.Gene secondGene ) {
        this.secondGene = secondGene;
    }

}