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

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionSummaryValueObject;
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

    private java.lang.String accession;

    private long arrayDesignCount;

    private long bioAssayCount;

    private java.lang.Long bioMaterialCount = null;

    private java.lang.String clazz;

    private java.lang.Long coexpressionLinkCount = null;

    private java.util.Date dateArrayDesignLastUpdated;

    private java.util.Date dateCached;

    private java.util.Date dateCreated;

    private java.util.Date dateDifferentialAnalysis;

    private java.util.Date dateLastUpdated;

    private java.util.Date dateLinkAnalysis;

    private java.util.Date dateMissingValueAnalysis;

    private java.util.Date dateProcessedDataVectorComputation;

    private long designElementDataVectorCount;

    private java.lang.String differentialAnalysisEventType;

    private java.lang.Long differentialExpressionAnalysisId;

    private java.lang.String externalDatabase;

    private java.lang.String externalUri;

    private boolean hasBothIntensities = false;

    private Boolean hasEitherIntensity = false;

    private java.lang.Boolean hasProbeSpecificForQueryGene;

    private java.lang.Long id;

    private java.lang.String investigators;

    private boolean isPublic = true;

    private java.lang.String linkAnalysisEventType;

    private java.lang.Double minPvalue;

    private java.lang.String missingValueAnalysisEventType;

    private java.lang.String name;

    private java.lang.Integer numAnnotations;

    private java.lang.Integer numPopulatedFactors;

    private java.lang.String owner;

    private java.lang.String processedDataVectorComputationEventType;

    private java.lang.Long processedExpressionVectorCount = null;

    private java.lang.Integer pubmedId;

    private java.lang.Long rawCoexpressionLinkCount = null;

    private java.util.Collection<AuditEventValueObject> sampleRemovedFlags;

    private java.lang.String shortName;

    private java.lang.String source;

    private java.lang.Long sourceExperiment;

    private java.lang.String taxon;

    private java.lang.String technologyType;

    private AuditEventValueObject troubleFlag;

    private AuditEventValueObject validatedFlag;
    
    private Collection<DifferentialExpressionSummaryValueObject> diffExpressedProbes;

    public ExpressionExperimentValueObject() {
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
                otherBean.getDateCached(), otherBean.getHasProbeSpecificForQueryGene(), otherBean.getMinPvalue(),
                otherBean.getHasEitherIntensity(), otherBean.getDiffExpressedProbes() );
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
            java.lang.Boolean hasProbeSpecificForQueryGene, java.lang.Double minPvalue, Boolean hasEitherIntensity, Collection<DifferentialExpressionSummaryValueObject> probeIds ) {
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
        this.hasEitherIntensity = hasEitherIntensity;
        this.diffExpressedProbes = probeIds;
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
            this.setDiffExpressedProbes( otherBean.getDiffExpressedProbes() );
        }
    }

    /**
     * 
     */
    public java.lang.String getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    public long getArrayDesignCount() {
        return this.arrayDesignCount;
    }

    /**
     * 
     */
    public long getBioAssayCount() {
        return this.bioAssayCount;
    }

    /**
     * 
     */
    public java.lang.Long getBioMaterialCount() {
        return this.bioMaterialCount;
    }

    /**
     * <p>
     * The type of BioAssaySet this represents.
     * </p>
     */
    public java.lang.String getClazz() {
        return this.clazz;
    }

    /**
     * 
     */
    public java.lang.Long getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

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

    /**
     * <p>
     * The date this object was generated.
     * </p>
     */
    public java.util.Date getDateCached() {
        return this.dateCached;
    }

    /**
     * 
     */
    public java.util.Date getDateCreated() {
        return this.dateCreated;
    }

    /**
     * 
     */
    public java.util.Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    /**
     * 
     */
    public java.util.Date getDateLastUpdated() {
        return this.dateLastUpdated;
    }

    /**
     * 
     */
    public java.util.Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    /**
     * 
     */
    public java.util.Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    /**
     * 
     */
    public java.util.Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    /**
     * 
     */
    public long getDesignElementDataVectorCount() {
        return this.designElementDataVectorCount;
    }

    /**
     * 
     */
    public java.lang.String getDifferentialAnalysisEventType() {
        return this.differentialAnalysisEventType;
    }

    /**
     * 
     */
    public java.lang.Long getDifferentialExpressionAnalysisId() {
        return this.differentialExpressionAnalysisId;
    }

    /**
     * 
     */
    public java.lang.String getExternalDatabase() {
        return this.externalDatabase;
    }

    /**
     * 
     */
    public java.lang.String getExternalUri() {
        return this.externalUri;
    }

    /**
     * @return true if the experiment has any intensity information available. Relevant for two-channel studies.
     */
    public Boolean getHasEitherIntensity() {
        return hasEitherIntensity;
    }

    /**
     * <p>
     * Used in display of gene-wise analysis results.
     * </p>
     */
    public java.lang.Boolean getHasProbeSpecificForQueryGene() {
        return this.hasProbeSpecificForQueryGene;
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
    public java.lang.String getInvestigators() {
        return this.investigators;
    }

    /**
     * 
     */
    public java.lang.String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    /**
     * 
     */
    public java.lang.Double getMinPvalue() {
        return this.minPvalue;
    }

    /**
     * 
     */
    public java.lang.String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    /**
     * 
     */
    public java.lang.String getName() {
        return this.name;
    }

    /**
     * <p>
     * The number of terms (Characteristics) the experiment has to describe it.
     * </p>
     */
    public java.lang.Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    /**
     * <p>
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials)
     * </p>
     */
    public java.lang.Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    /**
     * <p>
     * The user name of the experiment's owner, if any.
     * </p>
     */
    public java.lang.String getOwner() {
        return this.owner;
    }

    /**
     * 
     */
    public java.lang.String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    /**
     * 
     */
    public java.lang.Long getProcessedExpressionVectorCount() {
        return this.processedExpressionVectorCount;
    }

    /**
     * 
     */
    public java.lang.Integer getPubmedId() {
        return this.pubmedId;
    }

    /**
     * <p>
     * The amount of raw links that the EE contributed to any of the coexpressed genes. by raw we mean before
     * filtering/stringency was applied.
     * </p>
     */
    public java.lang.Long getRawCoexpressionLinkCount() {
        return this.rawCoexpressionLinkCount;
    }

    /**
     * <p>
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life of a
     * data set, so this is a collection of AuditEvents.
     * </p>
     */
    public java.util.Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
    }

    /**
     * 
     */
    public java.lang.String getShortName() {
        return this.shortName;
    }

    /**
     * 
     */
    public java.lang.String getSource() {
        return this.source;
    }

    /**
     * <p>
     * The ID of the source experiment, if this is an ExpressionExperimentSubSet
     * </p>
     */
    public java.lang.Long getSourceExperiment() {
        return this.sourceExperiment;
    }

    /**
     * 
     */
    public java.lang.String getTaxon() {
        return this.taxon;
    }

    /**
     * 
     */
    public java.lang.String getTechnologyType() {
        return this.technologyType;
    }

    /**
     * 
     */
    public AuditEventValueObject getTroubleFlag() {
        return this.troubleFlag;
    }

    /**
     * 
     */
    public AuditEventValueObject getValidatedFlag() {
        return this.validatedFlag;
    }

    /**
     * 
     */
    public boolean isHasBothIntensities() {
        return this.hasBothIntensities;
    }

    /**
     * <p>
     * If true, this data set has been made public. If false, it is private and is only viewable by some users.
     * </p>
     */
    public boolean isIsPublic() {
        return this.isPublic;
    }

    public void setAccession( java.lang.String accession ) {
        this.accession = accession;
    }

    public void setArrayDesignCount( long arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    public void setBioAssayCount( long bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    public void setBioMaterialCount( java.lang.Long bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    public void setClazz( java.lang.String clazz ) {
        this.clazz = clazz;
    }

    public void setCoexpressionLinkCount( java.lang.Long coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    public void setDateArrayDesignLastUpdated( java.util.Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public void setDateCached( java.util.Date dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDateCreated( java.util.Date dateCreated ) {
        this.dateCreated = dateCreated;
    }

    public void setDateDifferentialAnalysis( java.util.Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public void setDateLastUpdated( java.util.Date dateLastUpdated ) {
        this.dateLastUpdated = dateLastUpdated;
    }

    public void setDateLinkAnalysis( java.util.Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public void setDateMissingValueAnalysis( java.util.Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public void setDateProcessedDataVectorComputation( java.util.Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    public void setDesignElementDataVectorCount( long designElementDataVectorCount ) {
        this.designElementDataVectorCount = designElementDataVectorCount;
    }

    public void setDifferentialAnalysisEventType( java.lang.String differentialAnalysisEventType ) {
        this.differentialAnalysisEventType = differentialAnalysisEventType;
    }

    public void setDifferentialExpressionAnalysisId( java.lang.Long differentialExpressionAnalysisId ) {
        this.differentialExpressionAnalysisId = differentialExpressionAnalysisId;
    }

    public void setExternalDatabase( java.lang.String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setExternalUri( java.lang.String externalUri ) {
        this.externalUri = externalUri;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    public void setHasProbeSpecificForQueryGene( java.lang.Boolean hasProbeSpecificForQueryGene ) {
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
    }

    public void setId( java.lang.Long id ) {
        this.id = id;
    }

    public void setInvestigators( java.lang.String investigators ) {
        this.investigators = investigators;
    }

    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public void setLinkAnalysisEventType( java.lang.String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public void setMinPvalue( java.lang.Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public void setMissingValueAnalysisEventType( java.lang.String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    public void setName( java.lang.String name ) {
        this.name = name;
    }

    public void setNumAnnotations( java.lang.Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    public void setNumPopulatedFactors( java.lang.Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public void setOwner( java.lang.String owner ) {
        this.owner = owner;
    }

    public void setProcessedDataVectorComputationEventType( java.lang.String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public void setProcessedExpressionVectorCount( java.lang.Long processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    public void setPubmedId( java.lang.Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    public void setRawCoexpressionLinkCount( java.lang.Long rawCoexpressionLinkCount ) {
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
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

    public void setShortName( java.lang.String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( java.lang.String source ) {
        this.source = source;
    }

    public void setSourceExperiment( java.lang.Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    public void setTaxon( java.lang.String taxon ) {
        this.taxon = taxon;
    }

    public void setTechnologyType( java.lang.String technologyType ) {
        this.technologyType = technologyType;
    }

    public void setTroubleFlag( AuditEventValueObject troubleFlag ) {
        this.troubleFlag = troubleFlag;
    }

    public void setValidatedFlag( AuditEventValueObject validatedFlag ) {
        this.validatedFlag = validatedFlag;
    }

    public Collection<DifferentialExpressionSummaryValueObject> getDiffExpressedProbes() {
        return diffExpressedProbes;
    }

    public void setDiffExpressedProbes( Collection<DifferentialExpressionSummaryValueObject> diffExpressedProbes ) {
        this.diffExpressedProbes = diffExpressedProbes;
    }

}