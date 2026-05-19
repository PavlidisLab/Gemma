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
/*
 * Originated in baseCode (ubic.basecode.ontology.*); pulled in-tree for Gemma 2.0
 * (Phase 3 search/ontology Step 3). Ported from Jena 2.x (com.hp.hpl.jena.*)
 * to Jena 4.x (org.apache.jena.*) namespace. Configuration-related lookups
 * continue to use baseCode's ubic.basecode.util.Configuration via the
 * still-classpath baseCode JAR.
 */
package ubic.gemma.core.ontology.basecode.jena;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.GraphPersonality;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.ontology.impl.OntClassImpl;
import org.apache.jena.rdf.model.Resource;
import ubic.gemma.core.ontology.basecode.model.OntologyIndividual;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;

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
