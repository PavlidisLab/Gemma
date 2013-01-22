/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologyIndexer;
import ubic.basecode.ontology.search.OntologySearch;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.larq.IndexLARQ;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyIndexerTest {

    private static final String ONTNAME_FOR_TESTS = "mgedtest";
    private static Log log = LogFactory.getLog( OntologyIndexerTest.class.getName() );

    @Test
    public final void testCellListings() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/loader/ontology/mged.owl.gz" ) );
        OntModel model = OntologyLoader.loadMemoryModel( is, "owl-test", OntModelSpec.OWL_MEM_TRANS_INF );
        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model, true );

        Collection<OntologyTerm> names = OntologySearch.matchClasses( model, index, "cell*" );
        for ( OntologyTerm ot : names ) {
            if ( !ot.toString().startsWith( "Cell" ) ) throw new Exception( ot + " does not start with Cell" );
        }
        index.close();
    }

    @Test
    public final void testIndexing() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/loader/ontology/mged.owl.gz" ) );
        OntModel model = OntologyLoader.loadMemoryModel( is, "owl-test", OntModelSpec.OWL_MEM_TRANS_INF );

        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model, true );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, "Bedding" );

        assertEquals( 1, name.size() );
        index.close();
    }

    /**
     * See bug 2920
     * 
     * @throws Exception
     */
    @Test
    public final void testOmitBadPredicates() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/niforgantest.owl.xml" );
        OntModel model = OntologyLoader.loadMemoryModel( is, "NIFTEST", OntModelSpec.OWL_MEM_TRANS_INF );
        is.close();

        IndexLARQ index = OntologyIndexer.indexOntology( "NIFTEST", model, true );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, "Organ" );
        for ( OntologyTerm ontologyTerm : name ) {
            log.debug( ontologyTerm );
        }
        assertEquals( 4, name.size() );

        name = OntologySearch.matchClasses( model, index, "Anatomical entity" );
        for ( OntologyTerm ontologyTerm : name ) {
            log.debug( ontologyTerm );
        }
        assertEquals( 1, name.size() );

        name = OntologySearch.matchClasses( model, index, "liver" ); // this is an "example" that we want to avoid
                                                                     // leading to "Organ".
        for ( OntologyTerm ontologyTerm : name ) {
            log.debug( ontologyTerm );
        }
        assertEquals( 0, name.size() );

        index.close();
    }

    /**
     * See bug 3269
     * 
     * @throws Exception
     */
    @Test
    public final void testOmitBadPredicates2() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/eftest.owl.xml" );
        OntModel model = OntologyLoader.loadMemoryModel( is, "EFTEST", OntModelSpec.OWL_MEM_TRANS_INF );
        is.close();

        IndexLARQ index = OntologyIndexer.indexOntology( "EFTEST", model, true );

        // positive control
        Collection<OntologyTerm> searchResults = OntologySearch.matchClasses( model, index, "monocyte" );
        assertTrue( "Should have found something for 'monocyte'", !searchResults.isEmpty() );
        assertEquals( 1, searchResults.size() );

        // this is a "definition" that we want to avoid leading to "Monocyte".
        searchResults = OntologySearch.matchClasses( model, index, "liver" );
        for ( OntologyTerm ontologyTerm : searchResults ) {
            fail( "Should not have found " + ontologyTerm.toString() );
        }
        assertEquals( 0, searchResults.size() );

        index.close();
    }

    /**
     * @throws Exception
     */
    @Test
    public final void testPersistance() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/loader/ontology/mged.owl.gz" ) );
        OntModel model = OntologyLoader.loadMemoryModel( is, "owl-test", OntModelSpec.OWL_MEM_TRANS_INF );

        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model, true );
        index.close();

        // now load it off disk
        index = OntologyIndexer.getSubjectIndex( ONTNAME_FOR_TESTS );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, "beddin*" );

        assertEquals( 1, name.size() );
        index.close();
    }

    @Test
    public final void testPersistanceFail() {
        OntologyIndexer.eraseIndex( ONTNAME_FOR_TESTS );
        try {
            OntologyIndexer.getSubjectIndex( ONTNAME_FOR_TESTS );
            fail( "Should have gotten an exception" );
        } catch ( Exception e ) {
        }
    }

}
