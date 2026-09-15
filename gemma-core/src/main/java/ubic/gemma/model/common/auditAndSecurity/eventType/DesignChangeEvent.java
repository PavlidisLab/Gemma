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


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Emitted by the {@code PUT /datasets/{id}/design} apply path when a proposed
 * {@link ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject}
 * is successfully written back as the experiment's new design.
 * <p>
 * Carries the same audit-trail semantics as its parent
 * {@link ExperimentalDesignUpdatedEvent} (so existing trail queries on the
 * parent class continue to pick it up) but disambiguates curator-initiated
 * whole-design replacements from the older imperative
 * {@code addUpdateEvent(ee, ExperimentalDesignUpdatedEvent.class, ...)} callers
 * that mutate factor metadata in narrower ways.
 * <p>
 * Idempotent no-ops do NOT emit this event: the apply method short-circuits
 * when the preflight summary shows zero changes, so repeated PUTs of an
 * already-applied design produce one event, not many. See
 * {@code AUDIT_PHASE_C_RECCE.md} for the declarative-audit pattern this event
 * participates in.
 */
@Entity
@DiscriminatorValue("DesignChangeEvent")
public class DesignChangeEvent extends ExperimentalDesignUpdatedEvent {
}
