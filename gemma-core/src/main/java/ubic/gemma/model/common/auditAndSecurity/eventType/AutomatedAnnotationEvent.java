/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
 * @deprecated this event type was used by the auto-tagger, which has been removed. This event type has been kept
 * so that the audit-trails can still properly display these events, but creation new events of this type should not
 * occur.
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Still present in database
@Deprecated
public class AutomatedAnnotationEvent extends ubic.gemma.model.common.auditAndSecurity.eventType.AnnotationEvent {

}