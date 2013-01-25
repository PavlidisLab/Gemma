package ubic.gemma.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignProbeMapperService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A probe mapper spaces task .
 *
 * @author keshav
 * @version $Id$
 */
@Component
public class ArrayDesignProbeMapperTaskImpl implements ArrayDesignProbeMapperTask {

    @Autowired
    private ArrayDesignProbeMapperService arrayDesignProbeMapperService = null;

    private ArrayDesignProbeMapTaskCommand command;

    @Override
    public void setCommand(ArrayDesignProbeMapTaskCommand command) {
        this.command = command;
    }

    /*
             * (non-Javadoc)
             * @see
             * ubic.gemma.grid.javaspaces.task.expression.arrayDesign.ArrayDesignProbeMapperTask#execute(ubic.gemma.grid.javaspaces
             * .expression.arrayDesign.SpacesProbeMapperCommand)
             */
    @Override
    public TaskResult execute() {

        ArrayDesign ad = command.getArrayDesign();

        arrayDesignProbeMapperService.processArrayDesign( ad );

        /*
         * FIXME get rid of web dependency
         */
        TaskResult result = new TaskResult( command, new ModelAndView( new RedirectView( "/Gemma" ) ) );

        return result;
    }

}
