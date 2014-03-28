package ubic.gemma.tasks.analysis.coexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.tasks.AbstractTask;

/**
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class LinkAnalysisTaskImpl extends AbstractTask<TaskResult, LinkAnalysisTaskCommand> implements LinkAnalysisTask {

    @Autowired
    private LinkAnalysisService linkAnalysisService;

    @Override
    public TaskResult execute() {
        linkAnalysisService.process( taskCommand.getExpressionExperiment(), taskCommand.getFilterConfig(),
                taskCommand.getLinkAnalysisConfig() );

        TaskResult result = new TaskResult( taskCommand, null );
        return result;

    }
}
