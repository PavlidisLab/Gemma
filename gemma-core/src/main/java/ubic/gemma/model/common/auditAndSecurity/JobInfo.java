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

package ubic.gemma.model.common.auditAndSecurity;

import lombok.Getter;
import lombok.Setter;
import ubic.gemma.model.common.Identifiable;

import java.util.Date;

@Getter
@Setter
public class JobInfo implements Identifiable, SecuredNotChild {

    private Boolean runningStatus = Boolean.TRUE;
    private String failedMessage;
    private Date startTime;
    private Date endTime;
    private Integer phases = 1;
    private String description;
    private String messages;
    private String taskId;
    private Long id;
    private User user;

    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof JobInfo ) ) {
            return false;
        }
        final JobInfo that = ( JobInfo ) object;
        return this.id != null && that.getId() != null && this.id.equals( that.getId() );
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public static final class Factory {

        public static JobInfo newInstance() {
            return new JobInfo();
        }

        public static JobInfo newInstance( Boolean runningStatus, Date startTime, Integer phases ) {
            final JobInfo entity = new JobInfo();
            entity.setRunningStatus( runningStatus );
            entity.setStartTime( startTime );
            entity.setPhases( phases );
            return entity;
        }

        public static JobInfo newInstance( Boolean runningStatus, String failedMessage, Date startTime, Date endTime,
                Integer phases, String description, String messages, String taskId, User user ) {
            final JobInfo entity = new JobInfo();
            entity.setRunningStatus( runningStatus );
            entity.setFailedMessage( failedMessage );
            entity.setStartTime( startTime );
            entity.setEndTime( endTime );
            entity.setPhases( phases );
            entity.setDescription( description );
            entity.setMessages( messages );
            entity.setTaskId( taskId );
            entity.setUser( user );
            return entity;
        }
    }

}