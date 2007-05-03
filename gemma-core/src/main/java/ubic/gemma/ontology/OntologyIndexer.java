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

import com.hp.hpl.jena.query.larq.IndexBuilderSubject;
import com.hp.hpl.jena.query.larq.IndexLARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyIndexer {

    private static Log log = LogFactory.getLog( OntologyIndexer.class.getName() );

    public static IndexLARQ indexOntology( String url ) {
        IndexLARQ index = buildSubjectIndex( url );
        return index;
    }

    static IndexLARQ buildSubjectIndex( String datafile ) {
        // ---- Read and index all literal strings.
        // IndexBuilderString larqBuilder = new IndexBuilderString(/* file */);

        // ---- Read and index all subjects

        Model model = ModelFactory.createDefaultModel();

        IndexBuilderSubject larqSubjectBuilder = new IndexBuilderSubject( /* file */);
        model.register( larqSubjectBuilder );
        FileManager.get().readModel( model, datafile );

        larqSubjectBuilder.closeForWriting();
        model.unregister( larqSubjectBuilder );

        IndexLARQ index = larqSubjectBuilder.getIndex();

        return index;
    }

}
