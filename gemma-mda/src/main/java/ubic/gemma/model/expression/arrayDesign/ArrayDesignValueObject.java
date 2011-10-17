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

import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

/**
 * Value object for quickly displaying varied information about Array Designs.
 * 
 * @version $Id$
 * @author paul et al
 */
public class ArrayDesignValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8259245319391937522L;

    private String color;

    private String dateCached;

    private java.util.Date dateCreated;

    private String description;

    private Long designElementCount;

    private Long expressionExperimentCount;

    private Boolean hasBlatAssociations;

    private Boolean hasGeneAssociations;

    private Boolean hasSequenceAssociations;

    private Long id;

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

    private String numProbesToKnownGenes;

    private String numProbesToPredictedGenes;

    private String numProbesToProbeAlignedRegions;

    private String shortName;

    private Boolean troubled = false;

    private String taxon;

    private String technologyType;

    private ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject troubleEvent;

    private ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject validationEvent;

    private boolean hasAnnotationFile;

    public ArrayDesignValueObject() {
    }

    /**
     * Copies constructor from other ArrayDesignValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public ArrayDesignValueObject( ArrayDesignValueObject otherBean ) {
        this( otherBean.getName(), otherBean.getShortName(), otherBean.getDesignElementCount(), otherBean.getTaxon(),
                otherBean.getExpressionExperimentCount(), otherBean.getHasSequenceAssociations(), otherBean
                        .getHasBlatAssociations(), otherBean.getHasGeneAssociations(), otherBean.getId(), otherBean
                        .getColor(), otherBean.getNumProbeSequences(), otherBean.getNumProbeAlignments(), otherBean
                        .getNumProbesToGenes(), otherBean.getNumProbesToProbeAlignedRegions(), otherBean
                        .getNumProbesToPredictedGenes(), otherBean.getNumProbesToKnownGenes(), otherBean.getNumGenes(),
                otherBean.getDateCached(), otherBean.getLastSequenceUpdate(), otherBean.getLastSequenceAnalysis(),
                otherBean.getLastGeneMapping(), otherBean.getIsSubsumed(), otherBean.getIsSubsumer(), otherBean
                        .getIsMerged(), otherBean.getIsMergee(), otherBean.getLastRepeatMask(), otherBean
                        .getTroubleEvent(), otherBean.getValidationEvent(), otherBean.getDateCreated(), otherBean
                        .getDescription(), otherBean.getTechnologyType() );
    }

    public ArrayDesignValueObject( String name, String shortName, Long designElementCount, String taxon,
            Long expressionExperimentCount, Boolean hasSequenceAssociations, Boolean hasBlatAssociations,
            Boolean hasGeneAssociations, Long id, String color, String numProbeSequences, String numProbeAlignments,
            String numProbesToGenes, String numProbesToProbeAlignedRegions, String numProbesToPredictedGenes,
            String numProbesToKnownGenes, String numGenes, String dateCached, java.util.Date lastSequenceUpdate,
            java.util.Date lastSequenceAnalysis, java.util.Date lastGeneMapping, Boolean isSubsumed,
            Boolean isSubsumer, Boolean isMerged, Boolean isMergee, java.util.Date lastRepeatMask,
            AuditEventValueObject troubleEvent, AuditEventValueObject validationEvent, java.util.Date dateCreated,
            String description, String technologyType ) {
        this.name = name;
        this.shortName = shortName;
        this.designElementCount = designElementCount;
        this.taxon = taxon;
        this.expressionExperimentCount = expressionExperimentCount;
        this.hasSequenceAssociations = hasSequenceAssociations;
        this.hasBlatAssociations = hasBlatAssociations;
        this.hasGeneAssociations = hasGeneAssociations;
        this.id = id;
        this.color = color;
        this.numProbeSequences = numProbeSequences;
        this.numProbeAlignments = numProbeAlignments;
        this.numProbesToGenes = numProbesToGenes;
        this.numProbesToProbeAlignedRegions = numProbesToProbeAlignedRegions;
        this.numProbesToPredictedGenes = numProbesToPredictedGenes;
        this.numProbesToKnownGenes = numProbesToKnownGenes;
        this.numGenes = numGenes;
        this.dateCached = dateCached;
        this.lastSequenceUpdate = lastSequenceUpdate;
        this.lastSequenceAnalysis = lastSequenceAnalysis;
        this.lastGeneMapping = lastGeneMapping;
        this.isSubsumed = isSubsumed;
        this.isSubsumer = isSubsumer;
        this.isMerged = isMerged;
        this.isMergee = isMergee;
        this.lastRepeatMask = lastRepeatMask;
        this.troubleEvent = troubleEvent;
        this.validationEvent = validationEvent;
        this.dateCreated = dateCreated;
        this.description = description;
        this.technologyType = technologyType;
    }

    /**
     * Copies all properties from the argument value object into this value object.
     */
    public void copy( ArrayDesignValueObject otherBean ) {
        if ( otherBean != null ) {
            this.setName( otherBean.getName() );
            this.setShortName( otherBean.getShortName() );
            this.setDesignElementCount( otherBean.getDesignElementCount() );
            this.setTaxon( otherBean.getTaxon() );
            this.setExpressionExperimentCount( otherBean.getExpressionExperimentCount() );
            this.setHasSequenceAssociations( otherBean.getHasSequenceAssociations() );
            this.setHasBlatAssociations( otherBean.getHasBlatAssociations() );
            this.setHasGeneAssociations( otherBean.getHasGeneAssociations() );
            this.setId( otherBean.getId() );
            this.setColor( otherBean.getColor() );
            this.setNumProbeSequences( otherBean.getNumProbeSequences() );
            this.setNumProbeAlignments( otherBean.getNumProbeAlignments() );
            this.setNumProbesToGenes( otherBean.getNumProbesToGenes() );
            this.setNumProbesToProbeAlignedRegions( otherBean.getNumProbesToProbeAlignedRegions() );
            this.setNumProbesToPredictedGenes( otherBean.getNumProbesToPredictedGenes() );
            this.setNumProbesToKnownGenes( otherBean.getNumProbesToKnownGenes() );
            this.setNumGenes( otherBean.getNumGenes() );
            this.setDateCached( otherBean.getDateCached() );
            this.setLastSequenceUpdate( otherBean.getLastSequenceUpdate() );
            this.setLastSequenceAnalysis( otherBean.getLastSequenceAnalysis() );
            this.setLastGeneMapping( otherBean.getLastGeneMapping() );
            this.setIsSubsumed( otherBean.getIsSubsumed() );
            this.setIsSubsumer( otherBean.getIsSubsumer() );
            this.setIsMerged( otherBean.getIsMerged() );
            this.setIsMergee( otherBean.getIsMergee() );
            this.setLastRepeatMask( otherBean.getLastRepeatMask() );
            this.setTroubleEvent( otherBean.getTroubleEvent() );
            this.setValidationEvent( otherBean.getValidationEvent() );
            this.setDateCreated( otherBean.getDateCreated() );
            this.setDescription( otherBean.getDescription() );
            this.setTechnologyType( otherBean.getTechnologyType() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ArrayDesignValueObject other = ( ArrayDesignValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        if ( shortName == null ) {
            if ( other.shortName != null ) return false;
        } else if ( !shortName.equals( other.shortName ) ) return false;
        return true;
    }

    /**
     * 
     */
    public String getColor() {
        return this.color;
    }

    /**
     * 
     */
    public String getDateCached() {
        return this.dateCached;
    }

    /**
     * <p>
     * The date the Array Design was created
     * </p>
     */
    public java.util.Date getDateCreated() {
        return this.dateCreated;
    }

    /**
     * 
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * 
     */
    public Long getDesignElementCount() {
        return this.designElementCount;
    }

    /**
     * 
     */
    public Long getExpressionExperimentCount() {
        return this.expressionExperimentCount;
    }

    /**
     * @return
     */
    public boolean getHasAnnotationFile() {
        return hasAnnotationFile;
    }

    /**
     * 
     */
    public Boolean getHasBlatAssociations() {
        return this.hasBlatAssociations;
    }

    /**
     * 
     */
    public Boolean getHasGeneAssociations() {
        return this.hasGeneAssociations;
    }

    /**
     * 
     */
    public Boolean getHasSequenceAssociations() {
        return this.hasSequenceAssociations;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * <p>
     * Indicates this array design is the merger of other array designs.
     * </p>
     */
    public Boolean getIsMerged() {
        return this.isMerged;
    }

    /**
     * <p>
     * Indicates that this array design has been merged into another.
     * </p>
     */
    public Boolean getIsMergee() {
        return this.isMergee;
    }

    /**
     * <p>
     * Indicate if this array design is subsumed by some other array design.
     * </p>
     */
    public Boolean getIsSubsumed() {
        return this.isSubsumed;
    }

    /**
     * <p>
     * Indicates if this array design subsumes some other array design(s)
     * </p>
     */
    public Boolean getIsSubsumer() {
        return this.isSubsumer;
    }

    /**
     * 
     */
    public java.util.Date getLastGeneMapping() {
        return this.lastGeneMapping;
    }

    /**
     * 
     */
    public java.util.Date getLastRepeatMask() {
        return this.lastRepeatMask;
    }

    /**
     * 
     */
    public java.util.Date getLastSequenceAnalysis() {
        return this.lastSequenceAnalysis;
    }

    /**
     * 
     */
    public java.util.Date getLastSequenceUpdate() {
        return this.lastSequenceUpdate;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * The number of unique genes that this array design maps to.
     * </p>
     */
    public String getNumGenes() {
        return this.numGenes;
    }

    /**
     * <p>
     * The number of probes that have BLAT alignments.
     * </p>
     */
    public String getNumProbeAlignments() {
        return this.numProbeAlignments;
    }

    /**
     * <p>
     * The number of probes that map to bioSequences.
     * </p>
     */
    public String getNumProbeSequences() {
        return this.numProbeSequences;
    }

    /**
     * <p>
     * The number of probes that map to genes. This count includes probe-aligned regions, predicted genes, and known
     * genes.
     * </p>
     */
    public String getNumProbesToGenes() {
        return this.numProbesToGenes;
    }

    /**
     * <p>
     * The number of probes that map to known genes.
     * </p>
     */
    public String getNumProbesToKnownGenes() {
        return this.numProbesToKnownGenes;
    }

    /**
     * <p>
     * The number of probes that map to predicted genes.
     * </p>
     */
    public String getNumProbesToPredictedGenes() {
        return this.numProbesToPredictedGenes;
    }

    /**
     * <p>
     * The number of probes that map to probe-aligned regions.
     * </p>
     */
    public String getNumProbesToProbeAlignedRegions() {
        return this.numProbesToProbeAlignedRegions;
    }

    /**
     * 
     */
    public String getShortName() {
        return this.shortName;
    }

    /**
     * 
     */
    public String getTaxon() {
        return this.taxon;
    }

    /**
     * 
     */
    public String getTechnologyType() {
        return this.technologyType;
    }

    /**
     * @return the troubled
     */
    public Boolean getTroubled() {
        return troubled;
    }

    /**
     * <p>
     * The last uncleared TroubleEvent associated with this ArrayDesign.
     * </p>
     */
    public AuditEventValueObject getTroubleEvent() {
        return this.troubleEvent;
    }

    /**
     * The last uncleared TroubleEvent associated with this ArrayDesign.
     */
    public AuditEventValueObject getValidationEvent() {
        return this.validationEvent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        result = prime * result + ( ( shortName == null ) ? 0 : shortName.hashCode() );
        return result;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public void setDateCached( String dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDateCreated( java.util.Date dateCreated ) {
        this.dateCreated = dateCreated;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setDesignElementCount( Long designElementCount ) {
        this.designElementCount = designElementCount;
    }

    public void setExpressionExperimentCount( Long expressionExperimentCount ) {
        this.expressionExperimentCount = expressionExperimentCount;
    }

    public void setHasAnnotationFile( boolean b ) {
        this.hasAnnotationFile = b;
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

    public void setId( Long id ) {
        this.id = id;
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

    public void setNumProbesToKnownGenes( String numProbesToKnownGenes ) {
        this.numProbesToKnownGenes = numProbesToKnownGenes;
    }

    public void setNumProbesToPredictedGenes( String numProbesToPredictedGenes ) {
        this.numProbesToPredictedGenes = numProbesToPredictedGenes;
    }

    public void setNumProbesToProbeAlignedRegions( String numProbesToProbeAlignedRegions ) {
        this.numProbesToProbeAlignedRegions = numProbesToProbeAlignedRegions;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
    }

    /**
     * @param troubled the troubled to set
     */
    public void setTroubled( Boolean troubled ) {
        this.troubled = troubled;
    }

    public void setTroubleEvent( AuditEventValueObject troubleEvent ) {
        this.troubleEvent = troubleEvent;
    }

    public void setValidationEvent( AuditEventValueObject validationEvent ) {
        this.validationEvent = validationEvent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getShortName();
    }

    // ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject value-object java merge-point
}