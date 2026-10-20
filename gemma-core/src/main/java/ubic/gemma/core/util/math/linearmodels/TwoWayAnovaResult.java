/*
 * The baseCode project
 *
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.core.util.math.linearmodels;

public interface TwoWayAnovaResult extends AnovaResult {

    String getFactorAName();

    double getMainEffectADof();

    double getMainEffectAFStat();

    double getMainEffectAPValue();

    String getFactorBName();

    double getMainEffectBDof();

    double getMainEffectBFStat();

    double getMainEffectBPValue();

    /**
     * Indicate if this two-way ANOVA result has an interaction effect.
     */
    boolean hasInteraction();

    double getInteractionDof();

    double getInteractionFStat();

    double getInteractionPValue();
}
