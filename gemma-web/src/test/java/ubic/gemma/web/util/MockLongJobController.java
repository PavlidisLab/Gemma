package ubic.gemma.web.util;

import ubic.gemma.job.TaskCommand;

public interface MockLongJobController {

    /**
     * 
     */
    public static final int JOB_LENGTH = 2000;

    public abstract String runJob( TaskCommand command );

}