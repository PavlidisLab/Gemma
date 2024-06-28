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

import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.AbstractDescribable;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Taxon;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

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
@SuppressWarnings("unused")
@Indexed
public class BioSequence extends AbstractDescribable implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6620431603579954167L;
    private Long length;
    private String sequence;
    private Boolean isApproximateLength;
    private Boolean isCircular;
    @Nullable
    private PolymerType polymerType;
    @Nullable
    private SequenceType type;
    private Double fractionRepeats;
    private ubic.gemma.model.common.description.DatabaseEntry sequenceDatabaseEntry;
    private Taxon taxon;
    private Set<BioSequence2GeneProduct> bioSequence2GeneProduct = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public BioSequence() {
    }

    @Override
    @DocumentId
    public Long getId() {
        return super.getId();
    }

    @Override
    @Field
    public String getName() {
        return super.getName();
    }

    public Set<BioSequence2GeneProduct> getBioSequence2GeneProduct() {
        return this.bioSequence2GeneProduct;
    }

    public void setBioSequence2GeneProduct( Set<BioSequence2GeneProduct> bioSequence2GeneProduct ) {
        this.bioSequence2GeneProduct = bioSequence2GeneProduct;
    }

    /**
     * @return The fraction of the sequences determined to be made up of repeats (e.g., via repeat masker)
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

    @Nullable
    public PolymerType getPolymerType() {
        return this.polymerType;
    }

    public void setPolymerType( @Nullable PolymerType polymerType ) {
        this.polymerType = polymerType;
    }

    public String getSequence() {
        return this.sequence;
    }

    /**
     * @param sequence The actual nucleotic sequence as in ATGC
     */
    public void setSequence( String sequence ) {
        this.sequence = sequence;
    }

    @IndexedEmbedded
    public DatabaseEntry getSequenceDatabaseEntry() {
        return this.sequenceDatabaseEntry;
    }

    public void setSequenceDatabaseEntry( DatabaseEntry sequenceDatabaseEntry ) {
        this.sequenceDatabaseEntry = sequenceDatabaseEntry;
    }

    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    @Nullable
    public SequenceType getType() {
        return this.type;
    }

    public void setType( @Nullable SequenceType type ) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        int nameHash = this.getName() == null ? 0 : this.getName().hashCode();
        int taxonHash = this.getTaxon() == null ? 0 : this.getTaxon().hashCode();
        int lengthHash = this.getLength() == null ? 0 : this.getLength().hashCode();
        int dbHash = this.getSequenceDatabaseEntry() == null ? 0 : this.getSequenceDatabaseEntry().hashCode();
        int seqHash = 0;
        if ( dbHash == 0 && nameHash == 0 && lengthHash == 0 && this.getSequence() != null )
            seqHash = this.getSequence().hashCode();
        return 29 * nameHash + seqHash + dbHash + taxonHash + lengthHash;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object )
            return true;
        if ( !( object instanceof BioSequence ) ) {
            return false;
        }
        final BioSequence that = ( BioSequence ) object;
        if ( this.getId() != null && that.getId() != null )
            return this.getId().equals( that.getId() );
        return Objects.equals( getName(), that.getName() )
                && Objects.equals( getSequenceDatabaseEntry(), that.getSequenceDatabaseEntry() )
                && Objects.equals( getTaxon(), that.getTaxon() )
                && Objects.equals( getLength(), that.getLength() )
                && Objects.equals( getSequence(), that.getSequence() );
    }

    public static final class Factory {
        public static BioSequence newInstance() {
            return new BioSequence();
        }

        public static BioSequence newInstance( Taxon taxon ) {
            final BioSequence entity = new BioSequence();
            entity.setTaxon( taxon );
            return entity;
        }

        public static BioSequence newInstance( String name, Taxon taxon ) {
            final BioSequence entity = new BioSequence();
            entity.setName( name );
            entity.setTaxon( taxon );
            return entity;
        }
    }

}