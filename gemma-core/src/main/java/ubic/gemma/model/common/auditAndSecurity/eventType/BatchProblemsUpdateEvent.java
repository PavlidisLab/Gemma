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

package ubic.gemma.model.common.auditAndSecurity.eventType;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Event that tracks when batch effects or problems are detected.
 * <p>
 * There are three relevant fields in {@link ExpressionExperiment} whose change triggers this event:
 *<ul>
 *     <li>{@link ExpressionExperiment#getBatchEffect()}</li>
 *     <li>{@link ExpressionExperiment#getBatchEffectStatistics()}</li>
 *     <li>{@link ExpressionExperiment#getBatchConfound()}</li>
 *</ul>
 * @author paul
 */
public class BatchProblemsUpdateEvent extends ExpressionExperimentAnalysisEvent {

}
