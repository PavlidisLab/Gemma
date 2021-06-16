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

package ubic.gemma.core.search;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.tasks.maintenance.IndexerTask;
import ubic.gemma.core.tasks.maintenance.IndexerTaskCommand;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author kelsey
 */
public class SearchServiceTest extends BaseSpringContextTest {
    private static final String GENE_URI = "http://purl.org/commons/record/ncbi_gene/";
    private static final String SPINAL_CORD = "http://purl.obolibrary.org/obo/FMA_7647";
    private static final String BRAIN_CAVITY = "http://purl.obolibrary.org/obo/FMA_242395";

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private IndexerTask indexerTask;

    private ExpressionExperiment ee;
    private Gene gene;

    private String geneNcbiId;

    /**
     * This is not configured as a regular test fixture on purpose, it is called explicitly by tests.
     * 
     * @throws Exception
     */
    public void setup() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/fma.test.owl" )) {
            assert is != null;

            // this abuses the service as our example is a legacy FMA test (not uberon), but it doesn't matter since we're loading from a file anyway.
            // this will fail if the loading of uberon is enabled - it will collide.
            ontologyService.getUberonService().loadTermsInNameSpace( is, true );
        }
        ee = this.getTestPersistentBasicExpressionExperiment();

        Characteristic eeCharSpinalCord = Characteristic.Factory.newInstance();
        eeCharSpinalCord.setCategory( SearchServiceTest.SPINAL_CORD );
        eeCharSpinalCord.setCategoryUri( SearchServiceTest.SPINAL_CORD );
        eeCharSpinalCord.setValue( "spinal cord" );
        eeCharSpinalCord.setValueUri( SearchServiceTest.SPINAL_CORD );
        characteristicService.create( eeCharSpinalCord );

        Characteristic eeCharGeneURI = Characteristic.Factory.newInstance();
        eeCharGeneURI.setCategory( SearchServiceTest.GENE_URI );
        eeCharGeneURI.setCategoryUri( SearchServiceTest.GENE_URI );
        eeCharGeneURI.setValue( SearchServiceTest.GENE_URI );
        eeCharGeneURI.setValueUri( SearchServiceTest.GENE_URI );
        characteristicService.create( eeCharGeneURI );

        Characteristic eeCharCortexURI = Characteristic.Factory.newInstance();
        eeCharCortexURI.setCategory( SearchServiceTest.BRAIN_CAVITY );
        eeCharCortexURI.setCategoryUri( SearchServiceTest.BRAIN_CAVITY );
        eeCharCortexURI.setValue( "cavity of brain" );
        eeCharCortexURI.setValueUri( SearchServiceTest.BRAIN_CAVITY );
        characteristicService.create( eeCharCortexURI );

        Collection<Characteristic> chars = new HashSet<>();
        chars.add( eeCharSpinalCord );
        chars.add( eeCharGeneURI );
        chars.add( eeCharCortexURI );
        ee.setCharacteristics( chars );
        eeService.update( ee );

        gene = this.getTestPersistentGene();

        this.geneNcbiId = RandomStringUtils.randomNumeric( 8 );
        gene.setNcbiGeneId( new Integer( geneNcbiId ) );
        geneService.update( gene );

    }

    public void tearDown() {
        try {
            if ( gene != null )
                geneService.remove( gene );
            if ( ee != null )
                eeService.remove( ee );
        } catch ( Exception e ) {
            // no big deal.
        }
    }

    /**
     * Tests that general search terms are resolved to their proper ontology terms and objects tagged with those terms
     * are found, -- requires LARQ index.
     */
    @Test
    public void testGeneralSearch4Brain() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.noSearches();
        settings.setQuery( "Brain" ); // should hit 'cavity of brain'.
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics( true );
        settings.setUseDatabase( false );
        settings.setUseIndices( false );

        Collection<OntologyTerm> ontologyhits = ontologyService.findTerms( "brain" );
        assertTrue( !ontologyhits.isEmpty() ); // making sure this isn't a problem, rather than the search per se.

        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        List<SearchResult> eer = found.get( ExpressionExperiment.class );
        assertTrue( !eer.isEmpty() );

        for ( SearchResult sr : eer ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                this.tearDown();
                return;
            }
        }

        this.tearDown();
        fail( "Didn't get expected result from search" );
    }

    /**
     * Tests that gene uris get handled correctly
     */
    @Test
    public void testGeneUriSearch() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( SearchServiceTest.GENE_URI + this.geneNcbiId );
        settings.setSearchGenes( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( Gene.class ) ) {
            if ( sr.getResultObject().equals( gene ) ) {
                this.tearDown();
                return;
            }
        }

        this.tearDown();
        fail( "Didn't get expected result from search" );

    }

    @Test
    public void testSearchByBibRefIdProblems() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        BibliographicReference bibref = fetcher.retrieveByHTTP( 9600966 );
        bibref = persisterHelper.persist( bibref );
        assertTrue( bibref.getAbstractText().contains(
                "ase proved to be a de novo mutation. In the third kindred, affected brothers both have a" ) );

        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );

        indexerTask.setTaskCommand( c );
        indexerTask.execute();

        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.noSearches();
        settings.setQuery( "de novo mutation" );
        settings.setSearchBibrefs( true );

        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );
        for ( SearchResult sr : found.get( BibliographicReference.class ) ) {
            if ( sr.getResultObject().equals( bibref ) ) {
                this.tearDown();
                return;
            }
        }
        this.tearDown();
        fail( "Didn't get expected result from search" );
    }

    @Test
    public void testSearchByBibRefIdProblemsB() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        BibliographicReference bibref = fetcher.retrieveByHTTP( 22780917 );
        bibref = persisterHelper.persist( bibref );
        assertTrue( bibref.getAbstractText().contains(
                "d to chromosome 22q12. Our results confirm chromosome 22q12 as the solitary locus for FFEVF" ) );

        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );

        indexerTask.setTaskCommand( c );
        indexerTask.execute();

        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.noSearches();
        settings.setQuery( "confirm chromosome 22q12" );
        settings.setSearchBibrefs( true );

        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );
        for ( SearchResult sr : found.get( BibliographicReference.class ) ) {
            if ( sr.getResultObject().equals( bibref ) ) {
                this.tearDown();
                return;
            }
        }

        this.tearDown();
        fail( "Didn't get expected result from search" );
    }

