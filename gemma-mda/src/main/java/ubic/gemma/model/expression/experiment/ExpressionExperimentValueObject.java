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

package ubic.gemma.model.expression.experiment;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;

/**
 * @author kelsey
 * @version
 */
public class ExpressionExperimentValueObject implements java.io.Serializable {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5678747537830051610L;

    public ExpressionExperimentValueObject() {
    }

    public ExpressionExperimentValueObject( java.lang.Long id, java.lang.String name,
            java.lang.String externalDatabase, java.lang.String externalUri, java.lang.String source,
            java.lang.String accession, long bioAssayCount, java.lang.String taxon, java.lang.Long bioMaterialCount,
            long designElementDataVectorCount, long arrayDesignCount, java.lang.String shortName,
            java.lang.String linkAnalysisEventType, java.util.Date dateArrayDesignLastUpdated,
            AuditEventValueObject validatedFlag, java.lang.String technologyType, boolean hasBothIntensities,
            java.lang.Integer numAnnotations, java.lang.Integer numPopulatedFactors,
            java.util.Date dateDifferentialAnalysis, java.lang.String differentialAnalysisEventType,
            java.util.Collection sampleRemovedFlags, boolean isPublic, java.lang.String clazz,
            java.lang.Long sourceExperiment, java.lang.Long differentialExpressionAnalysisId,
            java.lang.Integer pubmedId, java.lang.String investigators, java.lang.String owner,
            java.util.Date dateCreated, AuditEventValueObject troubleFlag, java.lang.Long coexpressionLinkCount,
            java.lang.String processedDataVectorComputationEventType, java.lang.String missingValueAnalysisEventType,
            java.util.Date dateLinkAnalysis, java.lang.Long rawCoexpressionLinkCount,
            java.util.Date dateProcessedDataVectorComputation, java.util.Date dateMissingValueAnalysis,
            java.lang.Long processedExpressionVectorCount, java.util.Date dateLastUpdated, java.util.Date dateCached,
            java.lang.Boolean hasProbeSpecificForQueryGene, java.lang.Double minPvalue ) {
        this.id = id;
        this.name = name;
        this.externalDatabase = externalDatabase;
        this.externalUri = externalUri;
        this.source = source;
        this.accession = accession;
        this.bioAssayCount = bioAssayCount;
        this.taxon = taxon;
        this.bioMaterialCount = bioMaterialCount;
        this.designElementDataVectorCount = designElementDataVectorCount;
        this.arrayDesignCount = arrayDesignCount;
        this.shortName = shortName;
        this.linkAnalysisEventType = linkAnalysisEventType;
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
        this.validatedFlag = validatedFlag;
        this.technologyType = technologyType;
        this.hasBothIntensities = hasBothIntensities;
        this.numAnnotations = numAnnotations;
        this.numPopulatedFactors = numPopulatedFactors;
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
        this.differentialAnalysisEventType = differentialAnalysisEventType;
        this.sampleRemovedFlags = sampleRemovedFlags;
        this.isPublic = isPublic;
        this.clazz = clazz;
        this.sourceExperiment = sourceExperiment;
        this.differentialExpressionAnalysisId = differentialExpressionAnalysisId;
        this.pubmedId = pubmedId;
        this.investigators = investigators;
        this.owner = owner;
        this.dateCreated = dateCreated;
        this.troubleFlag = troubleFlag;
        this.coexpressionLinkCount = coexpressionLinkCount;
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
        this.dateLinkAnalysis = dateLinkAnalysis;
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
        this.processedExpressionVectorCount = processedExpressionVectorCount;
        this.dateLastUpdated = dateLastUpdated;
        this.dateCached = dateCached;
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
        this.minPvalue = minPvalue;
    }

