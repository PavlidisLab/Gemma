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

import ubic.gemma.model.common.AbstractIdentifiable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Represents quality information about a data set. The class name comes from the research project name, GEEQ.
 * See the variables getters javadoc for further description. The scoring rules are implemented in the
 * GeeqServiceImpl, which also exposes public methods for experiment scoring.
 * <p>
 * The score used to have a second component, Suitability, scored from platform and publication properties. Those
 * features are microarray-era and degenerate for RNA-seq — processed RNA-seq data lands on a GENELIST platform, so
 * platform amount, technology consistency, popularity and size were pinned — and the code was removed. The columns
 * remain in the database because Gemma 1.0 still writes them.
 *
 * @author paul, tesarst
 */
@Entity
@Table(name = "GEEQ", indexes = {
        @Index(name = "GEEQ_DETECTED_QUALITY_SCORE", columnList = "DETECTED_QUALITY_SCORE"),
        @Index(name = "GEEQ_MANUAL_QUALITY_SCORE", columnList = "MANUAL_QUALITY_SCORE"),
        @Index(name = "GEEQ_MANUAL_QUALITY_OVERRIDE", columnList = "MANUAL_QUALITY_OVERRIDE")
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Geeq extends AbstractIdentifiable {

    @Column(name = "DETECTED_QUALITY_SCORE", nullable = false, columnDefinition = "DOUBLE")
    private double detectedQualityScore;
    @Column(name = "MANUAL_QUALITY_SCORE", nullable = false, columnDefinition = "DOUBLE")
    private double manualQualityScore;
    @Column(name = "MANUAL_QUALITY_OVERRIDE", nullable = false, columnDefinition = "BIT")
    private boolean manualQualityOverride;

    /*
     * Quality score factors
     */

    @Column(name = "NO_VECTORS", nullable = false, columnDefinition = "BIT")
    private boolean noVectors;

    @Column(name = "SCORE_OUTLIERS", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreOutliers;
    @Column(name = "CORRMAT_ISSUES", nullable = false, columnDefinition = "TINYINT")
    private byte corrMatIssues;

    @Column(name = "SCORE_SAMPLE_MEAN_CORRELATION", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreSampleMeanCorrelation;
    @Column(name = "SCORE_SAMPLE_MEDIAN_CORRELATION", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreSampleMedianCorrelation;
    @Column(name = "SCORE_SAMPLE_CORRELATION_VARIANCE", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreSampleCorrelationVariance;
    @Column(name = "SCORE_PLATFORMS_TECH", nullable = false, columnDefinition = "DOUBLE")
    private double qScorePlatformsTech;

    @Column(name = "SCORE_REPLICATES", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreReplicates;
    @Column(name = "REPLICATES_ISSUES", nullable = false, columnDefinition = "TINYINT")
    private byte replicatesIssues;

    @Column(name = "SCORE_BATCH_INFO", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreBatchInfo;
    @Column(name = "BATCH_CORRECTED", nullable = false, columnDefinition = "BIT")
    private boolean batchCorrected;

    @Column(name = "SCORE_BATCH_EFFECT", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreBatchEffect;
    @Column(name = "MANUAL_HAS_STRONG_BATCH_EFFECT", nullable = false, columnDefinition = "BIT")
    private boolean manualHasStrongBatchEffect;
    @Column(name = "MANUAL_HAS_NO_BATCH_EFFECT", nullable = false, columnDefinition = "BIT")
    private boolean manualHasNoBatchEffect;
    @Column(name = "MANUAL_BATCH_EFFECT_ACTIVE", nullable = false, columnDefinition = "BIT")
    private boolean manualBatchEffectActive;

    @Column(name = "SCORE_BATCH_CONFOUND", nullable = false, columnDefinition = "DOUBLE")
    private double qScoreBatchConfound;
    @Column(name = "MANUAL_HAS_BATCH_CONFOUND", nullable = false, columnDefinition = "BIT")
    private boolean manualHasBatchConfound;
    @Column(name = "MANUAL_BATCH_CONFOUND_ACTIVE", nullable = false, columnDefinition = "BIT")
    private boolean manualBatchConfoundActive;

    @Column(name = "OTHER_ISSUES", columnDefinition = "VARCHAR(500)")
    private String otherIssues;

    @Transient
    public double[] getQualityScoreArray() {
        return new double[] { this.qScoreOutliers, this.qScoreSampleMeanCorrelation, this.qScoreSampleMedianCorrelation,
                this.qScoreSampleCorrelationVariance, this.qScorePlatformsTech, this.qScoreReplicates,
                this.qScoreBatchInfo, this.qScoreBatchEffect, this.qScoreBatchConfound };
    }

    @Transient
    public double[] getQualityScoreWeightsArray() {
        return new double[] { 1, 0, 1, 0, 1, 1, 1, 1, 1 };
    }

    /**
     * @return Quality refers to data quality, wherein the same study could have been done twice with the same technical
     * parameters and in one case yield bad quality data, and in another high quality data.
     * The quality score can be overridden. The manual value is stored in manualQualityScore, while
     * manualQualityOverride boolean value denotes whether the manual value should be used.
     */
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

    public boolean isManualQualityOverride() {
        return manualQualityOverride;
    }

    public void setManualQualityOverride( boolean manualQualityOverride ) {
        this.manualQualityOverride = manualQualityOverride;
    }

    /**
     * @return Presence of non-removed outliers:
     * -1.0 if there are any outliers
     * +1.0 if there are no outliers
     * extra (in corrMatIssues):
     * 1 if the correlation matrix is empty
     * 2 if the correlation matrix has NaN values
     */
    public double getqScoreOutliers() {
        return qScoreOutliers;
    }

    public void setqScoreOutliers( double qScoreOutliers ) {
        this.qScoreOutliers = qScoreOutliers;
    }

    /**
     * @return Platform technologies
     * -1.0 if any platform is two-color
     * +1.0 otherwise
     */
    public double getqScorePlatformsTech() {
        return qScorePlatformsTech;
    }

    public void setqScorePlatformsTech( double qScorePlatformsTech ) {
        this.qScorePlatformsTech = qScorePlatformsTech;
    }

    /**
     * @return Number of replicates - ee has to have design and more than one condition
     * -1.0 if lowest replicate amount &lt; GEEQ_WORST_REPLICATION_THRESHOLD &amp; !=1 or if there are problems
     * +0.0 if lowest replicate amount &lt; GEEQ_MEDIUM_REPLICATION_THRESHOLD &amp; !=1
     * +1.0 otherwise
     * extra (in replicatesIssues):
     * 1 if the experiment has no design
     * 2 if there were no factor values found
     * 3 if all replicate amounts were 1
     * 4 if lowest replicate was 0 (that really should not happen though)
     * See GeeqServiceImpl for thresholds
     */
    public double getqScoreReplicates() {
        return qScoreReplicates;
    }

    public void setqScoreReplicates( double qScoreReplicates ) {
        this.qScoreReplicates = qScoreReplicates;
    }

    /**
     * @return State of batch info
     * -1.0 if no batch info available
     * +1.0 otherwise
     */
    public double getqScoreBatchInfo() {
        return qScoreBatchInfo;
    }

    public void setqScoreBatchInfo( double qScoreBatchInfo ) {
        this.qScoreBatchInfo = qScoreBatchInfo;
    }

    /**
     * @return Batch effect without batch correction. Can be overridden.
     * -1.0 if batch pVal &lt; 0.0001 or (manualHasStrongBatchEffect &amp; manualBatchEffectActive)
     * +1.0 if batch pVal &gt; 0.1 or (!manualHasNoBatchEffect &amp; manualBatchEffectActive)
     * +0.0 otherwise
     * extra:
     * batchCorrected = true, if data was batch-corrected
     */
    public double getqScoreBatchEffect() {
        return qScoreBatchEffect;
    }

    public void setqScoreBatchEffect( double qScoreBatchEffect ) {
        this.qScoreBatchEffect = qScoreBatchEffect;
    }

    public boolean isManualHasStrongBatchEffect() {
        return manualHasStrongBatchEffect;
    }

    public void setManualHasStrongBatchEffect( boolean manualHasStrongBatchEffect ) {
        this.manualHasStrongBatchEffect = manualHasStrongBatchEffect;
    }

    public boolean isManualHasNoBatchEffect() {
        return manualHasNoBatchEffect;
    }

    public void setManualHasNoBatchEffect( boolean manualHasNoBatchEffect ) {
        this.manualHasNoBatchEffect = manualHasNoBatchEffect;
    }

    public boolean isManualBatchEffectActive() {
        return manualBatchEffectActive;
    }

    public void setManualBatchEffectActive( boolean manualBatchEffectOverride ) {
        this.manualBatchEffectActive = manualBatchEffectOverride;
    }

    /**
     * @return Batch confound
     * -1.0 if data confound detected or (manualHasBatchConfound &amp; manualBatchConfoundActive)
     * +1.0 otherwise
     */
    public double getqScoreBatchConfound() {
        return qScoreBatchConfound;
    }

    public void setqScoreBatchConfound( double qScoreBatchConfound ) {
        this.qScoreBatchConfound = qScoreBatchConfound;
    }

    public boolean isManualHasBatchConfound() {
        return manualHasBatchConfound;
    }

    public void setManualHasBatchConfound( boolean manualHasBatchConfound ) {
        this.manualHasBatchConfound = manualHasBatchConfound;
    }

    public boolean isManualBatchConfoundActive() {
        return manualBatchConfoundActive;
    }

    public void setManualBatchConfoundActive( boolean manualBatchConfoundActive ) {
        this.manualBatchConfoundActive = manualBatchConfoundActive;
    }

    /**
     * @return Using the mean sample correlation r:
     * +r use the computed value
     * +0.0 if correlation matrix is empty
     */
    public double getqScoreSampleMeanCorrelation() {
        return qScoreSampleMeanCorrelation;
    }

    public void setqScoreSampleMeanCorrelation( double qScoreSampleMeanCorrelation ) {
        this.qScoreSampleMeanCorrelation = qScoreSampleMeanCorrelation;
    }

    /**
     * @return Using the median sample correlation m:
     * +m use the computed value
     * +0.0 if correlation matrix is empty
     */
    public double getqScoreSampleMedianCorrelation() {
        return qScoreSampleMedianCorrelation;
    }

    public void setqScoreSampleMedianCorrelation( double qScoreSampleMedianCorrelation ) {
        this.qScoreSampleMedianCorrelation = qScoreSampleMedianCorrelation;
    }

    /**
     * @return Using the sample correlation variance v:
     * +v use the computed value
     * +0.0 if correlation matrix is empty
     */
    public double getqScoreSampleCorrelationVariance() {
        return qScoreSampleCorrelationVariance;
    }

    public void setqScoreSampleCorrelationVariance( double qScoreSampleCorrelationVariance ) {
        this.qScoreSampleCorrelationVariance = qScoreSampleCorrelationVariance;
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

    public String getOtherIssues() {
        return otherIssues;
    }

    public void setOtherIssues( String otherIssues ) {
        this.otherIssues = otherIssues;
    }

    public void addOtherIssues( String issue ) {
        this.otherIssues += issue + "\n";
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o )
            return true;
        if ( !( o instanceof Geeq ) )
            return false;
        Geeq geeq = ( Geeq ) o;
        if ( getId() != null && geeq.getId() != null ) {
            return getId().equals( geeq.getId() );
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