//    @Test
//    public void testSearchByBibRefId() {
//        try {
//            this.setup();
//        } catch ( Exception e ) {
//            e.printStackTrace();
//        }
//        String id;
//        if ( ee.getPrimaryPublication() == null ) {
//            PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
//            BibliographicReference bibref = fetcher.retrieveByHTTP( 21878914 );
//            bibref = ( BibliographicReference ) persisterHelper.persist( bibref );
//            ee.setPrimaryPublication( bibref );
//            eeService.update( ee );
//            id = "21878914";
//        } else {
//            id = ee.getPrimaryPublication().getPubAccession().getAccession();
//        }
//
//        log.info( "indexing ..." );
//
//        IndexerTaskCommand c = new IndexerTaskCommand();
//        c.setIndexBibRef( true );
//
//        indexerTask.setTaskCommand( c );
//        indexerTask.execute();
//
//        SearchSettings settings = SearchSettings.Factory.newInstance();
//        settings.noSearches();
//        settings.setQuery( id );
//        settings.setSearchExperiments( true );
//        settings.setUseCharacteristics( false );
//        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
//        assertTrue( !found.isEmpty() );
//        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
//            if ( sr.getResultId().equals( ee.getId() ) ) {
//                this.tearDown();
//                return;
//            }
//        }
//
//        this.tearDown();
//        fail( "Didn't get expected result from search" );
//    }

    /**
     * Test we find EE tagged with a child term that matches the given uri.
     */
    @Test
    public void testURIChildSearch() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( "http://purl.obolibrary.org/obo/FMA_83153" ); // OrganComponent of Neuraxis; superclass of
        // 'spinal cord'.
        settings.setSearchExperiments( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                this.tearDown();
                return;
            }
        }
        this.tearDown();
        fail( "Didn't get expected result from search" );
    }

    /**
     * Does the search engine correctly match the spinal cord URI and find objects directly tagged with that URI
     */
    @Test
    public void testURISearch() {
        try {
            this.setup();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        SearchSettings settings = SearchSettings.Factory.newInstance();
        settings.setQuery( SearchServiceTest.SPINAL_CORD );
        settings.setSearchExperiments( true );
        settings.setUseDatabase( false );
        settings.setUseIndices( false );
        settings.setUseCharacteristics( true );
        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
        assertTrue( !found.isEmpty() );

        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                this.tearDown();
                return;
            }
        }
        this.tearDown();
        fail( "Didn't get expected result from search" );
    }

}
