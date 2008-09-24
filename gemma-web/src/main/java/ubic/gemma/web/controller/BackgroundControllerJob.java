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
import org.directwebremoting.WebContext;
import org.directwebremoting.WebContextFactory;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.validation.BindException;

import ubic.gemma.web.util.MessageUtil;

/**
 * @author klc
 * @version $Id$
 */
public abstract class BackgroundControllerJob<T> implements Callable<T> {

    protected Log log = LogFactory.getLog( this.getClass().getName() );

    protected String taskId;
    protected Object command;
    protected SecurityContext securityContext;
    protected HttpSession session;

    private MessageUtil messageUtil;

    private BindException errors;

    private boolean doForward;

    public BindException getErrors() {
        return errors;
    }

    public void setErrors( BindException errors ) {
        this.errors = errors;
    }

    public void setDoForward( boolean doForward ) {
        this.doForward = doForward;
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

    public void setTaskId( String taskId ) {
        this.taskId = taskId;
    }

    public BackgroundControllerJob( String taskId, Object commandObj ) {
        this.securityContext = SecurityContextHolder.getContext();
        this.taskId = taskId;
        this.command = commandObj;
    }

    /**
     * @param securityContext
     * @param command
     * @param jobDescription
     */
    public BackgroundControllerJob( String taskId, Object commandObj, MessageUtil messenger ) {
        this( messenger );
        this.securityContext = SecurityContextHolder.getContext();
        this.taskId = taskId;
        this.command = commandObj;
    }

    /**
     * Use this for AJAX calls where the session will be obtained from the AJAX framework.
     * 
     * @param msgUtil
     */
    public BackgroundControllerJob( MessageUtil msgUtil ) {
        this();
        this.messageUtil = msgUtil;
    }

    /**
     * 
     */
    public BackgroundControllerJob() {
        super();
        this.securityContext = SecurityContextHolder.getContext();
        WebContext ctx = WebContextFactory.get();
        if ( ctx != null ) {
            this.session = ctx.getSession( false );
        }

    }

    /**
     * Use this where the session is available from a regular HttpRequest object.
     * 
     * @param msgUtil
     * @param session
     */
    public BackgroundControllerJob( MessageUtil msgUtil, HttpSession session ) {
        super();
        this.securityContext = SecurityContextHolder.getContext();
        this.session = session;
        this.messageUtil = msgUtil;
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
        } else {
            log.warn( "Could not save message to the session" );
        }
    }

    /**
     * This should be called in the first line of the implementation of the call method.
     */
    protected void init() {
        if ( this.securityContext == null ) {
            throw new IllegalStateException( "Security context was not provided" );
        }
        if ( this.securityContext.getAuthentication() == null ) {
            throw new IllegalStateException( "No authentication was provided" );
        }
        SecurityContextHolder.setContext( securityContext );
    }
}
