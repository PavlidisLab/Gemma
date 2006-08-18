/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.search.io.file;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;

/**
 * Indexes .txt files in the directory tree.
 * 
 * @author keshav
 * @author Erik Hatcher
 * @version $Id$
 */
public class Indexer {
    private static Log log = LogFactory.getLog( Indexer.class );

    // FIXME - this is really a file indexer ... create an Indexer interface and implement with FileIndexer,
    // DataSourceIndexer, etc.

    /**
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception {
        if ( args.length != 2 ) {
            throw new Exception( "Usage: java " + Indexer.class.getName() + " <index dir> <data dir>" );
        }
        File indexDir = new File( args[0] );
        File dataDir = new File( args[1] );

        long start = new Date().getTime();
        int numIndexed = index( indexDir, dataDir );
        long end = new Date().getTime();

        log.info( "Indexing " + numIndexed + " files took " + ( end - start ) + " milliseconds" );
    }

    /**
     * @param indexDir
     * @param dataDir
     * @return
     * @throws IOException
     */
    public static int index( File indexDir, File dataDir ) throws IOException {

        if ( !dataDir.exists() || !dataDir.isDirectory() ) {
            throw new IOException( dataDir + " does not exist or is not a directory" );
        }

        IndexWriter writer = new IndexWriter( indexDir, new StandardAnalyzer(), true );
        writer.setUseCompoundFile( false );

        indexDirectory( writer, dataDir );

        int numIndexed = writer.docCount();
        writer.optimize();
        writer.close();
        return numIndexed;
    }

    /**
     * @param writer
     * @param dir
     * @throws IOException
     */
    private static void indexDirectory( IndexWriter writer, File dir ) throws IOException {

        File[] files = dir.listFiles();

        for ( int i = 0; i < files.length; i++ ) {
            File f = files[i];
            if ( f.isDirectory() ) {
                indexDirectory( writer, f ); // recurse
            } else if ( f.getName().endsWith( ".txt" ) ) {
                indexFile( writer, f );
            }
        }
    }

    /**
     * @param writer
     * @param f
     * @throws IOException
     */
    private static void indexFile( IndexWriter writer, File f ) throws IOException {

        if ( f.isHidden() || !f.exists() || !f.canRead() ) {
            return;
        }

        log.info( "Indexing " + f.getCanonicalPath() );

        Document doc = new Document();
        doc.add( Field.Text( "contents", new FileReader( f ) ) );
        doc.add( Field.Keyword( "filename", f.getCanonicalPath() ) );
        writer.addDocument( doc );
    }
}
