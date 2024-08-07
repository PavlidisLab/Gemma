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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.Hibernate;
import ubic.gemma.model.annotations.GemmaRestOnly;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author kelsey
 */
@Data
@EqualsAndHashCode(of = { "ncbiId", "officialSymbol", "taxon" }, callSuper = true)
public class GeneValueObject extends IdentifiableValueObject<Gene> implements Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7098036090107647318L;
    /**
     * Gene aliases, sorted alphabetically.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private SortedSet<String> aliases;
    /**
     * How many experiments "involve" (manipulate, etc.) this gene
     */
    @JsonIgnore
    private Integer associatedExperimentCount = 0;
    @JsonIgnore
    private Integer compositeSequenceCount = 0; // number of probes
    @JsonIgnore
    private String description;
    @JsonIgnore
    private Collection<GeneSetValueObject> geneSets = null;
    @JsonIgnore
    private Collection<GeneValueObject> homologues = null;
    /**
     * Was this gene directly used in a query? Or is it inferred somehow. The default is true, use this when you need to
     * differentiate
     */
    @JsonIgnore
    private Boolean isQuery = true;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Double multifunctionalityRank;
    @JsonIgnore
    private String name;
    private Integer ncbiId;
    private String ensemblId;
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<DatabaseEntryValueObject> accessions;
    @JsonIgnore
    private double[] nodeDegreeNegRanks;
    @JsonIgnore
    private double[] nodeDegreePosRanks;
    /**
     * Array containing number of links supported by 0,1,2, .... data sets (value in first index is always 0) for
     * negative correlation coexpression
     */
    @JsonIgnore
    private int[] nodeDegreesNeg;
    /**
     * Array containing number of links supported by 0,1,2, .... data sets (value in first index is always 0) for
     * positive coexpression
     */
    @JsonIgnore
    private int[] nodeDegreesPos;
    @JsonIgnore
    private Integer numGoTerms = 0;
    private String officialName;
    private String officialSymbol;
    @JsonIgnore
    private Collection<CharacteristicValueObject> phenotypes;
    @JsonIgnore
    private Integer platformCount;
    @JsonIgnore
    private Double score; // This is for genes in gene sets might have a rank or a score associated with them.

    // those are serialized manually for the REST API
    @Nullable
    @JsonIgnore
    private TaxonValueObject taxon;
    @JsonIgnore
    private boolean includeTaxon = true;

    /**
     * Required when using the class as a spring bean.
     */
    public GeneValueObject() {
        super();
    }

    public GeneValueObject( Long id ) {
        super( id );
    }

    /**
     * @param gene gene
     */
    public GeneValueObject( Gene gene ) {
        super( gene );
        this.ncbiId = gene.getNcbiGeneId();
        this.officialName = gene.getOfficialName();
        this.officialSymbol = gene.getOfficialSymbol();
        if ( gene.getTaxon() != null ) {
            this.taxon = new TaxonValueObject( gene.getTaxon() );
        }
        this.name = gene.getName();
        this.description = gene.getDescription();
        this.ensemblId = gene.getEnsemblId();
        if ( gene.getMultifunctionality() != null && Hibernate.isInitialized( gene.getMultifunctionality() ) ) {
            this.multifunctionalityRank = gene.getMultifunctionality().getRank();
        } else {
            this.multifunctionalityRank = null;
        }
        if ( gene.getAliases() != null && Hibernate.isInitialized( gene.getAliases() ) ) {
            this.aliases = gene.getAliases().stream().map( GeneAlias::getAlias ).collect( Collectors.toCollection( TreeSet::new ) );
        }
        if ( Hibernate.isInitialized( gene.getAccessions() ) ) {
            this.accessions = gene.getAccessions().stream()
                    .map( DatabaseEntryValueObject::new )
                    .collect( Collectors.toSet() );
        }
    }

    /**
     * @param includeTaxon include full taxon details in {@link #getTaxonForGemmaRest()}, otherwise only
     *                     {@link #getTaxonIdForGemmaRest()} is filled
     */
    public GeneValueObject( Gene gene, boolean includeTaxon ) {
        this( gene );
        this.includeTaxon = includeTaxon;
    }

    /**
     * Copies constructor from other GeneValueObject
     *
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public GeneValueObject( GeneValueObject otherBean ) {
        super( otherBean );
        this.name = otherBean.name;
        this.ncbiId = otherBean.ncbiId;
        this.officialSymbol = otherBean.officialSymbol;
        this.officialName = otherBean.officialName;
        this.description = otherBean.description;
        this.score = otherBean.score;
        this.taxon = otherBean.taxon;
        this.includeTaxon = otherBean.includeTaxon;
        if ( otherBean.aliases != null ) {
            this.aliases = new TreeSet<>( otherBean.aliases );
        }
        this.multifunctionalityRank = otherBean.multifunctionalityRank;
        if ( otherBean.accessions != null ) {
            this.accessions = new HashSet<>( otherBean.accessions );
        }
    }

    public GeneValueObject( Long geneId, String geneSymbol, String geneOfficialName, Taxon taxon ) {
        super( geneId );
        this.officialSymbol = geneSymbol;
        this.officialName = geneOfficialName;
        this.taxon = new TaxonValueObject( taxon );
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
        SortedSet<String> aliases = new TreeSet<>();
        for ( GeneAlias ga : gene.getAliases() ) {
            aliases.add( ga.getAlias() );
        }
        geneValueObject.setAliases( aliases );
    }

    @GemmaWebOnly
    public Long getTaxonId() {
        return taxon == null ? null : taxon.getId();
    }

    @GemmaRestOnly
    @JsonProperty("taxon")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is mutually exclusive with `taxonId`.")
    public TaxonValueObject getTaxonForGemmaRest() {
        return includeTaxon ? taxon : null;
    }

    @GemmaRestOnly
    @JsonProperty("taxonId")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "This property is mutually exclusive with `taxon`.")
    public Long getTaxonIdForGemmaRest() {
        return !includeTaxon && taxon != null ? taxon.getId() : null;
    }

    @GemmaWebOnly
    public String getTaxonCommonName() {
        return taxon == null ? null : taxon.getCommonName();
    }

    @GemmaWebOnly
    public String getTaxonScientificName() {
        return taxon == null ? null : taxon.getScientificName();
    }

    @Override
    public String toString() {
        return "GeneValueObject [" + ( id != null ? "id=" + id + ", " : "" ) + ( officialSymbol != null ?
                "officialSymbol=" + officialSymbol + ", " :
                "" ) + ( officialName != null ? "officialName=" + officialName : "" ) + "]";
    }
}