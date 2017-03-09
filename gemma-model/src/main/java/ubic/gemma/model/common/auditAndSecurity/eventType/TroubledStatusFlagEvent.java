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
 * Audit event added when some type of "problem" or "trouble" notice is added to the entity.
 *
 * @author Paul
 */
public class TroubledStatusFlagEvent extends CurationDetailsEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7335601529423635731L;

    /**
     * No-arg constructor added to satisfy javabean contract
     */
    public TroubledStatusFlagEvent() {
    }

    /**
     * Constructs new instances of {@link TroubledStatusFlagEvent}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of
         * {@link TroubledStatusFlagEvent}.
         */
        public static TroubledStatusFlagEvent newInstance() {
            return new TroubledStatusFlagEvent();
        }

    }

}