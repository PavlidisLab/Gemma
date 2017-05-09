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

import java.util.Collection;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.PhysicalLocation;

/**
 * 
 */
public abstract class GeneProduct extends ubic.gemma.model.genome.ChromosomeFeature {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.gene.GeneProduct}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.gene.GeneProduct}.
         */
        public static ubic.gemma.model.genome.gene.GeneProduct newInstance() {
            return new ubic.gemma.model.genome.gene.GeneProductImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6559927399916235369L;
    private ubic.gemma.model.genome.gene.GeneProductType type;

    private String ncbiGi;

    private Collection<DatabaseEntry> accessions = new java.util.HashSet<>();

    private Collection<PhysicalLocation> exons = new java.util.HashSet<>();

    private ubic.gemma.model.genome.Gene gene;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneProduct() {
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.common.description.DatabaseEntry> getAccessions() {
        return this.accessions;
    }

    /**
     * 
     */
    public Collection<ubic.gemma.model.genome.PhysicalLocation> getExons() {
        return this.exons;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Gene getGene() {
        return this.gene;
    }

    /**
     * <p>
     * GI for the gene product (if available)
     * </p>
     */
    public String getNcbiGi() {
        return this.ncbiGi;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProductType getType() {
        return this.type;
    }

    public void setAccessions( Collection<ubic.gemma.model.common.description.DatabaseEntry> accessions ) {
        this.accessions = accessions;
    }

    public void setExons( Collection<ubic.gemma.model.genome.PhysicalLocation> exons ) {
        this.exons = exons;
    }

    public void setGene( ubic.gemma.model.genome.Gene gene ) {
        this.gene = gene;
    }

    public void setNcbiGi( String ncbiGi ) {
        this.ncbiGi = ncbiGi;
    }

    public void setType( ubic.gemma.model.genome.gene.GeneProductType type ) {
        this.type = type;
    }

}