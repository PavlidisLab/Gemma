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

import ubic.basecode.ontology.model.ChainedStatement;
import ubic.basecode.ontology.model.ChainedStatementObject;
import ubic.basecode.ontology.model.OntologyProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.common.description.Property;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ChainedStatementImpl extends AbstractStatement implements ChainedStatement {

    private ChainedStatementObject object;

    public ChainedStatementImpl( OntologyTerm term ) {
        super( term );
    }

    public ChainedStatementImpl( OntologyTerm term, OntologyProperty property ) {
        super( property, term );
    }

    public ChainedStatementImpl( OntologyTerm term, OntologyProperty property, ChainedStatementObject chained ) {
        super( property, term );
        this.object = chained;
    }

    @Override
    public ChainedStatementObject getObject() {
        return object;
    }

    @Override
    public void setObject( ChainedStatementObject object ) {
        this.object = object;
    }

    @Override
    protected Property makeProperty() {

        Property p = Property.Factory.newInstance();
        p.setValue( this.getProperty().getLabel() );
        p.setValueUri( this.getProperty().getUri() );
        return p;
    }

}
