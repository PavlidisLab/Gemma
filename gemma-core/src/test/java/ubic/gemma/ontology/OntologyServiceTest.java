/*
 * The Gemma project
 * 
 * Copyright (c) 2007-2013 University of British Columbia
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class OntologyServiceTest extends BaseSpringContextTest {

    @Autowired
    private OntologyService os;

    /** 
     */
    @Test
    public final void testFindExactMatch() throws Exception {
        if ( !os.getExperimentalFactorOntologyService().isOntologyLoaded() ) {
            os.getExperimentalFactorOntologyService().startInitializationThread( true );
            int c = 0;
            while ( !os.getExperimentalFactorOntologyService().isOntologyLoaded() ) {
                Thread.sleep( 10000 );
                log.info( "Waiting for Ontology to load" );
                if ( ++c > 20 ) {
                    fail( "Ontology load timeout" );
                }
            }
        }
        Collection<Characteristic> name = os.findExactTerm( "male", "http://www.ebi.ac.uk/efo/EFO_0001266", null );
        for ( Characteristic characteristic : name ) {
            log.info( characteristic );
        }
        assertTrue( name.size() > 0 );
    }

    @Test
    public void testObsolete() throws Exception {
        os.getDiseaseOntologyService().startInitializationThread( true );
        int c = 0;

        while ( !os.getDiseaseOntologyService().isOntologyLoaded() ) {
            Thread.sleep( 10000 );
            log.info( "Waiting for DiseaseOntology to load" );
            if ( ++c > 20 ) {
                fail( "Ontology load timeout" );
            }
        }

        OntologyTerm t1 = os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050001" );
        assertNotNull( t1 );

        // Actinomadura madurae infectious disease
        assertTrue( os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050001" ) );

        // inflammatory diarrhea, not obsolete as of May 2012.
        assertNotNull( os.getTerm( "http://purl.obolibrary.org/obo/DOID_0050132" ) );
        assertTrue( !os.isObsolete( "http://purl.obolibrary.org/obo/DOID_0050132" ) );

    }
}
