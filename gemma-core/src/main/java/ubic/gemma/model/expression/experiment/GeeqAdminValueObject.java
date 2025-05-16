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

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Represents administrative geeq information. On top of the classic VO, this one also exposes
 * the underlying variables behind the public scores for suitability, quality, batch effect and batch confound.
 *
 * @author paul, tesarst
 */
@SuppressWarnings("unused") // Used in frontend
@Getter
@Setter
@ToString
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
        super();
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
}
