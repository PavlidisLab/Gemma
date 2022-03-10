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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.Hibernate;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;

/**
 * @author kelsey
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possibly used in front end
public class GeneValueObject extends IdentifiableValueObject<Gene> implements Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7098036090107647318L;
    private Collection<String> aliases;
    /**
     * How many experiments "involve" (manipulate, etc.) this gene
     */
    private Integer associatedExperimentCount = 0;
    private Integer compositeSequenceCount = 0; // number of probes
    private String description;
    private Collection<GeneSetValueObject> geneSets = null;
    private Collection<GeneValueObject> homologues = null;
    /**
     * Was this gene directly used in a query? Or is it inferred somehow. The default is true, use this when you need to
     * differentiate
     */
    private Boolean isQuery = true;
    private Double multifunctionalityRank = 0.0;
    @JsonIgnore
    private String name;
    private Integer ncbiId;
    private String ensemblId;
    private double[] nodeDegreeNegRanks;
    private double[] nodeDegreePosRanks;
    /**
     * Array containing number of links supported by 0,1,2, .... data sets (value in first index is always 0) for
     * negative correlation coexpression
     */
    private int[] nodeDegreesNeg;
    /**
     * Array containing number of links supported by 0,1,2, .... data sets (value in first index is always 0) for
     * positive coexpression
     */
    private int[] nodeDegreesPos;
    private Integer numGoTerms = 0;
    private String officialName;
    private String officialSymbol;
    private Collection<CharacteristicValueObject> phenotypes;
    private Integer platformCount;
    private Double score; // This is for genes in gene sets might have a rank or a score associated with them.
    private String taxonCommonName;
    private Long taxonId;
    private String taxonScientificName;

    /**
     * Required when using the class as a spring bean.
     */
    public GeneValueObject() {
    }

    public GeneValueObject( Long id ) {
        super( id );
    }

    /**
     * Aliases are not filled in.
     *
     * @param gene gene
     */
    public GeneValueObject( Gene gene ) {
        super( gene.getId() );
        this.ncbiId = gene.getNcbiGeneId();
        this.officialName = gene.getOfficialName();
        this.officialSymbol = gene.getOfficialSymbol();
        if ( gene.getTaxon() != null && Hibernate.isInitialized( gene.getTaxon() ) ) {
            this.taxonId = gene.getTaxon().getId();
            this.taxonScientificName = gene.getTaxon().getScientificName();
            this.setTaxonCommonName( gene.getTaxon().getCommonName() );
        }
        this.name = gene.getName();
        this.description = gene.getDescription();
        this.ensemblId = gene.getEnsemblId();
    }

    /**
     * Copies constructor from other GeneValueObject
     *
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    @SuppressWarnings("CopyConstructorMissesField") // Only copying constructor argument fields
    public GeneValueObject( GeneValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), null, otherBean.getNcbiId(), otherBean.getOfficialSymbol(),
                otherBean.getOfficialName(), otherBean.getDescription(), otherBean.getScore(), otherBean.getTaxonId(),
                otherBean.getTaxonScientificName(), otherBean.getTaxonCommonName() );
    }

    public GeneValueObject( Long id, String name, Collection<String> aliases, Integer ncbiId, String officialSymbol,
            String officialName, String description, Double score, Long taxonId, String taxonScientificName,
            String taxonCommonName ) {
        super( id );
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
        super( geneId );
        this.officialSymbol = geneSymbol;
        this.officialName = geneOfficialName;
        this.taxonId = taxon.getId();
        this.taxonCommonName = taxon.getCommonName();
    }

    /**
     * Converts a Gene to a GeneValueObject
     *
     * @param gene a gene to be converted to a value object
     * @return value object with the same basic characteristics as the given gene, including aliases.
     */
    public static GeneValueObject convert2ValueObject( Gene gene ) {
        if ( gene == null )
            return null;

        GeneValueObject geneValueObject = new GeneValueObject( gene );

        GeneValueObject.addConvertedAliases( gene, geneValueObject );

        return geneValueObject;
    }

    /**
     * A static method for easily converting GeneSetMembers into GeneValueObjects
     *
     * @param setMembers gene set members
     * @return gene VOs
     */
    public static Collection<GeneValueObject> convertMembers2GeneValueObjects( Collection<GeneSetMember> setMembers ) {

        Collection<GeneValueObject> converted = new HashSet<>();
        if ( setMembers == null )
            return converted;

        for ( GeneSetMember member : setMembers ) {
            if ( member == null )
                continue;
            GeneValueObject geneValueObject = new GeneValueObject( member.getGene() );
            GeneValueObject.addConvertedAliases( member.getGene(), geneValueObject );
            converted.add( geneValueObject );
        }

        return converted;
    }

    private static void addConvertedAliases( Gene gene, GeneValueObject geneValueObject ) {
        LinkedList<String> aliases = new LinkedList<>();
        for ( GeneAlias ga : gene.getAliases() ) {
            aliases.add( ga.getAlias() );
        }
        geneValueObject.setAliases( aliases );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        if ( id != null )
            return id.hashCode();
        if ( ncbiId != null )
            return ncbiId.hashCode();
        result = prime * result + ( ( officialSymbol == null ) ? 0 : officialSymbol.hashCode() );
        result = prime * result + ( ( taxonId == null ) ? 0 : taxonId.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        GeneValueObject other = ( GeneValueObject ) obj;

        if ( id != null ) {
            return other.id != null && ( Objects.equals( this.id, other.id ) );
        }
        if ( ncbiId != null ) {
            return other.ncbiId != null && Objects.equals( this.ncbiId, other.ncbiId );
        }

        if ( officialSymbol == null ) {
            if ( other.officialSymbol != null )
                return false;
        } else if ( !officialSymbol.equals( other.officialSymbol ) )
            return false;
        if ( taxonId == null ) {
            return other.taxonId == null;
        } else
            return taxonId.equals( other.taxonId );
    }

    @Override
    public String toString() {
        return "GeneValueObject [" + ( id != null ? "id=" + id + ", " : "" ) + ( officialSymbol != null ?
                "officialSymbol=" + officialSymbol + ", " :
                "" ) + ( officialName != null ? "officialName=" + officialName : "" ) + "]";
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases( Collection<String> aliases ) {
        this.aliases = aliases;
    }

    public Integer getAssociatedExperimentCount() {
        return associatedExperimentCount;
    }

    public void setAssociatedExperimentCount( Integer associatedExperimentCount ) {
        this.associatedExperimentCount = associatedExperimentCount;
    }

    public Integer getCompositeSequenceCount() {
        return compositeSequenceCount;
    }

    public void setCompositeSequenceCount( Integer compositeSequenceCount ) {
        this.compositeSequenceCount = compositeSequenceCount;
    }

    /**
     * @return public Long getTaxonId() { return taxonId; } public void setTaxonId( Long taxonId ) { this.taxonId = taxonId; }
     */
    public String getDescription() {
        return this.description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public Collection<GeneSetValueObject> getGeneSets() {
        return geneSets;
    }

    public void setGeneSets( Collection<GeneSetValueObject> geneSets ) {
        this.geneSets = geneSets;
    }

    public Collection<GeneValueObject> getHomologues() {
        return homologues;
    }

    public void setHomologues( Collection<GeneValueObject> homologues ) {
        this.homologues = homologues;
    }

    public Boolean getIsQuery() {
        return isQuery;
    }

    public void setIsQuery( Boolean isQuery ) {
        this.isQuery = isQuery;
    }

    public Double getMultifunctionalityRank() {
        return multifunctionalityRank;
    }

    public void setMultifunctionalityRank( Double multifunctionalityRank ) {
        this.multifunctionalityRank = multifunctionalityRank;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public Integer getNcbiId() {
        return this.ncbiId;
    }

    public void setNcbiId( Integer ncbiId ) {
        this.ncbiId = ncbiId;
    }

    public double[] getNodeDegreeNegRanks() {
        return nodeDegreeNegRanks;
    }

    public void setNodeDegreeNegRanks( double[] nodeDegreeNegRanks ) {
        this.nodeDegreeNegRanks = nodeDegreeNegRanks;
    }

    public double[] getNodeDegreePosRanks() {
        return nodeDegreePosRanks;
    }

    public void setNodeDegreePosRanks( double[] nodeDegreePosRanks ) {
        this.nodeDegreePosRanks = nodeDegreePosRanks;
    }

    public int[] getNodeDegreesNeg() {
        return nodeDegreesNeg;
    }

    public void setNodeDegreesNeg( int[] nodeDegreesNeg ) {
        this.nodeDegreesNeg = nodeDegreesNeg;
    }

    public int[] getNodeDegreesPos() {
        return nodeDegreesPos;
    }

    public void setNodeDegreesPos( int[] nodeDegreesPos ) {
        this.nodeDegreesPos = nodeDegreesPos;
    }

    public Integer getNumGoTerms() {
        return numGoTerms;
    }

    public void setNumGoTerms( Integer numGoTerms ) {
        this.numGoTerms = numGoTerms;
    }

    public String getOfficialName() {
        return this.officialName;
    }

    public void setOfficialName( String officialName ) {
        this.officialName = officialName;
    }

    public String getOfficialSymbol() {
        return this.officialSymbol;
    }

    public void setOfficialSymbol( String officialSymbol ) {
        this.officialSymbol = officialSymbol;
    }

    public Collection<CharacteristicValueObject> getPhenotypes() {
        return phenotypes;
    }

    public void setPhenotypes( Collection<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public Integer getPlatformCount() {
        return platformCount;
    }

    public void setPlatformCount( Integer platformCount ) {
        this.platformCount = platformCount;
    }

    public Double getScore() {
        return score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    /**
     * @return the taxonCommonName
     */
    public String getTaxonCommonName() {
        return taxonCommonName;
    }

    /**
     * @param taxonCommonName the taxonCommonName to set
     */
    public void setTaxonCommonName( String taxonCommonName ) {
        this.taxonCommonName = taxonCommonName;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public String getTaxonScientificName() {
        return taxonScientificName;
    }

    public void setTaxonScientificName( String taxonScientificName ) {
        this.taxonScientificName = taxonScientificName;
    }

    public String getEnsemblId() {
        return ensemblId;
    }
}