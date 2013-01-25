package ubic.gemma.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.analysis.sequence.RepeatScan;
import ubic.gemma.job.TaskResult;
import ubic.gemma.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collection;

/**
 * An array design repeat scan spaces task
 *
 * @author keshav
 * @version $Id$
 */
@Component
@Scope("prototype")
public class ArrayDesignRepeatScanTaskImpl implements ArrayDesignRepeatScanTask {

    @Autowired
    private ArrayDesignService arrayDesignService;

    private ArrayDesignRepeatScanTaskCommand command;

    @Override
    public void setCommand(ArrayDesignRepeatScanTaskCommand command) {
        this.command = command;
    }

    /*
             * (non-Javadoc)
             *
             * @see
             * ubic.gemma.grid.javaspaces.task.analysis.sequence.ArrayDesignRepeatScanTask#execute(ubic.gemma.grid.javaspaces
             * .analysis .sequence.SpacesArrayDesignRepeatScanCommand)
             */
    @Override
    public TaskResult execute() {

        ArrayDesign ad = command.getArrayDesign();

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad );
        RepeatScan scanner = new RepeatScan();
        scanner.repeatScan( sequences );

        TaskResult result = new TaskResult( command, new ModelAndView( new RedirectView( "/Gemma" ) ) );

        return result;
    }

}
