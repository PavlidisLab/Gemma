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

import ubic.basecode.ontology.model.InstanceStatement;
import ubic.basecode.ontology.model.OntologyIndividual;
import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.CharacteristicProperty;
import ubic.gemma.model.common.description.Property;
import ubic.gemma.model.common.description.VocabCharacteristic;

/**
 * @author pavlidis
 * @version $Id$
 */
public class InstanceStatementImpl extends AbstractStatement implements InstanceStatement {

    private OntologyIndividual individual;

    public InstanceStatementImpl( OntologyTerm term, OntologyProperty property, OntologyIndividual individual ) {
        super( property, term );
        this.individual = individual;
    }

    @Override
    public OntologyIndividual getObject() {
        return individual;
    }

    @Override
    public CharacteristicProperty makeProperty() {
        Property p = Property.Factory.newInstance();
        p.setValueUri( this.getProperty().getUri() );
        p.setValue( this.getProperty().getLabel() );
        VocabCharacteristic o = VocabCharacteristic.Factory.newInstance();
        o.setValueUri( individual.getUri() );
        o.setValue( individual.getLabel() );
        p.setObject( o );
        return p;
    }

}
