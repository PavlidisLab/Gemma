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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
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
 * @version $Id$
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
        FMAOntologyService fmaOntologyService = ontologyService.getFmaOntologyService();
        fmaOntologyService.init( true );
        waitForOntology( fmaOntologyService );

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
