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
package ubic.gemma.model.genome.biosequence;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.description.DatabaseEntry;

import java.util.Collection;

/**
 * <p>
 * The sequence of a biological polymer such as a protein or DNA. BioSequences may be artificial, such as Affymetrix
 * reporter oligonucleotide chains, or they may be the sequence
 * </p>
 * <p>
 * of nucleotides associated with a gene product. This class only represents the sequence itself ("ATCGCCG..."), not the
 * physical item, and not the database entry for the sequence.
 * </p>
 */
public abstract class BioSequence extends ubic.gemma.model.common.Describable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5548459682099905305L;
    private Long length;
    private String sequence;
    private Boolean isApproximateLength;
    private Boolean isCircular;
    private ubic.gemma.model.genome.biosequence.PolymerType polymerType;
    private ubic.gemma.model.genome.biosequence.SequenceType type;
    private Double fractionRepeats;
    private ubic.gemma.model.common.description.DatabaseEntry sequenceDatabaseEntry;
    private ubic.gemma.model.genome.Taxon taxon;
    private Collection<BioSequence2GeneProduct> bioSequence2GeneProduct = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public BioSequence() {
    }

    public Collection<BioSequence2GeneProduct> getBioSequence2GeneProduct() {
        return this.bioSequence2GeneProduct;
    }

    public void setBioSequence2GeneProduct( Collection<BioSequence2GeneProduct> bioSequence2GeneProduct ) {
        this.bioSequence2GeneProduct = bioSequence2GeneProduct;
    }

    /**
     * <p>
     * The fraction of the sequences determined to be made up of repeats (e.g., via repeat masker)
     * </p>
     */
    public Double getFractionRepeats() {
        return this.fractionRepeats;
    }

    public void setFractionRepeats( Double fractionRepeats ) {
        this.fractionRepeats = fractionRepeats;
    }

    public Boolean getIsApproximateLength() {
        return this.isApproximateLength;
    }

    public void setIsApproximateLength( Boolean isApproximateLength ) {
        this.isApproximateLength = isApproximateLength;
    }

    public Boolean getIsCircular() {
        return this.isCircular;
    }

    public void setIsCircular( Boolean isCircular ) {
        this.isCircular = isCircular;
    }

    public Long getLength() {
        return this.length;
    }

    public void setLength( Long length ) {
        this.length = length;
    }

    public ubic.gemma.model.genome.biosequence.PolymerType getPolymerType() {
        return this.polymerType;
    }

    public void setPolymerType( ubic.gemma.model.genome.biosequence.PolymerType polymerType ) {
        this.polymerType = polymerType;
    }

    public String getSequence() {
        return this.sequence;
    }

    /**
     * The actual nucleotic sequence as in ATGC
     */
    public void setSequence( String sequence ) {
        this.sequence = sequence;
    }

    public DatabaseEntry getSequenceDatabaseEntry() {
        return this.sequenceDatabaseEntry;
    }

    public void setSequenceDatabaseEntry( DatabaseEntry sequenceDatabaseEntry ) {
        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
    }

    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

    public ubic.gemma.model.genome.biosequence.SequenceType getType() {
        return this.type;
    }

    public void setType( ubic.gemma.model.genome.biosequence.SequenceType type ) {
        this.type = type;
    }

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.biosequence.BioSequence}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.biosequence.BioSequence}.
         */
        public static ubic.gemma.model.genome.biosequence.BioSequence newInstance() {
            return new ubic.gemma.model.genome.biosequence.BioSequenceImpl();
        }

        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.biosequence.BioSequence}, taking all required
         * and/or read-only properties as arguments.
         */
        public static ubic.gemma.model.genome.biosequence.BioSequence newInstance(
                ubic.gemma.model.genome.Taxon taxon ) {
            final ubic.gemma.model.genome.biosequence.BioSequence entity = new ubic.gemma.model.genome.biosequence.BioSequenceImpl();
            entity.setTaxon( taxon );
            return entity;
        }
    }

}