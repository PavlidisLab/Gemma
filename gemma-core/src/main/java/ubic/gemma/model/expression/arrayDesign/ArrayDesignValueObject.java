/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.model.expression.arrayDesign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.model.common.description.DatabaseEntryValueObject;
import ubic.gemma.model.common.description.Versioned;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.model.util.ModelUtils;

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Value object for quickly displaying varied information about Array Designs.
 *
 * @author paul et al
 */
@SuppressWarnings("unused") // Used in front end
@Data
@EqualsAndHashCode(of = { "shortName" }, callSuper = true)
@CommonsLog
public class ArrayDesignValueObject extends AbstractCuratableValueObject<ArrayDesign> implements Versioned {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8259245319391937522L;

    public static Collection<ArrayDesignValueObject> create( Collection<ArrayDesign> subsumees ) {
        Collection<ArrayDesignValueObject> r = new HashSet<>();
        for ( ArrayDesign ad : subsumees ) {
            r.add( new ArrayDesignValueObject( ad ) );
        }
        return r;
    }

    @JsonIgnore
    private Boolean blackListed = false;
    @Deprecated
    @Schema(implementation = TechnologyType.class)
    private String color; // FIXME redundant with technologyType
    @JsonIgnore
    private String dateCached;
    private String description;
    @JsonIgnore
    private Integer designElementCount;
    private Long expressionExperimentCount;
    @JsonIgnore
    private Boolean hasBlatAssociations;

    @JsonIgnore
    private Boolean hasGeneAssociations;

    @JsonIgnore
    private Boolean hasSequenceAssociations;
    @JsonIgnore
    private Boolean isAffymetrixAltCdf = false;
    /**
     * Indicates this array design is the merger of other array designs.
     */
    private Boolean isMerged;
    /**
     * Indicates that this array design has been merged into another.
     */
    private Boolean isMergee;
    /**
     * Indicate if this array design is subsumed by some other array design.
     */
    @JsonIgnore
    private Boolean isSubsumed;
    /**
     * Indicates if this array design subsumes some other array design(s)
     */
    @JsonIgnore
    private Boolean isSubsumer;
    @JsonIgnore
    private Date lastGeneMapping;
    @JsonIgnore
    private Date lastRepeatMask;
    @JsonIgnore
    private Date lastSequenceAnalysis;
    @JsonIgnore
    private Date lastSequenceUpdate;
    private String name;


    @JsonIgnore
    private Date createDate;

    /**
     * The number of unique genes that this array design maps to.
     */
    @JsonIgnore
    private String numGenes;
    /**
     * The number of probes that have BLAT alignments.
     */
    @JsonIgnore
    private String numProbeAlignments;
    /**
     * The number of probes that map to bioSequences.
     */
    @JsonIgnore
    private String numProbeSequences;
    /**
     * The number of probes that map to genes. This count includes probe-aligned regions, predicted genes, and known
     * genes.
     */
    @JsonIgnore
    private String numProbesToGenes;
    private String shortName;
    @JsonProperty("numberOfSwitchedExpressionExperiments")
    private Long switchedExpressionExperimentCount = 0L; // how many "hidden" associations there are.
    @Nullable
    @JsonProperty("taxon")
    private TaxonValueObject taxonObject;
    @Schema(implementation = TechnologyType.class)
    private String technologyType;

    // for the Versioned interface
    private String releaseVersion;
    private URL releaseUrl;


    /**
     * Main external reference.
     */
    @Nullable
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Set<DatabaseEntryValueObject> externalReferences;

    public ArrayDesignValueObject() {
        super();
    }

    public ArrayDesignValueObject( Long id ) {
        super( id );
    }

