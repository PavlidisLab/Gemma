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

import ubic.gemma.model.IdentifiableValueObject;

/**
 * Simplified representation of Geeq
 *
 * @author paul, tesarst
 */
@SuppressWarnings("unused") // Used in front end
public class GeeqValueObject extends IdentifiableValueObject<Geeq> {

    private double detectedQualityScore;
    private double manualQualityScore;
    private boolean manualQualityOverride;

    private double detectedSuitabilityScore;
    private double manualSuitabilityScore;
    private boolean manualSuitabilityOverride;

    /*
     * Suitability score factors
     */

    private double sScorePublication;
    private double sScorePlatformAmount;
    private double sScorePlatformsTechMulti;
    private double sScoreAvgPlatformPopularity;
    private double sScoreAvgPlatformSize;
    private double sScoreSampleSize;
    private double sScoreRawData;
    private double sScoreMissingValues;

    /*
     * Quality score factors
     */

    private double qScoreOutliers;
    private double qScoreSampleMeanCorrelation;
    private double qScoreSampleMedianCorrelation;
    private double qScoreSampleCorrelationVariance;
    private double qScorePlatformsTech;
    private double qScoreReplicates;
    private double qScoreBatchInfo;
    private double qScoreBatchEffect;
    private boolean manualHasStrongBatchEffect;
    private boolean manualHasNoBatchEffect;
    private boolean manualBatchEffectActive;
    private double qScoreBatchConfound;
    private boolean manualHasBatchConfound;
    private boolean manualBatchConfoundActive;

    /**
     * Required when using the class as a spring bean
     */
    public GeeqValueObject() {
    }

    public GeeqValueObject( Object[] row ) {
        super( ( Long ) row[0] );
        this.detectedQualityScore = ( double ) row[1];
        this.manualQualityScore = ( double ) row[2];
        this.manualQualityOverride = ( boolean ) row[3];
        this.detectedSuitabilityScore = ( double ) row[4];
        this.manualSuitabilityScore = ( double ) row[5];
        this.manualSuitabilityOverride = ( boolean ) row[6];
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
        this.qScoreBatchEffect = ( double ) row[23];
        this.manualHasStrongBatchEffect = ( boolean ) row[24];
        this.manualHasNoBatchEffect = ( boolean ) row[25];
        this.manualBatchEffectActive = ( boolean ) row[26];
        this.qScoreBatchConfound = ( double ) row[27];
        this.manualHasBatchConfound = ( boolean ) row[28];
        this.manualBatchConfoundActive = ( boolean ) row[29];
    }

    public GeeqValueObject( Geeq g ) {
        super( g.getId() );
        this.detectedQualityScore = g.getDetectedQualityScore();
        this.manualQualityScore = g.getManualQualityScore();
        this.manualQualityOverride = g.getManualQualityOverride();
        this.detectedSuitabilityScore = g.getDetectedSuitabilityScore();
        this.manualSuitabilityScore = g.getManualSuitabilityScore();
        this.manualSuitabilityOverride = g.getManualSuitabilityOverride();
        this.sScorePublication = g.getSScorePublication();
        this.sScorePlatformAmount = g.getSScorePlatformAmount();
        this.sScorePlatformsTechMulti = g.getSScorePlatformsTechMulti();
        this.sScoreAvgPlatformPopularity = g.getSScoreAvgPlatformPopularity();
        this.sScoreAvgPlatformSize = g.getSScoreAvgPlatformSize();
        this.sScoreSampleSize = g.getSScoreSampleSize();
        this.sScoreRawData = g.getSScoreRawData();
        this.sScoreMissingValues = g.getSScoreMissingValues();
        this.qScoreOutliers = g.getQScoreOutliers();
        this.qScoreSampleMeanCorrelation = g.getQScoreSampleMeanCorrelation();
        this.qScoreSampleMedianCorrelation = g.getQScoreSampleMedianCorrelation();
        this.qScoreSampleCorrelationVariance = g.getQScoreSampleCorrelationVariance();
        this.qScorePlatformsTech = g.getQScorePlatformsTech();
        this.qScoreReplicates = g.getQScoreReplicates();
        this.qScoreBatchInfo = g.getQScoreBatchInfo();
        this.qScoreBatchEffect = g.getQScoreBatchEffect();
        this.manualHasStrongBatchEffect = g.getManualHasStrongBatchEffect();
        this.manualHasNoBatchEffect = g.getManualHasNoBatchEffect();
        this.manualBatchEffectActive = g.getManualBatchEffectActive();
        this.qScoreBatchConfound = g.getQScoreBatchConfound();
        this.manualHasBatchConfound = g.getManualHasBatchConfound();
        this.manualBatchConfoundActive = g.getManualBatchConfoundActive();
    }

