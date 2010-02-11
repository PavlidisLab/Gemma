/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.search;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.junit.Test;

/**
 * @author paul
 * @version $Id$
 */
public class LuceneTest {

    private static Log log = LogFactory.getLog( LuceneTest.class );

    /**
     * Searching uses a ram index to deal with queries using logical operators. Though it can often be finiky.
     */
    @Test
    public void luceneRamIndexTest() {

        RAMDirectory idx = new RAMDirectory();
        Analyzer analyzer = new StandardAnalyzer();
        try {
            IndexWriter writer = new IndexWriter( idx, analyzer, true, MaxFieldLength.LIMITED );
            Document doc = new Document();
            Field f = new Field( "content", "I have a small braintest", Field.Store.YES, Field.Index.ANALYZED );
            doc.add( f );
            writer.addDocument( doc );
            writer.close();

            IndexSearcher searcher = new IndexSearcher( idx );
            TopDocCollector hc = new TopDocCollector( 1000 );

            QueryParser parser = new QueryParser( "content", analyzer );
            Query parsedQuery;
            try {
                parsedQuery = parser.parse( "braintest" );
            } catch ( ParseException e ) {
                throw new RuntimeException( "Cannot parse query: " + e.getMessage() );
            }
            searcher.search( parsedQuery, hc );

            TopDocs topDocs = hc.topDocs();

            int hitcount = topDocs.totalHits;
            assertTrue( hitcount >= 1 );

        } catch ( IOException ioe ) {
            log.warn( "unable to create ram index: " + ioe );
        }

    }

}
