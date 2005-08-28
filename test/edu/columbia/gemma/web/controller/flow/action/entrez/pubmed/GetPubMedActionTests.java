package edu.columbia.gemma.web.controller.flow.action.entrez.pubmed;

import junit.framework.TestCase;

import org.easymock.MockControl;
import org.springframework.webflow.test.AbstractFlowExecutionTests;
import org.springframework.webflow.test.MockRequestContext;
import org.springframework.webflow.Event;

import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.web.controller.flow.action.entrez.pubmed.GetPubMedAction;

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
        setDependencyCheck(false);
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