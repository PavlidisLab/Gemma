/*
 * The Gemma project Copyright (c) 2010 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package ubic.gemma.search;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.providers.FMAOntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author kelsey
 * @version $Id
 */
public class SearchServiceTest extends BaseSpringContextTest {
    private static final String GENE_URI = "http://purl.org/commons/record/ncbi_gene/20655";

    private static final String BRAIN_STEM = "http://purl.org/obo/owl/FMA#FMA_7647";

    private static final String BRAIN_CAVITY = "http://purl.org/obo/owl/FMA#FMA_242395";

    // private static final String PREFRONTAL_CORTEX_URI = "http://purl.org/obo/owl/FMA#FMA_224850";
    @Autowired
    CharacteristicService characteristicService;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    SearchService searchService;

    @Autowired
    OntologyService ontologyService;

    private ExpressionExperiment ee;
    private VocabCharacteristic eeCharSpinalCord;
    private VocabCharacteristic eeCharGeneURI;
    private VocabCharacteristic eeCharCortexURI;

    /**
     * Does the search engine correctly match the spinal cord URI and find objects directly tagged with that URI
     */
    @Test
    public void testURISearch() {
        FMAOntologyService fmaOntologyService = ontologyService.getFmaOntologyService();
        fmaOntologyService.init( true );
        waitForOntology( fmaOntologyService );

        SearchSettings settings = new SearchSettings();
        settings.setQuery( BRAIN_STEM );
        settings.setSearchExperiments( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        boolean f = false;
        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                f = true;
            }
        }

        assertTrue( f );
    }

    /**
     * Tests that gene uris get handled correctly
     */
    @Test
    public void testGeneUriSearch() {

        SearchSettings settings = new SearchSettings();
        settings.setQuery( GENE_URI );
        settings.setSearchExperiments( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );
        boolean f = false;
        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                f = true;
            }
        }

        assertTrue( f );

    }

    /**
     * Tests that general search terms are resolved to their proper ontology terms and objects tagged with those terms
     * are found.
     */
    @Test
    public void testGeneralSearch4Brain() {
        FMAOntologyService fmaOntologyService = ontologyService.getFmaOntologyService();
        fmaOntologyService.init( true );

        waitForOntology( fmaOntologyService );

        SearchSettings settings = new SearchSettings();
        settings.setQuery( "Brain" );
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        boolean f = false;
        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                f = true;
            }
        }

        assertTrue( f );

    }

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

            QueryParser parser = new QueryParser( "", analyzer );
            Query parsedQuery;
            try {
                parsedQuery = parser.parse( "braintest" );
            } catch ( ParseException e ) {
                throw new RuntimeException( "Cannot parse query: " + e.getMessage() );
            }
            //searcher.search( parsedQuery, hc );

             TopDocs topDocs = searcher.search( parsedQuery, 10 );//hc.topDocs();

            int hitcount = topDocs.totalHits;
            assertTrue( hitcount >= 1 );

        } catch ( IOException ioe ) {
            log.warn( "unable to create ram index: " + ioe );
        }

    }

    // Pass in the given ontology you want to wait to finish loading.
    private void waitForOntology( AbstractOntologyService os ) {
        while ( !os.isOntologyLoaded() ) {
            try {
                Thread.sleep( 5000 );
            } catch ( InterruptedException ie ) {
                log.warn( ie );
            }
            log.info( "Waiting for FMA Ontology to load" );
        }
    }

    /**
     * @exception Exception
     */
    @Before
    public void setup() throws Exception {

        // In case the fma ontology isn't set to be initizlized the the Gemma.properties file
        ontologyService.getFmaOntologyService().init( true );

        ee = this.getTestPersistentBasicExpressionExperiment();

        eeCharSpinalCord = VocabCharacteristic.Factory.newInstance();
        eeCharSpinalCord.setCategory( BRAIN_STEM );
        eeCharSpinalCord.setCategoryUri( BRAIN_STEM );
        eeCharSpinalCord.setValue( BRAIN_STEM );
        eeCharSpinalCord.setValueUri( BRAIN_STEM );
        characteristicService.create( eeCharSpinalCord );

        eeCharGeneURI = VocabCharacteristic.Factory.newInstance();
        eeCharGeneURI.setCategory( GENE_URI );
        eeCharGeneURI.setCategoryUri( GENE_URI );
        eeCharGeneURI.setValue( GENE_URI );
        eeCharGeneURI.setValueUri( GENE_URI );
        characteristicService.create( eeCharGeneURI );

        eeCharCortexURI = VocabCharacteristic.Factory.newInstance();
        eeCharCortexURI.setCategory( BRAIN_CAVITY );
        eeCharCortexURI.setCategoryUri( BRAIN_CAVITY );
        eeCharCortexURI.setValue( BRAIN_CAVITY );
        eeCharCortexURI.setValueUri( BRAIN_CAVITY );
        characteristicService.create( eeCharCortexURI );

        Collection<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( eeCharSpinalCord );
        chars.add( eeCharGeneURI );
        chars.add( eeCharCortexURI );
        ee.setCharacteristics( chars );
        eeService.update( ee );

    }

}
