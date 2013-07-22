package ubic.gemma.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.tasks.AbstractTask;

/**
 * A probe mapper spaces task .
 *
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class ArrayDesignProbeMapperTaskImpl extends AbstractTask<TaskResult, ArrayDesignProbeMapTaskCommand>
        implements ArrayDesignProbeMapperTask {

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService = null;

    @Override
    public TaskResult execute() {
        ArrayDesign ad = taskCommand.getArrayDesign();

        arrayDesignProbeMapperService.processArrayDesign( ad );

        /*
         * FIXME get rid of web dependency
         */
        TaskResult result = new TaskResult( taskCommand, new ModelAndView( new RedirectView( "/Gemma" ) ) );

        return result;
    }
}
