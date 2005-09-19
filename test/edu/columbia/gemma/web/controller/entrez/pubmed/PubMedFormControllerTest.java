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
package edu.columbia.gemma.web.controller.entrez.pubmed;

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

/**
 * <p>
 * Tests saving and deleting of items in {@link edu.columbia.gemma.web.controller.entrez.pubmed.PubMedFormController}.
 * </p>
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PubMedFormControllerTest extends BaseControllerTestCase {
    private static Log log = LogFactory.getLog( PubMedFormControllerTest.class.getName() );

    private PubMedFormController c = null;
    private BibliographicReference br = null;
    private MockHttpServletRequest req = null;
    private ModelAndView mav = null;
    private boolean skip = false;

    public void setUp() throws Exception {
        int pubMedId = 15699352;

        c = ( PubMedFormController ) ctx.getBean( "pubMedFormController" );

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
     * Tests the edit() method;
     * 
     * @throws Exception
     */
    public void testEdit() throws Exception {
        if ( !skip ) {
            log.debug( "testing edit" );

            req = new MockHttpServletRequest( "GET", "/editBibRef.htm" );
            req.addParameter( "pubMedId", br.getPubAccession().getAccession() );
            mav = c.handleRequest( req, new MockHttpServletResponse() );
            assertEquals( "pubMed.Search.criteria.view", mav.getViewName() );
        } else {
            log.info( "skipped test " + this.getName() );
        }

    }

    /**
     * Tests the save() method.
     * 
     * @throws Exception
     */
    public void testSave() throws Exception {
        if ( !skip ) {
            log.debug( "testing save" );

            req = new MockHttpServletRequest( "POST", "/editBibRef.htm" );
            req.addParameter( "pubMedId", br.getPubAccession().getAccession() );

            mav = c.handleRequest( req, new MockHttpServletResponse() );
            String errorsKey = BindException.ERROR_KEY_PREFIX + c.getCommandName();
            Errors errors = ( Errors ) mav.getModel().get( errorsKey );
            assertNull( errors );
            // FIXME put this back in when you are using success messages.
            // assertNotNull( req.getSession().getAttribute( "message" ) );
        } else {
            log.info( "skipped test " + this.getName() );
        }
    }
}
