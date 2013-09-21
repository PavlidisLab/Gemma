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

/**
 * 
 */
public abstract class PhysicalLocation extends ubic.gemma.model.genome.ChromosomeLocation {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.PhysicalLocation}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.PhysicalLocation}.
         */
        public static ubic.gemma.model.genome.PhysicalLocation newInstance() {
            return new ubic.gemma.model.genome.PhysicalLocationImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.PhysicalLocation}, taking all required and/or
         * read-only properties as arguments.
         */
        public static ubic.gemma.model.genome.PhysicalLocation newInstance(
                ubic.gemma.model.genome.Chromosome chromosome ) {
            final ubic.gemma.model.genome.PhysicalLocation entity = new ubic.gemma.model.genome.PhysicalLocationImpl();
            entity.setChromosome( chromosome );
            return entity;
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.PhysicalLocation}, taking all possible properties
         * (except the identifier(s))as arguments.
         */
        public static ubic.gemma.model.genome.PhysicalLocation newInstance(
                ubic.gemma.model.genome.Chromosome chromosome, Long nucleotide, Integer nucleotideLength,
                String strand, Integer bin ) {
            final ubic.gemma.model.genome.PhysicalLocation entity = new ubic.gemma.model.genome.PhysicalLocationImpl();
            entity.setChromosome( chromosome );
            entity.setNucleotide( nucleotide );
            entity.setNucleotideLength( nucleotideLength );
            entity.setStrand( strand );
            entity.setBin( bin );
            return entity;
        }
    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5426735852697486498L;
    private Long nucleotide;

    private Integer nucleotideLength = Integer.valueOf( new Integer( 1 ) );

    private String strand;

    private Integer bin;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public PhysicalLocation() {
    }

    /**
     * 
     */
    public abstract int computeOverlap( ubic.gemma.model.genome.PhysicalLocation other );

    /**
     * <p>
     * Index to speed up queries
     * </p>
     */
    public Integer getBin() {
        return this.bin;
    }

    /**
     * 
     */
    public Long getNucleotide() {
        return this.nucleotide;
    }

    /**
     * 
     */
    public Integer getNucleotideLength() {
        return this.nucleotideLength;
    }

    /**
     * 
     */
    public String getStrand() {
        return this.strand;
    }

    /**
     * <p>
     * Determine if two PhysicalLocations are very close to each other. If this.equals(that) would return true this
     * always returns true. Otherwise the meaning of "very close" is implementation-specific, but generally means they
     * are on the same chromosome and are at overlapping locations and/or are separated by a small distance measured in
     * nucleotides.
     * </p>
     */
    public abstract boolean nearlyEquals( Object object );

    public void setBin( Integer bin ) {
        this.bin = bin;
    }

    public void setNucleotide( Long nucleotide ) {
        this.nucleotide = nucleotide;
    }

    public void setNucleotideLength( Integer nucleotideLength ) {
        this.nucleotideLength = nucleotideLength;
    }

    public void setStrand( String strand ) {
        this.strand = strand;
    }

}