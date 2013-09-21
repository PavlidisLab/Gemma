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
 * To note that the data set has one or more outlier samples.
 * </p>
 */
public abstract class OutlierSampleTrouble extends
        ubic.gemma.model.common.auditAndSecurity.eventType.TroubleStatusFlagEventImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.common.auditAndSecurity.eventType.OutlierSampleTrouble}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.common.auditAndSecurity.eventType.OutlierSampleTrouble}.
         */
        public static ubic.gemma.model.common.auditAndSecurity.eventType.OutlierSampleTrouble newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.eventType.OutlierSampleTroubleImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7211120608210539559L;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public OutlierSampleTrouble() {
    }

}