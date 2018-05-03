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
package ubic.gemma.core.ontology;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.testing.BaseSpringContextTest;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Paul
 */
public class CharacteristicSortTest extends BaseSpringContextTest {

    @Autowired
    OntologyService ontologyService;

    @Test
    public final void testSortCharacteristics() {
        // does not use spring context

        List<CharacteristicValueObject> cl = new ArrayList<>();
        cl.add( new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "g", "gggg", "gggg_", "g", null, null ) ) );

        cl.add( new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "xused", "x", "xused", "x", null, null ) ) );

        // will be first
        CharacteristicValueObject a = new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "a", "a", "aused", "a", null, null ) );
        a.setNumTimesUsed( 3 );
        a.setAlreadyPresentInDatabase( true );
        cl.add( a );

        CharacteristicValueObject vo = new CharacteristicValueObject(
                VocabCharacteristic.Factory.newInstance( "b", "bbbb", "bbbbb", "http://bbbb", "b", null, null ) );
        vo.setNumTimesUsed( 5 );
        vo.setAlreadyPresentInDatabase( true );
        cl.add( vo );

        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory
                .newInstance( "a", "aaaa", null, "aaaa_", "http://aaaa_", "a", null ) ) );
        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory
                .newInstance( "d", "dddd", null, "dddd_", "http://dddd_", "d", null ) ) );
        cl.add( new CharacteristicValueObject( VocabCharacteristic.Factory
                .newInstance( "af", "aaaf", null, "aaaff", "http://aaaff", "af", null ) ) );

        ontologyService.sort( cl );

        assertEquals( "bbbbb", cl.get( 0 ).getValue() );

        // assertEquals( "x", cl.get( 2 ).getValue() );
        // assertEquals( "aaaa", cl.get( 3 ).getValue() );
        // assertEquals( "aaaf", cl.get( 4 ).getValue() );
        //
        // assertEquals( "d", cl.get( 5 ).getValue() );
        // assertEquals( "gggg", cl.get( 6 ).getValue() );

    }
}
