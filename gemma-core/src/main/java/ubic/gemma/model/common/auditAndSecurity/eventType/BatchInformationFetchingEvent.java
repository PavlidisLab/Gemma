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
 * Indicates that batch information was successfully obtained.
 * <p>
 * Successful obtention is not indicative of problematic batch information and the {@link FailedBatchInformationFetchingEvent}
 * for representing such cases.
 * </p>
 * Use {@link BatchInformationMissingEvent} for missing batch information.
 * @author poirigui
 */
public class BatchInformationFetchingEvent extends BatchInformationEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 4635587632131568136L;
}