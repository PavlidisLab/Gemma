package ubic.gemma.web.controller;

import org.springframework.web.servlet.ModelAndView;

/**
 * @author keshav
 * @version $Id$
 */
public abstract class BaseControllerJob<T> extends BackgroundControllerJob {

    public BaseControllerJob( String taskId, Object commandObj ) {
        super( taskId, commandObj );
    }

    /**
     * @param command
     * @return
     */
    protected abstract ModelAndView processJob( BaseCommand command );

}
