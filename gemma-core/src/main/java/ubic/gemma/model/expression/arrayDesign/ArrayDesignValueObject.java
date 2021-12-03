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

import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Value object for quickly displaying varied information about Array Designs.
 *
 * @author paul et al
 */
@SuppressWarnings("unused") // Used in front end
public class ArrayDesignValueObject extends AbstractCuratableValueObject<ArrayDesign> implements Serializable {
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

    private Boolean blackListed = false;
    private String color; // FIXME redundant with technologyType
    private String dateCached;
    private String description;
    private Integer designElementCount;
    private Integer expressionExperimentCount;
    private Boolean hasBlatAssociations;

    private Boolean hasGeneAssociations;

    private Boolean hasSequenceAssociations;
    private Boolean isAffymetrixAltCdf = false;
    private Boolean isMerged;
    private Boolean isMergee;
    private Boolean isSubsumed;
    private Boolean isSubsumer;
    private java.util.Date lastGeneMapping;
    private java.util.Date lastRepeatMask;
    private java.util.Date lastSequenceAnalysis;
    private java.util.Date lastSequenceUpdate;
    private String name;
    private String numGenes;
    private String numProbeAlignments;
    private String numProbeSequences;
    private String numProbesToGenes;
    private String shortName;
    private Integer switchedExpressionExperimentCount = 0; // how many "hidden" assocations there are.
    private String taxon;
    private Long taxonID;

    private String technologyType;

