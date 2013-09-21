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
 * Signifies that the probes were renamed from their original values. This is sometimes done if the original data input
 * had very generic identifiers (such as consecutive integers).
 * </p>
 */
public abstract class ArrayDesignProbeRenamingEvent extends
        ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEventImpl {

    /**
     * Constructs new instances of
     * {@link ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of
         * {@link ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent}.
         */
        public static ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEvent newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignProbeRenamingEventImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -5458770150436736181L;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ArrayDesignProbeRenamingEvent() {
    }

}