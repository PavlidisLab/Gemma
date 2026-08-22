package ubic.gemma.core.tasks.maintenance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.core.job.AbstractTask;
import ubic.gemma.core.job.TaskResult;

@Component
@Scope("prototype")
public class ArrayDesignReportTaskImpl extends AbstractTask<ArrayDesignReportTaskCommand>
        implements ArrayDesignReportTask {

    private final Log log = LogFactory.getLog( ArrayDesignReportTask.class.getName() );

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Override
    public TaskResult call() {
        TaskResult result = newTaskResult( null );

        if ( getTaskCommand().doAll() ) {
            arrayDesignReportService.generateArrayDesignReport();
            // Separate file, and the source of the "all platforms" figures.
            arrayDesignReportService.generateAllArrayDesignReport();
        } else if ( getTaskCommand().getArrayDesign() != null ) {
            arrayDesignReportService.generateArrayDesignReport( getTaskCommand().getArrayDesign().getId() );
        } else {
            log.warn( "TaskCommand was not valid, nothing being done" );
        }

        return result;
    }
}
