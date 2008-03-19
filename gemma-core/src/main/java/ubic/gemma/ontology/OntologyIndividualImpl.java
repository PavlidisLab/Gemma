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

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.enhanced.EnhGraph;
import com.hp.hpl.jena.enhanced.GraphPersonality;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.impl.OntClassImpl;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyIndividualImpl extends AbstractOntologyResource implements OntologyIndividual {

        private static final long serialVersionUID = -6164561945940667693L;
    
    private Individual ind;
    private ExternalDatabase sourceOntology;
    private String uri;

    public OntologyIndividualImpl( Individual ind, ExternalDatabase sourceOntology ) {
        this.ind = ind;
        this.sourceOntology = sourceOntology;
        this.uri = ind.getURI();
    }

    @Override
    public ExternalDatabase getSourceOntology() {
        return sourceOntology;
    }

    @Override
    public String getUri() {
        return uri;
    }

    public String getLabel() {
        return this.toString();
    }

    @Override
    public String toString() {
        String label = ind.getLabel( null );
        if ( label == null ) label = ind.getLocalName();
        if ( label == null ) label = ind.getURI();
        return label;
    }

    public OntologyTerm getInstanceOf() {
        Resource type = ind.getRDFType();

        OntClass cl = null;
        EnhGraph g = new EnhGraph( type.getModel().getGraph(), new GraphPersonality() );
        if ( OntClassImpl.factory.canWrap( type.asNode(), g ) ) {
            cl = new OntClassImpl( type.asNode(), g );
        } else {
            throw new IllegalStateException( "sorry, can't handle that of instance" );
        }

        return new OntologyTermImpl( cl, sourceOntology );
    }
}
