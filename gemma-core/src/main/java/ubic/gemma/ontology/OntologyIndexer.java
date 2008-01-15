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

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.larq.IndexBuilderSubject;
import com.hp.hpl.jena.query.larq.IndexLARQ;

/**
 * @author pavlidis
 * @version $Id$
 */
public class OntologyIndexer {

    /**
     * Location under gemma.appdata.home where indexes will be stored
     */
    private static final String INDEX_DIR = "compass";

    private static Log log = LogFactory.getLog( OntologyIndexer.class.getName() );

    /**
     * Loads or creates an index from an existing OntModel. Any existing index will loaded.
     * 
     * @param url
     * @param name
     * @param model
     * @return
     */
    public static IndexLARQ indexOntology( String name, OntModel model ) {
        try {
            return getSubjectIndex( name );
        } catch ( Exception e ) {
            log.info( "Error loading index from disk, re-indexing" );
            return index( name, model );
        }
    }

    public static void eraseIndex( String name ) {
        File indexdir = getIndexPath( name );
        for ( File f : indexdir.listFiles() ) {
            f.delete();
        }
    }

    /**
     * Create an on-disk index from an existing OntModel. Any existing index will be overwritten.
     * 
     * @param datafile or uri
     * @param name used to refer to this index later
     * @param model
     * @return
     */
    private static IndexLARQ index( String name, OntModel model ) {

        File indexdir = getIndexPath( name );

        IndexBuilderSubject larqSubjectBuilder = new IndexBuilderSubject( indexdir );

        // -- Create an index based on existing statements
        // TODO: this needs to be refactored.
        log.info( "making selector" );
        larqSubjectBuilder.indexStatements( model.listStatements( new IndexerSelector() ) );
        // -- Finish indexing
        larqSubjectBuilder.closeForWriting();
        // -- Create the access index
        IndexLARQ index = larqSubjectBuilder.getIndex();

        return index;
    }

    /**
     * @param name
     * @return
     */
    public static IndexLARQ getSubjectIndex( String name ) {
        log.info( "Loading index: " + name );
        File indexdir = getIndexPath( name );
        try {
            FSDirectory directory = FSDirectory.getDirectory( indexdir );
            if ( IndexReader.indexExists( directory ) ) {
                IndexReader reader = IndexReader.open( directory );
                return new IndexLARQ( reader );
            } else {
                throw new IllegalArgumentException( "No index with name " + name );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param name
     * @return
     */
    private static File getIndexPath( String name ) {
        String path = ConfigUtils.getString( "gemma.appdata.home" ) + File.separator + INDEX_DIR + File.separator
                + "gemma" + File.separator + "ontology" + File.separator + name;

        File indexdir = new File( path );
        return indexdir;
    }

}
