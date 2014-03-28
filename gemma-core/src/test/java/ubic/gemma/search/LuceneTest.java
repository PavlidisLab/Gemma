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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
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
    public void luceneRamIndexTest() throws Exception {
        try (RAMDirectory idx = new RAMDirectory(); Analyzer analyzer = new StandardAnalyzer( Version.LUCENE_36 );) {

            IndexWriterConfig iwc = new IndexWriterConfig( Version.LUCENE_36, analyzer );
            try (IndexWriter writer = new IndexWriter( idx, iwc );) {
                Document doc = new Document();
                Field f = new Field( "content", "I have a small braintest", Field.Store.YES, Field.Index.ANALYZED );
                doc.add( f );
                writer.addDocument( doc );
                doc = new Document();
                f = new Field( "content", "I have a small braddintest", Field.Store.YES, Field.Index.ANALYZED );
                doc.add( f );
                writer.addDocument( doc );
                doc = new Document();
                f = new Field( "content", "I have a small brasaaafintest", Field.Store.YES, Field.Index.ANALYZED );
                doc.add( f );
                writer.addDocument( doc );
                doc = new Document();
                f = new Field( "content", "I have a small braidagagntest", Field.Store.YES, Field.Index.ANALYZED );
                doc.add( f );
                writer.addDocument( doc );
            }

            try (IndexReader ir = IndexReader.open( idx ); IndexSearcher searcher = new IndexSearcher( ir );) {

                TopDocsCollector<ScoreDoc> hc = TopScoreDocCollector.create( 1, true );

                QueryParser parser = new QueryParser( Version.LUCENE_36, "content", analyzer );
                Query parsedQuery;

                parsedQuery = parser.parse( "braintest" );

                searcher.search( parsedQuery, hc );

                TopDocs topDocs = hc.topDocs();

                int hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );
            }
        }
    }

    public void luceneTestB( Analyzer analyzer ) throws Exception {

        IndexWriterConfig iwc = new IndexWriterConfig( Version.LUCENE_36, analyzer );
        try (RAMDirectory idx = new RAMDirectory(); IndexWriter writer = new IndexWriter( idx, iwc );) {
            Document doc = new Document();
            Field f = new Field( "content", "Parkinson's disease", Field.Store.YES, Field.Index.ANALYZED );
            doc.add( f );
            writer.addDocument( doc );

            doc = new Document();
            f = new Field( "content", "fooo", Field.Store.YES, Field.Index.ANALYZED );
            doc.add( f );
            writer.addDocument( doc );
            log.info( doc );

            writer.close();

            try (IndexReader ir = IndexReader.open( idx ); IndexSearcher searcher = new IndexSearcher( ir );) {
                TopDocsCollector<ScoreDoc> hc = TopScoreDocCollector.create( 1, true );

                QueryParser parser = new QueryParser( Version.LUCENE_36, "content", analyzer );
                parser.setAutoGeneratePhraseQueries( true );
                parser.setEnablePositionIncrements( true );

                Query parsedQuery;

                parsedQuery = parser.parse( "Parkinson's disease" );
                log.info( parsedQuery.toString() );
                hc = TopScoreDocCollector.create( 1, true );
                searcher.search( parsedQuery, hc );
                TopDocs topDocs = hc.topDocs();
                int hitcount = topDocs.totalHits;
                assertTrue( parsedQuery.toString(), hitcount > 0 );
                log.info( searcher.doc( topDocs.scoreDocs[0].doc ).getFieldable( "content" ) );

                parsedQuery = parser.parse( "parkinson's disease" );
                log.info( parsedQuery.toString() );
                hc = TopScoreDocCollector.create( 1, true );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( parsedQuery.toString(), hitcount > 0 );

                parsedQuery = parser.parse( "\"parkinson's disease\"" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( parsedQuery.toString(), hitcount > 0 );

                parsedQuery = parser.parse( "\"parkinsons disease \"" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( parsedQuery.toString(), hitcount > 0 );

                parsedQuery = parser.parse( "\"parkinson disease \"" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( parsedQuery.toString(), hitcount > 0 );
                parsedQuery = parser.parse( "parkinsons" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );
                parsedQuery = parser.parse( "parkinson" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );

                parsedQuery = parser.parse( "parkinson*" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );
                log.info( searcher.doc( topDocs.scoreDocs[0].doc ).getFieldable( "index" ) );

                parsedQuery = parser.parse( "park*" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );

                parsedQuery = parser.parse( "parkinson's AND disease" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );

                parsedQuery = parser.parse( "'parkinson's AND disease'" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );

                parsedQuery = parser.parse( "parkinson disease" );
                hc = TopScoreDocCollector.create( 1, true );
                log.info( parsedQuery.toString() );
                searcher.search( parsedQuery, hc );
                topDocs = hc.topDocs();
                hitcount = topDocs.totalHits;
                assertTrue( hitcount > 0 );
                log.info( searcher.doc( topDocs.scoreDocs[0].doc ).getFieldable( "content" ) );

                // parsedQuery = parser.parse( "parknson" );
                // hc = TopScoreDocCollector.create( 1, true );
                // log.info( parsedQuery.toString() );
                // searcher.search( parsedQuery, hc );
                // topDocs = hc.topDocs();
                // hitcount = topDocs.totalHits;
                // assertTrue( hitcount > 0 );
                // log.info( searcher.doc( topDocs.scoreDocs[0].doc ).getFieldable( "content" ) );
            }
        }
    }

    // @Test
    // public void testSnowBallAnalyzer() throws Exception {
    // works, but deprecated
    // Analyzer analyzer = new SnowballAnalyzer( Version.LUCENE_36, "English" );
    // luceneTestB( analyzer );
    // }

    // @Test
    // public void testStandardAnalyzer() throws Exception {
    // // this will fail.
    // Analyzer analyzer = new StandardAnalyzer( Version.LUCENE_36 );
    // luceneTestB( analyzer );
    // }

    @Test
    public void testEnglishAnalyzer() throws Exception {
        try (Analyzer analyzer = new EnglishAnalyzer( Version.LUCENE_36 );) {
            luceneTestB( analyzer );
        }
    }
}
