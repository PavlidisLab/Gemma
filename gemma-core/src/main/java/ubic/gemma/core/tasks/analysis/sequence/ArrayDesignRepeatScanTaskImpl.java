package ubic.gemma.core.tasks.analysis.sequence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.sequence.RepeatScan;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;
import ubic.gemma.core.loader.expression.arrayDesign.ArrayDesignSequenceAlignmentServiceImpl;
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

    @Value("${repeatMasker.exe}")
    private String repeatMaskerExe;

    @Override
    public TaskResult call() {

        ArrayDesign ad = getTaskCommand().getArrayDesign();

        ad = arrayDesignService.thaw( ad );

        Collection<BioSequence> sequences = ArrayDesignSequenceAlignmentServiceImpl.getSequences( ad );
        RepeatScan scanner = new RepeatScan( repeatMaskerExe );
        scanner.repeatScan( sequences );

        return newTaskResult( null );
    }

}
