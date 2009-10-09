/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.genome.taxon;

import junit.framework.TestCase;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @version $Id$
 * @deprecated
 */
public class SupportedTaxaTest extends TestCase {

    public void testSupported() throws Exception {
        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Mus musculus" );
        assertTrue( SupportedTaxa.contains( t ) );
    }

    public void testNotSupported() throws Exception {
        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Freddy Fender" );
        assertTrue( !SupportedTaxa.contains( t ) );
    }

}
