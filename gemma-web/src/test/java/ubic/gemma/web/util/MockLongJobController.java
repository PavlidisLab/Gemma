package ubic.gemma.web.util;

import ubic.gemma.job.TaskCommand;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @version $Id$
 */
public interface MockLongJobController {

    /**
     * 
     */
    public static final int JOB_LENGTH = 2000;

    public abstract String runJob( TaskCommand command );

}