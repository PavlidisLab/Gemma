/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

package ubic.gemma.model.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author kelsey
 * @version $Id$
 */
public class GeneValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7098036090107647318L;

    public static Collection<GeneValueObject> convert2ValueObjects( Collection<Gene> genes ) {

        Collection<GeneValueObject> converted = new HashSet<GeneValueObject>();
        if ( genes == null ) return converted;

        for ( Gene g : genes ) {
            if ( g == null ) continue;
            converted.add( new GeneValueObject( g.getId(), g.getName(), getAliasStrings( g ), g.getNcbiId(), g
                    .getOfficialSymbol(), g.getOfficialName(), g.getDescription(), null, g.getTaxon().getId(), g
                    .getTaxon().getScientificName(), g.getTaxon().getCommonName() ) );
        }

        return converted;
    }

    /**
     * A static method for easily converting GeneSetMembers into GeneValueObjects
     * 
     * @param genes
     * @return
     */
    public static Collection<GeneValueObject> convertMembers2GeneValueObjects( Collection<GeneSetMember> genes ) {

        Collection<GeneValueObject> converted = new HashSet<GeneValueObject>();
        if ( genes == null ) return converted;

        for ( GeneSetMember g : genes ) {
            if ( g == null ) continue;
            converted.add( new GeneValueObject( g ) );
        }

        return converted;
    }

    private java.lang.String description;

    private java.lang.Long id;

    private java.lang.String name;

    private Collection<java.lang.String> aliases;

    private java.lang.String ncbiId;

    private java.lang.String officialName;

    private java.lang.String officialSymbol;

    private Double score; // This is for genes in genesets might have a rank or a score associated with them.

    private String taxonCommonName;

    private java.lang.Long taxonId;

    private java.lang.String taxonScientificName;

    public GeneValueObject() {
    }

    public GeneValueObject( Gene gene ) {
        this.id = gene.getId();
        this.ncbiId = gene.getNcbiId();
        this.officialName = gene.getOfficialName();
        this.officialSymbol = gene.getOfficialSymbol();
        this.taxonScientificName = gene.getTaxon().getScientificName();
        this.setTaxonCommonName( gene.getTaxon().getCommonName() );
        this.name = gene.getName();
        this.description = gene.getDescription();
        this.taxonId = gene.getTaxon().getId();
        this.aliases = getAliasStrings( gene );
    }

    /**
     * Copy constructor for GeneSetMember
     * 
     * @param otherBean
     */
    public GeneValueObject( GeneSetMember otherBean ) {

        this( otherBean.getGene().getId(), otherBean.getGene().getName(), getAliasStrings( otherBean.getGene() ),
                otherBean.getGene().getNcbiId(), otherBean.getGene().getOfficialSymbol(), otherBean.getGene()
                        .getOfficialName(), otherBean.getGene().getDescription(), otherBean.getScore(), otherBean
                        .getGene().getTaxon().getId(), otherBean.getGene().getTaxon().getScientificName(), otherBean
                        .getGene().getTaxon().getCommonName() );
    }

    /**
     * Copies constructor from other GeneValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws java.lang.NullPointerException if the argument is <code>null</code>
     */
    public GeneValueObject( GeneValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), otherBean.getAliases(), otherBean.getNcbiId(), otherBean
                .getOfficialSymbol(), otherBean.getOfficialName(), otherBean.getDescription(), otherBean.getScore(),
                otherBean.getTaxonId(), otherBean.getTaxonScientificName(), otherBean.getTaxonCommonName() );
    }

    public GeneValueObject( java.lang.Long id, java.lang.String name, Collection<java.lang.String> aliases,
            java.lang.String ncbiId, java.lang.String officialSymbol, java.lang.String officialName,
            java.lang.String description, Double score, Long taxonId, String taxonScientificName, String taxonCommonName ) {
        this.id = id;
        this.name = name;
        this.ncbiId = ncbiId;
        this.officialSymbol = officialSymbol;
        this.officialName = officialName;
        this.description = description;
        this.score = score;
        this.taxonId = taxonId;
        this.taxonScientificName = taxonScientificName;
        this.taxonCommonName = taxonCommonName;
        this.aliases = aliases;
    }

    public GeneValueObject( Long geneId, String geneSymbol, String geneOfficialName, Taxon taxon ) {
        this.id = geneId;
        this.officialSymbol = geneSymbol;
        this.officialName = geneOfficialName;
        this.taxonId = taxon.getId();
        this.taxonCommonName = taxon.getCommonName();
    }

    /**
     * Copies all properties from the argument value object into this value object.
     */
    public void copy( GeneValueObject otherBean ) {
        if ( otherBean != null ) {
            this.setId( otherBean.getId() );
            this.setName( otherBean.getName() );
            this.setNcbiId( otherBean.getNcbiId() );
            this.setOfficialSymbol( otherBean.getOfficialSymbol() );
            this.setOfficialName( otherBean.getOfficialName() );
            this.setDescription( otherBean.getDescription() );
            this.setScore( otherBean.getScore() );
            this.setAliases( otherBean.getAliases() );
        }
    }

    public static Collection<String> getAliasStrings( Gene gene ) {
        Collection<java.lang.String> aliases = new ArrayList<String>();
        // catch doesn't prevent error messages in logs -- why?
        /*
         * try{
         * 
         * Collection<GeneAlias> aliasObjs = gene.getAliases(); Iterator<GeneAlias> iter = aliasObjs.iterator(); while(
         * iter.hasNext()){ aliases.add( iter.next().getAlias() ); } }catch(org.hibernate.LazyInitializationException
         * e){ return aliases; }
         */
        return aliases;

    }

    /**
     * public java.lang.Long getTaxonId() { return taxonId; } public void setTaxonId( java.lang.Long taxonId ) {
     * this.taxonId = taxonId; }
     */
    public java.lang.String getDescription() {
        return this.description;
    }

    /**
     * 
     */
    public java.lang.Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public java.lang.String getName() {
        return this.name;
    }

    public Collection<java.lang.String> getAliases() {
        return aliases;
    }

    public void setAliases( Collection<java.lang.String> aliases ) {
        this.aliases = aliases;
    }

    /**
     * 
     */
    public java.lang.String getNcbiId() {
        return this.ncbiId;
    }

    /**
     * 
     */
    public java.lang.String getOfficialName() {
        return this.officialName;
    }

    /**
     * 
     */
    public java.lang.String getOfficialSymbol() {
        return this.officialSymbol;
    }

    public Double getScore() {
        return score;
    }

    /**
     * @return the taxonCommonName
     */
    public String getTaxonCommonName() {
        return taxonCommonName;
    }

    public java.lang.Long getTaxonId() {
        return taxonId;
    }

    public java.lang.String getTaxonScientificName() {
        return taxonScientificName;
    }

    public void setDescription( java.lang.String description ) {
        this.description = description;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    public void setName( java.lang.String name ) {
        this.name = name;
    }

    public void setNcbiId( java.lang.String ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public void setOfficialName( java.lang.String officialName ) {
        this.officialName = officialName;
    }

    public void setOfficialSymbol( java.lang.String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    /**
     * @param taxonCommonName the taxonCommonName to set
     */
    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    public void setTaxonId( java.lang.Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTaxonScientificName( java.lang.String taxonScientificName ) {
        this.taxonScientificName = taxonScientificName;
    }

}