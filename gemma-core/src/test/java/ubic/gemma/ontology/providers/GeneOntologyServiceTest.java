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
package ubic.gemma.ontology.providers;

import java.io.InputStream;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;

/**
 * @author Paul
 * @version $Id$
 */
@SuppressWarnings("static-access")
public class GeneOntologyServiceTest extends TestCase {
    GeneOntologyServiceImpl gos;
    private static Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );

    public final void testAllParents() throws Exception {
        String id = "GO:1234567";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 2, terms.size() );
    }

    public final void testGetAspect() throws Exception {
        String aspect = GeneOntologyServiceImpl.getTermAspect( "GO:1234567" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceImpl.getTermAspect( "GO:0000107" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceImpl.getTermAspect( "GO:0016791" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceImpl.getTermAspect( "GO:1234567" ).toString().toLowerCase(); // should be cached.
        assertEquals( "molecular_function", aspect );
        aspect = GeneOntologyServiceImpl.getTermAspect( "GO:1234567" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
    }

    public final void testAsRegularGoId() throws Exception {
        String id = "GO:0000107";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        String formatedId = gos.asRegularGoId( termForId );
        assertEquals( id, formatedId );
    }

    public final void testGetAllChildren() throws Exception {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 2, terms.size() );
    }

    public final void testGetChildren() throws Exception {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 1, terms.size() );
    }

    public final void testGetChildrenPartOf() throws Exception {
        String id = "GO:0003720";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId, true );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        // has a part.
        assertEquals( 1, terms.size() );
    }

    public final void testGetDefinition() throws Exception {
        String id = "GO:0000007";
        String definition = gos.getTermDefinition( id );
        assertNotNull( definition );
        log.info( definition );
        assertTrue( definition.startsWith( "Catalysis of the transfer" ) );
    }

    public final void testGetParents() throws Exception {
        String id = "GO:1234567";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 1, terms.size() );
    }

    public final void testGetParentsPartOf() throws Exception {
        String id = "GO:0000332";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        // is a subclass and partof.
        assertEquals( 2, terms.size() );
    }

    public final void testGetTermForId() throws Exception {
        String id = "GO:0000310";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        assertEquals( "xanthine phosphoribosyltransferase activity", termForId.getTerm() );
    }

    // note: no spring context.
    @Override
    protected void setUp() throws Exception {
        gos = new GeneOntologyServiceImpl();
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/molecular-function.test.owl" );
        assert is != null;
        gos.loadTermsInNameSpace( is );
        log.info( "Ready to test" );
    }

}
