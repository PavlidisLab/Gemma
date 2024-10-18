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
package ubic.gemma.web.controller.common.description.bibref;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLParser;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the BibliographicReferenceController
 *
 * @author keshav
 * @author pavlidis
 */
public class BibRefControllerTest extends BaseSpringWebTest {

    @Autowired
    private BibliographicReferenceService brs;

    private BibliographicReference br = null;

    /*
     * Add a bibliographic reference to the database for testing purposes.
     */
    @Before
    public void setUp() throws Exception {

        assert brs != null;

        PubMedXMLParser pmp = new PubMedXMLParser();

        ExternalDatabase pubmed = externalDatabaseService.findByName( ExternalDatabases.PUBMED );
        assertNotNull( pubmed );

        try {
            Collection<BibliographicReference> brl = pmp
                    .parse( this.getClass().getResourceAsStream( "/data/pubmed-test.xml" ) );
            br = brl.iterator().next();

            /* set the bib ref's pubmed accession number to the database entry. */
            br.setPubAccession( this.getTestPersistentDatabaseEntry( pubmed ) );

            /* bibref is now set. Call service to persist to database. */
            br = brs.findOrCreate( br );

            assert br.getId() != null;
        } catch ( IOException e ) {
            if ( e.getCause() instanceof java.net.ConnectException ) {
                assumeNoException( "Test skipped due to connection exception", e );
            } else if ( e.getCause() instanceof java.net.UnknownHostException ) {
                assumeNoException( "Test skipped due to unknown host exception", e );
            } else {
                throw ( e );
            }
        }
    }

    /*
     * Tests deleting. Asserts that a message is returned from BibRefController confirming successful deletion.
     */
    @Test
    public void testDelete() throws Exception {
        String accession = br.getPubAccession().getAccession();
        perform( post( "/deleteBibRef.html" ).param( "acc", accession ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "bibRefView" ) )
                .andExpect( model().attribute( "bibliographicReference", br ) );
        assertThat( brs.findByExternalId( accession, ExternalDatabases.PUBMED ) )
                .isNull();
    }

    /*
     * Tests the exception handling of the controller's remove method.
     */
    @Test
    public void testDeleteOfNonExistingEntry() throws Exception {
        /* set pubMedId to a non-existent id in gemdtest. */
        perform( post( "/deleteBibRef.html" ).param( "acc", "00000000" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    public void testShowById() throws Exception {
        perform( get( "/bibRefView.html" ).param( "id", String.valueOf( br.getId() ) ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "bibRefView" ) )
                .andExpect( model().attribute( "bibliographicReferenceId", br.getId() ) )
                .andExpect( model().attribute( "existsInSystem", Boolean.TRUE ) )
                .andExpect( model().attribute( "byAccession", Boolean.FALSE ) )
                .andExpect( model().attributeDoesNotExist( "accession" ) );
    }

    @Test
    public void testShowByPubMedId() throws Exception {
        perform( get( "/bibRefView.html" ).param( "accession", br.getPubAccession().getAccession() ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "bibRefView" ) )
                .andExpect( model().attribute( "bibliographicReferenceId", br.getId() ) )
                .andExpect( model().attribute( "existsInSystem", Boolean.TRUE ) )
                .andExpect( model().attribute( "byAccession", Boolean.TRUE ) )
                .andExpect( model().attribute( "accession", br.getPubAccession().getAccession() ) );
    }

    @Test
    public void testShowByPubMedIdThatDoesNotExistInSystem() throws Exception {
        perform( get( "/bibRefView.html" ).param( "accession", "1294000" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "bibRefView" ) )
                .andExpect( model().attribute( "bibliographicReferenceId", nullValue() ) )
                .andExpect( model().attribute( "existsInSystem", Boolean.FALSE ) )
                .andExpect( model().attribute( "byAccession", Boolean.TRUE ) )
                .andExpect( model().attribute( "accession", "1294000" ) );
    }

    @Test
    public void testShowAllForExperiments() throws Exception {
        perform( get( "/showAllEeBibRefs.html" ) )
                .andExpect( status().isOk() )
                .andExpect( view().name( "bibRefAllExperiments" ) )
                .andExpect( model().attributeExists( "citationToEEs" ) );
    }
}
