package ubic.gemma.core.tasks.analysis.coexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.job.AbstractTask;

/**
 * @author keshav
 */
@Component
@Scope("prototype")
public class LinkAnalysisTaskImpl extends AbstractTask<LinkAnalysisTaskCommand>
        implements LinkAnalysisTask {

    @Autowired
    private LinkAnalysisService linkAnalysisService;

    @Override
    public TaskResult call() {
        linkAnalysisService.process( taskCommand.getExpressionExperiment(), taskCommand.getFilterConfig(),
                taskCommand.getLinkAnalysisConfig() );

        return new TaskResult( taskCommand, null );

    }
}
