/*
 * The gemma-core project
 * 
 * Copyright (c) 2021 University of British Columbia
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

package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * Used to indicate that the suitability status of an experiment is the default. This should be used only after an
 * {@link UnsuitableForDifferentialExpressionAnalysisEvent}
 * event has been added, as the default is to assume an experiment is suitable.
 * 
 * @author paul
 */
public class ResetSuitabilityForDifferentialExpressionAnalysisEvent extends DifferentialExpressionSuitabilityEvent {

    /**
     * 
     */
    private static final long serialVersionUID = 5909581992797452478L;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     */
    public ResetSuitabilityForDifferentialExpressionAnalysisEvent() {
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
    public static final class Factory {

        public static ubic.gemma.model.common.auditAndSecurity.eventType.ResetSuitabilityForDifferentialExpressionAnalysisEvent newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.eventType.ResetSuitabilityForDifferentialExpressionAnalysisEvent();
        }

    }

}
