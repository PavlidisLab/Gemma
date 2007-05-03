/*
 * The Gemma-ONT_REV project
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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologySearch {

    private static Log log = LogFactory.getLog( OntologySearch.class.getName() );

    public static void performQuery( Model model, IndexLARQ index, String queryString ) {
        // Make globally available
        LARQ.setDefaultIndex( index );

        Query query = QueryFactory.create( queryString );
        query.serialize( System.out );
        System.out.println();

        QueryExecution qExec = QueryExecutionFactory.create( query, model );
        // LARQ.setDefaultIndex(qExec.getContext(), index) ;
        ResultSetFormatter.out( System.out, qExec.execSelect(), query );
        qExec.close();
    }

    public static Collection<OntologyTerm> matchClasses( Model model, IndexLARQ index, String string ) {

        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        NodeIterator iterator = index.searchModelByIndex( model, string );
        while ( iterator.hasNext() ) {
            Resource r = ( Resource ) iterator.next();
            if ( r.isURIResource() ) {
                log.info( "found: " + r );
                
            }

            // OntClass impl = new OntClassImpl( r.asNode(), new EnhGraph( model.getGraph(), new GraphPersonality() ) );
            //
            // OntologyTermImpl impl2 = new OntologyTermImpl( impl, null );
            // results.add( impl2 );
            // log.info( impl2 );
        }
        return results;
        //
        // String query = "PREFIX pf: <http://jena.hpl.hp.com/ARQ/property#> SELECT * { ?lit pf:textMatch '" + string
        // + "' . ?lit ?p ?o }";
        // performQuery( model, index, query );

    }
}
