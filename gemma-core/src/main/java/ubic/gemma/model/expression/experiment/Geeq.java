/*
 * The gemma project
 *
 * Copyright (c) 2015 University of British Columbia
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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;

import javax.persistence.Transient;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents quality information about a data set. The class name comes from the research project name, GEEQ.
 * The score has two components: Quality and Suitability. See the variables javadoc for further description.
 * The scoring rules are implemented in the GeeqServiceImpl, which also exposes public methods for experiment
 * scoring.
 *
 * @author paul, tesarst
 */
public class Geeq implements Identifiable, Serializable {

    private static final long serialVersionUID = 4783171234360698630L;
    private Long id;
    private AuditEvent lastRun;
    private AuditEvent lastManualOverride;
    private AuditEvent lastBatchEffectChange;
    private AuditEvent lastBatchConfoundChange;

    /**
     * Quality refers to data quality, wherein the same study could have been done twice with the same technical
     * parameters and in one case yield bad quality data, and in another high quality data.
     * The quality score can be overridden. The manual value is stored in manualQualityScore, while
     * manualQualityOverride boolean value denotes whether the manual value should be used.
     */
    private double detectedQualityScore;
    private double manualQualityScore;
    private boolean manualQualityOverride;

    /**
     * Suitability mostly refers to technical aspects which, if we were doing the study ourselves, we would have
     * altered to make it optimal for analyses of the sort used in Gemma.
     * The suitability score can be overridden. The manual value is stored in manualSuitabilityScore, while
     * manualSuitabilityOverride boolean value denotes whether the manual value should be used.
     */
    private double detectedSuitabilityScore;
    private double manualSuitabilityScore;
    private boolean manualSuitabilityOverride;

    /*
     * Suitability score factors
     */

    /**
     * <p>-1.0 - if experiment has no publication</p>
     * <p>-0.7 - if date not filled in</p>
     * <p>-0.5 if date &lt; 2006</p>
     * <p>-0.3 if date &lt; 2009</p>
     * <p>+1.0 otherwise</p>
     */
    private double sScorePublication;

    /**
     * The amount of platforms the experiment uses:
     * <p>-1.0 if amount &gt; 2</p>
     * <p>-0.5 if amount &gt; 1</p>
     * <p>+1.0 otherwise</p>
     */
    private double sScorePlatformAmount;

    /**
     * Extra punishment for platform technology inconsistency
     * <p>-1.0 if platforms amount &gt; 1 and platforms do not have the same technology type</p>
     * <p>+1.0 otherwise</p>
     */
    private double sScorePlatformsTechMulti;

    /**
     * Score for each platforms popularity: (final score is average of scores for all used platforms)
     * <p>-1.0 if used in &lt; 10 EEs</p>
     * <p>-0.5 if used in &lt; 20 EEs</p>
     * <p>+0.0 if used in &lt; 50 EEs</p>
     * <p>+0.5 if used in &lt; 100 EEs</p>
     * <p>+1.0 otherwise
     */
    private double sScoreAvgPlatformPopularity;

    /**
     * Score for each platforms size: (final score is average of scores for all used platforms)
     * <p>-1.0 if gene count &lt; 5k</p>
     * <p>-0.5 if gene count &lt; 10k</p>
     * <p>+0.0 if gene count &lt; 15k</p>
     * <p>+0.5 if gene count &lt; 18k</p>
     * <p>+1.0 otherwise
     */
    private double sScoreAvgPlatformSize;

    /**
     * The amount of samples in the experiment
     * <p>-1.0 if sample size &lt; 20
     * <p>-0.5 if sample size &lt; 50
     * <p>+0.0 if sample size &lt; 100
     * <p>+0.5 if sample size &lt; 200
     * <p>+1.0 otherwise
     */
    private double sScoreSampleSize;

    /**
     * Raw data availability (shows also as the 'external' badge)
     * <p>-1.0 if no raw data available</p>
     * <p>+1.0 otherwise</p>
     */
    private double sScoreRawData;

    /**
     * Missing values
     * <p>-1.0 if experiment has any missing values or there are no computed vectors</p>
     * <p>+1.0 otherwise (assumed if experiment has raw data available)</p>
     * extra:
     * noVectors = true, if experiment has no computed vectors
     */
    private double sScoreMissingValues;
    private boolean noVectors;

    /*
     * Quality score factors
     */

    /**
     * Ratio of detected (non-removed) outliers vs sample size:
     * <p>-1.0 if ratio &gt; 5%</p>
     * <p>-0.5 if ratio &gt; 2%</p>
     * <p>+0.0 if ratio &gt; 0.1%</p>
     * <p>+0.5 if ratio &gt; 0% small punishment for very large experiments with one bad apple</p>
     * <p>+1.0 if ratio = 0%</p>
     * extra (in corrMatIssues):
     * 1 if the correlation matrix is empty
     * 2 if the correlation matrix has NaN values
     */
    private double qScoreOutliers;
    private byte corrMatIssues;

