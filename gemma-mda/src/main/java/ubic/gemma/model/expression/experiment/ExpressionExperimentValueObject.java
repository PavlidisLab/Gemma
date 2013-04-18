/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecureValueObject;

/**
 * @author kelsey
 * @version $Id$
 */
public class ExpressionExperimentValueObject implements Comparable<ExpressionExperimentValueObject>, SecureValueObject {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5678747537830051610L;

    @Override
    public Class<? extends Securable> getSecurableClass() {
        if ( this.isSubset ) {
            return ExpressionExperimentSubSetImpl.class;
        }
        return ExpressionExperimentImpl.class;
    }

    /**
     * @param collection
     * @return
     */
    public static Collection<ExpressionExperimentValueObject> convert2ValueObjects(
            Collection<? extends BioAssaySet> collection ) {
        Collection<ExpressionExperimentValueObject> result = new ArrayList<ExpressionExperimentValueObject>();
        for ( BioAssaySet ee : collection ) {
            result.add( new ExpressionExperimentValueObject( ee ) );
        }
        return result;
    }

    /**
     * @param collection
     * @return
     */
    public static List<ExpressionExperimentValueObject> convert2ValueObjectsOrdered(
            List<ExpressionExperiment> collection ) {
        List<ExpressionExperimentValueObject> result = new ArrayList<ExpressionExperimentValueObject>();
        for ( ExpressionExperiment ee : collection ) {
            result.add( new ExpressionExperimentValueObject( ee ) );
        }
        return result;
    }

    private String accession;

    private Integer arrayDesignCount;

    private Date autoTagDate;

    private String batchFetchEventType;

    private Integer bioAssayCount;

    private Integer bioMaterialCount = null;

    private String clazz;

    private Integer coexpressionLinkCount = null;

    private Boolean currentUserHasWritePermission = false;

    private Boolean currentUserIsOwner = false;

    private Date dateArrayDesignLastUpdated;

    private Date dateBatchFetch;

    private Date dateCached;

    private Date dateCreated;

    private Date dateDifferentialAnalysis;

    private Date dateLastUpdated;

    private Date dateLinkAnalysis;

    private Date dateMissingValueAnalysis;

    private Date datePcaAnalysis;

    private Date dateProcessedDataVectorComputation;

    private Integer designElementDataVectorCount;

    private Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses = new HashSet<DifferentialExpressionAnalysisValueObject>();

    private Long experimentalDesign;

    private String externalDatabase;

    private String externalUri;

    private Boolean hasBothIntensities = false;

    private Boolean hasEitherIntensity = false;

    private Boolean hasProbeSpecificForQueryGene;

    private Long id;

    private String investigators;

    private Boolean isPublic = true;

    private boolean isShared = false;

    private String linkAnalysisEventType;

    private Double minPvalue;

    private String missingValueAnalysisEventType;

    private String name;

    private Integer numAnnotations;

    private Integer numPopulatedFactors;

    private String owner;

    private String pcaAnalysisEventType;

    private String processedDataVectorComputationEventType;

    private Integer processedExpressionVectorCount = null;

    private Integer pubmedId;

    private Collection<AuditEventValueObject> sampleRemovedFlags;

    private String shortName;

    private String source;

    private Long sourceExperiment;

    private String taxon;

    private Long taxonId;

    private Long parentTaxonId;

    public Long getParentTaxonId() {
        return parentTaxonId;
    }

    public void setParentTaxonId( Long parentTaxonId ) {
        this.parentTaxonId = parentTaxonId;
    }

    private String technologyType;

    private boolean troubled = false;

    private String troubleDetails = "(Reason for trouble not populated)";

    private boolean validated = false;

    private boolean isSubset = false;

    public boolean isSubset() {
        return isSubset;
    }

    public void setSubset( boolean isSubset ) {
        this.isSubset = isSubset;
    }

    public ExpressionExperimentValueObject() {
    }

    public ExpressionExperimentValueObject( BioAssaySet ee ) {
        this.id = ee.getId();
        if ( ee instanceof ExpressionExperiment ) {
            this.shortName = ( ( ExpressionExperiment ) ee ).getShortName();
        } else {
            assert ee instanceof ExpressionExperimentSubSet;
            this.isSubset = true;
        }

        this.name = ee.getName();
        /*
         * FIXME this doesn't populate enough stuff.
         */
    }

