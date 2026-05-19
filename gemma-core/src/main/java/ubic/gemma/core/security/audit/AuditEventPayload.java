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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.security.audit;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Marker for typed, JSON-serialised payloads carried by an {@link
 * ubic.gemma.model.common.auditAndSecurity.AuditEvent}. Implementations are
 * records (one per concrete event type that needs structured per-event data).
 * Each defines its own structural schema directly through its record
 * components; Jackson handles serialisation automatically.
 *
 * <p>The type discriminator is the record's {@code @JsonTypeName} (or its
 * simple class name when {@code @JsonTypeName} is absent), stored under the
 * {@code @type} property in the serialised JSON. This lets the
 * {@code AUDIT_EVENT.PAYLOAD} string be deserialised back to the correct
 * record subtype without compile-time knowledge of which subtype it is, e.g.:
 *
 * <pre>{@code
 *   AuditEventPayload p = objectMapper.readValue(ev.getPayload(), AuditEventPayload.class);
 *   if (p instanceof BatchInformationFetchingPayload bif) { ... }
 * }</pre>
 *
 * <p>Phase A intentionally leaves this interface unsealed: no concrete
 * payload records exist yet outside tests, and the sealed {@code permits}
 * list would need to enumerate them. Phase B will tighten this to
 * {@code sealed interface AuditEventPayload permits …, …, …} once the
 * record taxonomy is set, at which point Jackson polymorphic deserialisation
 * also benefits from the closed world.
 *
 * <p>See {@code AUDIT_SYSTEM_AUDIT.md} Phase A.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@type"
)
public interface AuditEventPayload {

    /** Sentinel payload for events that carry no extra structured data. */
    record None() implements AuditEventPayload {}
}
