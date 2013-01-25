package ubic.gemma.job;

import java.util.concurrent.Future;

public interface RemoteTaskRunningService {
    public void submit ( TaskCommand taskCommand );

    public void cancelQueuedTask( String taskId );
    public Future getRunningTaskFuture( String taskId );

    void shutdown();
}

