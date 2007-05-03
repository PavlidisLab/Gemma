/*
 * The Gemma-ONT_REV project
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

import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class VocabCharacteristicBuilder {

    public static VocabCharacteristic addStatement( VocabCharacteristic toAddTo, CharacteristicStatement s ) {
        s.addToCharacteristic( toAddTo );
        return toAddTo;
    }

    // /**
    // * @param statements
    // * @return
    // */
    // public static VocabCharacteristic build( Collection<CharacteristicStatement> statements ) {
    // VocabCharacteristic v = null;
    // for ( CharacteristicStatement statement : statements ) {
    // if ( v == null ) {
    // v = statement.toCharacteristic();
    // } else {
    // statement.addToCharacteristic( v );
    // }
    // }
    // return v;
    //
    // }
}
