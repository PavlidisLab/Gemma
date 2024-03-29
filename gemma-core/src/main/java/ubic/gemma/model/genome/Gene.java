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

import org.hibernate.search.annotations.*;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.Multifunctionality;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a functionally transcribed unit in the genome, recognized by other databases (NCBI, Ensembl).
 */
@Indexed
public class Gene extends ChromosomeFeature {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5693198926006383546L;
    private String officialSymbol;
    private String officialName;
    private Integer ncbiGeneId;
    private String ensemblId; //Non-unique for roughly 2000 genes as of Aug 11th 2017
    private Set<GeneProduct> products = new HashSet<>();
    private Set<GeneAlias> aliases = new HashSet<>();
    private Taxon taxon;
    private Set<DatabaseEntry> accessions = new HashSet<>();
    private Multifunctionality multifunctionality;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public Gene() {
    }

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof Gene ) ) {
            return false;
        }
        final Gene that = ( Gene ) object;

        if ( this.getId() == null || that.getId() == null || !this.getId().equals( that.getId() ) ) {

            // to be unambiguous need NCBI id OR (symbol + taxon + (name OR physical location))

            boolean bothHaveNcbi = this.getNcbiGeneId() != null && that.getNcbiGeneId() != null;

            if ( bothHaveNcbi ) {
                return this.getNcbiGeneId().equals( that.getNcbiGeneId() );
            }

            boolean bothHaveSymbol = this.getOfficialSymbol() != null && that.getOfficialSymbol() != null;
            boolean bothHaveTaxon = this.getTaxon() != null && that.getTaxon() != null;

            if ( bothHaveTaxon && bothHaveSymbol && this.getTaxon().equals( that.getTaxon() ) && this
                    .getOfficialSymbol().equalsIgnoreCase( that.getOfficialSymbol() ) ) {

                boolean bothHaveName = this.getOfficialName() != null && that.getOfficialName() != null;
                boolean bothHavePhysicalLocation =
                        this.getPhysicalLocation() != null && that.getPhysicalLocation() != null;

                if ( bothHaveName ) {
                    return this.getOfficialName().equals( that.getOfficialName() );
                } else
                    /*
                     * The gene must be thawed, which isn't certain, but if the gene is persistent, we _probably_
                     * wouldn't get this far. See bug 1840, which involves code that _shouldn't_ get this far but it
                     * does.
                     */
                    return bothHavePhysicalLocation && this.getPhysicalLocation().equals( that.getPhysicalLocation() );
                // can't decide, assume unequal.

            }
            return false; //

        }
        return true;

    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( this.getId() == null ? this.computeHashCode() : this.getId().hashCode() );
        return hashCode;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder( super.toString() );
        if ( officialSymbol != null ) {
            b.append( " Official Symbol=" ).append( officialSymbol );
        }
        if ( officialName != null ) {
            b.append( " Official Name=" ).append( officialName );
        }
        if ( ncbiGeneId != null ) {
            b.append( " NCBI Gene ID=" ).append( ncbiGeneId );
        }
        return b.toString();
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

    @IndexedEmbedded
    public Set<GeneAlias> getAliases() {
        return this.aliases;
    }

    public void setAliases( Set<GeneAlias> aliases ) {
        this.aliases = aliases;
    }

    /**
     * @return An Ensembl ID for the gene.
     */
    @Field(analyze = Analyze.NO)
    public String getEnsemblId() {
        return this.ensemblId;
    }

    public void setEnsemblId( String ensemblId ) {
        this.ensemblId = ensemblId;
    }

    public Multifunctionality getMultifunctionality() {
        return this.multifunctionality;
    }

    public void setMultifunctionality( Multifunctionality multifunctionality ) {
        this.multifunctionality = multifunctionality;
    }

    @Field(analyze = Analyze.NO)
    public Integer getNcbiGeneId() {
        return this.ncbiGeneId;
    }

    public void setNcbiGeneId( Integer ncbiGeneId ) {
        this.ncbiGeneId = ncbiGeneId;
    }

    @Field(analyze = Analyze.NO)
    public String getOfficialName() {
        return this.officialName;
    }

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    @Field(analyze = Analyze.NO)
    public String getOfficialSymbol() {
        return this.officialSymbol;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    @IndexedEmbedded
    public Set<GeneProduct> getProducts() {
        return this.products;
    }

    public void setProducts( Set<GeneProduct> products ) {
        this.products = products;
    }

    /**
     * @return Note that a Gene also has a chromosome, so the organism can be inferred that way as well. This direct association
     * is a denormalization for queries that don't care about location, just species-membership.
     */
    @IndexedEmbedded
    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

    private int computeHashCode() {
        int hashCode = 29;

        if ( this.getNcbiGeneId() != null ) {
            hashCode += this.getNcbiGeneId().hashCode();
            return hashCode;
        }

        if ( this.getOfficialSymbol() != null ) {
            hashCode += this.getOfficialSymbol().hashCode();
        }

        if ( this.getTaxon() != null ) {
            hashCode += this.getTaxon().hashCode();
        }

        if ( this.getOfficialName() != null ) {
            hashCode += this.getOfficialName().hashCode();
        } else if ( this.getPhysicalLocation() != null ) {
            hashCode += this.getPhysicalLocation().hashCode();
        } else if ( this.getProducts() != null && this.getProducts().size() > 0 ) {
            GeneProduct gp = this.getProducts().iterator().next();
            hashCode += gp.hashCode();
        }

        hashCode += super.hashCode();

        return hashCode;
    }

    public static final class Factory {
        public static Gene newInstance() {
            return new Gene();
        }
    }

}