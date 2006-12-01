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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import ubic.gemma.web.util.MessageUtil;

/**
 * Extend this to create a MultiActionController.
 * 
 * @author keshav
 * @version $Id$
 * @spring.property name="messageUtil" ref="messageUtil"
 */
public abstract class BaseMultiActionController extends MultiActionController {

    protected Log log = LogFactory.getLog( getClass().getName() );
    private MessageUtil messageUtil;
    /**
     * @param request
     * @param messageCode - if no message is found, this is used to form the message (instead of throwing an exception).
     * @param parameters
     */
    protected void addMessage( HttpServletRequest request, String messageCode, Object[] parameters ) {
        try {
            request.getSession().setAttribute( "messages",
                    getMessageSourceAccessor().getMessage( messageCode, parameters ) );
        } catch ( NoSuchMessageException e ) {
            request.getSession().setAttribute( "messages", "??" + messageCode + "??" );
        }
    }
    
    protected void saveMessage( HttpServletRequest request, String msg ) {
        this.messageUtil.saveMessage( request, msg );
    }

    /**
     * @return the messageUtil
     */
    public MessageUtil getMessageUtil() {
        return this.messageUtil;
    }
    
    /**
     * @param messageUtil the messageUtil to set
     */
    public void setMessageUtil( MessageUtil messageUtil ) {
        this.messageUtil = messageUtil;
    }
}