    /**
     * Copies constructor from other ExpressionExperimentValueObject
     * 
     * @param otherBean, cannot be <code>null</code>
     * @throws NullPointerException if the argument is <code>null</code>
     */
    public ExpressionExperimentValueObject( ExpressionExperimentValueObject otherBean ) {
        this( otherBean.getId(), otherBean.getName(), otherBean.getExternalDatabase(), otherBean.getExternalUri(),
                otherBean.getSource(), otherBean.getAccession(), otherBean.getBioAssayCount(), otherBean.getTaxon(),
                otherBean.getTaxonId(), otherBean.getBioMaterialCount(), otherBean.getDesignElementDataVectorCount(),
                otherBean.getArrayDesignCount(), otherBean.getShortName(), otherBean.getLinkAnalysisEventType(),
                otherBean.getDateArrayDesignLastUpdated(), otherBean.getValidated(), otherBean.getTechnologyType(),
                otherBean.isHasBothIntensities(), otherBean.getNumAnnotations(), otherBean.getNumPopulatedFactors(),
                otherBean.getDateDifferentialAnalysis(), otherBean.getSampleRemovedFlags(), otherBean.getIsPublic(),
                otherBean.getUserCanWrite(), otherBean.getClazz(), otherBean.getSourceExperiment(), otherBean
                        .getPubmedId(), otherBean.getInvestigators(), otherBean.getOwner(), otherBean.getDateCreated(),
                otherBean.getTroubled(), otherBean.getCoexpressionLinkCount(), otherBean
                        .getProcessedDataVectorComputationEventType(), otherBean.getMissingValueAnalysisEventType(),
                otherBean.getDateLinkAnalysis(), otherBean.getDateProcessedDataVectorComputation(), otherBean
                        .getDateMissingValueAnalysis(), otherBean.getProcessedExpressionVectorCount(), otherBean
                        .getDateLastUpdated(), otherBean.getDateCached(), otherBean.getHasProbeSpecificForQueryGene(),
                otherBean.getMinPvalue(), otherBean.getHasEitherIntensity(), otherBean.getExperimentalDesign(),
                otherBean.getAutoTagDate(), otherBean.getDifferentialExpressionAnalyses(), otherBean
                        .getDateBatchFetch(), otherBean.getDatePcaAnalysis(), otherBean.getPcaAnalysisEventType(),
                otherBean.getBatchFetchEventType(), otherBean.getTroubleDetails(), otherBean.isSubset(), otherBean
                        .getParentTaxonId() );
    }

    public ExpressionExperimentValueObject( Long id, String name, String externalDatabase, String externalUri,
            String source, String accession, Integer bioAssayCount, String taxon, Long taxonId,
            Integer bioMaterialCount, Integer designElementDataVectorCount, Integer arrayDesignCount, String shortName,
            String linkAnalysisEventType, Date dateArrayDesignLastUpdated, boolean validatedFlag,
            String technologyType, boolean hasBothIntensities, Integer numAnnotations, Integer numPopulatedFactors,
            Date dateDifferentialAnalysis, Collection<AuditEventValueObject> sampleRemovedFlags, boolean isPublic,
            boolean currentUserHasWritePermission, String clazz, Long sourceExperiment, Integer pubmedId,
            String investigators, String owner, Date dateCreated, boolean troubleFlag, Integer coexpressionLinkCount,
            String processedDataVectorComputationEventType, String missingValueAnalysisEventType,
            Date dateLinkAnalysis, Date dateProcessedDataVectorComputation, Date dateMissingValueAnalysis,
            Integer processedExpressionVectorCount, Date dateLastUpdated, Date dateCached,
            Boolean hasProbeSpecificForQueryGene, Double minPvalue, Boolean hasEitherIntensity,
            Long experimentalDesign, Date autoTagDate,
            Collection<DifferentialExpressionAnalysisValueObject> diffAnalyses, Date batchAnalysisDate,
            Date pcaAnalysisDate, String pcaAnalysisEventType, String batchFetchEventType, String troubleDetails,
            Boolean isSubset, Long parentTaxonId ) {
        this.id = id;
        this.name = name;
        this.externalDatabase = externalDatabase;
        this.externalUri = externalUri;
        this.source = source;
        this.accession = accession;
        this.bioAssayCount = bioAssayCount;
        this.taxon = taxon;
        this.taxonId = taxonId;
        this.bioMaterialCount = bioMaterialCount;
        this.designElementDataVectorCount = designElementDataVectorCount;
        this.arrayDesignCount = arrayDesignCount;
        this.shortName = shortName;
        this.linkAnalysisEventType = linkAnalysisEventType;
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
        this.validated = validatedFlag;
        this.technologyType = technologyType;
        this.hasBothIntensities = hasBothIntensities;
        this.numAnnotations = numAnnotations;
        this.numPopulatedFactors = numPopulatedFactors;
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
        this.sampleRemovedFlags = sampleRemovedFlags;
        this.isPublic = isPublic;
        this.currentUserHasWritePermission = currentUserHasWritePermission;
        this.clazz = clazz;
        this.sourceExperiment = sourceExperiment;
        this.pubmedId = pubmedId;
        this.investigators = investigators;
        this.owner = owner;
        this.dateCreated = dateCreated;
        this.troubled = troubleFlag;
        this.coexpressionLinkCount = coexpressionLinkCount;
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
        this.dateLinkAnalysis = dateLinkAnalysis;
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
        this.processedExpressionVectorCount = processedExpressionVectorCount;
        this.dateLastUpdated = dateLastUpdated;
        this.dateCached = dateCached;
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
        this.minPvalue = minPvalue;
        this.hasEitherIntensity = hasEitherIntensity;
        this.experimentalDesign = experimentalDesign;
        this.autoTagDate = autoTagDate;
        this.differentialExpressionAnalyses = diffAnalyses;
        this.dateBatchFetch = batchAnalysisDate;
        this.datePcaAnalysis = pcaAnalysisDate;
        this.pcaAnalysisEventType = pcaAnalysisEventType;
        this.batchFetchEventType = batchFetchEventType;
        this.troubleDetails = troubleDetails;
        this.isSubset = isSubset;
        this.parentTaxonId = parentTaxonId;
    }

