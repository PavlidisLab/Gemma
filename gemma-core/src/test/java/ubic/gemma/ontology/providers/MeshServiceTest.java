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
package ubic.gemma.ontology.providers;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MeshServiceTest extends TestCase {

    public void testFind() throws Exception {
        // TEST DISABLED.
        // OntologyTerm term = MeshService.find( "Genome, Human" );
        // assertNotNull( term );
        // note this is slow the first time but is fast once the index and model are initialized. At this writing
        // mesh.owl yields > 280,000 statements in the persistent store.
        return;
    }

    public void testGetParents() throws Exception {
      //  OntologyTerm term = MeshService.find( "Thermography" );
     //   assertNotNull( term );
     //   Collection<OntologyTerm> parents = MeshService.getParents( term );
      //  assertEquals( 10, parents.size() );
    }

}
