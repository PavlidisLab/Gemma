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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.UberonOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.loader.entrez.pubmed.PubMedXMLFetcher;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.tasks.maintenance.IndexerTask;
import ubic.gemma.core.tasks.maintenance.IndexerTaskCommand;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

/**
 * @author kelsey
 */
public class SearchServiceIntegrationTest extends BaseSpringContextTest {
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

    /* fixtures */
    private ExpressionExperiment ee;
    private Gene gene;
    private String geneNcbiId;

    @Before
    public void setUp() throws Exception {
        try ( InputStream is = this.getClass().getResourceAsStream( "/data/loader/ontology/fma.test.owl" ) ) {
            assert is != null;

            // this abuses the service as our example is a legacy FMA test (not uberon), but it doesn't matter since we're loading from a file anyway.
            // this will fail if the loading of uberon is enabled - it will collide.
            UberonOntologyService uberonService = ontologyService.getUberonService();
            assertTrue( uberonService.isOntologyLoaded() );
            assertTrue( uberonService.isEnabled() );
            uberonService.loadTermsInNameSpace( is, true );
        }
        ee = this.getTestPersistentBasicExpressionExperiment();

        Characteristic eeCharSpinalCord = Characteristic.Factory.newInstance();
        eeCharSpinalCord.setCategory( SearchServiceIntegrationTest.SPINAL_CORD );
        eeCharSpinalCord.setCategoryUri( SearchServiceIntegrationTest.SPINAL_CORD );
        eeCharSpinalCord.setValue( "spinal cord" );
        eeCharSpinalCord.setValueUri( SearchServiceIntegrationTest.SPINAL_CORD );
        eeCharSpinalCord = characteristicService.create( eeCharSpinalCord );

        Characteristic eeCharGeneURI = Characteristic.Factory.newInstance();
        eeCharGeneURI.setCategory( SearchServiceIntegrationTest.GENE_URI );
        eeCharGeneURI.setCategoryUri( SearchServiceIntegrationTest.GENE_URI );
        eeCharGeneURI.setValue( SearchServiceIntegrationTest.GENE_URI );
        eeCharGeneURI.setValueUri( SearchServiceIntegrationTest.GENE_URI );
        eeCharGeneURI = characteristicService.create( eeCharGeneURI );

        Characteristic eeCharCortexURI = Characteristic.Factory.newInstance();
        eeCharCortexURI.setCategory( SearchServiceIntegrationTest.BRAIN_CAVITY );
        eeCharCortexURI.setCategoryUri( SearchServiceIntegrationTest.BRAIN_CAVITY );
        eeCharCortexURI.setValue( "cavity of brain" );
        eeCharCortexURI.setValueUri( SearchServiceIntegrationTest.BRAIN_CAVITY );
        eeCharCortexURI = characteristicService.create( eeCharCortexURI );

        Set<Characteristic> chars = new HashSet<>();
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

    @After
    public void tearDown() {
        geneService.remove( gene );
        eeService.remove( ee );
    }

    /**
     * Tests that general search terms are resolved to their proper ontology terms and objects tagged with those terms
     * are found, -- requires LARQ index.
     */
    @Test
    public void testGeneralSearch4Brain() throws SearchException, OntologySearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( "Brain" ) // should hit 'cavity of brain'.
                .resultType( ExpressionExperiment.class )
                .useCharacteristics( true )
                .useDatabase( false )
                .useIndices( false )
                .build();

        Collection<OntologyTerm> ontologyhits = ontologyService.findTerms( "brain" );
        assertFalse( ontologyhits.isEmpty() ); // making sure this isn't a problem, rather than the search per se.

        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );

        List<SearchResult<ExpressionExperiment>> eer = found.get( ExpressionExperiment.class );
        assertFalse( eer.isEmpty() );

        for ( SearchResult<ExpressionExperiment> sr : eer ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    /**
     * Tests that gene uris get handled correctly
     */
    @Test
    public void testGeneUriSearch() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( SearchServiceIntegrationTest.GENE_URI + this.geneNcbiId )
                .resultType( Gene.class )
                .build();
        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );

