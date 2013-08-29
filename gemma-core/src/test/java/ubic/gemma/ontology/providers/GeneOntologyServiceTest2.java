/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.ontology.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import org.junit.BeforeClass;
import org.junit.Test;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl.GOAspect;

/**
 * Additional tests with updated ontology file, fixing problems getting aspects.
 * 
 * @author Paul
 * @version $Id$
 */
public class GeneOntologyServiceTest2 {
    static GeneOntologyServiceImpl gos;

    // note: no spring context.
    @BeforeClass
    public static void setUp() throws Exception {
        gos = new GeneOntologyServiceImpl();
        InputStream is = new GZIPInputStream(
                GeneOntologyServiceTest.class.getResourceAsStream( "/data/loader/ontology/go.bptest.owl.gz" ) );
        assert is != null;
        gos.loadTermsInNameSpace( is );
    }

    @Test
    public final void testParents() {
        String id = "GO:0034118"; // regulation of erythrocyte aggregation

        OntologyTerm termForId = GeneOntologyServiceImpl.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getParents( termForId );
        assertEquals( 1, terms.size() );
        OntologyTerm par = terms.iterator().next();
        assertEquals( "http://purl.obolibrary.org/obo/GO_0034110", par.getUri() ); // regulation of homotypic cell-cell
                                                                                   // adhesion
    }

    @Test
    public final void testAllParents() {
        String id = "GO:0034118"; // regulation of erythrocyte aggregation

        OntologyTerm termForId = GeneOntologyServiceImpl.getTermForId( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = gos.getAllParents( termForId );

        // excludes "regulates" relations, excludes root.

        /*
         * regulation of homotypic cell-cell adhesion
         * 
         * regulation of cell-cell adhesion
         * 
         * regulation of cell adhesion
         * 
         * regulation of cellular process
         * 
         * regulation of biological process
         * 
         * biological regulation
         * 
         * (biological process)
         */
        assertEquals( 6, terms.size() );
    }

    /**
     * 
     */
    @Test
    public final void testGetAspect() {
        GOAspect termAspect = gos.getTermAspect( "GO:0034118" ); // regulation of erythrocyte aggregationS
        assertNotNull( termAspect );

        String aspect = termAspect.toString().toLowerCase();
        assertEquals( "biological_process", aspect );

    }

    /**
     * 
     */
    @Test
    public final void testGetAspect2() {
        GOAspect termAspect = gos.getTermAspect( "GO:0007272" ); // ensheathment of neurons
        assertNotNull( termAspect );
        String aspect = termAspect.toString().toLowerCase();
        assertEquals( "biological_process", aspect );
    }

}
