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


import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

/**
 * Emitted when an {@code AnnotationSet} row is appended to a
 * {@code PreboardedExperiment} (or, for the private curation API, to a
 * loaded {@code ExpressionExperiment}).
 *
 * <p>The event does NOT inline the JSON payload; it references the
 * {@code AnnotationSet} row by id (carried in the audit row's
 * {@code NOTE} until the structured {@code AUDIT_EVENT.PAYLOAD} column
 * lands).</p>
 */
@Entity
@DiscriminatorValue("AnnotationSetEvent")
public class AnnotationSetEvent extends AuditEventType {
}
