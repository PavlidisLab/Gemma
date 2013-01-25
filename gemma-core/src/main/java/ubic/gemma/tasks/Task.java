package ubic.gemma.tasks;

import ubic.gemma.job.TaskCommand;
import ubic.gemma.job.TaskResult;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 03/01/13
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public interface Task<T extends TaskResult, C extends TaskCommand>  {

    void setCommand (C taskCommand);
    T execute();

}
