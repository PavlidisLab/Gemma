/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.grid.javaspaces;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.progress.TaskRunningService;
import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * Tasks extending this are to be executed in a JavaSpaces environment. This base class serves as starting point for all
 * tasks, providing common functionality such as logging.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class BaseSpacesTask implements SpacesTask {

    protected GigaSpacesTemplate gigaSpacesTemplate = null;

    protected String taskId;

    private Log log = LogFactory.getLog( this.getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        this.taskId = TaskRunningService.generateTaskId();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#getTaskId()
     */
    public String getTaskId() {
        return this.taskId;
    }

    /**
     * Creates a new progress appender for tasks. Typically, this method should be called at the beginning of the
     * execute method of the task.
     * 
     * @param clazz
     * @return {@link SpacesProgressAppender}
     */
    public SpacesProgressAppender initProgressAppender( Class<? extends SpacesTask> clazz ) {

        log.info( "Executing task " + clazz.getSimpleName() + " with id " + this.taskId );

        log.debug( "Current Thread: " + Thread.currentThread().getName() + " Authentication: "
                + SecurityContextHolder.getContext().getAuthentication() );

        Logger logger = LogManager.getLogger( "ubic.gemma" );
        logger.setLevel( Level.INFO );

        Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
        baseCodeLogger.setLevel( Level.INFO );

        if ( gigaSpacesTemplate == null )
            throw new RuntimeException( "Will not be able to log information for the task " + clazz.getSimpleName() );

        SpacesProgressAppender javaSpacesAppender = new SpacesProgressAppender( gigaSpacesTemplate, taskId );

        /* Add the appender to the gemma logger */
        if ( !logger.isAttached( javaSpacesAppender ) ) {
            logger.addAppender( javaSpacesAppender );
        }

        /* Add the appender to the basecode logger */
        if ( !baseCodeLogger.isAttached( javaSpacesAppender ) ) {
            baseCodeLogger.addAppender( javaSpacesAppender );
        }
        return javaSpacesAppender;
    }

    /**
     * @param gigaSpacesTemplate
     */
    public void setGigaSpacesTemplate( GigaSpacesTemplate gigaSpacesTemplate ) {
        this.gigaSpacesTemplate = gigaSpacesTemplate;
        this.gigaSpacesTemplate.setFifo( false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.grid.javaspaces.SpacesTask#setTaskId(java.lang.String)
     */
    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    /**
     * Removes the {@link SpacesProgressAppender} from the list of appenders, and removes the progress entry from the
     * space. Typically, this is called after at the end of a task's execute method.
     */
    public void tidyProgress( SpacesProgressAppender spacesProgressAppender ) {
        if ( spacesProgressAppender != null ) {

            /* Remove the appender from the gemma logger */
            Logger logger = LogManager.getLogger( "ubic.gemma" );
            logger.removeAppender( spacesProgressAppender );

            /* Remove the appender from the basecode logger */
            Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
            baseCodeLogger.removeAppender( spacesProgressAppender );

            spacesProgressAppender.removeProgressEntry();
        }
    }

}
