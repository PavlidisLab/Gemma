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
 * <p>
 * Event indicating that the status of this entity is OK. This can be used after a troublestatusevent to indicate that
 * 'all is well'.
 * </p>
 *
 * @author Paul
 */
public class NotTroubledStatusFlagEvent
        extends ubic.gemma.model.common.auditAndSecurity.eventType.TroubledStatusFlagEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -8586752080144045085L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public NotTroubledStatusFlagEvent() {
    }

    /**
     * Constructs new instances of {@link NotTroubledStatusFlagEvent}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link NotTroubledStatusFlagEvent}.
         */
        public static NotTroubledStatusFlagEvent newInstance() {
            return new NotTroubledStatusFlagEvent();
        }

    }

}