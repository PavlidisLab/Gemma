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

import org.apache.commons.lang3.builder.CompareToBuilder;

import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal") // Constant is preferred
public class PhysicalLocation extends ChromosomeLocation {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6580769809003779541L;

    private static final int BIN_FIRST_SHIFT = 17; /* How much to shift to get to finest bin. */
    private static final int BIN_NEXT_SHIFT = 3; /* How much to shift to get to next larger bin. */
    private static final int[] BIN_OFFSETS_EXTENDED = { 4096 + 512 + 64 + 8 + 1, 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1,
            0 };
    private static final int[] BIN_OFFSETS = { 512 + 64 + 8 + 1, 64 + 8 + 1, 8 + 1, 1, 0 };
    private static final int BIN_RANGE_MAX_END_512M = ( 512 * 1024 * 1024 );
    private static final int BIN_OFFSET_OLD_TO_EXTENDED = 4681;

    private Long nucleotide;
    private Integer nucleotideLength = 1;
    private String strand;
    private Integer bin;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    @SuppressWarnings("WeakerAccess") // Required by spring
    public PhysicalLocation() {
    }

    public static int computeOverlap( long starta, long enda, long startb, long endb ) {
        if ( starta > enda )
            throw new IllegalArgumentException( "Start " + starta + " must be before end " + enda );
        if ( startb > endb )
            throw new IllegalArgumentException( "Start " + startb + " must be before end " + endb );

        long overlap;
        if ( endb < starta || enda < startb ) {
            overlap = 0;
        } else if ( starta <= startb ) {
            if ( enda < endb ) {
                overlap = enda - startb; // overhang on the left
            } else {
                overlap = endb - startb; // includes entire target
            }
        } else if ( enda < endb ) { // entirely contained within target.
            overlap = enda - starta; // length of our test sequence.
        } else {
            overlap = endb - starta; // overhang on the right
        }

        assert overlap >= 0 : "Negative overlap";
        assert ( double ) overlap / ( double ) ( enda - starta ) <= 1.0 : "Overlap longer than sequence";
        // if ( log.isTraceEnabled() ) log.trace( "Overlap=" + overlap );
        return ( int ) overlap;
    }

    /**
     * @param start start
     * @param end   end
     * @return bin that this start-end segment is in
     */
    @SuppressWarnings("unused") // Possible external use
    public int binFromRange( int start, int end ) {
        if ( end <= PhysicalLocation.BIN_RANGE_MAX_END_512M )
            return this.binFromRangeStandard( start, end );
        return this.binFromRangeExtended( start, end );
    }

