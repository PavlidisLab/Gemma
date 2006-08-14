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
package ubic.gemma.web.controller.common.description;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.loader.entrez.pubmed.PubMedXMLParser;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * Tests the BibliographicReferenceController
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class BibRefControllerTest extends BaseTransactionalSpringContextTest {
    private static Log log = LogFactory.getLog( BibRefControllerTest.class.getName() );

    private BibliographicReference br = null;
    private MockHttpServletRequest req = null;

    /**
     * Add a bibliographic reference to the database for testing purposes.
     */
    @Override
    public void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        BibliographicReferenceService brs = ( BibliographicReferenceService ) getBean( "bibliographicReferenceService" );

        PubMedXMLParser pmp = new PubMedXMLParser();
        Collection<BibliographicReference> brl = pmp.parse( getClass().getResourceAsStream( "/data/pubmed-test.xml" ) );
        br = brl.iterator().next();

        /* set the bib ref's pubmed accession number to the database entry. */
        br.setPubAccession( this.getTestPersistentDatabaseEntry() );

        /* bibref is now set. Call service to persist to database. */
        br = brs.findOrCreate( br );

    }

    /**
     * Tests deleting. Asserts that a message is returned from BibRefController confirming successful deletion.
     * 
     * @throws Exception
     */
    public void testDelete() throws Exception {
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );
        log.debug( "testing delete" );

        req = new MockHttpServletRequest( "POST", "/bibRef/deleteBibRef.html" );
        req.addParameter( "_eventId", "delete" );
        req.addParameter( "pubMedId", br.getPubAccession().getAccession() );
        ModelAndView mav = brc.handleRequest( req, new MockHttpServletResponse() );
        assertTrue( mav != null );
        assertEquals( "bibRefSearch", mav.getViewName() );
    }

    /**
     * Tests the exception handling of the controller's delete method.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testDeleteOfNonExistingEntry() throws Exception {
        /* set pubMedId to a non-existent id in gemdtest. */
        req = new MockHttpServletRequest( "POST", "/bibRef/deleteBibRef.html" );
        req.addParameter( "_eventId", "delete" );
        req.addParameter( "accession", "00000000" );
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );

        ModelAndView b = brc.handleRequest( req, new MockHttpServletResponse() );
        assert b != null; // ?
        assertEquals( "bibRefSearch", b.getViewName() );
        // in addition there should be a message "0000000 not found".
        Collection<String> errors = ( Collection<String> ) req.getAttribute( "errors" );
        assertTrue( errors.size() > 0 );
        assertTrue( errors.iterator().next().startsWith( "00000000" ) );

    }

    /**
     * Tests getting all the bibrefs, which is implemented in
     * {@link ubic.gemma.controller.entrez.pubmed.BibliographicReferenceController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testGetAllBibliographicReferences() throws Exception {
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );

        req = new MockHttpServletRequest( "GET", "/bibRef/showAllBibRef.html" );

        ModelAndView mav = brc.handleRequest( req, new MockHttpServletResponse() );
        assertTrue( mav != null );
        Map m = mav.getModel();

        assertNotNull( m.get( "bibliographicReferences" ) );
        assertEquals( mav.getViewName(), "pubMed.GetAll.results.view" );
    }
}
