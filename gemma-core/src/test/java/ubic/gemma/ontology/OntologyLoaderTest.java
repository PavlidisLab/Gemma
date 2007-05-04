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

import java.util.Collection;

import ubic.gemma.ontology.OntologyLoader;
import ubic.gemma.ontology.OntologyRestriction;
import ubic.gemma.ontology.OntologyTerm;

import junit.framework.TestCase;

/**
 * @author Paul
 * @version $Id$
 */
public class OntologyLoaderTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // OntologyLoader.wipePersistentStore();
    }

    /**
     * Test method for {@link ubic.gemma.ontology.OntologyLoader#load(java.lang.String)}.
     */
    public void testLoad() throws Exception {
        OntologyLoader.loadPersistentModel(
                "http://www.berkeleybop.org/ontologies/obo-all/cellular_component/cellular_component.owl", true );

        // for ( OntologyResource t : terms ) {
        // if ( !( t instanceof OntologyTerm ) ) continue;
        // OntologyTerm term = ( OntologyTerm ) t;
        // System.err.print( term );
        // if ( term.isRoot() ) System.err.print( " (Root)" );
        // System.err.println();
        // Collection<OntologyTerm> restrictedRange = term.getParents( true );
        // if ( restrictedRange.size() > 0 ) {
        // for ( OntologyTerm term2 : restrictedRange ) {
        // if ( term2 instanceof OntologyRestriction ) {
        // System.err.println( " " + term2.toString() );
        // }
        // }
        // }
        //
        // }
    }

    public void testLoadMged() throws Exception {
        OntologyLoader.loadPersistentModel( "http://www.berkeleybop.org/ontologies/obo-all/mged/mged.owl", true );
        // for ( OntologyResource t : terms ) {
        // if ( !( t instanceof OntologyTerm ) ) continue;
        // OntologyTerm term = ( OntologyTerm ) t;
        // System.err.print( term );
        // if ( term.isRoot() ) System.err.print( " (Root)" );
        // System.err.println();
        // Collection<OntologyTerm> restrictedRange = term.getParents( true );
        // if ( restrictedRange.size() > 0 ) {
        // for ( OntologyTerm term2 : restrictedRange ) {
        // if ( term2 instanceof OntologyRestriction ) {
        // System.err.println( " " + term2.toString() );
        // }
        // }
        // }
        //
        // }
    }

}
