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
@SuppressWarnings("static-access")
public class GeneOntologyServiceTest extends TestCase {
    static GeneOntologyService gos;
    private static Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );

    // note: no spring context.
    @Override
    protected void setUp() throws Exception {
        if ( gos == null ) {
            gos = new GeneOntologyService();
            gos.forceLoadOntology();
            while ( !gos.isReady() ) {
                Thread.sleep( 1000 );
            }
            log.info( "Ready to test" );
            return;
        }
        log.info( "Still ready" );
       
    }

    public final void testGetTermForId() throws Exception {
        String id = "GO:0000310";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        assertEquals( "xanthine phosphoribosyltransferase activity", termForId.getTerm() );
    }

    public final void testGetDefinition() throws Exception {
        String id = "GO:0000007";
        String definition = gos.getTermDefinition( id );
        assertNotNull( definition );
        log.info( definition );
        assertTrue( definition.startsWith( "Catalysis of the transfer" ) );
    }

    public final void testGetChildren() throws Exception {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 66, terms.size() );
    }

    public final void testGetAllChildren() throws Exception {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 126, terms.size() );
    }

    public final void testGetParents() throws Exception {
        String id = "GO:0003720";  
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 1, terms.size() );
    }
    
    
    public final void testGetChildrenPartOf() throws Exception {
        String id = "GO:0003720";  
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllChildren( termForId, true);

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        // has a part.
        assertEquals( 2, terms.size() );
    }
    
    public final void testGetAllParentsPartOf() throws Exception {
        String id = "GO:0000332";  
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId, true);

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        // is a subclass and partof.
        assertEquals( 7, terms.size() );
    }

    public final void testAllParents() throws Exception {
        String id = "GO:0003720";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId );

        for ( OntologyTerm term : terms ) {
            log.info( term );
        }
        assertEquals( 6, terms.size() );
    }

    public final void testAsRegularGoId() throws Exception {
        String id = "GO:0000107";
        OntologyTerm termForId = gos.getTermForId( id );
        assertNotNull( termForId );
        String formatedId = gos.asRegularGoId( termForId );
        assertEquals( id, formatedId );
    }

}
