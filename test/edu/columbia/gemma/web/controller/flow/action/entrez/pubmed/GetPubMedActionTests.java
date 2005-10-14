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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.springframework.webflow.test.AbstractFlowExecutionTests;
import org.springframework.webflow.test.MockRequestContext;
import org.springframework.webflow.Event;
import org.springframework.webflow.ViewDescriptor;

import edu.columbia.gemma.common.description.BibliographicReferenceImpl;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.web.controller.flow.bibref.GetPubMedAction;

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
        BibliographicReferenceService bibliographicReferenceService = createMock( BibliographicReferenceService.class );
        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        bibliographicReferenceService.findByExternalId( "100009491" );
        expectLastCall().andReturn( null );
        replay( bibliographicReferenceService );

        this.startFlow();
        assertCurrentStateEquals( "criteria.view" );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "pubMedId", "100009491" );

        Event result = action.execute( context );
        assertEquals( "error", result.getId() );

        ViewDescriptor view = signalEvent( result );
        this.assertModelAttributeNull( "bibliographicReference", view );

        verify( bibliographicReferenceService );
    }

    public void testDoExecuteActionSuccess() throws Exception {
        BibliographicReferenceService bibliographicReferenceService = createMock( BibliographicReferenceService.class );
        GetPubMedAction action = new GetPubMedAction();
        action.setBibliographicReferenceService( bibliographicReferenceService );
        bibliographicReferenceService.findByExternalId( "19491" );
        expectLastCall().andReturn( new BibliographicReferenceImpl() );
        replay( bibliographicReferenceService );

        this.startFlow();
        assertCurrentStateEquals( "criteria.view" );
        MockRequestContext context = new MockRequestContext();
        context.getFlowScope().setAttribute( "pubMedId", "19491" );

        Event result = action.execute( context );
        assertEquals( "success", result.getId() );

        ViewDescriptor view = signalEvent( result );
        this.assertModelAttributeNotNull( "bibliographicReference", view );

        verify( bibliographicReferenceService );
    }

    @Override
    protected String flowId() {
        return "pubMed.Search";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath:WEB-INF/action-servlet.xml",
                "classpath:WEB-INF/applicationContext-hibernate.xml",
                "classpath:WEB-INF/applicationContext-security.xml",
                "classpath:WEB-INF/applicationContext-validation.xml", "classpath:WEB-INF/localTestdataSource.xml" };
    }
}