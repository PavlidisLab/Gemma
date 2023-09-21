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
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * @author Paul
 */
public class CharacteristicSortTest {

    @Test
    public final void testSortCharacteristics() {
        // does not use spring context

        List<CharacteristicValueObject> cl = new ArrayList<>();
        cl.add( new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "g", "gggg", "gggg_", null, "g", null, null ) ) );

        cl.add( new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "xused", "x", "xused", null, "x", null, null ) ) );

        // will be first
        CharacteristicValueObject a = new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "a", "a", "aused", null, "a", null, null ) );
        a.setNumTimesUsed( 3 );
        a.setAlreadyPresentInDatabase( true );
        cl.add( a );

        CharacteristicValueObject vo = new CharacteristicValueObject(
                Characteristic.Factory.newInstance( "b", "bbbb", "bbbbb", "http://bbbb", "b", null, null ) );
        vo.setNumTimesUsed( 5 );
        vo.setAlreadyPresentInDatabase( true );
        cl.add( vo );

        cl.add( new CharacteristicValueObject( Characteristic.Factory
                .newInstance( "a", "aaaa", "", "aaaa_", "http://aaaa_", "a", null ) ) );
        cl.add( new CharacteristicValueObject( Characteristic.Factory
                .newInstance( "d", "dddd", "", "dddd_", "http://dddd_", "d", null ) ) );
        cl.add( new CharacteristicValueObject( Characteristic.Factory
                .newInstance( "af", "aaaf", "", "aaaff", "http://aaaff", "af", null ) ) );

        cl.sort( OntologyServiceImpl.getCharacteristicComparator( "kkqiwe1i23u198" ) );

        assertThat( cl )
                .extracting( "valueUri", "value", "numTimesUsed" )
                .containsExactly(
                        tuple( "http://bbbb", "bbbbb", 5 ),
                        tuple( "aaaa_", "", 0 ),
                        tuple( "dddd_", "", 0 ),
                        tuple( "aaaff", "", 0 ),
                        tuple( null, "aused", 3 ),
                        tuple( null, "gggg_", 0 ),
                        tuple( null, "xused", 0 ) );
    }
}
