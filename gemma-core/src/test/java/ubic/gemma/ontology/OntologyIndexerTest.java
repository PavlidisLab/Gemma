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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.query.larq.LARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import junit.framework.TestCase;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyIndexerTest extends TestCase {

    private static Log log = LogFactory.getLog( OntologyIndexerTest.class.getName() );

    public final void testIndexing() throws Exception {
        String url = "http://www.berkeleybop.org/ontologies/obo-all/mged/mged.owl";
        OntModel model = OntologyLoader.getMemoryModel( url, OntModelSpec.OWL_MEM_RDFS_INF );
        IndexLARQ index = OntologyIndexer.indexOntology( url );
        OntologySearch.matchClasses( model, index, "bedding" );
    }
    
    
    public final void testIndexingDbModel() throws Exception {
        String url = "http://www.berkeleybop.org/ontologies/obo-all/mged/mged.owl";
        OntModel model = OntologyLoader.getMemoryModel( url, OntModelSpec.OWL_MEM_RDFS_INF );
        IndexLARQ index = OntologyIndexer.indexOntology( url );
        OntologySearch.matchClasses( model, index, "bedding" );
    }
}
