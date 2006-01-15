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
package edu.columbia.gemma.web.flow;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.webflow.RequestContext;
import org.springframework.webflow.action.FormAction;

/**
 * All web flow FormActions should subclass this instead of the standard FormAction. This wraps a regular FormAction and
 * adds the functionality of the AbstractFlowAction.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see http://forum.springframework.org/viewtopic.php?t=9600
 */
public abstract class AbstractFlowFormAction extends FormAction implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    protected MessageSourceAccessor messageSourceAccessor;

    protected boolean isContextRequired() {
        return true;
    }

    protected final MessageSourceAccessor getMessageSourceAccessor() throws IllegalStateException {
        if ( this.messageSourceAccessor == null && isContextRequired() ) {
            throw new IllegalStateException( "ApplicationObjectSupport instance [" + this
                    + "] does not run in an ApplicationContext" );
        }
        return this.messageSourceAccessor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public final void setApplicationContext( ApplicationContext context ) throws BeansException {
        if ( context == null && !isContextRequired() ) {
            // reset internal context state
            this.applicationContext = null;
            this.messageSourceAccessor = null;
        }
        if ( this.applicationContext == null ) {
            // initialize with passed-in context
            if ( !requiredContextClass().isInstance( context ) ) {
                throw new ApplicationContextException( "Invalid application context: needs to be of type ["
                        + requiredContextClass().getName() + "]" );
            }
            this.applicationContext = context;
            this.messageSourceAccessor = new MessageSourceAccessor( context );
            initApplicationContext();
        } else {
            // ignore reinitialization if same context passed in
            if ( this.applicationContext != context ) {
                throw new ApplicationContextException(
                        "Cannot reinitialize with different application context: current one is ["
                                + this.applicationContext + "], passed-in one is [" + context + "]" );
            }
        }
    }

    protected Class requiredContextClass() {
        return ApplicationContext.class;
    }

    protected void initApplicationContext() throws BeansException {
    }

    public final ApplicationContext getApplicationContext() throws IllegalStateException {
        if ( this.applicationContext == null && isContextRequired() ) {
            throw new IllegalStateException( "ApplicationObjectSupport instance [" + this
                    + "] does not run in an ApplicationContext" );
        }
        return applicationContext;
    }

    protected Object addMessage( RequestContext context, String messageKey, Object[] parameters ) {
        return context.getRequestScope().setAttribute( "messages",
                getMessageSourceAccessor().getMessage( messageKey, parameters ) );
    }

}
