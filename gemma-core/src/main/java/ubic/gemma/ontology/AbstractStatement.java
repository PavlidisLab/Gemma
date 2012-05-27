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

import ubic.basecode.ontology.model.CharacteristicStatement;
import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.CharacteristicProperty;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public abstract class AbstractStatement implements CharacteristicStatement {

    private OntologyProperty property;
    private OntologyTerm term;

    public AbstractStatement( OntologyProperty property, OntologyTerm term ) {
        super();
        this.property = property;
        this.term = term;
    }

    public AbstractStatement( OntologyTerm term ) {
        this.term = term;
    }

    public void addToCharacteristic( VocabCharacteristic v ) {
        if ( !v.getValueUri().equals( this.getSubject().getUri() ) ) {
            throw new IllegalArgumentException( "Cannot add " + this.getSubject() + " statement to " + v );
        }
        CharacteristicProperty p = makeProperty();
        v.getProperties().add( p );
    }

    @Override
    public OntologyProperty getProperty() {
        return property;
    }

    @Override
    public OntologyTerm getSubject() {
        return term;
    }

    public final VocabCharacteristic toCharacteristic() {
        if ( this.getProperty() == null ) throw new IllegalStateException( "Must set property first" );
        VocabCharacteristic v = initVocabCharacteristic();
        CharacteristicProperty p = makeProperty();
        v.getProperties().add( p );
        return v;
    }

    @Override
    public String toString() {
        return this.getSubject() + " " + this.getProperty() + " " + this.getObject();
    }

    protected abstract CharacteristicProperty makeProperty();

    private VocabCharacteristic initVocabCharacteristic() {
        VocabCharacteristic v = VocabCharacteristic.Factory.newInstance();

        v.setValueUri( this.getSubject().getUri() );
        v.setValue( this.getSubject().getTerm() );
        return v;
    }

}
