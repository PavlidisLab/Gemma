/*
 * The Gemma project Copyright (c) 2010 University of British Columbia Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */

package ubic.gemma.search;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.ontology.providers.AbstractOntologyService;
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
public class SearchServiceTest extends BaseSpringContextTest
{
    private static final String GENE_URI = "http://purl.org/commons/record/ncbi_gene/20655";

    private static final String BRAIN_STEM = "http://purl.org/obo/owl/FMA#FMA_7647";

    private static final String PREFRONTAL_CORTEX_URI = "http://purl.org/obo/owl/FMA#FMA_224850";
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
    private  VocabCharacteristic eeCharGeneURI;
    private  VocabCharacteristic eeCharCortexURI;
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
     * Does the search engine correctly match the spinal cord URI and find objects directly tagged with that URI
     */
    @Test
    public void testURISearch(){
        
        waitForOntology(ontologyService.getFmaOntologyService());

        SearchSettings settings = new SearchSettings();
        settings.setQuery( BRAIN_STEM );
        settings.setSearchExperiments( true );
        Map<Class<?>,List<SearchResult>> found = this.searchService.search( settings );
        if (found.isEmpty())
            assertTrue(false);
        
           for(SearchResult sr : found.get( ExpressionExperiment.class )){
               if (sr.getResultObject().equals( ee) ){
                   assertTrue(true);
                   return;
               }
           }
           
           assertTrue(false);
    }
    
    @Test
    public void testGeneUriSearch(){
        
        SearchSettings settings = new SearchSettings();
        settings.setQuery( GENE_URI );
        settings.setSearchExperiments( true );
        Map<Class<?>,List<SearchResult>> found = this.searchService.search( settings );
        if ((found == null) ||(found.isEmpty()))
            assertTrue(false);
        
           for(SearchResult sr : found.get( ExpressionExperiment.class )){
               if (sr.getResultObject().equals( ee) ){
                   assertTrue(true);
                   return;
               }
           }
           
           assertTrue(false);
        
    }
    
    @Test
    public void testGeneralSearch4Brain(){
        
        waitForOntology(ontologyService.getFmaOntologyService());

        SearchSettings settings = new SearchSettings();
        settings.setQuery( "Brain" );
        settings.setSearchExperiments( true );
        settings.setUseCharacteristics(  true );
        Map<Class<?>,List<SearchResult>> found = this.searchService.search( settings );
        if ((found == null) ||(found.isEmpty()))
            assertTrue(false);
        
           for(SearchResult sr : found.get( ExpressionExperiment.class )){
               if (sr.getResultObject().equals( ee) ){
                   assertTrue(true);
                   return;
               }
           }
           
           assertTrue(false);
        
    }

    //Pass in the given ontology you want to wait to finish loading. 
    private void waitForOntology(AbstractOntologyService os) {
        while ( !os.isOntologyLoaded() ) {
            try{
            Thread.sleep( 1000 );
            }catch(InterruptedException ie){
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
            ee = this.getTestPersistentBasicExpressionExperiment();
            
            eeCharSpinalCord = VocabCharacteristic.Factory.newInstance();
            eeCharSpinalCord.setCategory( "test" );
            eeCharSpinalCord.setCategoryUri( "test" );
            eeCharSpinalCord.setValue(BRAIN_STEM);
            eeCharSpinalCord.setValueUri( BRAIN_STEM );
            characteristicService.create( eeCharSpinalCord );
            

            eeCharGeneURI = VocabCharacteristic.Factory.newInstance();
            eeCharGeneURI.setCategory( "test" );
            eeCharGeneURI.setCategoryUri( "test" );
            eeCharGeneURI.setValue(GENE_URI);
            eeCharGeneURI.setValueUri( GENE_URI );
            characteristicService.create( eeCharGeneURI );

            eeCharCortexURI = VocabCharacteristic.Factory.newInstance();
            eeCharCortexURI.setCategory( "test" );
            eeCharCortexURI.setCategoryUri( "test" );
            eeCharCortexURI.setValue(PREFRONTAL_CORTEX_URI);
            eeCharCortexURI.setValueUri( PREFRONTAL_CORTEX_URI );
            characteristicService.create( eeCharCortexURI );
            
            Collection<Characteristic> chars = new HashSet<Characteristic>();
            chars.add( eeCharSpinalCord );
            chars.add(eeCharGeneURI);
            chars.add( eeCharCortexURI );
            ee.setCharacteristics( chars );
            eeService.update( ee );
   
        

            
    }
    
}
