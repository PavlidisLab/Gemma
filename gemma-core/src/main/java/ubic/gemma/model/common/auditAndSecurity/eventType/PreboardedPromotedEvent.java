/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Emitted on {@code POST /preboarded/{id}/promote}, when a
 * {@code PreboardedExperiment} is promoted to a loaded
 * {@code ExpressionExperiment}.
 *
 * <p>See {@code HANDOFF_PROPOSED_EXPERIMENT_WORKFLOW.md} §"Audit-event hooks".
 * The promotion implementation rebinds the AnnotationSet rows' FK from
 * the preboarded to the EE (new-row + FK rebind approach); this event is
 * appended to the EE's audit trail so the post-promotion history is
 * unified on the EE side.</p>
 */
@Entity
@DiscriminatorValue("PreboardedPromotedEvent")
public class PreboardedPromotedEvent extends AuditEventType {
}
