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
@SuppressWarnings("unused") // Used in frontend
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
        this.manualQualityOverride = g.isManualQualityOverride();
        this.detectedSuitabilityScore = g.getDetectedSuitabilityScore();
        this.manualSuitabilityScore = g.getManualSuitabilityScore();
        this.manualSuitabilityOverride = g.isManualSuitabilityOverride();

        this.qScoreBatchEffect = g.getqScoreBatchEffect();
        this.manualHasStrongBatchEffect = g.isManualHasStrongBatchEffect();
        this.manualHasNoBatchEffect = g.isManualHasNoBatchEffect();
        this.manualBatchEffectActive = g.isManualBatchEffectActive();
        this.qScoreBatchConfound = g.getqScoreBatchConfound();
        this.manualHasBatchConfound = g.isManualHasBatchConfound();
        this.manualBatchConfoundActive = g.isManualBatchConfoundActive();
        this.otherIssues = g.getOtherIssues();
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

    public boolean isManualQualityOverride() {
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

    public boolean isManualSuitabilityOverride() {
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

    public void setManualBatchEffectActive( boolean manualBatchEffectActive ) {
        this.manualBatchEffectActive = manualBatchEffectActive;
    }

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

    public String getOtherIssues() {
        return otherIssues;
    }

    public void setOtherIssues( String otherIssues ) {
        this.otherIssues = otherIssues;
    }
}
