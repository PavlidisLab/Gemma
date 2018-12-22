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
 * deprecated. This event type isn't really workable, since it is related to creating files outside of the database (so
 * those files might not exist, get moved, etc.)
 * 
 * @deprecated
 * @author     paul
 */
@Deprecated
public class ArrayDesignAnnotationFileEvent
        extends ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 8966878123241112793L;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public ArrayDesignAnnotationFileEvent() {
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent newInstance() {
            return new ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnnotationFileEvent();
        }

    }

}