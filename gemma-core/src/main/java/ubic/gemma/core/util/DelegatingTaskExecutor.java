package ubic.gemma.core.util;

import org.springframework.core.task.TaskExecutor;

public interface DelegatingTaskExecutor extends TaskExecutor {

    TaskExecutor getDelegate();
}