    /**
     * Required when using the class as a spring bean.
     */
    public ArrayDesignValueObject() {
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
        this.taxon = ad.getPrimaryTaxon().getCommonName();
        this.taxonID = ad.getPrimaryTaxon().getId();
        if ( ad.getTechnologyType() != null ) {
            this.technologyType = ad.getTechnologyType().toString();
        }

        TechnologyType c = ad.getTechnologyType();
        if ( c != null ) {
            this.technologyType = c.toString();
            this.color = c.getValue();
        }

        // no need to initialize them to know if the entities exist
        this.isMergee = ad.getMergedInto() != null;
        this.isAffymetrixAltCdf = ad.getAlternativeTo() != null;
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
        this.taxon = arrayDesignValueObject.taxon;
        this.taxonID = arrayDesignValueObject.taxonID;
        this.technologyType = arrayDesignValueObject.technologyType;
        this.isAffymetrixAltCdf = arrayDesignValueObject.isAffymetrixAltCdf;
        this.blackListed = arrayDesignValueObject.blackListed;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        ArrayDesignValueObject other = ( ArrayDesignValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) {
                return false;
            }
        } else if ( !id.equals( other.id ) )
            return false;
        if ( shortName == null ) {
            return other.shortName == null;
        }
        return shortName.equals( other.shortName );
    }

    public Boolean getBlackListed() {
        return blackListed;
    }

    public String getColor() {
        return this.color;
    }

    public String getDateCached() {
        return this.dateCached;
    }

    public String getDescription() {
        return this.description;
    }

    public Integer getDesignElementCount() {
        return this.designElementCount;
    }

    public Integer getExpressionExperimentCount() {
        return this.expressionExperimentCount;
    }

    public Boolean getHasBlatAssociations() {
        return this.hasBlatAssociations;
    }

    public Boolean getHasGeneAssociations() {
        return this.hasGeneAssociations;
    }

    public Boolean getHasSequenceAssociations() {
        return this.hasSequenceAssociations;
    }

    public Boolean getIsAffymetrixAltCdf() {
        return isAffymetrixAltCdf;
    }

    /**
     * @return Indicates this array design is the merger of other array designs.
     */
    public Boolean getIsMerged() {
        return this.isMerged;
    }

    /**
     * @return Indicates that this array design has been merged into another.
     */
    public Boolean getIsMergee() {
        return this.isMergee;
    }

    /**
     * @return Indicate if this array design is subsumed by some other array design.
     */
    public Boolean getIsSubsumed() {
        return this.isSubsumed;
    }

    /**
     * @return Indicates if this array design subsumes some other array design(s)
     */
    public Boolean getIsSubsumer() {
        return this.isSubsumer;
    }

    public java.util.Date getLastGeneMapping() {
        return this.lastGeneMapping;
    }

    public java.util.Date getLastRepeatMask() {
        return this.lastRepeatMask;
    }

    public java.util.Date getLastSequenceAnalysis() {
        return this.lastSequenceAnalysis;
    }

    public java.util.Date getLastSequenceUpdate() {
        return this.lastSequenceUpdate;
    }

    public String getName() {
        return this.name;
    }

    /**
     * @return The number of unique genes that this array design maps to.
     */
    public String getNumGenes() {
        return this.numGenes;
    }

    /**
     * @return The number of probes that have BLAT alignments.
     */
    public String getNumProbeAlignments() {
        return this.numProbeAlignments;
    }

    /**
     * @return The number of probes that map to bioSequences.
     */
    public String getNumProbeSequences() {
        return this.numProbeSequences;
    }

    /**
     * @return The number of probes that map to genes. This count includes probe-aligned regions, predicted genes, and
     *         known
     *         genes.
     */
    public String getNumProbesToGenes() {
        return this.numProbesToGenes;
    }

    public String getShortName() {
        return this.shortName;
    }

    public Integer getSwitchedExpressionExperimentCount() {
        return switchedExpressionExperimentCount;
    }

    public String getTaxon() {
        return this.taxon;
    }

    public Long getTaxonID() {
        return taxonID;
    }

    public String getTechnologyType() {
        return this.technologyType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        if ( id == null ) {
            result = prime * result + ( ( shortName == null ) ? 0 : shortName.hashCode() );
        }
        return result;
    }

    public void setBlackListed( Boolean blackListed ) {
        this.blackListed = blackListed;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public void setDateCached( String dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setDesignElementCount( Integer designElementCount ) {
        this.designElementCount = designElementCount;
    }

    public void setExpressionExperimentCount( Integer expressionExperimentCount ) {
        this.expressionExperimentCount = expressionExperimentCount;
    }

    public void setHasBlatAssociations( Boolean hasBlatAssociations ) {
        this.hasBlatAssociations = hasBlatAssociations;
    }

    public void setHasGeneAssociations( Boolean hasGeneAssociations ) {
        this.hasGeneAssociations = hasGeneAssociations;
    }

    public void setHasSequenceAssociations( Boolean hasSequenceAssociations ) {
        this.hasSequenceAssociations = hasSequenceAssociations;
    }

    public void setIsAffymetrixAltCdf( Boolean isAffymetrixAltCdf ) {
        this.isAffymetrixAltCdf = isAffymetrixAltCdf;
    }

    public void setIsMerged( Boolean isMerged ) {
        this.isMerged = isMerged;
    }

    public void setIsMergee( Boolean isMergee ) {
        this.isMergee = isMergee;
    }

    public void setIsSubsumed( Boolean isSubsumed ) {
        this.isSubsumed = isSubsumed;
    }

    public void setIsSubsumer( Boolean isSubsumer ) {
        this.isSubsumer = isSubsumer;
    }

    public void setLastGeneMapping( java.util.Date lastGeneMapping ) {
        this.lastGeneMapping = lastGeneMapping;
    }

    public void setLastRepeatMask( java.util.Date lastRepeatMask ) {
        this.lastRepeatMask = lastRepeatMask;
    }

    public void setLastSequenceAnalysis( java.util.Date lastSequenceAnalysis ) {
        this.lastSequenceAnalysis = lastSequenceAnalysis;
    }

    public void setLastSequenceUpdate( java.util.Date lastSequenceUpdate ) {
        this.lastSequenceUpdate = lastSequenceUpdate;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumGenes( String numGenes ) {
        this.numGenes = numGenes;
    }

    public void setNumProbeAlignments( String numProbeAlignments ) {
        this.numProbeAlignments = numProbeAlignments;
    }

    public void setNumProbeSequences( String numProbeSequences ) {
        this.numProbeSequences = numProbeSequences;
    }

    public void setNumProbesToGenes( String numProbesToGenes ) {
        this.numProbesToGenes = numProbesToGenes;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSwitchedExpressionExperimentCount( Integer switchedExpressionExperimentCount ) {
        this.switchedExpressionExperimentCount = switchedExpressionExperimentCount;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public void setTaxonID( Long taxonID ) {
        this.taxonID = taxonID;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
    }

    @Override
    public String toString() {
        return this.getShortName();
    }

}