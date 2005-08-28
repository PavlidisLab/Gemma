/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import org.easymock.MockControl;
import org.springframework.webflow.test.AbstractFlowExecutionTests;
import org.springframework.webflow.test.MockRequestContext;
import org.springframework.webflow.Event;

import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.web.controller.flow.action.entrez.pubmed.GetPubMedAction;

// //// here is an example of what this looks like, from the sample code
// public class SearchPersonFlowTests extends AbstractFlowExecutionTests {
//
// public SearchPersonFlowTests() {
// setDependencyCheck(false);
// }
//
// protected String flowId() {
// return "searchFlow";
// }
//
// protected String[] getConfigLocations() {
// return new String[] { "classpath:org/springframework/webflow/samples/phonebook/deploy/service-layer.xml",
// "classpath:org/springframework/webflow/samples/phonebook/deploy/web-layer.xml" };
// }
//
// public void testStartFlow() {
// startFlow();
// assertCurrentStateEquals("displayCriteria");
// }
//    
// public void testCriteriaView_Submit_Success() {
// startFlow();
// Map parameters = new HashMap();
// parameters.put("firstName", "Keith");
// parameters.put("lastName", "Donald");
// ViewDescriptor view = signalEvent(event("search", parameters));
// assertCurrentStateEquals("displayResults");
// Assert.collectionAttributeSizeEquals(view, "persons", 1);
// }
//    
// public void testCriteriaView_Submit_Error() {
// startFlow();
// // simulate user error by not passing in any params
// signalEvent(event("search"));
// assertCurrentStateEquals("displayCriteria");
// }
//
// }

/**
 * Test the PubMedAction used in the flow pubMed.Search
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GetPubMedActionTests extends AbstractFlowExecutionTests {

    public GetPubMedActionTests() {
        setDependencyCheck( false );
    }

    public void testDoExecuteActionError() throws Exception {
        // set up mock object
        MockControl control = MockControl.createControl( BibliographicReferenceService.class );
        BibliographicReferenceService bibliographicReferenceService = ( BibliographicReferenceService ) control
                .getMock();
        bibliographicReferenceService.findByExternalId( "19491" );
        control.setReturnValue( null, 1 );
        control.replay();

        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "pubMedId", "19491" );
        Event result = action.execute( context );
        assertEquals( "error", result.getId() );

        // FIXME
        // this.assertModelAttributeNull( "bibliographicReference" );
        // was: this.assertAttributeNotPresent( context.getRequestScope(), "bibliographicReference" );
        control.verify();
    }

    public void testDoExecuteActionSuccess() throws Exception {

        // set up mock object BibliographicReferenceService
        MockControl control = MockControl.createControl( BibliographicReferenceService.class );
        BibliographicReferenceService bibliographicReferenceService = ( BibliographicReferenceService ) control
                .getMock();
        bibliographicReferenceService.findByExternalId( "19491" );
        // method getBibliographicReferenceByTitle(String s) called once.
        control.setReturnValue( new BibliographicReferenceImpl(), 1 );
        control.replay();

        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "pubMedId", "19491" );
        Event result = action.execute( context );
        assertEquals( "success", result.getId() );

        // FIXME
        // this.assertModelAttributeNotNull( "bibliographicReference" );
        // was: assertAttributePresent( context.getRequestScope(), "bibliographicReference" );
        control.verify();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.webflow.test.AbstractFlowExecutionTests#flowId()
     */
    @Override
    protected String flowId() {
        return "foo";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "/edu/columbia/gemma/web/controller/flow/entrez/pubmed/pubMedDetail-flow.xml",
                "/edu/columbia/gemma/web/controller/flow/entrez/pubmed/pubMedSearch-flow.xml" };
    }
}