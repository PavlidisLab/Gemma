/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.search;

import static org.junit.Assert.*;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.gemma.core.ontology.OntologyService;

/**
 * TODO Document Me
 * 
 * @author paul
 */
public class OntologyInferenceTest {

    protected final Log log = LogFactory.getLog( this.getClass() );
    UberonOntologyService m;
    @Autowired
    OntologyService ontologyService;

    @Before
    public void setup() throws Exception {
        m = new UberonOntologyService();

        if ( !m.isEnabled() ) m.startInitializationThread( true );

        int i = 0;
        while ( !m.isOntologyLoaded() ) {
            Thread.sleep( 3000 );
            i++;
            if ( i % 3 == 0 )
                log.info( "Waiting for Uberon to load ... " + i );

            if ( i > 40 ) throw new IllegalStateException( "Uberon Ontology didn't load in time" );
        }
    }

    @Test
    public void test() throws Exception {
        log.info( "===========================" );

        Collection<OntologyTerm> hits = m.findTerm( "brain" );
        String sought = "dorsal striatum";

        boolean found = false;
        for ( OntologyTerm ontologyTerm : hits ) {
            log.info( ">>> " + ontologyTerm );
            Collection<OntologyTerm> children = ontologyTerm.getChildren( false );
            for ( OntologyTerm ontologyTerm2 : children ) {
                log.info( ontologyTerm2 );
                if ( ontologyTerm2.getTerm().equals( sought ) ) {
                    found = true;
                }
            }
        }
        assertTrue( "failed to find " + sought, found );

    }

    @Test
    public void testB() throws Exception {
        log.info( "===========================" );
        // hippocampus should not yield "cerebral cortex".
        Collection<OntologyTerm> hits = m.findTerm( "hippocampus" );
        String sought = "cerebral cortex";

        assertTrue( hits.size() > 0 );

        for ( OntologyTerm ontologyTerm : hits ) {
            if ( ontologyTerm.getTerm().equals( sought ) ) {
                fail( "Should not have found 'cerebral cortex'" );
            }
            log.info( ">>> " + ontologyTerm );
            Collection<OntologyTerm> children = ontologyTerm.getChildren( false );
            for ( OntologyTerm ontologyTerm2 : children ) {
                log.info( ontologyTerm2 );
                if ( ontologyTerm2.getTerm().equals( sought ) ) {
                    fail( "Should not have found 'cerebral cortex'" );
                }
            }
        }
    }

}
