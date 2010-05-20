/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.model.genome.gene;

/**
 * @author paul
 * @version $Id$
 */
public class GeneProductValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 1156628868995566223L;

    private java.lang.Long id;

    private java.lang.String ncbiId;

    private java.lang.String name;

    private java.lang.Long geneId;

    private java.lang.String type;

    private java.lang.String chromosome;

    private java.lang.String strand;

    private java.lang.Long nucleotideStart;

    private java.lang.Long nucleotideEnd;

    private GeneValueObject gene;

    public GeneProductValueObject() {
    }

    public GeneProductValueObject( GeneProduct entity ) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.type = entity.getType().getValue();
        this.gene = new GeneValueObject( entity.getGene() );
    }

    /**
     * 
     */
    public java.lang.String getChromosome() {
        return this.chromosome;
    }

    /**
     * 
     */
    public java.lang.Long getGeneId() {
        return this.geneId;
    }

    /**
     * 
     */
    public java.lang.Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public java.lang.String getName() {
        return this.name;
    }

    /**
     * 
     */
    public java.lang.String getNcbiId() {
        return this.ncbiId;
    }

    /**
     * 
     */
    public java.lang.Long getNucleotideEnd() {
        return this.nucleotideEnd;
    }

    /**
     * 
     */
    public java.lang.Long getNucleotideStart() {
        return this.nucleotideStart;
    }

    /**
     * 
     */
    public java.lang.String getStrand() {
        return this.strand;
    }

    /**
     * 
     */
    public java.lang.String getType() {
        return this.type;
    }

    public void setChromosome( java.lang.String chromosome ) {
        this.chromosome = chromosome;
    }

    public void setGeneId( java.lang.Long geneId ) {
        this.geneId = geneId;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    public void setName( java.lang.String name ) {
        this.name = name;
    }

    public void setNcbiId( java.lang.String ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public void setNucleotideEnd( java.lang.Long nucleotideEnd ) {
        this.nucleotideEnd = nucleotideEnd;
    }

    public void setNucleotideStart( java.lang.Long nucleotideStart ) {
        this.nucleotideStart = nucleotideStart;
    }

    public void setStrand( java.lang.String strand ) {
        this.strand = strand;
    }

    public void setType( java.lang.String type ) {
        this.type = type;
    }

    // ubic.gemma.model.genome.gene.GeneProductValueObject value-object java merge-point
}