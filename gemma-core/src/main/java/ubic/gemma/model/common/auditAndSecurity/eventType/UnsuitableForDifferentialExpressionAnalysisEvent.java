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
 * Indicates that the associated Experiment is NOT suitable for differential expression analysis.
 * <p>
 * Curators can add this event to document this determination, to avoid unnecessary repeat visits for curation and
 * re-determination. This event can be effectively reverted by adding a {@link ResetSuitabilityForDifferentialExpressionAnalysisEvent}.
 *
 * @author paul
 */
public class UnsuitableForDifferentialExpressionAnalysisEvent extends DifferentialExpressionSuitabilityEvent {

}