    /**
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( Object object ) {
        PhysicalLocation other = ( PhysicalLocation ) object;
        return new CompareToBuilder().append( this.getChromosome().getName(), other.getChromosome().getName() )
                .append( this.getNucleotide(), other.getNucleotide() ).toComparison();
    }

    @Override
    public int hashCode() {
        return Objects.hash( getChromosome(), getNucleotide(), getNucleotideLength() );
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof PhysicalLocation ) ) {
            return false;
        }
        final PhysicalLocation that = ( PhysicalLocation ) object;

        if ( getId() != null && that.getId() != null )
            return getId().equals( that.getId() );

        if ( !this.getChromosome().equals( that.getChromosome() ) ) {
            return false;
        }

        if ( this.getStrand() != null && that.getStrand() != null ) {
            if ( !this.getStrand().equals( that.getStrand() ) ) {
                return false;
            }
        }

        if ( this.getNucleotide() != null && that.getNucleotide() != null ) {
            if ( !this.getNucleotide().equals( that.getNucleotide() ) ) {
                return false;
            }
        }

        //noinspection SimplifiableIfStatement // Better readability
        if ( this.getNucleotideLength() != null && that.getNucleotideLength() != null ) {
            return this.getNucleotideLength().equals( that.getNucleotideLength() );
        }

        return true;
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Useful interface
    public int computeOverlap( PhysicalLocation other ) {

        if ( this.getId() == null || other.getId() == null || !this.getId().equals( other.getId() ) ) {
            if ( this.getChromosome() == null || other.getChromosome() == null )
                return 0;
            if ( !this.getChromosome().equals( other.getChromosome() ) )
                return 0;

            if ( this.getStrand() != null && other.getStrand() != null && !this.getStrand()
                    .equals( other.getStrand() ) ) {
                return 0;
            }

            if ( this.getNucleotide() != null && other.getNucleotide() != null && this.getNucleotideLength() != null
                    && other.getNucleotideLength() != null ) {
                long starta = this.getNucleotide();
                long enda = starta + this.getNucleotideLength();
                long startb = other.getNucleotide();
                long endb = startb + other.getNucleotideLength();

                return PhysicalLocation.computeOverlap( starta, enda, startb, endb );

            }
            return 0;
        }
        return other.getNucleotideLength(); // The two locations are the same object.
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( this.getChromosome() != null ) {
            buf.append( this.getChromosome().getTaxon().getScientificName() ).append( " chr " )
                    .append( this.getChromosome().getName() );
        }

        if ( this.getNucleotide() != null ) {
            buf.append( ":" ).append( this.getNucleotide() );
            if ( this.getNucleotideLength() != 0 ) {
                buf.append( "-" ).append( this.getNucleotide() + this.getNucleotideLength() );
            }
        }
        if ( this.getStrand() != null )
            buf.append( " on " ).append( this.getStrand() ).append( " strand" );

        return buf.toString();
    }

    /**
     * @return Index to speed up queries
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

    @SuppressWarnings("Duplicates") // generalization would be too complex
    private int binFromRangeExtended( int start, int end )
        /*
         * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
         * segment, for each 8M segment, for each 64M segment, for each 512M segment, and one top level bin for 4Gb. Note,
         * since start and end are int's, the practical limit is up to 2Gb-1, and thus, only four result bins on the second
         * level. A range goes into the smallest bin it will fit in.
         */ {
        int startBin = start, endBin = end - 1, i;
        startBin >>= PhysicalLocation.BIN_FIRST_SHIFT;
        endBin >>= PhysicalLocation.BIN_FIRST_SHIFT;
        for ( i = 0; i < PhysicalLocation.BIN_OFFSETS_EXTENDED.length; ++i ) {
            if ( startBin == endBin )
                return PhysicalLocation.BIN_OFFSET_OLD_TO_EXTENDED + PhysicalLocation.BIN_OFFSETS_EXTENDED[i]
                        + startBin;
            startBin >>= PhysicalLocation.BIN_NEXT_SHIFT;
            endBin >>= PhysicalLocation.BIN_NEXT_SHIFT;
        }
        throw new IllegalArgumentException(
                "start " + start + ", end " + end + " out of range in findBin (max is 512M)" );
    }

    @SuppressWarnings("Duplicates") // generalization would be too complex
    private int binFromRangeStandard( int start, int end )
        /*
         * Given start,end in chromosome coordinates assign it a bin. There's a bin for each 128k segment, for each 1M
         * segment, for each 8M segment, for each 64M segment, and for each chromosome (which is assumed to be less than
         * 512M.) A range goes into the smallest bin it will fit in.
         */ {
        int startBin = start, endBin = end - 1, i;
        startBin >>= PhysicalLocation.BIN_FIRST_SHIFT;
        endBin >>= PhysicalLocation.BIN_FIRST_SHIFT;
        for ( i = 0; i < PhysicalLocation.BIN_OFFSETS.length; ++i ) {
            if ( startBin == endBin )
                return PhysicalLocation.BIN_OFFSETS[i] + startBin;
            startBin >>= PhysicalLocation.BIN_NEXT_SHIFT;
            endBin >>= PhysicalLocation.BIN_NEXT_SHIFT;
        }
        throw new IllegalArgumentException(
                "start " + start + ", end " + end + " out of range in findBin (max is 512M)" );
    }

    public static final class Factory {

        public static PhysicalLocation newInstance() {
            return new PhysicalLocation();
        }

        public static PhysicalLocation newInstance( Chromosome chromosome ) {
            final PhysicalLocation entity = new PhysicalLocation();
            entity.setChromosome( chromosome );
            return entity;
        }

        @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
        public static PhysicalLocation newInstance( Chromosome chromosome, Long nucleotide, Integer nucleotideLength,
                String strand, Integer bin ) {
            final PhysicalLocation entity = new PhysicalLocation();
            entity.setChromosome( chromosome );
            entity.setNucleotide( nucleotide );
            entity.setNucleotideLength( nucleotideLength );
            entity.setStrand( strand );
            entity.setBin( bin );
            return entity;
        }
    }

}