package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.job.TaskCommand;

/**
 * Created with IntelliJ IDEA.
 * User: anton
 * Date: 03/01/13
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class AutoTaggerTaskCommand extends TaskCommand {

    public AutoTaggerTaskCommand(Long entityId) {
        super(entityId);
    }

    @Override
    public Class getTaskClass() {
        return AutoTaggerTask.class;
    }
}
