/*
 * The gemma project
 *
 * Copyright (c) 2018 University of British Columbia
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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

/**
 * Represents publicly available geeq information
 *
 * @author paul, tesarst
 */
@SuppressWarnings("unused") // Used in frontend
@Data
@EqualsAndHashCode(callSuper = true)
public class GeeqValueObject extends IdentifiableValueObject<Geeq> {

    private double publicQualityScore;
    private double publicSuitabilityScore;

    /*
     * Suitability score factors
     */

    @JsonProperty("sScorePublication")
    private double sScorePublication;
    @JsonProperty("sScorePlatformAmount")
    private double sScorePlatformAmount;
    @JsonProperty("sScorePlatformTechMulti")
    private double sScorePlatformsTechMulti;
    @JsonProperty("sScoreAvgPlatformPopularity")
    private double sScoreAvgPlatformPopularity;
    @JsonProperty("sScoreAvgPlatformSize")
    private double sScoreAvgPlatformSize;
    @JsonProperty("sScoreSampleSize")
    private double sScoreSampleSize;
    @JsonProperty("sScoreRawData")
    private double sScoreRawData;
    @JsonProperty("sScoreMissingValues")
    private double sScoreMissingValues;

    /*
     * Quality score factors
     */

    @JsonProperty("qScoreOutliers")
    private double qScoreOutliers;
    @JsonProperty("qScoreSampleMeanCorrelation")
    private double qScoreSampleMeanCorrelation;
    @JsonProperty("qScoreSampleMedianCorrelation")
    private double qScoreSampleMedianCorrelation;
    @JsonProperty("qScoreSampleCorrelationVariance")
    private double qScoreSampleCorrelationVariance;
    @JsonProperty("qScorePlatformsTech")
    private double qScorePlatformsTech;
    @JsonProperty("qScoreReplicates")
    private double qScoreReplicates;
    @JsonProperty("qScoreBatchInfo")
    private double qScoreBatchInfo;
    @JsonProperty("qScorePublicBatchEffect")
    private double qScorePublicBatchEffect;
    @JsonProperty("qScorePublicBatchConfound")
    private double qScorePublicBatchConfound;

    /*
     * Problem/info flags
     */

    private boolean noVectors;
    private byte corrMatIssues;
    private byte replicatesIssues;
    private boolean batchCorrected;

    /**
     * Required when using the class as a spring bean
     */
    @SuppressWarnings("WeakerAccess") //Spring needs it to be public
    public GeeqValueObject() {
    }

    public GeeqValueObject( Object[] row ) {
        super( ( Long ) row[0] );

        this.setPublicQualityScore( ( double ) row[1], ( double ) row[2], ( boolean ) row[3] );
        this.setPublicSuitabilityScore( ( double ) row[4], ( double ) row[5], ( boolean ) row[6] );

        this.sScorePublication = ( double ) row[7];
        this.sScorePlatformAmount = ( double ) row[8];
        this.sScorePlatformsTechMulti = ( double ) row[9];
        this.sScoreAvgPlatformPopularity = ( double ) row[10];
        this.sScoreAvgPlatformSize = ( double ) row[11];
        this.sScoreSampleSize = ( double ) row[12];
        this.sScoreRawData = ( double ) row[13];
        this.sScoreMissingValues = ( double ) row[14];
        this.qScoreOutliers = ( double ) row[15];
        this.qScoreSampleMeanCorrelation = ( double ) row[16];
        this.qScoreSampleMedianCorrelation = ( double ) row[17];
        this.qScoreSampleCorrelationVariance = ( double ) row[18];
        this.qScorePlatformsTech = ( double ) row[19];
        this.qScoreReplicates = ( double ) row[20];
        this.qScoreBatchInfo = ( double ) row[21];
        this.setQScorePublicBatchEffect( ( double ) row[23], ( boolean ) row[24], ( boolean ) row[25],
                ( boolean ) row[26] );
        this.setQScorePublicBatchConfound( ( double ) row[27], ( boolean ) row[28], ( boolean ) row[29] );
        this.noVectors = ( boolean ) row[30];
        this.corrMatIssues = ( byte ) row[31];
        this.replicatesIssues = ( byte ) row[32];
        this.batchCorrected = ( boolean ) row[33];
    }

    public GeeqValueObject( Geeq g ) {
        super( g.getId() );
        this.setPublicQualityScore( g.getDetectedQualityScore(), g.getManualQualityScore(),
                g.isManualQualityOverride() );
        this.setPublicSuitabilityScore( g.getDetectedSuitabilityScore(), g.getManualSuitabilityScore(),
                g.isManualSuitabilityOverride() );
        this.sScorePublication = g.getsScorePublication();
        this.sScorePlatformAmount = g.getsScorePlatformAmount();
        this.sScorePlatformsTechMulti = g.getsScorePlatformsTechMulti();
        this.sScoreAvgPlatformPopularity = g.getsScoreAvgPlatformPopularity();
        this.sScoreAvgPlatformSize = g.getsScoreAvgPlatformSize();
        this.sScoreSampleSize = g.getsScoreSampleSize();
        this.sScoreRawData = g.getsScoreRawData();
        this.sScoreMissingValues = g.getsScoreMissingValues();
        this.qScoreOutliers = g.getqScoreOutliers();
        this.qScoreSampleMeanCorrelation = g.getqScoreSampleMeanCorrelation();
        this.qScoreSampleMedianCorrelation = g.getqScoreSampleMedianCorrelation();
        this.qScoreSampleCorrelationVariance = g.getqScoreSampleCorrelationVariance();
        this.qScorePlatformsTech = g.getqScorePlatformsTech();
        this.qScoreReplicates = g.getqScoreReplicates();
        this.qScoreBatchInfo = g.getqScoreBatchInfo();
        this.setQScorePublicBatchEffect( g.getqScoreBatchEffect(), g.isManualHasStrongBatchEffect(),
                g.isManualHasNoBatchEffect(), g.isManualBatchEffectActive() );
        this.setQScorePublicBatchConfound( g.getqScoreBatchConfound(), g.isManualHasBatchConfound(),
                g.isManualBatchConfoundActive() );
        this.noVectors = g.isNoVectors();
        this.batchCorrected = g.isBatchCorrected();
        this.corrMatIssues = g.getCorrMatIssues();
        this.replicatesIssues = g.getReplicatesIssues();
    }

    private void setPublicQualityScore( double detected, double manual, boolean override ) {
        this.publicQualityScore = override ? manual : detected;
    }

    private void setPublicSuitabilityScore( double detected, double manual, boolean override ) {
        this.publicSuitabilityScore = override ? manual : detected;
    }

    private void setQScorePublicBatchEffect( double detected, boolean manualStrong, boolean manualNone,
            boolean override ) {
        this.qScorePublicBatchEffect = //
                !override ? detected : //
                        manualStrong ? GeeqServiceImpl.BATCH_EFF_STRONG : //
                                manualNone ? GeeqServiceImpl.BATCH_EFF_NONE : GeeqServiceImpl.BATCH_EFF_WEAK;
    }

    private void setQScorePublicBatchConfound( double detected, boolean manualHasConfound, boolean override ) {
        this.qScorePublicBatchConfound = //
                !override ? detected : //
                        manualHasConfound ? GeeqServiceImpl.BATCH_CONF_HAS : GeeqServiceImpl.BATCH_CONF_NO_HAS;
    }
}
