/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
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
 * Emitted when a single experiment-level annotation (tag) is removed from an
 * {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} via the
 * REST annotation write endpoints. Paired with {@link TagAddedEvent}; see
 * its javadoc for the rationale (per-tag granularity over the per-call
 * granularity provided by {@link ManualAnnotationEvent}).
 */
public class TagRemovedEvent extends AnnotationEvent {

}
