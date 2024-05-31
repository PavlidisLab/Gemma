package ubic.gemma.web.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.sequence.RepeatScan;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.util.Collection;

/**
 * An array design repeat scan spaces task
 *
 * @author keshav
 */
@Component
@Scope("prototype")
public class ArrayDesignRepeatScanTaskImpl extends AbstractTask<ArrayDesignRepeatScanTaskCommand>
        implements ArrayDesignRepeatScanTask {

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Override
    public TaskResult call() {

        ArrayDesign ad = taskCommand.getArrayDesign();

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad );
        RepeatScan scanner = new RepeatScan();
        scanner.repeatScan( sequences );

        return new TaskResult( taskCommand, new ModelAndView( new RedirectView( "/", true ) ) );
    }

}
