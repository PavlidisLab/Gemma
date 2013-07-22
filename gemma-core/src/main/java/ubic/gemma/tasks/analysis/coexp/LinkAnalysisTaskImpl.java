package ubic.gemma.tasks.analysis.coexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysis;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.tasks.AbstractTask;

/**
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class LinkAnalysisTaskImpl extends AbstractTask<TaskResult, LinkAnalysisTaskCommand>
        implements LinkAnalysisTask {

    @Autowired
    private LinkAnalysisService linkAnalysisService;

    @Override
    public TaskResult execute() {
        ExpressionExperiment ee = linkAnalysisService.loadDataForAnalysis( taskCommand.getExpressionExperiment().getId() );
        LinkAnalysis la = linkAnalysisService.doAnalysis( ee, taskCommand.getLinkAnalysisConfig(), taskCommand.getFilterConfig() );
        linkAnalysisService.saveResults( ee, la, taskCommand.getLinkAnalysisConfig(), taskCommand.getFilterConfig() );

        TaskResult result = new TaskResult( taskCommand, null );
        return result;

    }
}
