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
package ubic.gemma.ontology;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import junit.framework.TestCase;

/**
 * @author Paul
 * @version $Id$
 */
public class GeneOntologyServiceTest extends TestCase {
    GeneOntologyService gos;
    private static Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );

    // note: no spring context.
    @Override
    protected void setUp() throws Exception {
        gos = new GeneOntologyService();
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/molecular_function.test.owl" );
        gos.loadTermsInNameSpace( is );
        log.info( "Ready to test" );
    }

    public final void testGetTermForId() throws Exception {
        String id = "GO:0000119";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        assertEquals( "mediator complex", termForId.getTerm() );
    }

    public final void testGetDefinition() throws Exception {
        String id = "GO:0032477";
        String definition = gos.getTermDefinition( id );
        assertNotNull( definition );
        log.info( definition );
        assertTrue( definition.startsWith( "A homodimeric complex that possess" ) );
    }

    public final void testGetChildren() throws Exception {
        String id = "GO:0000109";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 6, terms.size() );
    }

    public final void testGetAllChildren() throws Exception {
        String id = "GO:0045239";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 11, terms.size() );
    }

    public final void testGetAllChildrenB() throws Exception {
        String id = "GO:0005759";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 19, terms.size() );
    }

    public final void testGetParents() throws Exception {
        String id = "GO:0005762"; // large mito.ribo. subunit.
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 3, terms.size() );
    }

    public final void testAllParents() throws Exception {
        String id = "GO:0005762";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 28, terms.size() );
    }
    
    public final void testAsRegularGoId() throws Exception {
        String id = "GO:0005762";
        OntologyTerm termForId = GeneOntologyService.getTermForId( id );
        assertNotNull(termForId);
        String formatedId = GeneOntologyService.asRegularGoId( termForId );
        assertEquals(id, formatedId);
    }

}
