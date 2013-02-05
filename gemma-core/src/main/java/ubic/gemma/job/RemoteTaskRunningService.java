package ubic.gemma.job;

import com.google.common.util.concurrent.ListenableFuture;

public interface RemoteTaskRunningService {

    public void submit ( TaskCommand taskCommand );

    public void cancelQueuedTask( String taskId );
    public ListenableFuture<TaskResult> getRunningTaskFuture( String taskId );

    void shutdown();
}

