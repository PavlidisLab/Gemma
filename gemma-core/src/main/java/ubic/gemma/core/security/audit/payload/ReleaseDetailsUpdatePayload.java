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
package ubic.gemma.core.security.audit.payload;

import org.springframework.lang.Nullable;
import ubic.gemma.core.security.audit.AuditEventPayload;

import java.util.Date;

/**
 * Structured payload for {@code ReleaseDetailsUpdateEvent} writes against
 * {@code ExternalDatabase}. Carries the per-release context that the legacy
 * imperative call passed as the 5-arg {@code addUpdateEvent} (note, detail,
 * performedDate):
 * <ul>
 *   <li>{@code releaseVersion} — the version string just set on the
 *       {@code ExternalDatabase} ({@code null} for a last-updated-only write).</li>
 *   <li>{@code releaseUrl} — the release-asset URL just set
 *       ({@code null} when not supplied).</li>
 *   <li>{@code lastUpdated} — the moment the source release was actually
 *       published / refreshed. Distinct from the audit row's own
 *       {@code performedDate} (which records when the audit-event row was
 *       written; with the {@code @Audited}-aspect migration these are
 *       {@code now()} and may drift slightly from {@code lastUpdated}).</li>
 *   <li>{@code detail} — short free-form sentence describing the transition
 *       (e.g. "Initial release version set to v42." /
 *       "Release version has been updated from v41 to v42." /
 *       "Release last updated moment has been updated."). Mirrors the
 *       legacy {@code AUDIT_EVENT.DETAIL} string. {@code null} when the
 *       update was a same-version refresh that produced no transition note
 *       (legacy code emitted a {@code null} detail in that case).</li>
 * </ul>
 *
 * <p>Phase C bucket 2g (audit-residual inventory #9 + #10).
 * The pre-migration 5-arg {@code addUpdateEvent} carried an explicit
 * {@code performedDate=lastUpdated}; under the aspect path the row's
 * {@code performedDate} is set by {@code AuditTrailServiceImpl} to
 * {@code now()}. {@code lastUpdated} is still recoverable from this payload
 * JSON and from {@code ExternalDatabase.lastUpdated} on the entity.
 */
public record ReleaseDetailsUpdatePayload(
        @Nullable String releaseVersion,
        @Nullable String releaseUrl,
        Date lastUpdated,
        @Nullable String detail
) implements AuditEventPayload {
}
