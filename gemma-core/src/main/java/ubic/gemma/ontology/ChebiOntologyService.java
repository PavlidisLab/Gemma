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

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Loads the CHEBI Ontology at startup in its own thread. Controlled in build.properties by load.chebiOntology = false
 * (defaults to true).
 * 
 * @author klc
 * @spring.bean id="chebiOntologyService"
 */
public class ChebiOntologyService extends AbstractOntologyService {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
     */
    @Override
    protected String getOntologyName() {

        return "chebiOntology";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyUrl()
     */
    @Override
    protected String getOntologyUrl() {
        return "http://www.berkeleybop.org/ontologies/owl/CHEBI.owl";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.ontology.AbstractOntologyService#loadModel(java.lang.String,
     *      com.hp.hpl.jena.ontology.OntModelSpec)
     */
    @Override
    protected OntModel loadModel( String url ) {
        return OntologyLoader.loadMemoryModel( url );
    }

}
