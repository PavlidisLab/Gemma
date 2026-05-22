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
 * Emitted when a single experiment-level annotation (tag) is added to an
 * {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} via the
 * REST annotation write endpoints.
 * <p>
 * Distinct from {@link ManualAnnotationEvent}, which is emitted by the bulk
 * {@code updateAnnotations} flow on a per-call basis (one event per call, not
 * per tag). {@code TagAddedEvent} fires per added tag so the per-row audit
 * trail required by {@code HANDOFF_DATASETS_ANNOTATIONS_WRITE.md} answers
 * "what was the state of this EE's tags at time T?" correctly.
 */
public class TagAddedEvent extends AnnotationEvent {

}
