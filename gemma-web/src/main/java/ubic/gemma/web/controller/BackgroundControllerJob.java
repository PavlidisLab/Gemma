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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.web.util.MessageUtil;

/**
 * @author pavlidis
 * @version $Id$
 */
/**
 * 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
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

    private HttpServletRequest request;

    /**
     * @return the request
     */
    public HttpServletRequest getRequest() {
        return this.request;
    }

    /**
     * @return the taskId
     */
    public String getTaskId() {
        return this.taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    /**
     * @param securityContext
     * @param command
     * @param jobDescription
     * 
     */
    public BackgroundControllerJob( String taskId, SecurityContext parentSecurityContext, HttpServletRequest request,
            Object commandObj, MessageUtil messenger ) {
        this(request, messenger);
        this.taskId = taskId;      
        this.command = commandObj;
        
    }
    
    public BackgroundControllerJob(HttpServletRequest request, MessageUtil msgUtil) {
        super();
        this.securityContext = SecurityContextHolder.getContext();
        this.request = request;
        this.session = request.getSession();
        this.messageUtil = msgUtil;
    }

    /**
     * @param session
     * @param msg
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpSession, java.lang.String)
     */
    public void saveMessage( String msg ) {
        log.info( msg );
        this.messageUtil.saveMessage( session, msg );
    }
    
    
    /**
     * This should be called in the first line of the implementation of the call method.
     *  
     */
    protected void init() {
        SecurityContextHolder.setContext( securityContext );        
    }
}
