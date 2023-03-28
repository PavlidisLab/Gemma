/*
 * The Gemma project
 *
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.core.ontology.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;

/**
 * @author Paul
 */
@SuppressWarnings("static-access")
public class GeneOntologyServiceTest {
    private static final Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );
    private static GeneOntologyServiceImpl gos;

    // note: no spring context.
    @BeforeClass
    public static void setUp() throws Exception {
        GeneOntologyServiceTest.gos = new GeneOntologyServiceImpl();
        /*
         * Note that this test file is out of date in some ways. See GeneOntologyServiceTest2
         */
        InputStream is = new GZIPInputStream(
                new ClassPathResource( "/data/loader/ontology/molecular-function.test.owl.gz" ).getInputStream() );
        // we must force indexing to get consistent test results
        GeneOntologyServiceTest.gos.loadTermsInNameSpace( is, true );
    }

    @AfterClass
    public static void tearDown() {
        GeneOntologyServiceTest.gos.clearCaches();
    }

    @Test
    public void testFindTerm() throws OntologySearchException {
        Collection<OntologyTerm> matches = gos.findTerm( "toxin" );
        assertEquals( 4, matches.size() );
    }

    @Test
    public void testFindTermWithMultipleTerms() throws OntologySearchException {
        Collection<OntologyTerm> matches = gos.findTerm( "toxin transporter activity" );
        assertEquals( 1, matches.size() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindTermWithEmptyQuery() throws OntologySearchException {
        gos.findTerm( " " );
    }

    @Test
    public void testFindIndividuals() throws OntologySearchException {
        Collection<OntologyIndividual> matches = gos.findIndividuals( "protein tag" );
        assertEquals( 1, matches.size() );
    }

    @Test
    public void testFindResources() throws OntologySearchException {
        Collection<OntologyResource> matches = gos.findResources( "electron carrier" );
        assertEquals( 4, matches.size() );
    }

    @Test
    public final void testAllParents() {
        String id = "GO:0035242";

        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, false );

        assertEquals( 10, terms.size() );
    }

    @Test
    public final void testAllParents2() {
        String id = "GO:0000006";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, false );

        assertFalse( terms.contains( termForId ) );
        assertEquals( 12, terms.size() );
    }

    @Test
    public final void testAsRegularGoId() {
        String id = "GO:0000107";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        String formatedId = GeneOntologyServiceTest.gos.asRegularGoId( termForId );
        assertEquals( id, formatedId );
    }

    @Test
    public final void testGetAllChildren() {
        String id = "GO:0016791";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( false, false );

        assertEquals( 136, terms.size() );
    }

    @Test
    public final void testGetAspect() {
        String aspect = GeneOntologyServiceTest.gos.getTermAspect( "GO:0000107" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceTest.gos.getTermAspect( "GO:0016791" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceTest.gos.getTermAspect( "GO:0000107" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
    }

    @Test
    public final void testGetChildren() {
        String id = "GO:0016791";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( true, false );

        assertEquals( 65, terms.size() );
    }

    // latest versions do not have definitions included.
    // public final void testGetDefinition() {
    // String id = "GO:0000007";
    // String definition = gos.getTermDefinition( id );
    // assertNotNull( definition );
    // assertTrue( definition.startsWith( "I am a test definition" ) );
    // }

    @Test
    public final void testGetChildrenPartOf() {
        String id = "GO:0023025";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( false, true );

        // has a part.
        assertEquals( 1, terms.size() );
    }

    @Test
    public final void testGetParents() {
        String id = "GO:0000014";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( true, false );

        for ( OntologyTerm term : terms ) {
            GeneOntologyServiceTest.log.info( term );
        }
        assertEquals( 1, terms.size() );
    }

    @Test
    public final void testGetParentsPartOf() {
        String id = "GO:0000332";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, true );

        for ( OntologyTerm term : terms ) {
            GeneOntologyServiceTest.log.info( term );
        }
        // is a subclass and partof.
        assertEquals( 7, terms.size() );
    }

    @Test
    public final void testGetTermForId() {
        String id = "GO:0000310";
        OntologyTerm termForId = GeneOntologyServiceTest.gos.getTermForId( id );
        assertNotNull( termForId );
        assertEquals( "xanthine phosphoribosyltransferase activity", termForId.getTerm() );
    }

}
