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
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

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
        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model );

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

        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, "Bedding" );

        assertEquals( 1, name.size() );
        index.close();
    }

    @Test
    public final void testPersistance() throws Exception {
        InputStream is = new GZIPInputStream( this.getClass().getResourceAsStream( "/data/loader/ontology/mged.owl.gz" ) );
        OntModel model = OntologyLoader.loadMemoryModel( is, "owl-test", OntModelSpec.OWL_MEM_TRANS_INF );

        IndexLARQ index = OntologyIndexer.indexOntology( ONTNAME_FOR_TESTS, model );
        index.close();

        // now load it off disk
        index = OntologyIndexer.getSubjectIndex( ONTNAME_FOR_TESTS );

        Collection<OntologyTerm> name = OntologySearch.matchClasses( model, index, "beddin*" );
        log.info( name.toString() );
        assertEquals( 1, name.size() );
        index.close();
    }

    @Test
    public final void testPersistanceFail() throws Exception {
        OntologyIndexer.eraseIndex( ONTNAME_FOR_TESTS );
        try {
            OntologyIndexer.getSubjectIndex( ONTNAME_FOR_TESTS );
            fail( "Should have gotten an exception" );
        } catch ( Exception e ) {
        }
    }

}
