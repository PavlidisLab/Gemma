package ubic.gemma.core.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.core.tasks.AbstractTask;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A probe mapper spaces task .
 *
 * @author keshav
 */
@Component
@Scope("prototype")
public class ArrayDesignProbeMapperTaskImpl extends AbstractTask<TaskResult, ArrayDesignProbeMapTaskCommand>
        implements ArrayDesignProbeMapperTask {

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService = null;

    @Override
    public TaskResult call() {
        ArrayDesign ad = taskCommand.getArrayDesign();

        arrayDesignProbeMapperService.processArrayDesign( ad );

        return new TaskResult( taskCommand, null );
    }
}
