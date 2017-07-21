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
package ubic.gemma.model.genome;

public abstract class PhysicalLocation extends ChromosomeLocation {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5426735852697486498L;
    private Long nucleotide;
    private Integer nucleotideLength = 1;
    private String strand;
    private Integer bin;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public PhysicalLocation() {
    }

    public abstract int computeOverlap( PhysicalLocation other );

    /**
     * <p>
     * Index to speed up queries
     * </p>
     */
    public Integer getBin() {
        return this.bin;
    }

    public void setBin( Integer bin ) {
        this.bin = bin;
    }

    public Long getNucleotide() {
        return this.nucleotide;
    }

    public void setNucleotide( Long nucleotide ) {
        this.nucleotide = nucleotide;
    }

    public Integer getNucleotideLength() {
        return this.nucleotideLength;
    }

    public void setNucleotideLength( Integer nucleotideLength ) {
        this.nucleotideLength = nucleotideLength;
    }

    public String getStrand() {
        return this.strand;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

    /**
     * Constructs new instances of {@link PhysicalLocation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link PhysicalLocation}.
         */
        public static PhysicalLocation newInstance() {
            return new PhysicalLocationImpl();
        }

        /**
         * Constructs a new instance of {@link PhysicalLocation}, taking all required and/or
         * read-only properties as arguments.
         */
        public static PhysicalLocation newInstance(
                Chromosome chromosome ) {
            final PhysicalLocation entity = new PhysicalLocationImpl();
            entity.setChromosome( chromosome );
            return entity;
        }

        /**
         * Constructs a new instance of {@link PhysicalLocation}, taking all possible properties
         * (except the identifier(s))as arguments.
         */
        public static PhysicalLocation newInstance(
                Chromosome chromosome, Long nucleotide, Integer nucleotideLength, String strand,
                Integer bin ) {
            final PhysicalLocation entity = new PhysicalLocationImpl();
            entity.setChromosome( chromosome );
            entity.setNucleotide( nucleotide );
            entity.setNucleotideLength( nucleotideLength );
            entity.setStrand( strand );
            entity.setBin( bin );
            return entity;
        }
    }

}