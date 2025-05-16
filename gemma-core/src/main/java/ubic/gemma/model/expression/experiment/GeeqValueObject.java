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
import lombok.Setter;
import lombok.ToString;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

/**
 * Represents publicly available geeq information
 *
 * @author paul, tesarst
 */
@SuppressWarnings("unused") // Used in frontend
@Setter
@ToString
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
        super();
    }

    public GeeqValueObject( Geeq g ) {
        super( g );
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

    public double getPublicQualityScore() {
        return publicQualityScore;
    }

    public double getPublicSuitabilityScore() {
        return publicSuitabilityScore;
    }


    @GemmaWebOnly
    public double getsScorePublication() {
        return sScorePublication;
    }

    @GemmaWebOnly
    public double getsScorePlatformAmount() {
        return sScorePlatformAmount;
    }

    @GemmaWebOnly
    public double getsScorePlatformsTechMulti() {
        return sScorePlatformsTechMulti;
    }

    @GemmaWebOnly
    public double getsScoreAvgPlatformPopularity() {
        return sScoreAvgPlatformPopularity;
    }

    @GemmaWebOnly
    public double getsScoreAvgPlatformSize() {
        return sScoreAvgPlatformSize;
    }

    @GemmaWebOnly
    public double getsScoreSampleSize() {
        return sScoreSampleSize;
    }

    @GemmaWebOnly
    public double getsScoreRawData() {
        return sScoreRawData;
    }

    @GemmaWebOnly
    public double getsScoreMissingValues() {
        return sScoreMissingValues;
    }

    @GemmaWebOnly
    public double getqScoreOutliers() {
        return qScoreOutliers;
    }

    @GemmaWebOnly
    public double getqScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    @GemmaWebOnly
    public double getqScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    @GemmaWebOnly
    public double getqScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    @GemmaWebOnly
    public double getqScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    @GemmaWebOnly
    public double getqScoreReplicates() {
        return qScoreReplicates;
    }

    @GemmaWebOnly
    public double getqScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    @GemmaWebOnly
    public double getqScorePublicBatchEffect() {
        return qScorePublicBatchEffect;
    }

    @GemmaWebOnly
    public double getqScorePublicBatchConfound() {
        return qScorePublicBatchConfound;
    }

    public boolean isNoVectors() {
        return noVectors;
    }

    public byte getCorrMatIssues() {
        return corrMatIssues;
    }

    public byte getReplicatesIssues() {
        return replicatesIssues;
    }

    public boolean isBatchCorrected() {
        return batchCorrected;
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
