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
package edu.columbia.gemma.web.controller.common.description;

import java.net.UnknownHostException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.servlet.ModelAndView;

import edu.columbia.gemma.BaseControllerTestCase;
import edu.columbia.gemma.common.description.BibliographicReference;
import edu.columbia.gemma.common.description.BibliographicReferenceService;
import edu.columbia.gemma.common.description.DatabaseEntry;
import edu.columbia.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import edu.columbia.gemma.web.controller.common.description.BibliographicReferenceController;
import edu.columbia.gemma.web.controller.entrez.pubmed.PubMedFormController;

/**
 * Tests the BibliographicReferenceController
 * 
 *
 * <hr>
 * <p>Copyright (c) 2004 - 2005 Columbia University
 * @author keshav
 * @version $Id$
 */
public class BibRefControllerTest extends BaseControllerTestCase {
    private static Log log = LogFactory.getLog( BibRefControllerTest.class.getName() );

    private BibliographicReferenceController c = null;
    private BibliographicReference br = null;
    private MockHttpServletRequest req = null;
    private ModelAndView mav = null;
    private boolean skip = false;
    
    /**
     * Add a bibliographic reference to the database for testing purposes.
     */
    public void setUp() throws Exception {
        int pubMedId = 15699352;

        c = ( BibliographicReferenceController ) ctx.getBean( "bibliographicReferenceController" );

        /* add a test bibliographicReference to the database. */
        BibliographicReferenceService brs = ( BibliographicReferenceService ) ctx
                .getBean( "bibliographicReferenceService" );

        PubMedXMLFetcher pmf = ( PubMedXMLFetcher ) ctx.getBean( "pubMedXmlFetcher" );

        /* get bibref over http. if connection cannot be made, set the skip flag. */
        try {
            br = pmf.retrieveByHTTP( pubMedId );

            /* create database entry. */
            DatabaseEntry de = DatabaseEntry.Factory.newInstance();

            /* set the accession of database entry to the pubmed id. */
            de.setAccession( ( new Integer( pubMedId ) ).toString() );

            /* set the bib ref's pubmed accession number to the database entry. */
            br.setPubAccession( de );

            /* bibref is now set. Call service to persist to database. */
            if ( !brs.alreadyExists( br ) ) brs.saveBibliographicReference( br );

        } catch ( UnknownHostException e ) {
            skip = true;
        }
    }

    public void tearDown() {
        c = null;
        br = null;
    }

    /**
     * Tests deleting.  Asserts that a message is returned from BibRefController confirming successful deletion.
     * 
     * @throws Exception
     */
    public void testDelete() throws Exception {
        if ( !skip ) {
            log.debug( "testing delete" );
            
            req = new MockHttpServletRequest( "POST", "Gemma/editBibRef.htm" );
            req.addParameter( "_eventId", "delete" );
            req.addParameter( "pubMedId", br.getPubAccession().getAccession() );
            mav = c.handleRequest( req, new MockHttpServletResponse() );
            assertEquals( "pubMed.GetAll.results.view", mav.getViewName() );
            assertNotNull(req.getSession().getAttribute("messages"));
        } else {
            log.info( "skipped test " + this.getName() );
        }

    }
    
    /**
     * Tests the exception handling of the controller's delete method.
     * @throws Exception
     */
//    public void testDeleteOfNonExistingEntry(){
//        if ( !skip ) {
//            log.debug( "testing delete" );
//            
//            /* set pubMedId to a non-existent id in gemdtest. */
//            br.getPubAccession().setAccession("00000000");
//            
//            req = new MockHttpServletRequest( "POST", "Gemma/editBibRef.htm" );
//            req.addParameter( "_eventId", "delete" );
//            req.addParameter( "pubMedId", br.getPubAccession().getAccession() );
//            try{
//                mav = c.handleRequest( req, new MockHttpServletResponse() );
//            }
//            catch(Exception e){
//                e.printStackTrace();
//            }
//            
//            assertEquals( "pubMed.GetAll.results.view", mav.getViewName() );
//            assertNotNull(req.getSession().getAttribute("message"));
//        } else {
//            log.info( "skipped test " + this.getName() );
//        }
//    }

    /**
     * Tests getting all the bibrefs, which is implemented in
     * {@link edu.columbia.gemma.web.controller.entrez.pubmed.BibliographicReferenceController} in method
     * {@link #handleRequest(HttpServletRequest request, HttpServletResponse response)}.
     * 
     * @throws Exception
     */
    public void testGetAllBibliographicReferences() throws Exception {
        BibliographicReferenceController brc = ( BibliographicReferenceController ) ctx
                .getBean( "bibliographicReferenceController" );
        
        req = new MockHttpServletRequest( "GET", "Gemma/bibRefs.htm" );

        ModelAndView mav = brc.handleRequest( req, ( HttpServletResponse ) null );

        Map m = mav.getModel();

        assertNotNull( m.get( "bibliographicReferences" ) );
        assertEquals( mav.getViewName(), "pubMed.GetAll.results.view" );
    }
}
