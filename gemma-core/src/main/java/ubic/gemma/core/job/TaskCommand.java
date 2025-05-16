/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.job;

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.core.tasks.analysis.expression.ExpressionExperimentLoadTaskCommand;

import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * This command class is used to allow communication of parameters for a task between a client and task running service,
 * which might be on a different computer.
 * This class can be used directly, or extended to create a command object to pass parameters for a specific task. See
 * for example {@link ExpressionExperimentLoadTaskCommand}. A entityId field is provided as a convenience for the case
 * when a primary key is all that is really needed.
 *
 * @author keshav
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Getter
@Setter
public abstract class TaskCommand implements Serializable {
    // How long we will wait for a started task before giving up waiting for it. Tasks running longer than this will be
    // cancelled. This does not include time spent queued.
    public static final long MAX_RUNTIME_MILLIS = 60 * 1000;
    /**
     * How long we will queue a task before giving up and cancelling it (default value)
     */
    public static final long MAX_QUEUING_MILLIS = 60 * 2 * 1000;
    /**
     * Should an email be sent to the user when the job is done?
     */
    private boolean emailAlert = false;
    /**
     * If true, the jobDetails associated with this task will be persisted in the database. Consider setting to false
     * for test jobs or other super-frequent maintenance tasks.
     */
    private Boolean persistJobDetails = true;
    /**
     * Used to propagate security to grid workers.
     */
    private final SecurityContext securityContext;
    /**
     * Task submitted, if known.
     */
    @Nullable
    private final String submitter;
    /**
     * How long we will allow this task to be queued before giving up, or -1 for no limit.
     */
    private long maxQueueMillis = TaskCommand.MAX_QUEUING_MILLIS;
    /**
     * How long we will allow this task to run before giving up, or -1 for no limit.
     */
    private long maxRuntimeMillis = TaskCommand.MAX_RUNTIME_MILLIS;

    public TaskCommand() {
        // security details.
        SecurityContext context = SecurityContextHolder.getContext();
        assert context != null;
        this.securityContext = context;

        Authentication authentication = context.getAuthentication();
        // can happen in test situations.
        if ( authentication != null ) {
            this.submitter = authentication.getName();
        } else {
            this.submitter = null;
        }
    }

    /**
     * Obtain the class of the {@link Task} object that will be run for this command.
     * <p>
     * If null, the task command cannot be submitted through {@link TaskRunningService#submitTaskCommand(TaskCommand)}
     * and a task object must be explicitly created.
     * <p>
     * For now, this how we map from TaskCommand to Task that actually runs it. We have to have this mapping somewhere
     * until we make Tasks themselves serializable. Tasks are not readily serializable because they have dependencies to
     * Spring services. at which point TaskCommand can be deprecated(or remain as TaskContext).
     */
    @Nullable
    public Class<? extends Task<?>> getTaskClass() {
        return null;
    }
}
