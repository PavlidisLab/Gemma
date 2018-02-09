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

/**
 * Represents administrative geeq information. On top of the classic VO, this one also exposes
 * the underlying variables behind the public scores for suitability, quality, batch effect and batch confound.
 *
 * @author paul, tesarst
 */
public class GeeqAdminValueObject extends GeeqValueObject {

    private double detectedQualityScore;
    private double manualQualityScore;
    private boolean manualQualityOverride;

    private double detectedSuitabilityScore;
    private double manualSuitabilityScore;
    private boolean manualSuitabilityOverride;

    private double qScoreBatchEffect;
    private boolean manualHasStrongBatchEffect;
    private boolean manualHasNoBatchEffect;
    private boolean manualBatchEffectActive;
    private double qScoreBatchConfound;
    private boolean manualHasBatchConfound;
    private boolean manualBatchConfoundActive;

    private String otherIssues;

    /**
     * Required when using the class as a spring bean
     */
    public GeeqAdminValueObject() {
    }

    public GeeqAdminValueObject( Object[] row ) {
        super( row );
        this.detectedQualityScore = ( double ) row[1];
        this.manualQualityScore = ( double ) row[2];
        this.manualQualityOverride = ( boolean ) row[3];
        this.detectedSuitabilityScore = ( double ) row[4];
        this.manualSuitabilityScore = ( double ) row[5];
        this.manualSuitabilityOverride = ( boolean ) row[6];

        this.qScoreBatchEffect = ( double ) row[23];
        this.manualHasStrongBatchEffect = ( boolean ) row[24];
        this.manualHasNoBatchEffect = ( boolean ) row[25];
        this.manualBatchEffectActive = ( boolean ) row[26];
        this.qScoreBatchConfound = ( double ) row[27];
        this.manualHasBatchConfound = ( boolean ) row[28];
        this.manualBatchConfoundActive = ( boolean ) row[29];
        this.otherIssues = ( String ) row[34];
    }

    public GeeqAdminValueObject( Geeq g ) {
        super( g );
        this.detectedQualityScore = g.getDetectedQualityScore();
        this.manualQualityScore = g.getManualQualityScore();
        this.manualQualityOverride = g.getManualQualityOverride();
        this.detectedSuitabilityScore = g.getDetectedSuitabilityScore();
        this.manualSuitabilityScore = g.getManualSuitabilityScore();
        this.manualSuitabilityOverride = g.getManualSuitabilityOverride();

        this.qScoreBatchEffect = g.getQScoreBatchEffect();
        this.manualHasStrongBatchEffect = g.getManualHasStrongBatchEffect();
        this.manualHasNoBatchEffect = g.getManualHasNoBatchEffect();
        this.manualBatchEffectActive = g.getManualBatchEffectActive();
        this.qScoreBatchConfound = g.getQScoreBatchConfound();
        this.manualHasBatchConfound = g.getManualHasBatchConfound();
        this.manualBatchConfoundActive = g.getManualBatchConfoundActive();
        this.otherIssues = g.getOtherIssues();
    }

