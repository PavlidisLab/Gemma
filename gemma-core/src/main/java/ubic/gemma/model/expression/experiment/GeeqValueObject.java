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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Setter;
import lombok.ToString;
import ubic.gemma.model.annotations.WithheldFromApi;
import ubic.gemma.model.annotations.WithheldFromApi.Reason;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

import org.springframework.lang.Nullable;
import java.util.Date;

/**
 * Represents publicly available geeq information
 *
 * @author paul, tesarst
 */
// Lombok @Setter generates setQScoreOutliers(...) from the field qScoreOutliers, and swagger
// mangles that setter name back to "qscoreOutliers" — a second, lowercase-s spelling of every
// score. Jackson never emitted it (the field's @JsonProperty settles the wire name), so these
// nine were phantom properties: advertised by the specification, absent from every response.
@JsonIgnoreProperties({ "qscoreOutliers", "qscoreSampleMeanCorrelation", "qscoreSampleMedianCorrelation", "qscoreSampleCorrelationVariance", "qscorePlatformsTech", "qscoreReplicates", "qscoreBatchInfo", "qscorePublicBatchEffect", "qscorePublicBatchConfound" })
@SuppressWarnings("unused") // Used in frontend
@Setter
@ToString
public class GeeqValueObject extends IdentifiableValueObject<Geeq> {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)

    private double publicQualityScore;

    /*
     * Quality score factors
     */

    @JsonProperty("qScoreOutliers")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreOutliers;
    @JsonProperty("qScoreSampleMeanCorrelation")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreSampleMeanCorrelation;
    @JsonProperty("qScoreSampleMedianCorrelation")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreSampleMedianCorrelation;
    @JsonProperty("qScoreSampleCorrelationVariance")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreSampleCorrelationVariance;
    @JsonProperty("qScorePlatformsTech")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScorePlatformsTech;
    @JsonProperty("qScoreReplicates")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreReplicates;
    @JsonProperty("qScoreBatchInfo")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScoreBatchInfo;
    @JsonProperty("qScorePublicBatchEffect")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScorePublicBatchEffect;
    @JsonProperty("qScorePublicBatchConfound")
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private double qScorePublicBatchConfound;

    /*
     * Problem/info flags
     */

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)

    private boolean noVectors;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private byte corrMatIssues;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private byte replicatesIssues;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
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
    @SuppressWarnings("WeakerAccess") //Spring needs it to be public
    public GeeqValueObject() {
        super();
    }

    public GeeqValueObject( Geeq g ) {
        super( g );
        this.setPublicQualityScore( g.getDetectedQualityScore(), g.getManualQualityScore(),
                g.isManualQualityOverride() );
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

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreOutliers() {
        return qScoreOutliers;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreReplicates() {
        return qScoreReplicates;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
    public double getqScorePublicBatchEffect() {
        return qScorePublicBatchEffect;
    }

    @WithheldFromApi(value = Reason.REDUNDANT,
            comment = "inert: the field carries @JsonProperty, which publishes this score regardless")
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

    private void setPublicQualityScore( double detected, double manual, boolean override ) {
        this.publicQualityScore = override ? manual : detected;
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
