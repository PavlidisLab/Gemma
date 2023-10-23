package ubic.gemma.model.genome.gene;
/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2018 University of British Columbia
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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.ChromosomeFeature;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;

import javax.persistence.Transient;
import java.util.Set;

@Indexed
public class GeneProduct extends ChromosomeFeature {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8414732389521430535L;
    private String ncbiGi;
    private Set<DatabaseEntry> accessions = new java.util.HashSet<>();
    /**
     * Only used in transient instances in sequence analysis. The entity relation in the database is never used and will
     * be removed.
     */
    private Set<PhysicalLocation> exons = new java.util.HashSet<>();
    private Gene gene;
    /**
     * Indicate if this GeneProduct is dummy.
     * <p>
     * Dummy {@link GeneProduct} are not listed in the {@link Gene#getProducts()} associations.
     */
    private boolean dummy;

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? this.computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof GeneProduct ) ) {
            return false;
        }
        final GeneProduct that = ( GeneProduct ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            boolean bothHaveNcbiGi = this.getNcbiGi() != null && that.getNcbiGi() != null;

            if ( bothHaveNcbiGi ) {
                return this.getNcbiGi().equals( that.getNcbiGi() );
            }

            boolean bothHaveGene = this.getGene() != null && that.getGene() != null;
            boolean bothHaveSymbol = this.getName() != null && that.getName() != null;

            return bothHaveSymbol && bothHaveGene && this.getName().equals( that.getName() ) && this.getGene()
                    .equals( that.getGene() );

        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder( super.toString() );
        if ( ncbiGi != null ) {
            buf.append( " NCBI GI=" ).append( ncbiGi );
        }
        if ( gene != null ) {
            buf.append( " Gene=" ).append( gene );
        }
        return buf.toString();
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

    @IndexedEmbedded
    public Set<DatabaseEntry> getAccessions() {
        return this.accessions;
    }

    public void setAccessions( Set<DatabaseEntry> accessions ) {
        this.accessions = accessions;
    }

    /**
     * Only used for transient instances in sequence analysis, we do not store exon locations in the database.
     *
     * @return physical locations of exons
     */
    @Transient
    public Set<PhysicalLocation> getExons() {
        return this.exons;
    }

    /**
     * Only used for transient instances, we do not store exon locations in the database.
     *
     * @param exons new physical locations of exons
     */
    @Transient
    public void setExons( Set<PhysicalLocation> exons ) {
        this.exons = exons;
    }

    public Gene getGene() {
        return this.gene;
    }

    public void setGene( Gene gene ) {
        this.gene = gene;
    }

    /**
     * @return GI for the gene product (if available)
     */
    @Field(analyze = Analyze.NO)
    public String getNcbiGi() {
        return this.ncbiGi;
    }

    public void setNcbiGi( String ncbiGi ) {
        this.ncbiGi = ncbiGi;
    }

    @Override
    @Field
    public String getPreviousNcbiId() {
        return super.getPreviousNcbiId();
    }

    public boolean isDummy() {
        return dummy;
    }

    public void setDummy( boolean dummy ) {
        this.dummy = dummy;
    }

    private int computeHashCode() {
        int hashCode = 0;

        if ( this.getNcbiGi() != null ) {
            hashCode += this.getNcbiGi().hashCode();
        } else if ( this.getName() != null && this.getGene() != null ) {
            hashCode += this.getName().hashCode();
            hashCode += this.getGene().hashCode();
        }

        return hashCode;
    }

    public static final class Factory {
        public static GeneProduct newInstance() {
            return new GeneProduct();
        }

    }

}