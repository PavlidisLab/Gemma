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
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.model.common.auditAndSecurity.UserQuery;
import ubic.gemma.model.common.auditAndSecurity.UserQueryService;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.tasks.maintenance.IndexerTask;
import ubic.gemma.tasks.maintenance.IndexerTaskCommand;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author kelsey
 * @version $Id$
 */
public class SearchServiceTest extends BaseSpringContextTest {
    private static final String GENE_URI = "http://purl.org/commons/record/ncbi_gene/";

    private static final String SPINAL_CORD = "http://purl.org/obo/owl/FMA#FMA_7647";

    private static final String BRAIN_CAVITY = "http://purl.org/obo/owl/FMA#FMA_242395";

    // private static final String PREFRONTAL_CORTEX_URI = "http://purl.org/obo/owl/FMA#FMA_224850";
    @Autowired
    CharacteristicService characteristicService;

    @Autowired
    ExpressionExperimentService eeService;

    @Autowired
    GeneService geneService;

    @Autowired
    SearchService searchService;

    @Autowired
    OntologyService ontologyService;

    @Autowired
    IndexerTask indexerTask;

    @Autowired
    UserQueryService userQueryService;

    private ExpressionExperiment ee;
    private Gene gene;
    private VocabCharacteristic eeCharSpinalCord;
    private VocabCharacteristic eeCharGeneURI;
    private VocabCharacteristic eeCharCortexURI;

    private UserQuery thePastUserQuery;

    private UserQuery theFutureUserQuery;

    boolean setup = false;

    private String geneNcbiId;

    /**
     * @exception Exception
     */
    @Before
    public void setup() throws Exception {

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/fma.test.owl" );
        assert is != null;

        ontologyService.getFmaOntologyService().loadTermsInNameSpace( is );

        ee = this.getTestPersistentBasicExpressionExperiment();

        eeCharSpinalCord = VocabCharacteristic.Factory.newInstance();
        eeCharSpinalCord.setCategory( SPINAL_CORD );
        eeCharSpinalCord.setCategoryUri( SPINAL_CORD );
        eeCharSpinalCord.setValue( SPINAL_CORD );
        eeCharSpinalCord.setValueUri( SPINAL_CORD );
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

        gene = this.getTestPeristentGene();

        this.geneNcbiId = RandomStringUtils.randomNumeric( 8 );
        gene.setNcbiId( geneNcbiId );
        gene.setNcbiGeneId( new Integer( geneNcbiId ) );
        geneService.update( gene );

        thePastUserQuery = UserQuery.Factory.newInstance();

        Calendar calendar = Calendar.getInstance();

        calendar.set( Calendar.YEAR, 1979 );// the past
        calendar.set( Calendar.MONTH, 1 );
        calendar.set( Calendar.DAY_OF_MONTH, 1 );

        thePastUserQuery.setLastUsed( calendar.getTime() );

        SearchSettings settings = SearchSettings.Factory.newInstance();

        settings.noSearches();
        settings.setQuery( "Brain" ); // should hit 'cavity of brain'.
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics( true );

        thePastUserQuery.setSearchSettings( settings );
        thePastUserQuery.setUrl( "someUrl" );

        calendar.add( Calendar.YEAR, 2000 );// the future

        theFutureUserQuery = UserQuery.Factory.newInstance();
        theFutureUserQuery.setLastUsed( calendar.getTime() );

        SearchSettings futureSettings = SearchSettings.Factory.newInstance();

        futureSettings.noSearches();
        futureSettings.setQuery( "Brain" ); // should hit 'cavity of brain'.
        futureSettings.setSearchExperiments( true );
        futureSettings.setUseCharacteristics( true );

        theFutureUserQuery.setSearchSettings( futureSettings );
        theFutureUserQuery.setUrl( "someUrl" );

        // save to db to load later to test if the pipes are clean
        userQueryService.create( thePastUserQuery );
        userQueryService.create( theFutureUserQuery );

    }

    @After
    public void tearDown() {
        if ( gene != null ) geneService.remove( gene );
        if ( ee != null ) eeService.delete( ee );
    }

    @Test
    public void testSearchByBibRefId() {

        String id;
        if ( ee.getPrimaryPublication() == null ) {
            PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
            BibliographicReference bibref = fetcher.retrieveByHTTP( 21878914 );
            bibref = ( BibliographicReference ) persisterHelper.persist( bibref );
            ee.setPrimaryPublication( bibref );
            eeService.update( ee );
            id = "21878914";
        } else {
            id = ee.getPrimaryPublication().getPubAccession().getAccession();
        }

        log.info( "indexing ..." );

        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );

        indexerTask.setCommand( c );
        indexerTask.execute();

        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.noSearches();
        settings.setQuery( id );
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics( false );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );
        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    /**
     * Tests that general search terms are resolved to their proper ontology terms and objects tagged with those terms
     * are found, -- requires LARQ index.
     */
    @Test
    public void testGeneralSearch4Brain() {

        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.noSearches();
        settings.setQuery( "Brain" ); // should hit 'cavity of brain'.
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    /**
     * Tests that gene uris get handled correctly
     */
    @Test
    public void testGeneUriSearch() {

        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( GENE_URI + this.geneNcbiId );
        settings.setSearchGenes( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( Gene.class ) ) {
            if ( sr.getResultObject().equals( gene ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );

    }

    /**
     * Test we find EE tagged with a child term that matches the given uri.
     */
    @Test
    public void testURIChildSearch() {
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( "http://purl.org/obo/owl/FMA#FMA_83153" ); // OrganComponent of Neuraxis; superclass of
                                                                      // 'spinal cord'.
        settings.setSearchExperiments( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                return;
            }
        }
        fail( "Didn't get expected result from search" );
    }

    /**
     * Does the search engine correctly match the spinal cord URI and find objects directly tagged with that URI
     */
    @Test
    public void testURISearch() {
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( SPINAL_CORD );
        settings.setSearchExperiments( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                return;
            }
        }
        fail( "Didn't get expected result from search" );
    }

    @Test
    public void testSearchForUpdatedQueryResults() {

        // test out the dao a bit
        UserQuery userQuery = userQueryService.load( thePastUserQuery.getId() );

        Map<Class<?>, List<SearchResult>> found = this.searchService.searchForNewlyCreatedUserQueryResults( userQuery );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultObject().equals( ee ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    @Test
    public void testSearchForUpdatedQueryResultsNoResults() {
        // test out the dao a bit
        UserQuery userQuery = userQueryService.load( theFutureUserQuery.getId() );

        Map<Class<?>, List<SearchResult>> found = this.searchService.searchForNewlyCreatedUserQueryResults( userQuery );
        assertTrue( found.isEmpty() );

    }

}
