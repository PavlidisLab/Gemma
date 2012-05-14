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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
 */
public class OntologyServiceTest extends BaseSpringContextTest {

    @Autowired
    OntologyService os;

    /**
     * This test can fail if the db isn't initialized public void testListAvailableOntologies() throws Exception {
     * Collection<Ontology> name = OntologyService.listAvailableOntologies(); assertTrue( name.size() > 0 ); }
     */
    @Test
    public final void testFindExactMatch() throws Exception {
        if ( !os.getMgedOntologyService().isOntologyLoaded() ) {
            os.getMgedOntologyService().startInitializationThread( true );
            int c = 0;
            while ( !os.getMgedOntologyService().isOntologyLoaded() ) {
                Thread.sleep( 1000 );
                log.info( "Waiting for Ontology to load" );
                if ( ++c > 30 ) {
                    fail( "Ontology load timeout" );
                }
            }
        }
        Collection<Characteristic> name = os.findExactTerm( "male",
                "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Sex", null );
        for ( Characteristic characteristic : name ) {
            log.info( characteristic );
        }
        assertTrue( name.size() > 0 );
    }

    @Test
    public void testObsolete() throws Exception {
        if ( !os.getDiseaseOntologyService().isOntologyLoaded() ) {
            os.getDiseaseOntologyService().startInitializationThread( true );
            int c = 0;

            while ( !os.getDiseaseOntologyService().isOntologyLoaded() ) {
                Thread.sleep( 1000 );
                log.info( "Waiting for DiseaseOntology to load" );
                if ( ++c > 30 ) {
                    fail( "Ontology load timeout" );
                }
            }
        }

        // Actinomadura madurae infectious disease
        assertTrue( os.isObsolete( "http://purl.org/obo/owl/DOID#DOID_0050001" ) );

        // inflammatory diarrhea, not obolete as of May 2012.
        assertTrue( !os.isObsolete( "http://purl.org/obo/owl/DOID#DOID_0050132" ) );

    }
}
