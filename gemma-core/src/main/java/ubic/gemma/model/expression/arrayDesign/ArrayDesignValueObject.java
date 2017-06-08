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

import org.hibernate.ScrollableResults;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.curation.AbstractCuratableValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;

/**
 * Value object for quickly displaying varied information about Array Designs.
 *
 * @author paul et al
 */
public class ArrayDesignValueObject extends AbstractCuratableValueObject<ArrayDesign>
        implements java.io.Serializable, Comparable<ArrayDesignValueObject> {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8259245319391937522L;

    private String color;
    private String dateCached;
    private String description;
    private Integer designElementCount;
    private Integer expressionExperimentCount;
    private Boolean hasBlatAssociations;
    private Boolean hasGeneAssociations;
    private Boolean hasSequenceAssociations;
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
    private String taxon;
    private String technologyType;

    private boolean hasAnnotationFile;

    public ArrayDesignValueObject( Long id ) {
        super( id );
    }

    /**
     * Copies constructor from other ArrayDesignValueObject
     *
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public ArrayDesignValueObject( ArrayDesignValueObject otherBean ) {
        this( otherBean.lastUpdated, otherBean.troubled, otherBean.lastTroubledEvent, otherBean.needsAttention,
                otherBean.lastNeedsAttentionEvent, otherBean.curationNote, otherBean.lastNoteUpdateEvent,
                otherBean.color, otherBean.dateCached, otherBean.description, otherBean.designElementCount,
                otherBean.expressionExperimentCount, otherBean.hasBlatAssociations, otherBean.hasGeneAssociations,
                otherBean.hasSequenceAssociations, otherBean.id, otherBean.isMerged, otherBean.isMergee,
                otherBean.isSubsumed, otherBean.isSubsumer, otherBean.lastGeneMapping, otherBean.lastRepeatMask,
                otherBean.lastSequenceAnalysis, otherBean.lastSequenceUpdate, otherBean.name, otherBean.numGenes,
                otherBean.numProbeAlignments, otherBean.numProbeSequences, otherBean.numProbesToGenes,
                otherBean.shortName, otherBean.taxon, otherBean.technologyType );
    }

    /**
     * This will only work if the object is thawed (lightly). Not everything will be filled in -- test before using!
     */
    public ArrayDesignValueObject( ArrayDesign ad ) {
        this( ad, null );
    }

    public ArrayDesignValueObject( ArrayDesign ad, Map<Long, Integer> eeCounts ) {
        super( ad );
        this.name = ad.getName();
        this.shortName = ad.getShortName();

        if ( ad.getTechnologyType() != null ) {
            this.technologyType = ad.getTechnologyType().toString();
            this.color = ad.getTechnologyType().getValue();
        }

        this.description = ad.getDescription();
        this.isMergee = ad.getMergedInto() != null;
        this.taxon = ad.getPrimaryTaxon().getCommonName();

        this.expressionExperimentCount = ( eeCounts == null || !eeCounts.containsKey( this.getId() ) ) ?
                0 :
                eeCounts.get( this.getId() );
    }

    /**
     * Creates a new array design from a given scrollable list.
     * ! Does not populate curation events ! - call {@link ArrayDesignDaoImpl#addCurationEvents} with the created instance.
     *
     * @param list     the list has to have its values correctly populated.
     *                 See {@link ArrayDesignDaoImpl#getEEValueObjectQueryString()}
     * @param eeCounts map containing the counts of EEs in ADs. If it also contains the count for
     *                 the AD that this VO will represent, the appropriate value in this VO will be populated.
     */
    public ArrayDesignValueObject( ScrollableResults list, Map<Long, Integer> eeCounts ) {
        super( list.getLong( 0 ) );
        this.name = list.getString( 1 );
        this.shortName = list.getString( 2 );

        TechnologyType color = ( TechnologyType ) list.get( 3 );
        if ( color != null ) {
            this.technologyType = color.toString();
            this.color = color.getValue();
        }

        this.description = list.getString( 4 );
        this.isMergee = list.get( 5 ) != null;

        this.lastUpdated = list.getDate( 6 );
        this.troubled = list.getBoolean( 7 );
        this.needsAttention = list.getBoolean( 8 );
        this.curationNote = list.getString( 9 );

        this.taxon = list.getString( 10 );

        this.expressionExperimentCount = ( eeCounts == null || !eeCounts.containsKey( this.getId() ) ) ?
                0 :
                eeCounts.get( this.getId() );
    }

    public ArrayDesignValueObject( Date lastUpdated, Boolean troubled, AuditEventValueObject troubledEvent,
            Boolean needsAttention, AuditEventValueObject needsAttentionEvent, String curationNote,
            AuditEventValueObject noteEvent, String color, String dateCached, String description,
            Integer designElementCount, Integer expressionExperimentCount, Boolean hasBlatAssociations,
            Boolean hasGeneAssociations, Boolean hasSequenceAssociations, Long id, Boolean isMerged, Boolean isMergee,
            Boolean isSubsumed, Boolean isSubsumer, Date lastGeneMapping, Date lastRepeatMask,
            Date lastSequenceAnalysis, Date lastSequenceUpdate, String name, String numGenes, String numProbeAlignments,
            String numProbeSequences, String numProbesToGenes, String shortName, String taxon, String technologyType ) {
        super( id, lastUpdated, troubled, troubledEvent, needsAttention, needsAttentionEvent, curationNote, noteEvent );
        this.color = color;
        this.dateCached = dateCached;
        this.description = description;
        this.designElementCount = designElementCount;
        this.expressionExperimentCount = expressionExperimentCount;
        this.hasBlatAssociations = hasBlatAssociations;
        this.hasGeneAssociations = hasGeneAssociations;
        this.hasSequenceAssociations = hasSequenceAssociations;
        this.isMerged = isMerged;
        this.isMergee = isMergee;
        this.isSubsumed = isSubsumed;
        this.isSubsumer = isSubsumer;
        this.lastGeneMapping = lastGeneMapping;
        this.lastRepeatMask = lastRepeatMask;
        this.lastSequenceAnalysis = lastSequenceAnalysis;
        this.lastSequenceUpdate = lastSequenceUpdate;
        this.name = name;
        this.numGenes = numGenes;
        this.numProbeAlignments = numProbeAlignments;
        this.numProbeSequences = numProbeSequences;
        this.numProbesToGenes = numProbesToGenes;
        this.shortName = shortName;
        this.taxon = taxon;
        this.technologyType = technologyType;
    }

    public static Collection<ArrayDesignValueObject> create( Collection<ArrayDesign> subsumees ) {
        Collection<ArrayDesignValueObject> r = new HashSet<>();
        for ( ArrayDesign ad : subsumees ) {
            r.add( new ArrayDesignValueObject( ad ) );
        }
        return r;
    }

    @Override
    public int compareTo( ArrayDesignValueObject arg0 ) {
        if ( arg0.getId() == null || this.getId() == null )
            return 0;
        return arg0.getId().compareTo( this.getId() );
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
            if ( other.shortName != null )
                return false;
        } else if ( !shortName.equals( other.shortName ) )
            return false;
        return true;
    }

    public String getColor() {
        return this.color;
    }

    public void setColor( String color ) {
        this.color = color;
    }

    public String getDateCached() {
        return this.dateCached;
    }

    public void setDateCached( String dateCached ) {
        this.dateCached = dateCached;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public Integer getDesignElementCount() {
        return this.designElementCount;
    }

    public void setDesignElementCount( Integer designElementCount ) {
        this.designElementCount = designElementCount;
    }

    public Integer getExpressionExperimentCount() {
        return this.expressionExperimentCount;
    }

    public void setExpressionExperimentCount( Integer expressionExperimentCount ) {
        this.expressionExperimentCount = expressionExperimentCount;
    }

    public boolean getHasAnnotationFile() {
        return hasAnnotationFile;
    }

    public void setHasAnnotationFile( boolean b ) {
        this.hasAnnotationFile = b;
    }

    public Boolean getHasBlatAssociations() {
        return this.hasBlatAssociations;
    }

    public void setHasBlatAssociations( Boolean hasBlatAssociations ) {
        this.hasBlatAssociations = hasBlatAssociations;
    }

    public Boolean getHasGeneAssociations() {
        return this.hasGeneAssociations;
    }

    public void setHasGeneAssociations( Boolean hasGeneAssociations ) {
        this.hasGeneAssociations = hasGeneAssociations;
    }

    public Boolean getHasSequenceAssociations() {
        return this.hasSequenceAssociations;
    }

    public void setHasSequenceAssociations( Boolean hasSequenceAssociations ) {
        this.hasSequenceAssociations = hasSequenceAssociations;
    }

    /**
     * <p>
     * Indicates this array design is the merger of other array designs.
     * </p>
     */
    public Boolean getIsMerged() {
        return this.isMerged;
    }

    public void setIsMerged( Boolean isMerged ) {
        this.isMerged = isMerged;
    }

    /**
     * <p>
     * Indicates that this array design has been merged into another.
     * </p>
     */
    public Boolean getIsMergee() {
        return this.isMergee;
    }

    public void setIsMergee( Boolean isMergee ) {
        this.isMergee = isMergee;
    }

    /**
     * <p>
     * Indicate if this array design is subsumed by some other array design.
     * </p>
     */
    public Boolean getIsSubsumed() {
        return this.isSubsumed;
    }

    public void setIsSubsumed( Boolean isSubsumed ) {
        this.isSubsumed = isSubsumed;
    }

    /**
     * <p>
     * Indicates if this array design subsumes some other array design(s)
     * </p>
     */
    public Boolean getIsSubsumer() {
        return this.isSubsumer;
    }

    public void setIsSubsumer( Boolean isSubsumer ) {
        this.isSubsumer = isSubsumer;
    }

    public java.util.Date getLastGeneMapping() {
        return this.lastGeneMapping;
    }

    public void setLastGeneMapping( java.util.Date lastGeneMapping ) {
        this.lastGeneMapping = lastGeneMapping;
    }

    public java.util.Date getLastRepeatMask() {
        return this.lastRepeatMask;
    }

    public void setLastRepeatMask( java.util.Date lastRepeatMask ) {
        this.lastRepeatMask = lastRepeatMask;
    }

    public java.util.Date getLastSequenceAnalysis() {
        return this.lastSequenceAnalysis;
    }

    public void setLastSequenceAnalysis( java.util.Date lastSequenceAnalysis ) {
        this.lastSequenceAnalysis = lastSequenceAnalysis;
    }

    public java.util.Date getLastSequenceUpdate() {
        return this.lastSequenceUpdate;
    }

    public void setLastSequenceUpdate( java.util.Date lastSequenceUpdate ) {
        this.lastSequenceUpdate = lastSequenceUpdate;
    }

    public String getName() {
        return this.name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    /**
     * <p>
     * The number of unique genes that this array design maps to.
     * </p>
     */
    public String getNumGenes() {
        return this.numGenes;
    }

    public void setNumGenes( String numGenes ) {
        this.numGenes = numGenes;
    }

    /**
     * <p>
     * The number of probes that have BLAT alignments.
     * </p>
     */
    public String getNumProbeAlignments() {
        return this.numProbeAlignments;
    }

    public void setNumProbeAlignments( String numProbeAlignments ) {
        this.numProbeAlignments = numProbeAlignments;
    }

    /**
     * <p>
     * The number of probes that map to bioSequences.
     * </p>
     */
    public String getNumProbeSequences() {
        return this.numProbeSequences;
    }

    public void setNumProbeSequences( String numProbeSequences ) {
        this.numProbeSequences = numProbeSequences;
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

    public void setNumProbesToGenes( String numProbesToGenes ) {
        this.numProbesToGenes = numProbesToGenes;
    }

    public String getShortName() {
        return this.shortName;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public String getTaxon() {
        return this.taxon;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    public String getTechnologyType() {
        return this.technologyType;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
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

    @Override
    public String toString() {
        return this.getShortName();
    }

}