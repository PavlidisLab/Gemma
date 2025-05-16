package ubic.gemma.core.util.concurrent;

import org.springframework.core.task.TaskExecutor;

/**
 * @author poirigui
 */
public interface DelegatingTaskExecutor extends TaskExecutor {

    TaskExecutor getDelegate();
}
