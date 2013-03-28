/*
 * The Gemma project
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
package ubic.gemma.ontology;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ubic.gemma.model.common.description.Characteristic;

/**
 * @author Paul
 * @version $Id$
 */
public class CharacteristicSortTest {

    /**
     * @throws Exception
     */
    @Test
    public final void testSortCharacteristics() throws Exception {
        // does not use spring context
        OntologyService os = new OntologyServiceImpl();
        List<Characteristic> cl = new ArrayList<Characteristic>();
        cl.add( Characteristic.Factory.newInstance( "g", "gggg", null, null, "gggg_", "g", null ) );

        cl.add( Characteristic.Factory.newInstance( "xused", OntologyServiceImpl.USED + "x", null, null, "xused", "x",
                null ) );

        // will be first
        cl.add( Characteristic.Factory
                .newInstance( "a", OntologyServiceImpl.USED + "a", null, null, "aused", "a", null ) );

        cl.add( Characteristic.Factory.newInstance( "b", "bbbb", null, null, "bbbbb", "b", null ) );
        cl.add( Characteristic.Factory.newInstance( "a", "aaaa", null, null, "aaaa_", "a", null ) );

        cl.add( Characteristic.Factory.newInstance( "d", "dddd", null, null, "dddd_", "d", null ) );
        cl.add( Characteristic.Factory.newInstance( "af", "aaaf", null, null, "aaaff", "af", null ) );

        os.sort( cl );

        assertEquals( OntologyServiceImpl.USED + "a", cl.get( 0 ).getDescription() );
        assertEquals( OntologyServiceImpl.USED + "x", cl.get( 1 ).getDescription() );
        assertEquals( "aaaa", cl.get( 2 ).getDescription() );
        assertEquals( "aaaf", cl.get( 3 ).getDescription() );
        assertEquals( "bbbb", cl.get( 4 ).getDescription() );
        assertEquals( "d", cl.get( 5 ).getName() );
        assertEquals( "gggg", cl.get( 6 ).getDescription() );

    }

}
