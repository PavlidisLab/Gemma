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
 * Read-compatibility marker for audit rows written by Gemma 2.0.
 * <p>
 * Gemma 2.0 emits this {@link AuditEventType} subclass; the 1.x line shares the audit trail, so it
 * must be able to load a row with this discriminator without a Hibernate {@code WrongClassException}.
 * The 1.x code never writes it and it carries no behaviour of its own — it exists only so the read
 * resolves. (In 2.0 it extends a richer hierarchy; here it is flattened onto an existing parent.)
 */
public class DatasetPublishedEvent extends AuditEventType {
}