    public GeeqAdminValueObject( Long id, double detectedQualityScore, double manualQualityScore,
            boolean manualQualityOverride, double detectedSuitabilityScore, double manualSuitabilityScore,
            boolean manualSuitabilityOverride, double sScorePublication, double sScorePlatformAmount,
            double sScorePlatformsTechMulti, double sScoreAvgPlatformPopularity, double sScoreAvgPlatformSize,
            double sScoreSampleSize, double sScoreRawData, double sScoreMissingValues, double qScoreOutliers,
            double qScoreSampleMeanCorrelation, double qScoreSampleMedianCorrelation,
            double qScoreSampleCorrelationVariance, double qScorePlatformsTech, double qScoreReplicates,
            double qScoreBatchInfo, double qScoreBatchEffect, boolean manualHasStrongBatchEffect,
            boolean manualHasNoBatchEffect, boolean manualBatchEffectActive, double qScoreBatchConfound,
            boolean manualHasBatchConfound, boolean manualBatchConfoundActive, String otherIssues ) {
        super( id, detectedQualityScore, manualQualityScore, manualQualityOverride, detectedSuitabilityScore,
                manualSuitabilityScore, manualSuitabilityOverride, sScorePublication, sScorePlatformAmount,
                sScorePlatformsTechMulti, sScoreAvgPlatformPopularity, sScoreAvgPlatformSize, sScoreSampleSize,
                sScoreRawData, sScoreMissingValues, qScoreOutliers, qScoreSampleMeanCorrelation,
                qScoreSampleMedianCorrelation, qScoreSampleCorrelationVariance, qScorePlatformsTech, qScoreReplicates,
                qScoreBatchInfo, qScoreBatchEffect, manualHasStrongBatchEffect, manualHasNoBatchEffect,
                manualBatchEffectActive, qScoreBatchConfound, manualHasBatchConfound, manualBatchConfoundActive );
        this.detectedQualityScore = detectedQualityScore;
        this.manualQualityScore = manualQualityScore;
        this.manualQualityOverride = manualQualityOverride;
        this.detectedSuitabilityScore = detectedSuitabilityScore;
        this.manualSuitabilityScore = manualSuitabilityScore;
        this.manualSuitabilityOverride = manualSuitabilityOverride;

        this.qScoreBatchEffect = qScoreBatchEffect;
        this.manualHasStrongBatchEffect = manualHasStrongBatchEffect;
        this.manualHasNoBatchEffect = manualHasNoBatchEffect;
        this.manualBatchEffectActive = manualBatchEffectActive;
        this.qScoreBatchConfound = qScoreBatchConfound;
        this.manualHasBatchConfound = manualHasBatchConfound;
        this.manualBatchConfoundActive = manualBatchConfoundActive;

        this.otherIssues = otherIssues;
    }

    public double getDetectedQualityScore() {
        return detectedQualityScore;
    }

    public double getDetectedSuitabilityScore() {
        return detectedSuitabilityScore;
    }

    public double getManualQualityScore() {
        return manualQualityScore;
    }

    public void setManualQualityScore( double manualQualityScore ) {
        this.manualQualityScore = manualQualityScore;
    }

    public boolean getManualQualityOverride() {
        return manualQualityOverride;
    }

    public void setManualQualityOverride( boolean manualQualityOverride ) {
        this.manualQualityOverride = manualQualityOverride;
    }

    public double getManualSuitabilityScore() {
        return manualSuitabilityScore;
    }

    public void setManualSuitabilityScore( double manualSuitabilityScore ) {
        this.manualSuitabilityScore = manualSuitabilityScore;
    }

    public boolean getManualSuitabilityOverride() {
        return manualSuitabilityOverride;
    }

    public void setManualSuitabilityOverride( boolean manualSuitabilityOverride ) {
        this.manualSuitabilityOverride = manualSuitabilityOverride;
    }

    public double getqScoreBatchEffect() {
        return qScoreBatchEffect;
    }

    public void setqScoreBatchEffect( double qScoreBatchEffect ) {
        this.qScoreBatchEffect = qScoreBatchEffect;
    }

    public boolean getManualHasStrongBatchEffect() {
        return manualHasStrongBatchEffect;
    }

    public void setManualHasStrongBatchEffect( boolean manualHasStrongBatchEffect ) {
        this.manualHasStrongBatchEffect = manualHasStrongBatchEffect;
    }

    public boolean getManualHasNoBatchEffect() {
        return manualHasNoBatchEffect;
    }

    public void setManualHasNoBatchEffect( boolean manualHasNoBatchEffect ) {
        this.manualHasNoBatchEffect = manualHasNoBatchEffect;
    }

    public boolean getManualBatchEffectActive() {
        return manualBatchEffectActive;
    }

    public void setManualBatchEffectActive( boolean manualBatchEffectActive ) {
        this.manualBatchEffectActive = manualBatchEffectActive;
    }

    public double getqScoreBatchConfound() {
        return qScoreBatchConfound;
    }

    public void setqScoreBatchConfound( double qScoreBatchConfound ) {
        this.qScoreBatchConfound = qScoreBatchConfound;
    }

    public boolean getManualHasBatchConfound() {
        return manualHasBatchConfound;
    }

    public void setManualHasBatchConfound( boolean manualHasBatchConfound ) {
        this.manualHasBatchConfound = manualHasBatchConfound;
    }

    public boolean getManualBatchConfoundActive() {
        return manualBatchConfoundActive;
    }

    public void setManualBatchConfoundActive( boolean manualBatchConfoundActive ) {
        this.manualBatchConfoundActive = manualBatchConfoundActive;
    }

    public String getOtherIssues() {
        return otherIssues;
    }

    public void setOtherIssues( String otherIssues ) {
        this.otherIssues = otherIssues;
    }
}
