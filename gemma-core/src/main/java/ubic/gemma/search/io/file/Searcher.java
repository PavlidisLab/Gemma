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

import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Hits;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import java.io.File;
import java.util.Date;

/**
 * Searches the indexed directory tree.
 * 
 * @author keshav
 * @author Erik Hatcher
 * @version $Id$
 */
public class Searcher {

    /**
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception {
        if ( args.length != 2 ) {
            throw new Exception( "Usage: java " + Searcher.class.getName() + " <index dir> <query>" );
        }

        File indexDir = new File( args[0] );
        String q = args[1];

        if ( !indexDir.exists() || !indexDir.isDirectory() ) {
            throw new Exception( indexDir + " does not exist or is not a directory." );
        }

        search( indexDir, q );
    }

    /**
     * @param indexDir
     * @param q
     * @throws Exception
     */
    public static void search( File indexDir, String q ) throws Exception {
        Directory fsDir = FSDirectory.getDirectory( indexDir, false );
        IndexSearcher is = new IndexSearcher( fsDir );

        Query query = QueryParser.parse( q, "contents", new StandardAnalyzer() );
        long start = new Date().getTime();
        Hits hits = is.search( query );
        long end = new Date().getTime();

        System.err.println( "Found " + hits.length() + " document(s) (in " + ( end - start )
                + " milliseconds) that matched query '" + q + "':" );

        for ( int i = 0; i < hits.length(); i++ ) {
            Document doc = hits.doc( i );
            System.out.println( doc.get( "filename" ) );
        }
    }
}
