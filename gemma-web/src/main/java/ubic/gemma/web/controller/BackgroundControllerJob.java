/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller;

import java.util.concurrent.Callable;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.util.MessageUtil;

/**
 * @author klc
 * @version $Id$
 */
public abstract class BackgroundControllerJob<T> implements Callable<T> {

    protected Log log = LogFactory.getLog( this.getClass().getName() );

    protected String taskId;

    protected HttpSession session;

    private MessageUtil messageUtil;

    private boolean doForward;

    protected Object command;

    protected final SecurityContext securityContext;

    public BackgroundControllerJob() {
        this.securityContext = SecurityContextHolder.getContext();
    }

    /**
     * Use this where the session is available from a regular HttpRequest object.
     * 
     * @param msgUtil
     * @param session
     */
    public BackgroundControllerJob( MessageUtil msgUtil, HttpSession session ) {
        this();
        this.session = session;
        this.messageUtil = msgUtil;
    }

    public BackgroundControllerJob( String taskId ) {
        this();
        this.taskId = taskId;
    }

    /**
     * @return the command
     */
    public Object getCommand() {
        return command;
    }

    public boolean getDoForward() {
        return this.doForward;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return this.taskId;
    }

    /**
     * @param session
     * @param msg
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpSession, java.lang.String)
     */
    public void saveMessage( String msg ) {
        log.info( msg );
        if ( session != null && this.messageUtil != null ) {
            this.messageUtil.saveMessage( session, msg );
        }
    }

    /**
     * @param command the command to set
     */
    public void setCommand( Object command ) {
        this.command = command;
    }

    public void setDoForward( boolean doForward ) {
        this.doForward = doForward;
    }

    /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    protected ProgressJob init( String jobDescription ) {
        SecurityContextHolder.setContext( this.securityContext );

        ProgressJob job = ProgressManager.createProgressJob( this.getTaskId(), jobDescription );
        return job;
    }

}
