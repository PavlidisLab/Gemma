/*
 * Copyright 2002-2004 the original author or authors. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package edu.columbia.gemma.controller.flow.action;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.mock.web.flow.MockRequestContext;
import org.springframework.test.JUnitAssertSupport;
import org.springframework.web.flow.Event;

import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;

public class GetPubMedActionTests extends TestCase {

    /**
     * JUnit support assertion facility
     */
    private JUnitAssertSupport asserts = new JUnitAssertSupport();

    public void testDoExecuteActionError() throws Exception {
        //set up mock object
        MockControl control = MockControl.createControl( BibliographicReferenceService.class );
        BibliographicReferenceService bibliographicReferenceService = ( BibliographicReferenceService ) control
                .getMock();
        bibliographicReferenceService.getBibliographicReferenceByTitle( "mock non-existing title" );
        control.setReturnValue( null, 1 );
        control.replay();

        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "title", "mock non-existing title" );
        Event result = action.execute( context );
        assertEquals( "error", result.getId() );
        asserts().assertAttributeNotPresent( context.getRequestScope(), "bibliographicReference" );
        control.verify();
    }

    public void testDoExecuteActionSuccess() throws Exception {

        //set up mock object BibliographicReferenceService
        MockControl control = MockControl.createControl( BibliographicReferenceService.class );
        BibliographicReferenceService bibliographicReferenceService = ( BibliographicReferenceService ) control
                .getMock();
        bibliographicReferenceService.getBibliographicReferenceByTitle( "mock existing title" );
        //method getBibliographicReferenceByTitle(String s) called once.
        control.setReturnValue( new BibliographicReferenceImpl(), 1 );
        control.replay();

        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "title", "mock existing title" );
        Event result = action.execute( context );
        assertEquals( "success", result.getId() );
        asserts().assertAttributePresent( context.getRequestScope(), "bibliographicReference" );
        control.verify();
    }
    
    /**
     * Returns a support class for doing additional JUnit assertion operations not supported out-of-the-box by JUnit
     * 3.8.1.
     * 
     * @return The junit assert support.
     */
    protected JUnitAssertSupport asserts() {
        return asserts;
    }
}