    /**
     * This will only work if the object is thawed (lightly). Not everything will be filled in -- test before using!
     *
     * @param ad ad
     */
    public ArrayDesignValueObject( ArrayDesign ad ) {
        super( ad );
        this.name = ad.getName();
        this.shortName = ad.getShortName();
        this.description = ad.getDescription();
        if ( ad.getPrimaryTaxon() != null ) {
            this.taxonObject = new TaxonValueObject( ad.getPrimaryTaxon() );
        } else {
            this.taxonObject = null;
        }
        if ( ad.getTechnologyType() != null ) {
            this.technologyType = ad.getTechnologyType().toString();
        }

        TechnologyType c = ad.getTechnologyType();
        if ( c != null ) {
            this.technologyType = c.toString();
            this.color = c.name();
        }

        // no need to initialize them to know if the entities exist
        this.isMergee = ad.getMergedInto() != null;
        this.isAffymetrixAltCdf = ad.getAlternativeTo() != null;

        if ( ModelUtils.isInitialized( ad.getExternalReferences() ) ) {
            this.externalReferences = ad.getExternalReferences().stream()
                    .map( DatabaseEntryValueObject::new )
                    .collect( Collectors.toSet() );
            for ( DatabaseEntryValueObject de : externalReferences ) {
                if ( de.getAccession().startsWith( "GPL" ) ) {
                    try {
                        releaseUrl = new URL( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=" + de.getAccession() );
                        break;
                    } catch ( MalformedURLException e ) {
                        log.warn( String.format( "Failed to form release URL for %s: %s.", ad, e.getMessage() ) );
                    }
                }
            }
        }
    }

    /**
     * Copies constructor from other ArrayDesignValueObject
     */
    protected ArrayDesignValueObject( ArrayDesignValueObject arrayDesignValueObject ) {
        super( arrayDesignValueObject );
        this.color = arrayDesignValueObject.color;
        this.dateCached = arrayDesignValueObject.dateCached;
        this.description = arrayDesignValueObject.description;
        this.designElementCount = arrayDesignValueObject.designElementCount;
        this.expressionExperimentCount = arrayDesignValueObject.expressionExperimentCount;
        this.hasBlatAssociations = arrayDesignValueObject.hasBlatAssociations;
        this.hasGeneAssociations = arrayDesignValueObject.hasGeneAssociations;
        this.hasSequenceAssociations = arrayDesignValueObject.hasSequenceAssociations;
        this.isMerged = arrayDesignValueObject.isMerged;
        this.isMergee = arrayDesignValueObject.isMergee;
        this.isSubsumed = arrayDesignValueObject.isSubsumed;
        this.isSubsumer = arrayDesignValueObject.isSubsumer;
        this.lastGeneMapping = arrayDesignValueObject.lastGeneMapping;
        this.lastRepeatMask = arrayDesignValueObject.lastRepeatMask;
        this.lastSequenceAnalysis = arrayDesignValueObject.lastSequenceAnalysis;
        this.lastSequenceUpdate = arrayDesignValueObject.lastSequenceUpdate;
        this.name = arrayDesignValueObject.name;
        this.numGenes = arrayDesignValueObject.numGenes;
        this.numProbeAlignments = arrayDesignValueObject.numProbeAlignments;
        this.numProbeSequences = arrayDesignValueObject.numProbeSequences;
        this.numProbesToGenes = arrayDesignValueObject.numProbesToGenes;
        this.shortName = arrayDesignValueObject.shortName;
        this.taxonObject = arrayDesignValueObject.taxonObject;
        this.technologyType = arrayDesignValueObject.technologyType;
        this.isAffymetrixAltCdf = arrayDesignValueObject.isAffymetrixAltCdf;
        this.blackListed = arrayDesignValueObject.blackListed;
        this.externalReferences = arrayDesignValueObject.externalReferences;
        this.switchedExpressionExperimentCount = arrayDesignValueObject.switchedExpressionExperimentCount;
        this.releaseVersion = arrayDesignValueObject.releaseVersion;
        this.releaseUrl = arrayDesignValueObject.releaseUrl;
        this.createDate = arrayDesignValueObject.createDate;
    }

    /**
     * @deprecated use {@link #getNumberOfExpressionExperiments()} instead.
     */
    @Deprecated
    public Long getExpressionExperimentCount() {
        return expressionExperimentCount;
    }

    public Long getNumberOfExpressionExperiments() {
        return expressionExperimentCount;
    }

    @GemmaWebOnly
    public String getTaxon() {
        return taxonObject == null ? null : taxonObject.getCommonName();
    }

    /**
     * @deprecated use {@link #getTaxonObject()} instead
     */
    @Deprecated
    public Long getTaxonID() {
        return taxonObject == null ? null : taxonObject.getId();
    }

    @Override
    public String toString() {
        return this.getShortName();
    }
}
