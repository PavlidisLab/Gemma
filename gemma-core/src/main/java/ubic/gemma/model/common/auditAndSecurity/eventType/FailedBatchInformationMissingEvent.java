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
 * Use to indicate that the batch information has been successfully looked for, but is not available, so we shouldn't
 * look again. Do not use to indicate other types of failure such as an unsupported raw data type.
 * <p>
 * @deprecated use {@link BatchInformationMissingEvent}
 */
@Deprecated
public class FailedBatchInformationMissingEvent extends BatchInformationMissingEvent {

}