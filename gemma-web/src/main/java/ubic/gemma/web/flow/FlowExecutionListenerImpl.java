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
package ubic.gemma.web.flow;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.springframework.webflow.RequestContext;
import org.springframework.webflow.State;
import org.springframework.webflow.execution.EnterStateVetoException;
import org.springframework.webflow.execution.FlowExecutionListenerAdapter;
import org.springframework.webflow.execution.servlet.ServletEvent;

/**
 * This is an aspect-like class that is a property of the
 * org.springframework.webflow.execution.servlet.ServletFlowExecutionManager. Every time a step in a flow is being run,
 * this class is consulted for tasks that have to be done. This should be used instead of trying to expose the
 * HttpServletSession directly in the flow action.
 * <hr>
 * <p>
 * Copyright (c) 2004-2006 University of British Columbia
 * 
 * @author pavlidis
 * @version $Id$
 * @see http://forum.springframework.org/viewtopic.php?t=8747
 * @see http://forum.springframework.org/viewtopic.php?t=5991
 */
public class FlowExecutionListenerImpl extends FlowExecutionListenerAdapter {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.webflow.execution.FlowExecutionListenerAdapter#sessionStarting(org.springframework.webflow.RequestContext,
     *      org.springframework.webflow.State, java.util.Map)
     */
    @Override
    @SuppressWarnings("unused")
    public void sessionStarting( RequestContext context, State startState, Map input ) throws EnterStateVetoException {
        copyAttributes( context );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.webflow.execution.FlowExecutionListenerAdapter#requestSubmitted(org.springframework.webflow.RequestContext)
     */
    @Override
    public void requestSubmitted( RequestContext context ) {
        copyAttributes( context );
    }

    /**
     * @param context
     */
    private void copyAttributes( RequestContext context ) {
        ServletRequest request = ( ( ServletEvent ) context.getSourceEvent() ).getRequest();
        Enumeration names = request.getAttributeNames();
        while ( names.hasMoreElements() ) {
            String name = ( String ) names.nextElement();
            context.getRequestScope().put( name, request.getAttribute( name ) );
        }
    }

}