    /**
     * Using the mean sample correlation r:
     * <p>+r use the computed value</p>
     * <p>+0.0 if correlation matrix is empty</p>
     */
    private double qScoreSampleMeanCorrelation;

    /**
     * Using the median sample correlation m:
     * <p>+m use the computed value</p>
     * <p>+0.0 if correlation matrix is empty</p>
     */
    private double qScoreSampleMedianCorrelation;

    /**
     * Using the sample correlation variance v:
     * <p>+v use the computed value</p>
     * <p>+0.0 if correlation matrix is empty</p>
     */
    private double qScoreSampleCorrelationVariance;

    /**
     * Platform technologies
     * <p>-1.0 if any platform is two-color</p>
     * <p>+1.0 otherwise</p>
     */
    private double qScorePlatformsTech;

    /**
     * Number of replicates - ee has to have design and more than one condition
     * <p>-1.0 if lowest replicate amount &lt; 4 & !=1 or if there are problems</p>
     * <p>+0.0 if lowest replicate amount &lt; 10 & !=1</p>
     * <p>+1.0 otherwise</p>
     * extra (in replicatesIssues):
     * 1 if the experiment has no design
     * 2 if there were no factor values found
     * 3 if all replicate amounts were 1
     * 4 if lowest replicate was 0 (that really should not happen though)
     */
    private double qScoreReplicates;
    private byte replicatesIssues;

    /**
     * State of batch info
     * <p>-1.0 if no batch info available</p>
     * <p>+1.0 otherwise</p>
     */
    private double qScoreBatchInfo;

    /**
     * Batch effect without batch correction. Can ve overridden.
     * <p>-1.0 if batch pVal &lt; 0.0001 or (manualHasStrongBatchEffect & manualBatchEffectActive);</p>
     * <p>+1.0 if batch pVal &gt; 0.1 or (!manualHasNoBatchEffect & manualBatchEffectActive);</p>
     * <p>+0.0 otherwise</p>
     * extra:
     * batchCorrected = true, if data was batch-corrected
     */
    private double qScoreBatchEffect;
    private boolean batchCorrected;

    private boolean manualHasStrongBatchEffect;
    private boolean manualHasNoBatchEffect;
    private boolean manualBatchEffectActive;

    /**
     * Batch confound
     * <p>-1.0 if data confound detected or (manualHasBatchConfound & manualBatchConfoundActive)</p>
     * <p>+1.0 otherwise</p>
     */
    private double qScoreBatchConfound;