    public GeeqValueObject( Long id, double detectedQualityScore, double manualQualityScore,
            boolean manualQualityOverride, double detectedSuitabilityScore, double manualSuitabilityScore,
            boolean manualSuitabilityOverride, double sScorePublication, double sScorePlatformAmount,
            double sScorePlatformsTechMulti, double sScoreAvgPlatformPopularity, double sScoreAvgPlatformSize,
            double sScoreSampleSize, double sScoreRawData, double sScoreMissingValues, double qScoreOutliers,
            double qScoreSampleMeanCorrelation, double qScoreSampleMedianCorrelation,
            double qScoreSampleCorrelationVariance, double qScorePlatformsTech, double qScoreReplicates,
            double qScoreBatchInfo, double qScoreBatchEffect, boolean manualHasStrongBatchEffect,
            boolean manualHasNoBatchEffect, boolean manualBatchEffectActive, double qScoreBatchConfound,
            boolean manualHasBatchConfound, boolean manualBatchConfoundActive ) {
        super( id );
        this.detectedQualityScore = detectedQualityScore;
        this.manualQualityScore = manualQualityScore;
        this.manualQualityOverride = manualQualityOverride;
        this.detectedSuitabilityScore = detectedSuitabilityScore;
        this.manualSuitabilityScore = manualSuitabilityScore;
        this.manualSuitabilityOverride = manualSuitabilityOverride;
        this.sScorePublication = sScorePublication;
        this.sScorePlatformAmount = sScorePlatformAmount;
        this.sScorePlatformsTechMulti = sScorePlatformsTechMulti;
        this.sScoreAvgPlatformPopularity = sScoreAvgPlatformPopularity;
        this.sScoreAvgPlatformSize = sScoreAvgPlatformSize;
        this.sScoreSampleSize = sScoreSampleSize;
        this.sScoreRawData = sScoreRawData;
        this.sScoreMissingValues = sScoreMissingValues;
        this.qScoreOutliers = qScoreOutliers;
        this.qScoreSampleMeanCorrelation = qScoreSampleMeanCorrelation;
        this.qScoreSampleMedianCorrelation = qScoreSampleMedianCorrelation;
        this.qScoreSampleCorrelationVariance = qScoreSampleCorrelationVariance;
        this.qScorePlatformsTech = qScorePlatformsTech;
        this.qScoreReplicates = qScoreReplicates;
        this.qScoreBatchInfo = qScoreBatchInfo;
        this.qScoreBatchEffect = qScoreBatchEffect;
        this.manualHasStrongBatchEffect = manualHasStrongBatchEffect;
        this.manualHasNoBatchEffect = manualHasNoBatchEffect;
        this.manualBatchEffectActive = manualBatchEffectActive;
        this.qScoreBatchConfound = qScoreBatchConfound;
        this.manualHasBatchConfound = manualHasBatchConfound;
        this.manualBatchConfoundActive = manualBatchConfoundActive;
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

    public double getsScorePublication() {
        return sScorePublication;
    }

    public void setsScorePublication( double sScorePublication ) {
        this.sScorePublication = sScorePublication;
    }

    public double getsScorePlatformAmount() {
        return sScorePlatformAmount;
    }

    public void setsScorePlatformAmount( double sScorePlatformAmount ) {
        this.sScorePlatformAmount = sScorePlatformAmount;
    }

    public double getsScorePlatformsTechMulti() {
        return sScorePlatformsTechMulti;
    }

    public void setsScorePlatformsTechMulti( double sScorePlatformsTechMulti ) {
        this.sScorePlatformsTechMulti = sScorePlatformsTechMulti;
    }

    public double getsScoreAvgPlatformPopularity() {
        return sScoreAvgPlatformPopularity;
    }

    public void setsScoreAvgPlatformPopularity( double sScoreAvgPlatformPopularity ) {
        this.sScoreAvgPlatformPopularity = sScoreAvgPlatformPopularity;
    }

    public double getsScoreAvgPlatformSize() {
        return sScoreAvgPlatformSize;
    }

    public void setsScoreAvgPlatformSize( double sScoreAvgPlatformSize ) {
        this.sScoreAvgPlatformSize = sScoreAvgPlatformSize;
    }

    public double getsScoreSampleSize() {
        return sScoreSampleSize;
    }

    public void setsScoreSampleSize( double sScoreSampleSize ) {
        this.sScoreSampleSize = sScoreSampleSize;
    }

    public double getsScoreRawData() {
        return sScoreRawData;
    }

    public void setsScoreRawData( double sScoreRawData ) {
        this.sScoreRawData = sScoreRawData;
    }

    public double getsScoreMissingValues() {
        return sScoreMissingValues;
    }

    public void setsScoreMissingValues( double sScoreMissingValues ) {
        this.sScoreMissingValues = sScoreMissingValues;
    }

    public double getqScoreOutliers() {
        return qScoreOutliers;
    }

    public void setqScoreOutliers( double qScoreOutliers ) {
        this.qScoreOutliers = qScoreOutliers;
    }

    public double getqScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    public void setqScoreSampleMeanCorrelation( double qScoreSampleMeanCorrelation ) {
        this.qScoreSampleMeanCorrelation = qScoreSampleMeanCorrelation;
    }

    public double getqScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    public void setqScorePlatformsTech( double qScorePlatformsTech ) {
        this.qScorePlatformsTech = qScorePlatformsTech;
    }

    public double getqScoreReplicates() {
        return qScoreReplicates;
    }

    public void setqScoreReplicates( double qScoreReplicates ) {
        this.qScoreReplicates = qScoreReplicates;
    }

    public double getqScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    public void setqScoreBatchInfo( double qScoreBatchInfo ) {
        this.qScoreBatchInfo = qScoreBatchInfo;
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

    public double getqScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    public void setqScoreSampleMedianCorrelation( double qScoreSampleMedianCorrelation ) {
        this.qScoreSampleMedianCorrelation = qScoreSampleMedianCorrelation;
    }

    public double getqScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    public void setqScoreSampleCorrelationVariance( double qScoreSampleCorrelationVariance ) {
        this.qScoreSampleCorrelationVariance = qScoreSampleCorrelationVariance;
    }
}
