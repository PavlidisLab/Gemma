package ubic.gemma.tasks.analysis.coexp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysis;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.job.TaskResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class LinkAnalysisTaskImpl implements LinkAnalysisTask {

    @Autowired private LinkAnalysisService linkAnalysisService;

    private LinkAnalysisTaskCommand command;

    @Override
    public void setCommand(LinkAnalysisTaskCommand command) {
        this.command = command;
    }

    @Override
    public TaskResult execute() {

        ExpressionExperiment ee = linkAnalysisService.loadDataForAnalysis( command.getExpressionExperiment().getId() );
        LinkAnalysis la = linkAnalysisService.doAnalysis( ee, command.getLinkAnalysisConfig(), command.getFilterConfig() );
        linkAnalysisService.saveResults( ee, la, command.getLinkAnalysisConfig(), command.getFilterConfig() );

        TaskResult result = new TaskResult( command, null );
        return result;

    }

}
