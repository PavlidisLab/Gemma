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

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.gene.GeneAlias;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.Multifunctionality;

/**
 * Represents a functionally transcribed unit in the genome, recognized by other databases (NCBI, Ensembl).
 */
public abstract class Gene extends ChromosomeFeature {

    /**
     * Constructs new instances of {@link ubic.gemma.model.genome.Gene}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.genome.Gene}.
         */
        public static Gene newInstance() {
            return new GeneImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5693198926006383546L;

    private String officialSymbol;

    private String officialName;

    private Integer ncbiGeneId;

    private String ensemblId;

    private Collection<GeneProduct> products = new HashSet<GeneProduct>();

    private Collection<GeneAlias> aliases = new HashSet<GeneAlias>();

    private Taxon taxon;

    private Collection<DatabaseEntry> accessions = new HashSet<DatabaseEntry>();

    private Multifunctionality multifunctionality;

    private Collection<PhenotypeAssociation> phenotypeAssociations = new HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Gene() {
    }

    /**
     * 
     */
    public Collection<DatabaseEntry> getAccessions() {
        return this.accessions;
    }

    /**
     * 
     */
    public Collection<GeneAlias> getAliases() {
        return this.aliases;
    }

    /**
     * An Ensembl ID for the gene.
     */
    public String getEnsemblId() {
        return this.ensemblId;
    }

    /**
     * 
     */
    public Multifunctionality getMultifunctionality() {
        return this.multifunctionality;
    }

    /**
     * 
     */
    public Integer getNcbiGeneId() {
        return this.ncbiGeneId;
    }

    /**
     * 
     */
    public String getOfficialName() {
        return this.officialName;
    }

    /**
     * 
     */
    public String getOfficialSymbol() {
        return this.officialSymbol;
    }

    /**
     * 
     */
    public Collection<PhenotypeAssociation> getPhenotypeAssociations() {
        return this.phenotypeAssociations;
    }

    /**
     * 
     */
    public Collection<GeneProduct> getProducts() {
        return this.products;
    }

    /**
     * Note that a Gene also has a chromosome, so the organism can be inferred that way as well. This direct association
     * is a denormalization for queries that don't care about location, just species-membership.
     */
    public Taxon getTaxon() {
        return this.taxon;
    }

    public void setAccessions( Collection<DatabaseEntry> accessions ) {
        this.accessions = accessions;
    }

    public void setAliases( Collection<GeneAlias> aliases ) {
        this.aliases = aliases;
    }

    public void setEnsemblId( String ensemblId ) {
        this.ensemblId = ensemblId;
    }

    public void setMultifunctionality( Multifunctionality multifunctionality ) {
        this.multifunctionality = multifunctionality;
    }

    public void setNcbiGeneId( Integer ncbiGeneId ) {
        this.ncbiGeneId = ncbiGeneId;
    }

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public void setPhenotypeAssociations( Collection<PhenotypeAssociation> phenotypeAssociations ) {
        this.phenotypeAssociations = phenotypeAssociations;
    }

    public void setProducts( Collection<GeneProduct> products ) {
        this.products = products;
    }

    public void setTaxon( Taxon taxon ) {
        this.taxon = taxon;
    }

}