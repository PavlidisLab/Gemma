/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.core.ontology;

import com.hp.hpl.jena.ontology.Ontology;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;

/**
 * Service to load an OWL-formatted ontology into the system. The first time the ontology is accessed it is persisted
 * into the local system (can be slow) and this version is used for future accesses.
 *
 * @author paul
 */
public class OntologyUtils {

    protected static void jenaOntToExternalDatabase( ExternalDatabase ontology, Ontology ont ) {
        StmtIterator iterator = ont.listProperties();
        ontology.setType( DatabaseType.ONTOLOGY );

        while ( iterator.hasNext() ) {
            Statement statement = iterator.nextStatement();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            if ( predicate.getLocalName().equals( "title" ) ) {
                ontology.setName( ubic.basecode.ontology.OntologyUtil.asString( object ) );
            } else if ( predicate.getLocalName().equals( "description" ) ) {
                ontology.setDescription( ubic.basecode.ontology.OntologyUtil.asString( object ) );
            } else if ( predicate.getLocalName().equals( "definition" ) ) {
                ontology.setDescription( ubic.basecode.ontology.OntologyUtil.asString( object ) );
            }
        }
    }

    /**
     * Ontology must be in the persistent store for this to work.
     *
     * @param url url
     * @return external db
     */
    protected static ExternalDatabase ontologyAsExternalDatabase( String url ) {
        ExternalDatabase ontology = ExternalDatabase.Factory.newInstance();
        ontology.setType( DatabaseType.ONTOLOGY );
        ontology.setWebUri( url );
        return ontology;
    }

}
