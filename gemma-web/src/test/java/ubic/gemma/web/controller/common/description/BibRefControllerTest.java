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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.annotation.reference.BibliographicReferenceService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLParser;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.testing.BaseSpringWebTest;
import ubic.gemma.web.controller.common.description.bibref.BibliographicReferenceController;

/**
 * Tests the BibliographicReferenceController
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 */
public class BibRefControllerTest extends BaseSpringWebTest {

    private BibliographicReference br = null;
    private MockHttpServletRequest req = null;

    boolean ready = false;

    /**
     * Add a bibliographic reference to the database for testing purposes.
     */
    @Before
    public void setup() throws Exception {

        BibliographicReferenceService brs = ( BibliographicReferenceService ) getBean( "bibliographicReferenceService" );

        assert brs != null;

        PubMedXMLParser pmp = new PubMedXMLParser();

        try {
            Collection<BibliographicReference> brl = pmp.parse( getClass()
                    .getResourceAsStream( "/data/pubmed-test.xml" ) );
            br = brl.iterator().next();

            /* set the bib ref's pubmed accession number to the database entry. */
            br.setPubAccession( this.getTestPersistentDatabaseEntry() );

            /* bibref is now set. Call service to persist to database. */
            br = brs.findOrCreate( br );

            assert br.getId() != null;
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                log.warn( "Test skipped due to connection exception" );
                return;
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                log.warn( "Test skipped due to unknown host exception" );
                return;
            } else {
                throw ( e );
            }
        }

        ready = true;
    }

    /**
     * Tests deleting. Asserts that a message is returned from BibRefController confirming successful deletion.
     * 
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
        if ( !ready ) {
            log.error( "Test skipped due to failure to connect to NIH" );
            return;
        }
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );
        log.debug( "testing delete" );

        req = new MockHttpServletRequest( "POST", "/bibRef/deleteBibRef.html" );
        req.addParameter( "_eventId", "delete" );
        req.addParameter( "acc", br.getPubAccession().getAccession() );

        ModelAndView mav = brc.delete( req, new MockHttpServletResponse() );
        assertNotNull( mav );
        assertEquals( "bibRefView", mav.getViewName() );
    }

    /**
     * Tests the exception handling of the controller's delete method.
     * 
     * @throws Exception
     */
    @Test
    public void testDeleteOfNonExistingEntry() throws Exception {
        if ( !ready ) {
            log.error( "Test skipped due to failure to connect to NIH" );
            return;
        }
        /* set pubMedId to a non-existent id in gemdtest. */
        req = new MockHttpServletRequest( "POST", "/bibRef/deleteBibRef.html" );
        String nonexistentpubmedid = "00000000";
        req.addParameter( "acc", nonexistentpubmedid );
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );

        ModelAndView b = brc.delete( req, new MockHttpServletResponse() );
        assert b != null; // ?
        assertEquals( "bibRefView", b.getViewName() );
        // in addition there should be a message "0000000 not found".
        // Collection<String> errors = ( Collection<String> ) req.getAttribute( "errors" );
        // assertTrue( errors != null && errors.size() > 0 );
        // assertTrue( "Got: " + errors.iterator().next(), errors.iterator().next().startsWith( nonexistentpubmedid ) );

    }

    /**
     * Tests viewing
     * 
     * @throws Exception
     */
    @Test
    public void testShow() throws Exception {
        if ( !ready ) {
            log.error( "Test skipped due to failure to connect to NIH" );
            return;
        }
        BibliographicReferenceController brc = ( BibliographicReferenceController ) getBean( "bibliographicReferenceController" );
        req = new MockHttpServletRequest( "POST", "/bibRef/bibRefView.html" );
        req.addParameter( "accession", "" + 1294000 );

        try {
            ModelAndView mav = brc.show( req, new MockHttpServletResponse() );
            assertNotNull( mav );
            assertEquals( "bibRefView", mav.getViewName() );
        } catch ( RuntimeException e ) {
            if ( e.getCause() instanceof IOException && e.getMessage().contains( "503" ) ) {
                log.warn( "503 error from NCBI, skipping test: ", e );
            } else {
                throw e;
            }
        }
    }
}
