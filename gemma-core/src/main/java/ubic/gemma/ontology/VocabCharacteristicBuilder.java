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
package ubic.gemma.ontology;

import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class VocabCharacteristicBuilder {

    public static VocabCharacteristic addStatement( VocabCharacteristic toAddTo, CharacteristicStatement<?> s ) {
        s.addToCharacteristic( toAddTo );
        return toAddTo;
    }

    /**
     * Generate an instance of a VocabCharacteristic for a given term. You can use this to create VCs when there are no
     * properties to attach.
     * 
     * @param term
     * @return
     */
    public static VocabCharacteristic makeInstance( OntologyTerm term ) {
        VocabCharacteristic v = VocabCharacteristic.Factory.newInstance();
        v.setValue( term.getTerm() );
        v.setValueUri( term.getUri() );
        return v;
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
