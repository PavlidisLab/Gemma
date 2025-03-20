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
 * Used to indicate the suitability status of an ExpressionExperiment for differential expression analysis. In practice,
 * the default is to assume the experiment is suitable, so implementations of this event are used to indicate
 * unsuitability, or to revert that unsuitability to the default.
 *
 * @author paul
 */
public abstract class DifferentialExpressionSuitabilityEvent extends AnalysisSuitabilityEvent {


}