        for ( SearchResult<Gene> sr : found.get( Gene.class ) ) {
            if ( gene.equals( sr.getResultObject() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    @Test
    @Category(SlowTest.class)
    public void testSearchByBibRefIdProblems() throws SearchException {
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        BibliographicReference bibref = fetcher.retrieveByHTTP( 9600966 );
        bibref = ( BibliographicReference ) persisterHelper.persist( bibref );
        assertTrue( bibref.getAbstractText().contains(
                "ase proved to be a de novo mutation. In the third kindred, affected brothers both have a" ) );

        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );

        indexerTask.setTaskCommand( c );
        try {
            indexerTask.call();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        SearchSettings settings = SearchSettings.builder()
                .query( "de novo mutation" )
                .resultType( BibliographicReference.class )
                .build();

        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );
        for ( SearchResult<BibliographicReference> sr : found.get( BibliographicReference.class ) ) {
            if ( bibref.equals( sr.getResultObject() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    @Test
    public void testSearchByBibRefIdProblemsB() throws SearchException {
        PubMedXMLFetcher fetcher = new PubMedXMLFetcher();
        BibliographicReference bibref = fetcher.retrieveByHTTP( 22780917 );
        bibref = ( BibliographicReference ) persisterHelper.persist( bibref );
        assertTrue( bibref.getAbstractText().contains(
                "d to chromosome 22q12. Our results confirm chromosome 22q12 as the solitary locus for FFEVF" ) );

        IndexerTaskCommand c = new IndexerTaskCommand();
        c.setIndexBibRef( true );

        indexerTask.setTaskCommand( c );
        try {
            indexerTask.call();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }

        SearchSettings settings = SearchSettings.builder()
                .query( "confirm chromosome 22q12" )
                .resultType( BibliographicReference.class )
                .build();

        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );
        for ( SearchResult<BibliographicReference> sr : found.get( BibliographicReference.class ) ) {
            if ( bibref.equals( sr.getResultObject() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

//    @Test
//    public void testSearchByBibRefId() {
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
//        settings.setQuery( id );
//        settings.setSearchExperiments( true );
//        settings.setUseCharacteristics( false );
//        Map<Class<?>, List<SearchResult>> found = this.searchService.search( settings );
//        assertTrue( !found.isEmpty() );
//        for ( SearchResult sr : found.get( ExpressionExperiment.class ) ) {
//            if ( sr.getResultId().equals( ee.getId() ) ) {
//                return;
//            }
//        }
//
//        fail( "Didn't get expected result from search" );
//    }

    /**
     * Test we find EE tagged with a child term that matches the given uri.
     */
    @Test
    public void testURIChildSearch() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( "http://purl.obolibrary.org/obo/FMA_83153" ) // OrganComponent of Neuraxis; superclass of
                // 'spinal cord'.
                .resultType( ExpressionExperiment.class )
                .build();
        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );

        for ( SearchResult<ExpressionExperiment> sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    /**
     * Does the search engine correctly match the spinal cord URI and find objects directly tagged with that URI
     */
    @Test
    public void testURISearch() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( SearchServiceIntegrationTest.SPINAL_CORD )
                .resultType( ExpressionExperiment.class )
                .useDatabase( false )
                .useIndices( false )
                .useCharacteristics( true )
                .build();
        SearchService.SearchResultMap found = this.searchService.search( settings );
        assertFalse( found.isEmpty() );

        for ( SearchResult<ExpressionExperiment> sr : found.get( ExpressionExperiment.class ) ) {
            if ( sr.getResultId().equals( ee.getId() ) ) {
                return;
            }
        }

        fail( "Didn't get expected result from search" );
    }

    @Test
    public void testLoadValueObject() throws SearchException {
        SearchSettings settings = SearchSettings.builder()
                .query( SearchServiceIntegrationTest.SPINAL_CORD )
                .resultType( ExpressionExperiment.class )
                .build();
        List<SearchResult<ExpressionExperiment>> results = searchService.search( settings, ExpressionExperiment.class );
        assertThat( results )
                .hasSize( 1 );
        SearchResult<IdentifiableValueObject<ExpressionExperiment>> resultVo = searchService.loadValueObject( results.get( 0 ) );
        // ensure that the resultType is preserved
        assertThat( resultVo.getResultType() )
                .isAssignableFrom( ExpressionExperiment.class );
        assertThat( resultVo.getResultId() )
                .isEqualTo( ee.getId() );
        assertThat( resultVo.getResultObject() )
                .isNotNull();
    }
}
