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
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

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
        List<CharacteristicValueObject> cl = new ArrayList<>();
        cl.add( new CharacteristicValueObject( Characteristic.Factory.newInstance( "g", "gggg", null, null, "gggg_",
                "g", null, null ) ) );

        cl.add( new CharacteristicValueObject( Characteristic.Factory.newInstance( "xused", "x", null, null, "xused",
                "x", null, null ) ) );

        // will be first
        CharacteristicValueObject a = new CharacteristicValueObject( Characteristic.Factory.newInstance( "a", "a",
                null, null, "aused", "a", null, null ) );
        a.setNumTimesUsed( 3 );
        a.setAlreadyPresentInDatabase( true );
        cl.add( a );

        CharacteristicValueObject vo = new CharacteristicValueObject( VocabCharacteristic.Factory.newInstance( "b",
                "bbbb", null, null, "bbbbb", "http://bbbb", "b", null, null ) );
        vo.setNumTimesUsed( 5 );
        vo.setAlreadyPresentInDatabase( true );
        cl.add( vo );

        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory.newInstance( "a", "aaaa", null, null,
                "aaaa_", "http://aaaa_", "a", null, null ) ) );
        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory.newInstance( "d", "dddd", null, null,
                "dddd_", "http://dddd_", "d", null, null ) ) );
        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory.newInstance( "af", "aaaf", null, null,
                "aaaff", "http://aaaff", "af", null, null ) ) );

        ( ( OntologyServiceImpl ) os ).sort( cl );

        assertEquals( "bbbbb", cl.get( 0 ).getValue() );

        // assertEquals( "x", cl.get( 2 ).getValue() );
        // assertEquals( "aaaa", cl.get( 3 ).getValue() );
        // assertEquals( "aaaf", cl.get( 4 ).getValue() );
        //
        // assertEquals( "d", cl.get( 5 ).getValue() );
        // assertEquals( "gggg", cl.get( 6 ).getValue() );

    }
}
