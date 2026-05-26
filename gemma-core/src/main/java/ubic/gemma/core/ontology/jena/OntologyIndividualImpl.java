/*
 * The baseCode project
 *
 * Copyright (c) 2007-2019 University of British Columbia
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
package ubic.gemma.core.ontology.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.GraphPersonality;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.impl.OntClassImpl;
import org.apache.jena.rdf.model.Resource;
import ubic.gemma.core.ontology.model.OntologyIndividual;
import ubic.gemma.core.ontology.model.OntologyTerm;

import java.util.Set;

/**
 * @author pavlidis
 */
class OntologyIndividualImpl extends AbstractOntologyResource implements OntologyIndividual {

    private final Individual ind;
    private final Set<Restriction> additionalRestrictions;

    public OntologyIndividualImpl( Individual ind, Set<Restriction> additionalRestrictions ) {
        super( ind );
        this.ind = ind;
        this.additionalRestrictions = additionalRestrictions;
    }

    @Override
    public OntologyTerm getInstanceOf() {
        Resource type = ind.getRDFType();

        OntClass cl;
        EnhGraph g = new EnhGraph( type.getModel().getGraph(), new GraphPersonality() );
        if ( OntClassImpl.factory.canWrap( type.asNode(), g ) ) {
            cl = new OntClassImpl( type.asNode(), g );
        } else {
            throw new IllegalStateException( "sorry, can't handle that of instance" );
        }

        return new OntologyTermImpl( cl, additionalRestrictions );
    }
}
