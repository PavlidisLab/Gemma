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

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * TODO Document Me
 * 
 * @author Paul
 * @version $Id$
 */
public class CharacteristicUtils {

    public static int compare( Characteristic a, Characteristic b ) {

        String ca;
        String cb;

        if ( a instanceof VocabCharacteristic ) {
            ca = OntologyTools.getLabel( ( VocabCharacteristic ) a );
        } else {  
            ca =  a .getValue();
        }

        if ( b instanceof VocabCharacteristic ) {
            cb = OntologyTools.getLabel( ( VocabCharacteristic ) b );
        } else {
            cb =  b .getValue();
        }
        return ca.compareTo( cb );

    }
    
  
}
