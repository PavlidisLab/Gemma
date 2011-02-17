/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.job;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springmodules.javaspaces.gigaspaces.GigaSpacesTemplate;

import ubic.gemma.job.grid.GridTaskInterceptor;
import ubic.gemma.job.grid.util.SpacesUtil;
import ubic.gemma.job.progress.grid.SpacesProgressAppender;

/**
 * Handles setup and teardown procedures needed when running tasks.
 * <p>
 * Note: If we're not running in a space, this doesn't do as much - the main code here actually runs on the worker, not
 * the client. For inprocess tasks, this is not critical (still useful!)
 * 
 * @author paul
 * @version $Id$
 * @see GridTaskInterceptor
 * @see BackgroundJob
 */
@Aspect
public class TaskMethodAdvice {

    private Log log = LogFactory.getLog( this.getClass() );

    private SpacesUtil spacesUtil;

    /**
     * @param pjp - must have the TaskCommand as the first argument. To usee the grid, TaskCommand must have
     *        willRunOnGrid set for this to do proper setup.
     * @return the TaskResult
     * @throws Throwable
     */
    @Around("@annotation(ubic.gemma.job.TaskMethod)")
    public TaskResult gridexecute( ProceedingJoinPoint pjp ) throws Throwable {

        Object[] args = pjp.getArgs();

        assert args.length == 1;

        TaskCommand command = ( TaskCommand ) args[0];

        assert command != null;

        SpacesProgressAppender appender = setup( pjp, command );

        if ( log.isDebugEnabled() )
            log.debug( "Starting task: " + pjp.getTarget().getClass().getSimpleName() + " ID: " + command.getTaskId()
                    + " User: " + SecurityContextHolder.getContext().getAuthentication().getPrincipal() );

        TaskResult result = ( TaskResult ) pjp.proceed();

        log.debug( "Finished task " + result.getTaskID() );

        wrapup( result, appender );

        return result;
    }

    /**
     * @param spacesUtil the spacesUtil to set
     */
    public void setSpacesUtil( SpacesUtil spacesUtil ) {
        this.spacesUtil = spacesUtil;
    }

    /**
     * @param pjp
     * @param command
     * @return {@link SpacesProgressAppender}
     */
    private SpacesProgressAppender setup( ProceedingJoinPoint pjp, TaskCommand command ) {

        // doesn't do us any good during a remote run since this is on the worker. But fine for
        // 'local' runs
        command.setStartTime();
        command.setTaskInterface( pjp.getTarget().getClass().getName() );
        command.setTaskMethod( pjp.getSignature().getName() );

        if ( !command.isWillRunOnGrid() ) {
            return null;
        }

        GigaSpacesTemplate gigaSpacesTemplate = spacesUtil.getGigaspacesTemplate();
        if ( gigaSpacesTemplate == null ) return null;

        /*
         * Security propagation.
         */
        assert command.getSecurityContext() != null;
        assert command.getSecurityContext().getAuthentication() != null;
        assert command.getTaskId() != null;
        SecurityContextHolder.setContext( ( command.getSecurityContext() ) );

        /*
         * Logging
         */
        SpacesProgressAppender javaSpacesAppender = new SpacesProgressAppender( gigaSpacesTemplate, command.getTaskId() );
        Logger logger = LogManager.getLogger( "ubic.gemma" );
        Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
        logger.addAppender( javaSpacesAppender );
        baseCodeLogger.addAppender( javaSpacesAppender );

        return javaSpacesAppender;
    }

    /**
     * @param result
     * @param spacesProgressAppender
     */
    private void wrapup( TaskResult result, SpacesProgressAppender spacesProgressAppender ) {

        if ( spacesProgressAppender == null ) return; // not gridified

        /* Remove the appender from the gemma logger */
        Logger logger = LogManager.getLogger( "ubic.gemma" );
        logger.removeAppender( spacesProgressAppender );

        /* Remove the appender from the basecode logger */
        Logger baseCodeLogger = LogManager.getLogger( "ubic.basecode" );
        baseCodeLogger.removeAppender( spacesProgressAppender );

        spacesProgressAppender.removeProgressEntry();

        SecurityContextHolder.clearContext();
        result.setRanInSpace( true );

    }

}
