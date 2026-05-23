/*
 * The gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

import org.springframework.lang.Nullable;
import java.util.Date;

/**
 * Public per-factor GEEQ breakdown. Mirrors {@link GeeqValueObject} but without the
 * {@code @GemmaWebOnly} JSON-suppression on the per-factor sScore* / qScore* getters, so the
 * decomposed scores reach REST clients. Admin-only fields exposed by
 * {@link GeeqAdminValueObject} (detected/manual override scores, free-text {@code otherIssues})
 * are deliberately omitted.
 *
 * @author paul, tesarst
 */
@SuppressWarnings("unused") // Used in REST clients
@Setter
@ToString
public class PublicGeeqValueObject extends IdentifiableValueObject<Geeq> {

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
     * Timestamp of the last {@link ubic.gemma.model.common.auditAndSecurity.eventType.GeeqEvent} for the
     * experiment, populated by callers that have access to the audit log. {@code null} when unknown or never
     * recorded.
     */
    @Nullable
    private Date lastComputed;

    /**
     * Required when using the class as a spring bean
     */
    @SuppressWarnings("WeakerAccess")
    public PublicGeeqValueObject() {
        super();
    }

    public PublicGeeqValueObject( Geeq g ) {
        super( g );
        this.publicQualityScore = g.isManualQualityOverride() ? g.getManualQualityScore() : g.getDetectedQualityScore();
        this.publicSuitabilityScore = g.isManualSuitabilityOverride() ? g.getManualSuitabilityScore() : g.getDetectedSuitabilityScore();
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
        this.qScorePublicBatchEffect = computePublicBatchEffect( g );
        this.qScorePublicBatchConfound = computePublicBatchConfound( g );
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

    public double getsScorePublication() {
        return sScorePublication;
    }

    public double getsScorePlatformAmount() {
        return sScorePlatformAmount;
    }

    public double getsScorePlatformsTechMulti() {
        return sScorePlatformsTechMulti;
    }

    public double getsScoreAvgPlatformPopularity() {
        return sScoreAvgPlatformPopularity;
    }

    public double getsScoreAvgPlatformSize() {
        return sScoreAvgPlatformSize;
    }

    public double getsScoreSampleSize() {
        return sScoreSampleSize;
    }

    public double getsScoreRawData() {
        return sScoreRawData;
    }

    public double getsScoreMissingValues() {
        return sScoreMissingValues;
    }

    public double getqScoreOutliers() {
        return qScoreOutliers;
    }

    public double getqScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    public double getqScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    public double getqScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    public double getqScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    public double getqScoreReplicates() {
        return qScoreReplicates;
    }

    public double getqScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    public double getqScorePublicBatchEffect() {
        return qScorePublicBatchEffect;
    }

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

    @Nullable
    public Date getLastComputed() {
        return lastComputed;
    }

    private static double computePublicBatchEffect( Geeq g ) {
        if ( !g.isManualBatchEffectActive() ) {
            return g.getqScoreBatchEffect();
        }
        if ( g.isManualHasStrongBatchEffect() ) {
            return GeeqServiceImpl.BATCH_EFF_STRONG;
        }
        if ( g.isManualHasNoBatchEffect() ) {
            return GeeqServiceImpl.BATCH_EFF_NONE;
        }
        return GeeqServiceImpl.BATCH_EFF_WEAK;
    }

    private static double computePublicBatchConfound( Geeq g ) {
        if ( !g.isManualBatchConfoundActive() ) {
            return g.getqScoreBatchConfound();
        }
        return g.isManualHasBatchConfound() ? GeeqServiceImpl.BATCH_CONF_HAS : GeeqServiceImpl.BATCH_CONF_NO_HAS;
    }
}