    /**
     * @param sampleRemovedFlags
     */
    public void auditEvents2SampleRemovedFlags( Collection<AuditEvent> s ) {
        Collection<AuditEventValueObject> converted = new HashSet<AuditEventValueObject>();

        for ( AuditEvent ae : s ) {
            converted.add( new AuditEventValueObject( ae ) );
        }

        this.sampleRemovedFlags = converted;
    }

    @Override
    public int compareTo( ExpressionExperimentValueObject arg0 ) {
        return this.getId().compareTo( arg0.getId() );
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
            this.setValidated( otherBean.getValidated() );
            this.setTechnologyType( otherBean.getTechnologyType() );
            this.setHasBothIntensities( otherBean.isHasBothIntensities() );
            this.setNumAnnotations( otherBean.getNumAnnotations() );
            this.setNumPopulatedFactors( otherBean.getNumPopulatedFactors() );
            this.setDateDifferentialAnalysis( otherBean.getDateDifferentialAnalysis() );
            this.setSampleRemovedFlags( otherBean.getSampleRemovedFlags() );
            this.setIsPublic( otherBean.getIsPublic() );
            this.setClazz( otherBean.getClazz() );
            this.setSourceExperiment( otherBean.getSourceExperiment() );
            this.setPubmedId( otherBean.getPubmedId() );
            this.setInvestigators( otherBean.getInvestigators() );
            this.setOwner( otherBean.getOwner() );
            this.setDateCreated( otherBean.getDateCreated() );
            this.setTroubled( otherBean.getTroubled() );
            this.setCoexpressionLinkCount( otherBean.getCoexpressionLinkCount() );
            this.setProcessedDataVectorComputationEventType( otherBean.getProcessedDataVectorComputationEventType() );
            this.setMissingValueAnalysisEventType( otherBean.getMissingValueAnalysisEventType() );
            this.setDateLinkAnalysis( otherBean.getDateLinkAnalysis() );
            this.setDateProcessedDataVectorComputation( otherBean.getDateProcessedDataVectorComputation() );
            this.setDateMissingValueAnalysis( otherBean.getDateMissingValueAnalysis() );
            this.setProcessedExpressionVectorCount( otherBean.getProcessedExpressionVectorCount() );
            this.setDateLastUpdated( otherBean.getDateLastUpdated() );
            this.setDateCached( otherBean.getDateCached() );
            this.setHasProbeSpecificForQueryGene( otherBean.getHasProbeSpecificForQueryGene() );
            this.setMinPvalue( otherBean.getMinPvalue() );
            this.setDateBatchFetch( otherBean.getDateBatchFetch() );
            this.setDatePcaAnalysis( otherBean.getDatePcaAnalysis() );
            this.setPcaAnalysisEventType( otherBean.getPcaAnalysisEventType() );
            this.setBatchFetchEventType( otherBean.getBatchFetchEventType() );

            this.setTroubleDetails( otherBean.getTroubleDetails() );
        }
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExpressionExperimentValueObject other = ( ExpressionExperimentValueObject ) obj;
        if ( id == null ) {
            if ( other.id != null ) return false;
        } else if ( !id.equals( other.id ) ) return false;
        return true;
    }

    /**
     * 
     */
    public String getAccession() {
        return this.accession;
    }

    /**
     * 
     */
    public Integer getArrayDesignCount() {
        return this.arrayDesignCount;
    }

    /**
     * @return the autoTagDate
     */
    public Date getAutoTagDate() {
        return autoTagDate;
    }

    public String getBatchFetchEventType() {
        return batchFetchEventType;
    }

    /**
     * 
     */
    public Integer getBioAssayCount() {
        return this.bioAssayCount;
    }

    /**
     * 
     */
    public Integer getBioMaterialCount() {
        return this.bioMaterialCount;
    }

    /**
     * <p>
     * The type of BioAssaySet this represents.
     * </p>
     */
    public String getClazz() {
        return this.clazz;
    }

    /**
     * 
     */
    public Integer getCoexpressionLinkCount() {
        return this.coexpressionLinkCount;
    }

    public Boolean getCurrentUserHasWritePermission() {
        return currentUserHasWritePermission;
    }

    public Boolean getCurrentUserIsOwner() {
        return currentUserIsOwner;
    }

    /**
     * <p>
     * The date the array design associated with the experiment was last updated. If there are multiple array designs
     * this should be the date of the most recent modification of any of them. This is used to help flag experiments
     * that need re-analysis due to changes in the underlying array design(s)
     * </p>
     */
    public Date getDateArrayDesignLastUpdated() {
        return this.dateArrayDesignLastUpdated;
    }

    public Date getDateBatchFetch() {
        return dateBatchFetch;
    }

    /**
     * <p>
     * The date this object was generated.
     * </p>
     */
    public Date getDateCached() {
        return this.dateCached;
    }

    /**
     * 
     */
    public Date getDateCreated() {
        return this.dateCreated;
    }

    /**
     * 
     */
    public Date getDateDifferentialAnalysis() {
        return this.dateDifferentialAnalysis;
    }

    /**
     * 
     */
    public Date getDateLastUpdated() {
        return this.dateLastUpdated;
    }

    /**
     * 
     */
    public Date getDateLinkAnalysis() {
        return this.dateLinkAnalysis;
    }

    /**
     * 
     */
    public Date getDateMissingValueAnalysis() {
        return this.dateMissingValueAnalysis;
    }

    public Date getDatePcaAnalysis() {
        return datePcaAnalysis;
    }

    /**
     * 
     */
    public Date getDateProcessedDataVectorComputation() {
        return this.dateProcessedDataVectorComputation;
    }

    /**
     * 
     */
    public Integer getDesignElementDataVectorCount() {
        return this.designElementDataVectorCount;
    }

    public Collection<DifferentialExpressionAnalysisValueObject> getDifferentialExpressionAnalyses() {
        return differentialExpressionAnalyses;
    }

    public Long getExperimentalDesign() {
        return experimentalDesign;
    }

    /**
     * 
     */
    public String getExternalDatabase() {
        return this.externalDatabase;
    }

    /**
     * 
     */
    public String getExternalUri() {
        return this.externalUri;
    }

    public Boolean getHasBothIntensities() {
        return hasBothIntensities;
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
    public Boolean getHasProbeSpecificForQueryGene() {
        return this.hasProbeSpecificForQueryGene;
    }

    /**
     * 
     */
    @Override
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public String getInvestigators() {
        return this.investigators;
    }

    /**
     * 
     */
    public String getLinkAnalysisEventType() {
        return this.linkAnalysisEventType;
    }

    /**
     * 
     */
    public Double getMinPvalue() {
        return this.minPvalue;
    }

    /**
     * 
     */
    public String getMissingValueAnalysisEventType() {
        return this.missingValueAnalysisEventType;
    }

    /**
     * 
     */
    public String getName() {
        return this.name;
    }

    /**
     * <p>
     * The number of terms (Characteristics) the experiment has to describe it.
     * </p>
     */
    public Integer getNumAnnotations() {
        return this.numAnnotations;
    }

    /**
     * <p>
     * The number of experimental factors the experiment has (counting those that are populated with biomaterials)
     * </p>
     */
    public Integer getNumPopulatedFactors() {
        return this.numPopulatedFactors;
    }

    /**
     * <p>
     * The user name of the experiment's owner, if any.
     * </p>
     */
    public String getOwner() {
        return this.owner;
    }

    public String getPcaAnalysisEventType() {
        return pcaAnalysisEventType;
    }

    /**
     * 
     */
    public String getProcessedDataVectorComputationEventType() {
        return this.processedDataVectorComputationEventType;
    }

    /**
     * 
     */
    public Integer getProcessedExpressionVectorCount() {
        return this.processedExpressionVectorCount;
    }

    /**
     * 
     */
    public Integer getPubmedId() {
        return this.pubmedId;
    }

    /**
     * <p>
     * Details of samples that were removed (or marked as outliers). This can happen multiple times in the life of a
     * data set, so this is a collection of AuditEvents.
     * </p>
     */
    public Collection<AuditEventValueObject> getSampleRemovedFlags() {
        return this.sampleRemovedFlags;
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
    public String getSource() {
        return this.source;
    }

    /**
     * <p>
     * The ID of the source experiment, if this is an ExpressionExperimentSubSet; otherwise will be null.
     * </p>
     */
    public Long getSourceExperiment() {
        return this.sourceExperiment;
    }

    /**
     * 
     */
    public String getTaxon() {
        return this.taxon;
    }

    /**
     * @return the taxonId
     */
    public Long getTaxonId() {
        return taxonId;
    }

    /**
     * 
     */
    public String getTechnologyType() {
        return this.technologyType;
    }

    /**
     * 
     */
    public boolean getTroubled() {
        return this.troubled;
    }

    public String getTroubleDetails() {
        return troubleDetails;
    }

    /**
     * 
     */
    public boolean getValidated() {
        return this.validated;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( id == null ) ? 0 : id.hashCode() );
        return result;
    }

    /**
     * 
     */
    public boolean isHasBothIntensities() {
        return this.hasBothIntensities;
    }

    public void setAccession( String accession ) {
        this.accession = accession;
    }

    public void setArrayDesignCount( Integer arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }

    /**
     * @param date
     */
    public void setAutoTagDate( Date date ) {
        this.autoTagDate = date;
    }

    public void setBatchFetchEventType( String batchFetchEventType ) {
        this.batchFetchEventType = batchFetchEventType;
    }

    public void setBioAssayCount( Integer bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }

    public void setBioMaterialCount( Integer bioMaterialCount ) {
        this.bioMaterialCount = bioMaterialCount;
    }

    public void setClazz( String clazz ) {
        this.clazz = clazz;
    }

    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }

    public void setDateArrayDesignLastUpdated( Date dateArrayDesignLastUpdated ) {
        this.dateArrayDesignLastUpdated = dateArrayDesignLastUpdated;
    }

    public void setDateBatchFetch( Date dateBatchFetch ) {
        this.dateBatchFetch = dateBatchFetch;
    }

    public void setDateCached( Date dateCached ) {
        this.dateCached = dateCached;
    }

    public void setDateCreated( Date dateCreated ) {
        this.dateCreated = dateCreated;
    }

    public void setDateDifferentialAnalysis( Date dateDifferentialAnalysis ) {
        this.dateDifferentialAnalysis = dateDifferentialAnalysis;
    }

    public void setDateLastUpdated( Date dateLastUpdated ) {
        this.dateLastUpdated = dateLastUpdated;
    }

    public void setDateLinkAnalysis( Date dateLinkAnalysis ) {
        this.dateLinkAnalysis = dateLinkAnalysis;
    }

    public void setDateMissingValueAnalysis( Date dateMissingValueAnalysis ) {
        this.dateMissingValueAnalysis = dateMissingValueAnalysis;
    }

    public void setDatePcaAnalysis( Date datePcaAnalysis ) {
        this.datePcaAnalysis = datePcaAnalysis;
    }

    public void setDateProcessedDataVectorComputation( Date dateProcessedDataVectorComputation ) {
        this.dateProcessedDataVectorComputation = dateProcessedDataVectorComputation;
    }

    public void setDesignElementDataVectorCount( Integer designElementDataVectorCount ) {
        this.designElementDataVectorCount = designElementDataVectorCount;
    }

    public void setDifferentialExpressionAnalyses(
            Collection<DifferentialExpressionAnalysisValueObject> differentialExpressionAnalyses ) {
        this.differentialExpressionAnalyses = differentialExpressionAnalyses;
    }

    public void setExperimentalDesign( Long experimentalDesign ) {
        this.experimentalDesign = experimentalDesign;
    }

    public void setExternalDatabase( String externalDatabase ) {
        this.externalDatabase = externalDatabase;
    }

    public void setExternalUri( String externalUri ) {
        this.externalUri = externalUri;
    }

    public void setHasBothIntensities( boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public void setHasBothIntensities( Boolean hasBothIntensities ) {
        this.hasBothIntensities = hasBothIntensities;
    }

    public void setHasEitherIntensity( Boolean hasEitherIntensity ) {
        this.hasEitherIntensity = hasEitherIntensity;
    }

    public void setHasProbeSpecificForQueryGene( Boolean hasProbeSpecificForQueryGene ) {
        this.hasProbeSpecificForQueryGene = hasProbeSpecificForQueryGene;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setInvestigators( String investigators ) {
        this.investigators = investigators;
    }

    @Override
    public void setIsPublic( boolean isPublic ) {
        this.isPublic = isPublic;
    }

    public void setLinkAnalysisEventType( String linkAnalysisEventType ) {
        this.linkAnalysisEventType = linkAnalysisEventType;
    }

    public void setMinPvalue( Double minPvalue ) {
        this.minPvalue = minPvalue;
    }

    public void setMissingValueAnalysisEventType( String missingValueAnalysisEventType ) {
        this.missingValueAnalysisEventType = missingValueAnalysisEventType;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public void setNumAnnotations( Integer numAnnotations ) {
        this.numAnnotations = numAnnotations;
    }

    public void setNumPopulatedFactors( Integer numPopulatedFactors ) {
        this.numPopulatedFactors = numPopulatedFactors;
    }

    public void setOwner( String owner ) {
        this.owner = owner;
    }

    public void setPcaAnalysisEventType( String pcaAnalysisEventType ) {
        this.pcaAnalysisEventType = pcaAnalysisEventType;
    }

    public void setProcessedDataVectorComputationEventType( String processedDataVectorComputationEventType ) {
        this.processedDataVectorComputationEventType = processedDataVectorComputationEventType;
    }

    public void setProcessedExpressionVectorCount( Integer processedExpressionVectorCount ) {
        this.processedExpressionVectorCount = processedExpressionVectorCount;
    }

    public void setPubmedId( Integer pubmedId ) {
        this.pubmedId = pubmedId;
    }

    public void setSampleRemovedFlags( Collection<AuditEventValueObject> sampleRemovedFlags ) {

        this.sampleRemovedFlags = sampleRemovedFlags;
    }

    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public void setSource( String source ) {
        this.source = source;
    }

    public void setSourceExperiment( Long sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

    public void setTaxon( String taxon ) {
        this.taxon = taxon;
    }

    /**
     * @param taxonId the taxonId to set
     */
    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setTechnologyType( String technologyType ) {
        this.technologyType = technologyType;
    }

    public void setTroubled( Boolean troubleFlag ) {
        this.troubled = troubleFlag;
    }

    public void setTroubleDetails( String troubleDetails ) {
        this.troubleDetails = troubleDetails;
    }

    public void setValidated( Boolean validatedFlag ) {
        this.validated = validatedFlag;
    }

    @Override
    public String toString() {
        return this.getShortName() + " (id = " + this.getId() + ")";
    }

    @Override
    public boolean getUserOwned() {
        return this.currentUserIsOwner;
    }

    @Override
    public void setUserOwned( boolean isUserOwned ) {
        this.currentUserIsOwner = isUserOwned;
    }

    @Override
    public boolean getUserCanWrite() {
        return this.currentUserHasWritePermission;
    }

    @Override
    public void setUserCanWrite( boolean userCanWrite ) {
        this.currentUserHasWritePermission = userCanWrite;
    }

    @Override
    public void setIsShared( boolean isShared ) {
        this.isShared = isShared;
    }

    @Override
    public boolean getIsPublic() {
        return this.isPublic;
    }

    @Override
    public boolean getIsShared() {
        return this.isShared;
    }

}