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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package ubic.gemma.model.common.auditAndSecurity.eventType;


import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;

/**
 * Emitted on {@code POST /datasets/{id}/publish?reviewer=X}, when a curator
 * publishes a dataset under a named reviewer.
 *
 * <p>Distinct from {@link MakePublicEvent}: that event captures the raw ACL
 * flip (the {@code IS_AUTHENTICATED_ANONYMOUSLY} read grant) without
 * curator attribution. {@code DatasetPublishedEvent} is the curator-workflow
 * publish step, with the reviewer encoded in the audit-event note.
 * Emitted by the publish endpoint regardless of whether the ACL flip
 * actually changed state (re-publishing an already-public dataset still
 * emits an audit row).</p>
 *
 * <p>See {@code GEMMA_UI_ENDPOINT_GAP.md} §3g for the UI-side motivation.</p>
 */
@Entity
@DiscriminatorValue("DatasetPublishedEvent")
public class DatasetPublishedEvent extends AuditEventType {
}
