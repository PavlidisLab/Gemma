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

import javax.annotation.Nullable;
import java.util.Objects;

public class PhysicalLocation extends ChromosomeLocation {

    private Long nucleotide;
    private Integer nucleotideLength = 1;
    private String strand;
    /**
     * Index to speed up queries.
     */
    @Nullable
    private Integer bin;

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

    @Nullable
    public Integer getBin() {
        return this.bin;
    }

    public void setBin( @Nullable Integer bin ) {
        this.bin = bin;
    }

    @Override
    public int hashCode() {
        return Objects.hash( getChromosome(), nucleotide, nucleotideLength, strand );
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
        if ( getId() != null && that.getId() != null ) {
            return getId().equals( that.getId() );
        } else {
            return Objects.equals( this.getChromosome(), that.getChromosome() )
                    && Objects.equals( this.getNucleotide(), that.getNucleotide() )
                    && Objects.equals( this.getNucleotideLength(), that.getNucleotideLength() )
                    && Objects.equals( this.getStrand(), that.getStrand() );
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if ( this.getChromosome() != null ) {
            buf.append( this.getChromosome().getTaxon().getScientificName() )
                    .append( " chr " ).append( this.getChromosome().getName() );
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