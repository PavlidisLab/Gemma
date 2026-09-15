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

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import ubic.gemma.model.common.Identifiable;

import java.util.Date;

@Entity
@Table(name = "JOB_INFO")
@Access(AccessType.FIELD)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
public class JobInfo implements Identifiable, SecuredNotChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", columnDefinition = "BIGINT")
    private Long id;

    @Column(name = "RUNNING_STATUS", nullable = false, columnDefinition = "TINYINT")
    private Boolean runningStatus = Boolean.TRUE;

    @Lob
    @Column(name = "FAILED_MESSAGE", columnDefinition = "text")
    private String failedMessage;

    @Column(name = "START_TIME", nullable = false, columnDefinition = "DATETIME(3)")
    private Date startTime;

    @Column(name = "END_TIME", columnDefinition = "DATETIME(3)")
    private Date endTime;

    @Column(name = "PHASES", nullable = false, columnDefinition = "INTEGER")
    private Integer phases = 1;

    @Column(name = "DESCRIPTION", columnDefinition = "VARCHAR(255)")
    private String description;

    @Lob
    @Column(name = "MESSAGES", columnDefinition = "longtext")
    private String messages;

    @Column(name = "TASK_ID", columnDefinition = "VARCHAR(255)")
    private String taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_FK", columnDefinition = "BIGINT")
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
