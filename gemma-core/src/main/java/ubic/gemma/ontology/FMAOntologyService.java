/*
 * The Gemma21 project
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

import java.io.IOException;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;



/**
 * Holds a  copy of the FMA Ontology on disk. This gets loaded on startup.
 * 
 * @author klc
 * @version $Id: FMAOntologyService.java
 * @spring.bean id="fmaOntologyService"
 */

public class FMAOntologyService extends AbstractOntologyService {

    @Override
    protected String getOntologyUrl() {
       return "http://www.berkeleybop.org/ontologies/obo-all/fma_lite/fma_lite.owl";
    }

    @Override
    protected OntModel loadModel( String url, OntModelSpec spec ) throws IOException {
        return OntologyLoader.loadPersistentModel( url, false );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.ontology.AbstractOntologyService#getOntologyName()
     */
    @Override
    protected String getOntologyName() {
        return "fmaOntology";
    }
    
    

}
