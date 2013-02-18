package ubic.gemma.job.executor.common;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.tasks.Task;

public interface TaskCommandToTaskMatcher {
    public Task match( TaskCommand taskCommand );
}