    private boolean manualHasBatchConfound;
    private boolean manualBatchConfoundActive;

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof Geeq ) )
            return false;
        Geeq geeq = ( Geeq ) o;
        return Objects.equals( getId(), geeq.getId() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( getId() );
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
     * Required by hibernate
     *
     * @param id the unique ID of the instance this object represents
     */
    private void setId( Long id ) {
        this.id = id;
    }

    @Transient
    public double[] getSuitabilityScoreArray() {
        return new double[] { this.sScorePublication, this.sScorePlatformAmount, this.sScorePlatformsTechMulti,
                this.sScoreAvgPlatformPopularity, this.sScoreAvgPlatformSize, this.sScoreSampleSize, this.sScoreRawData,
                this.sScoreMissingValues };
    }

    @Transient
    public double[] getQualityScoreArray() {
        return new double[] { this.qScoreOutliers, this.qScoreSampleMeanCorrelation, this.qScoreSampleMedianCorrelation,
                this.qScoreSampleCorrelationVariance, this.qScorePlatformsTech, this.qScoreReplicates,
                this.qScoreBatchInfo, this.qScoreBatchEffect, this.qScoreBatchConfound };
    }

    @Transient
    public double[] getSuitabilityScoreWeightsArray() {
        return new double[] { 1, 1, 1, 1, 1, 1, 1, 1 };
    }

    @Transient
    public double[] getQualityScoreWeightsArray() {
        return new double[] { 1, 0, 1, 0, 1, 1, 1, 1, 1 };
    }

    public double getDetectedQualityScore() {
        return detectedQualityScore;
    }

    public void setDetectedQualityScore( double detectedQualityScore ) {
        this.detectedQualityScore = detectedQualityScore;
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

    public double getDetectedSuitabilityScore() {
        return detectedSuitabilityScore;
    }

    public void setDetectedSuitabilityScore( double detectedSuitabilityScore ) {
        this.detectedSuitabilityScore = detectedSuitabilityScore;
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

    public double getSScorePublication() {
        return sScorePublication;
    }

    public void setSScorePublication( double sScorePublicationDate ) {
        this.sScorePublication = sScorePublicationDate;
    }

    public double getSScorePlatformAmount() {
        return sScorePlatformAmount;
    }

    public void setSScorePlatformAmount( double sScorePlatformAmount ) {
        this.sScorePlatformAmount = sScorePlatformAmount;
    }

    public double getSScorePlatformsTechMulti() {
        return sScorePlatformsTechMulti;
    }

    public void setSScorePlatformsTechMulti( double sScorePlatformsTechMulti ) {
        this.sScorePlatformsTechMulti = sScorePlatformsTechMulti;
    }

    public double getSScoreAvgPlatformPopularity() {
        return sScoreAvgPlatformPopularity;
    }

    public void setSScoreAvgPlatformPopularity( double sScoreAvgPlatformPopularity ) {
        this.sScoreAvgPlatformPopularity = sScoreAvgPlatformPopularity;
    }

    public double getSScoreAvgPlatformSize() {
        return sScoreAvgPlatformSize;
    }

    public void setSScoreAvgPlatformSize( double sScoreAvgPlatformSize ) {
        this.sScoreAvgPlatformSize = sScoreAvgPlatformSize;
    }

    public double getSScoreSampleSize() {
        return sScoreSampleSize;
    }

    public void setSScoreSampleSize( double sScoreSampleSize ) {
        this.sScoreSampleSize = sScoreSampleSize;
    }

    public double getSScoreRawData() {
        return sScoreRawData;
    }

    public void setSScoreRawData( double sScoreRawData ) {
        this.sScoreRawData = sScoreRawData;
    }

    public double getSScoreMissingValues() {
        return sScoreMissingValues;
    }

    public void setSScoreMissingValues( double sScoreMissingValues ) {
        this.sScoreMissingValues = sScoreMissingValues;
    }

    public double getQScoreOutliers() {
        return qScoreOutliers;
    }

    public void setQScoreOutliers( double qScoreOutliers ) {
        this.qScoreOutliers = qScoreOutliers;
    }

    public double getQScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    public void setQScorePlatformsTech( double qScorePlatformsTech ) {
        this.qScorePlatformsTech = qScorePlatformsTech;
    }

    public double getQScoreReplicates() {
        return qScoreReplicates;
    }

    public void setQScoreReplicates( double qScoreReplicates ) {
        this.qScoreReplicates = qScoreReplicates;
    }

    public double getQScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    public void setQScoreBatchInfo( double qScoreBatchInfo ) {
        this.qScoreBatchInfo = qScoreBatchInfo;
    }

    public double getQScoreBatchEffect() {
        return qScoreBatchEffect;
    }

    public void setQScoreBatchEffect( double qScoreBatchEffect ) {
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

    public void setManualBatchEffectActive( boolean manualBatchEffectOverride ) {
        this.manualBatchEffectActive = manualBatchEffectOverride;
    }

    public double getQScoreBatchConfound() {
        return qScoreBatchConfound;
    }

    public void setQScoreBatchConfound( double qScoreBatchConfound ) {
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

    public double getQScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    public void setQScoreSampleMeanCorrelation( double qScoreSampleMeanCorrelation ) {
        this.qScoreSampleMeanCorrelation = qScoreSampleMeanCorrelation;
    }

    public double getQScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    public void setQScoreSampleMedianCorrelation( double qScoreSampleMedianCorrelation ) {
        this.qScoreSampleMedianCorrelation = qScoreSampleMedianCorrelation;
    }

    public double getQScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    public void setQScoreSampleCorrelationVariance( double qScoreSampleCorrelationVariance ) {
        this.qScoreSampleCorrelationVariance = qScoreSampleCorrelationVariance;
    }

    public AuditEvent getLastRun() {
        return lastRun;
    }

    public void setLastRun( AuditEvent lastRun ) {
        this.lastRun = lastRun;
    }

    public AuditEvent getLastManualOverride() {
        return lastManualOverride;
    }

    public void setLastManualOverride( AuditEvent lastManualOverride ) {
        this.lastManualOverride = lastManualOverride;
    }

    public AuditEvent getLastBatchEffectChange() {
        return lastBatchEffectChange;
    }

    public void setLastBatchEffectChange( AuditEvent lastBatchEffectChange ) {
        this.lastBatchEffectChange = lastBatchEffectChange;
    }

    public AuditEvent getLastBatchConfoundChange() {
        return lastBatchConfoundChange;
    }

    public void setLastBatchConfoundChange( AuditEvent lastBatchConfoundChange ) {
        this.lastBatchConfoundChange = lastBatchConfoundChange;
    }

    public boolean isNoVectors() {
        return noVectors;
    }

    public void setNoVectors( boolean noVectors ) {
        this.noVectors = noVectors;
    }

    public byte getCorrMatIssues() {
        return corrMatIssues;
    }

    public void setCorrMatIssues( byte corrMatIssues ) {
        this.corrMatIssues = corrMatIssues;
    }

    public byte getReplicatesIssues() {
        return replicatesIssues;
    }

    public void setReplicatesIssues( byte replicatesIssues ) {
        this.replicatesIssues = replicatesIssues;
    }

    public boolean isBatchCorrected() {
        return batchCorrected;
    }

    public void setBatchCorrected( boolean batchCorrected ) {
        this.batchCorrected = batchCorrected;
    }
}
