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

import org.springframework.security.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.util.progress.grid.javaspaces.SpacesProgressAppender;

/**
 * Tasks extending this are to be executed in a JavaSpaces environment. This base class serves as starting point for all
 * tasks, providing common functionality such as logging.
 * 
 * @author keshav
 * @version $Id$
 */
public class BaseSpacesTask {

    private Log log = LogFactory.getLog( this.getClass() );

    protected GigaSpacesTemplate gigaSpacesTemplate = null;

    /**
     * Initializes the progress appender for tasks. Typically, this method should be called at the beginning of the
     * execute method of the task.
     * 
     * @param clazz
     */
    public void initProgressAppender( Class clazz ) {
        log.info( "Executing task " + clazz.getSimpleName() );

        log.debug( "Current Thread: " + Thread.currentThread().getName() + " Authentication: "
                + SecurityContextHolder.getContext().getAuthentication() );

        Logger logger = LogManager.getLogger( "ubic.gemma" );
        logger.setLevel( Level.INFO );

        if ( gigaSpacesTemplate == null )
            throw new RuntimeException( "Will not be able to log information for the task " + clazz.getSimpleName() );

        SpacesProgressAppender javaSpacesAppender = new SpacesProgressAppender( gigaSpacesTemplate );
        if ( !logger.isAttached( javaSpacesAppender ) ) {
            logger.addAppender( javaSpacesAppender );
        }
    }

    /**
     * @param gigaSpacesTemplate
     */
    public void setGigaSpacesTemplate( GigaSpacesTemplate gigaSpacesTemplate ) {
        this.gigaSpacesTemplate = gigaSpacesTemplate;
    }

}