    /**
     * Copies constructor from other ExpressionExperimentValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws java.lang.NullPointerException if the argument is <code>null</code>
     */
    public ExpressionExperimentValueObject( ExpressionExperimentValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), otherBean.getExternalDatabase(), otherBean.getExternalUri(),
                otherBean.getSource(), otherBean.getAccession(), otherBean.getBioAssayCount(), otherBean.getTaxon(),
                otherBean.getBioMaterialCount(), otherBean.getDesignElementDataVectorCount(), otherBean
                        .getArrayDesignCount(), otherBean.getShortName(), otherBean.getLinkAnalysisEventType(),
                otherBean.getDateArrayDesignLastUpdated(), otherBean.getValidatedFlag(), otherBean.getTechnologyType(),
                otherBean.isHasBothIntensities(), otherBean.getNumAnnotations(), otherBean.getNumPopulatedFactors(),
                otherBean.getDateDifferentialAnalysis(), otherBean.getDifferentialAnalysisEventType(), otherBean
                        .getSampleRemovedFlags(), otherBean.isIsPublic(), otherBean.getClazz(), otherBean
                        .getSourceExperiment(), otherBean.getDifferentialExpressionAnalysisId(), otherBean
                        .getPubmedId(), otherBean.getInvestigators(), otherBean.getOwner(), otherBean.getDateCreated(),
                otherBean.getTroubleFlag(), otherBean.getCoexpressionLinkCount(), otherBean
                        .getProcessedDataVectorComputationEventType(), otherBean.getMissingValueAnalysisEventType(),
                otherBean.getDateLinkAnalysis(), otherBean.getRawCoexpressionLinkCount(), otherBean
                        .getDateProcessedDataVectorComputation(), otherBean.getDateMissingValueAnalysis(), otherBean
                        .getProcessedExpressionVectorCount(), otherBean.getDateLastUpdated(),
                otherBean.getDateCached(), otherBean.getHasProbeSpecificForQueryGene(), otherBean.getMinPvalue() );
    }

    /**
     * Copies all properties from the argument value object into this value object.
     */
    public void copy( ExpressionExperimentValueObject otherBean ) {
        if ( otherBean != null ) {
            this.setId( otherBean.getId() );
            this.setName( otherBean.getName() );
            this.setExternalDatabase( otherBean.getExternalDatabase() );
            this.setExternalUri( otherBean.getExternalUri() );
            this.setSource( otherBean.getSource() );
            this.setAccession( otherBean.getAccession() );
            this.setBioAssayCount( otherBean.getBioAssayCount() );
            this.setTaxon( otherBean.getTaxon() );
            this.setBioMaterialCount( otherBean.getBioMaterialCount() );
            this.setDesignElementDataVectorCount( otherBean.getDesignElementDataVectorCount() );
            this.setArrayDesignCount( otherBean.getArrayDesignCount() );
            this.setShortName( otherBean.getShortName() );
            this.setLinkAnalysisEventType( otherBean.getLinkAnalysisEventType() );
            this.setDateArrayDesignLastUpdated( otherBean.getDateArrayDesignLastUpdated() );
            this.setValidatedFlag( otherBean.getValidatedFlag() );
            this.setTechnologyType( otherBean.getTechnologyType() );
            this.setHasBothIntensities( otherBean.isHasBothIntensities() );
            this.setNumAnnotations( otherBean.getNumAnnotations() );
            this.setNumPopulatedFactors( otherBean.getNumPopulatedFactors() );
            this.setDateDifferentialAnalysis( otherBean.getDateDifferentialAnalysis() );
            this.setDifferentialAnalysisEventType( otherBean.getDifferentialAnalysisEventType() );
            this.setSampleRemovedFlags( otherBean.getSampleRemovedFlags() );
            this.setIsPublic( otherBean.isIsPublic() );
            this.setClazz( otherBean.getClazz() );
            this.setSourceExperiment( otherBean.getSourceExperiment() );
            this.setDifferentialExpressionAnalysisId( otherBean.getDifferentialExpressionAnalysisId() );
            this.setPubmedId( otherBean.getPubmedId() );
            this.setInvestigators( otherBean.getInvestigators() );
            this.setOwner( otherBean.getOwner() );
            this.setDateCreated( otherBean.getDateCreated() );
            this.setTroubleFlag( otherBean.getTroubleFlag() );
            this.setCoexpressionLinkCount( otherBean.getCoexpressionLinkCount() );
            this.setProcessedDataVectorComputationEventType( otherBean.getProcessedDataVectorComputationEventType() );
            this.setMissingValueAnalysisEventType( otherBean.getMissingValueAnalysisEventType() );
            this.setDateLinkAnalysis( otherBean.getDateLinkAnalysis() );
            this.setRawCoexpressionLinkCount( otherBean.getRawCoexpressionLinkCount() );
            this.setDateProcessedDataVectorComputation( otherBean.getDateProcessedDataVectorComputation() );
            this.setDateMissingValueAnalysis( otherBean.getDateMissingValueAnalysis() );
            this.setProcessedExpressionVectorCount( otherBean.getProcessedExpressionVectorCount() );
            this.setDateLastUpdated( otherBean.getDateLastUpdated() );
            this.setDateCached( otherBean.getDateCached() );
            this.setHasProbeSpecificForQueryGene( otherBean.getHasProbeSpecificForQueryGene() );
            this.setMinPvalue( otherBean.getMinPvalue() );
        }
    }

    private java.lang.Long id;

    /**
     * 
     */
    public java.lang.Long getId() {
        return this.id;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    private java.lang.String name;

    /**
     * 
     */
    public java.lang.String getName() {
        return this.name;
    }

    public void setName( java.lang.String name ) {
        this.name = name;
    }

    private java.lang.String externalDatabase;

    /**
     * 
     */
    public java.lang.String getExternalDatabase() {
        return this.externalDatabase;
    }

    public void setExternalDatabase( java.lang.String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    private java.lang.String externalUri;

    /**
     * 
     */
    public java.lang.String getExternalUri() {
        return this.externalUri;
    }

    public void setExternalUri( java.lang.String externalUri ) {
        this.externalUri = externalUri;
    }

    private java.lang.String source;

    /**
     * 
     */
    public java.lang.String getSource() {
        return this.source;
    }

    public void setSource( java.lang.String source ) {
        this.source = source;
    }

    private java.lang.String accession;

    /**
     * 
     */
    public java.lang.String getAccession() {
        return this.accession;
    }

    public void setAccession( java.lang.String accession ) {
        this.accession = accession;
    }

    private long bioAssayCount;

    /**
     * 
     */
    public long getBioAssayCount() {
        return this.bioAssayCount;
    }

    public void setBioAssayCount( long bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    private java.lang.String taxon;

    /**
     * 
     */
    public java.lang.String getTaxon() {
        return this.taxon;
    }

    public void setTaxon( java.lang.String taxon ) {
        this.taxon = taxon;
    }

    private java.lang.Long bioMaterialCount = null;

    /**
     * 
     */
    public java.lang.Long getBioMaterialCount() {
        return this.bioMaterialCount;
    }

    public void setBioMaterialCount( java.lang.Long bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    private long designElementDataVectorCount;

    /**
     * 
     */
    public long getDesignElementDataVectorCount() {
        return this.designElementDataVectorCount;
    }

    public void setDesignElementDataVectorCount( long designElementDataVectorCount ) {
        this.designElementDataVectorCount = designElementDataVectorCount;
    }

    private long arrayDesignCount;

    /**
     * 
     */
    public long getArrayDesignCount() {
        return this.arrayDesignCount;
    }

    public void setArrayDesignCount( long arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    private java.lang.String shortName;

    /**
     * 
     */
    public java.lang.String getShortName() {
        return this.shortName;
    }

    public void setShortName( java.lang.String shortName ) {
        this.shortName = shortName;
    }

    private java.lang.String linkAnalysisEventType;

    /**
     * 
     */
    public java.lang.String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    public void setLinkAnalysisEventType( java.lang.String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    private java.util.Date dateArrayDesignLastUpdated;

    /**
     * <p>
     * The date the array design associated with the experiment was last updated. If there are multiple array designs
     * this should be the date of the most recent modification of any of them. This is used to help flag experiments
     * that need re-analysis due to changes in the underlying array design(s)
     * </p>
     */
    public java.util.Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    public void setDateArrayDesignLastUpdated( java.util.Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    private AuditEventValueObject validatedFlag;

    /**
     * 
     */
    public AuditEventValueObject getValidatedFlag() {
        return this.validatedFlag;
    }

    public void setValidatedFlag( AuditEventValueObject validatedFlag ) {
        this.validatedFlag = validatedFlag;
    }

    private java.lang.String technologyType;

    /**
     * 
     */
    public java.lang.String getTechnologyType() {
        return this.technologyType;
    }

    public void setTechnologyType( java.lang.String technologyType ) {
        this.technologyType = technologyType;
    }

    private boolean hasBothIntensities;

    /**
     * 
     */
    public boolean isHasBothIntensities() {
        return this.hasBothIntensities;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    private java.lang.Integer numAnnotations;

    /**
     * <p>
     * The number of terms (Characteristics) the experiment has to describe it.
     * </p>
     */
    public java.lang.Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    public void setNumAnnotations( java.lang.Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    private java.lang.Integer numPopulatedFactors;

    /**
     * <p>
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials)
     * </p>
     */
    public java.lang.Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    public void setNumPopulatedFactors( java.lang.Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    private java.util.Date dateDifferentialAnalysis;

    /**
     * 
     */
    public java.util.Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    public void setDateDifferentialAnalysis( java.util.Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    private java.lang.String differentialAnalysisEventType;

    /**
     * 
     */
    public java.lang.String getDifferentialAnalysisEventType() {
        return this.differentialAnalysisEventType;
    }

    public void setDifferentialAnalysisEventType( java.lang.String differentialAnalysisEventType ) {
        this.differentialAnalysisEventType = differentialAnalysisEventType;
    }

    private java.util.Collection<AuditEventValueObject> sampleRemovedFlags;

    /**
     * <p>
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life of a
     * data set, so this is a collection of AuditEvents.
     * </p>
     */
    public java.util.Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    public void setSampleRemovedFlags( java.util.Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    /**
     * @param sampleRemovedFlags
     */
    public void setSampleRemovedFlagsFromAuditEvent( java.util.Collection<AuditEvent> sampleRemovedFlags ) {
        Collection<AuditEventValueObject> converted = new HashSet<AuditEventValueObject>();

        for ( AuditEvent ae : sampleRemovedFlags ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    private boolean isPublic = true;

    /**
     * <p>
     * If true, this data set has been made public. If false, it is private and is only viewable by some users.
     * </p>
     */
    public boolean isIsPublic() {
        return this.isPublic;
    }

    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    private java.lang.String clazz;

    /**
     * <p>
     * The type of BioAssaySet this represents.
     * </p>
     */
    public java.lang.String getClazz() {
        return this.clazz;
    }

    public void setClazz( java.lang.String clazz ) {
        this.clazz = clazz;
    }

    private java.lang.Long sourceExperiment;

    /**
     * <p>
     * The ID of the source experiment, if this is an ExpressionExperimentSubSet
     * </p>
     */
    public java.lang.Long getSourceExperiment() {
        return this.sourceExperiment;
    }

    public void setSourceExperiment( java.lang.Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    private java.lang.Long differentialExpressionAnalysisId;

    /**
     * 
     */
    public java.lang.Long getDifferentialExpressionAnalysisId() {
        return this.differentialExpressionAnalysisId;
    }

    public void setDifferentialExpressionAnalysisId( java.lang.Long differentialExpressionAnalysisId ) {
        this.differentialExpressionAnalysisId = differentialExpressionAnalysisId;
    }

    private java.lang.Integer pubmedId;

    /**
     * 
     */
    public java.lang.Integer getPubmedId() {
        return this.pubmedId;
    }

    public void setPubmedId( java.lang.Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    private java.lang.String investigators;

    /**
     * 
     */
    public java.lang.String getInvestigators() {
        return this.investigators;
    }

    public void setInvestigators( java.lang.String investigators ) {
        this.investigators = investigators;
    }

    private java.lang.String owner;

    /**
     * <p>
     * The user name of the experiment's owner, if any.
     * </p>
     */
    public java.lang.String getOwner() {
        return this.owner;
    }

    public void setOwner( java.lang.String owner ) {
        this.owner = owner;
    }

    private java.util.Date dateCreated;

    /**
     * 
     */
    public java.util.Date getDateCreated() {
        return this.dateCreated;
    }

    public void setDateCreated( java.util.Date dateCreated ) {
        this.dateCreated = dateCreated;
    }

    private AuditEventValueObject troubleFlag;

    /**
     * 
     */
    public AuditEventValueObject getTroubleFlag() {
        return this.troubleFlag;
    }

    public void setTroubleFlag( AuditEventValueObject troubleFlag ) {
        this.troubleFlag = troubleFlag;
    }

    private java.lang.Long coexpressionLinkCount = null;

    /**
     * 
     */
    public java.lang.Long getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

    public void setCoexpressionLinkCount( java.lang.Long coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    private java.lang.String processedDataVectorComputationEventType;

    /**
     * 
     */
    public java.lang.String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    public void setProcessedDataVectorComputationEventType( java.lang.String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    private java.lang.String missingValueAnalysisEventType;

    /**
     * 
     */
    public java.lang.String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    public void setMissingValueAnalysisEventType( java.lang.String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    private java.util.Date dateLinkAnalysis;

    /**
     * 
     */
    public java.util.Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    public void setDateLinkAnalysis( java.util.Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    private java.lang.Long rawCoexpressionLinkCount = null;

    /**
     * <p>
     * The amount of raw links that the EE contributed to any of the coexpressed genes. by raw we mean before
     * filtering/stringency was applied.
     * </p>
     */
    public java.lang.Long getRawCoexpressionLinkCount() {
        return this.rawCoexpressionLinkCount;
    }

    public void setRawCoexpressionLinkCount( java.lang.Long rawCoexpressionLinkCount ) {
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
    }

    private java.util.Date dateProcessedDataVectorComputation;

    /**
     * 
     */
    public java.util.Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    public void setDateProcessedDataVectorComputation( java.util.Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    private java.util.Date dateMissingValueAnalysis;

    /**
     * 
     */
    public java.util.Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    public void setDateMissingValueAnalysis( java.util.Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    private java.lang.Long processedExpressionVectorCount = null;

    /**
     * 
     */
    public java.lang.Long getProcessedExpressionVectorCount() {
        return this.processedExpressionVectorCount;
    }

    public void setProcessedExpressionVectorCount( java.lang.Long processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    private java.util.Date dateLastUpdated;

    /**
     * 
     */
    public java.util.Date getDateLastUpdated() {
        return this.dateLastUpdated;
    }

    public void setDateLastUpdated( java.util.Date dateLastUpdated ) {
        this.dateLastUpdated = dateLastUpdated;
    }

    private java.util.Date dateCached;

    /**
     * <p>
     * The date this object was generated.
     * </p>
     */
    public java.util.Date getDateCached() {
        return this.dateCached;
    }

    public void setDateCached( java.util.Date dateCached ) {
        this.dateCached = dateCached;
    }

    private java.lang.Boolean hasProbeSpecificForQueryGene;

    /**
     * <p>
     * Used in display of gene-wise analysis results.
     * </p>
     */
    public java.lang.Boolean getHasProbeSpecificForQueryGene() {
        return this.hasProbeSpecificForQueryGene;
    }

    public void setHasProbeSpecificForQueryGene( java.lang.Boolean hasProbeSpecificForQueryGene ) {
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
    }

    private java.lang.Double minPvalue;

    /**
     * 
     */
    public java.lang.Double getMinPvalue() {
        return this.minPvalue;
    }

    public void setMinPvalue( java.lang.Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

}