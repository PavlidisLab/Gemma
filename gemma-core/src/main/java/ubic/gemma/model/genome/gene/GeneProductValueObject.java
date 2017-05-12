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

    private Long id;

    private String ncbiId;

    private String name;

    private Long geneId;

    private String type;

    private String chromosome;

    private String strand;

    private Long nucleotideStart;

    private Long nucleotideEnd;

    public GeneProductValueObject() {
    }

    public GeneProductValueObject( GeneProduct entity ) {
        this.id = entity.getId();
        this.name = entity.getName();
        this.type = entity.getType().getValue();
    }

    /**
     * 
     */
    public String getChromosome() {
        return this.chromosome;
    }

    /**
     * 
     */
    public Long getGeneId() {
        return this.geneId;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * 
     */
    public String getNcbiId() {
        return this.ncbiId;
    }

    /**
     * 
     */
    public Long getNucleotideEnd() {
        return this.nucleotideEnd;
    }

    /**
     * 
     */
    public Long getNucleotideStart() {
        return this.nucleotideStart;
    }

    /**
     * 
     */
    public String getStrand() {
        return this.strand;
    }

    /**
     * 
     */
    public String getType() {
        return this.type;
    }

    public void setChromosome( String chromosome ) {
        this.chromosome = chromosome;
    }

    public void setGeneId( Long geneId ) {
        this.geneId = geneId;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNcbiId( String ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public void setNucleotideEnd( Long nucleotideEnd ) {
        this.nucleotideEnd = nucleotideEnd;
    }

    public void setNucleotideStart( Long nucleotideStart ) {
        this.nucleotideStart = nucleotideStart;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    public void setType( String type ) {
        this.type = type;
    }

    // ubic.gemma.model.genome.gene.GeneProductValueObject value-object java merge-point
}