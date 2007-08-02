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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.ExternalDatabase;

import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Has a static method for finding out which ontologies are loaded into the system
 * and a general purpose find method that delegates to the many ontology services
 * 
 * @author pavlidis
 * @version $Id$
 * 
 * @spring.bean id="ontologyService"
 * @spring.property name="birnLexOntologyService" ref ="birnLexOntologyService"
 * @spring.property name="mgedOntologyService" ref ="mgedOntologyService"
 */
public class OntologyService {

    private static Log log = LogFactory.getLog( OntologyService.class.getName() );

    private BirnLexOntologyService birnLexOntologyService;
    private MgedOntologyService mgedOntologyService;
    
 

    /**
     * List the ontologies that are available locally.
     * 
     * @return
     */
    public static Collection<ubic.gemma.ontology.Ontology> listAvailableOntologies() {

        Collection<ubic.gemma.ontology.Ontology> ontologies = new HashSet<ubic.gemma.ontology.Ontology>();
        ModelMaker maker = OntologyLoader.getRDBMaker();
        ExtendedIterator iterator = maker.listModels();
        while ( iterator.hasNext() ) {
            String name = ( String ) iterator.next();
            ExternalDatabase database = OntologyLoader.ontologyAsExternalDatabase( name );
            ubic.gemma.ontology.Ontology o = new ubic.gemma.ontology.Ontology( database );
            ontologies.add( o );
        }
        return ontologies;

    }
    
    public Collection<OntologyTerm> findTerm(String search){
        
        Collection<OntologyTerm> terms =  birnLexOntologyService.findTerm( search );
        terms.addAll( mgedOntologyService.findTerm( search ) );
        
        return terms;
    }
    
    
    
    /**
     * @param birnLexOntologyService the birnLexOntologyService to set
     */
    public void setBirnLexOntologyService( BirnLexOntologyService birnLexOntologyService ) {
        this.birnLexOntologyService = birnLexOntologyService;
    }

    /**
     * @param mgedOntologyService the mgedOntologyService to set
     */
    public void setMgedOntologyService( MgedOntologyService mgedOntologyService ) {
        this.mgedOntologyService = mgedOntologyService;
    }

}
