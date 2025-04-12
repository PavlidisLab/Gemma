/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GeneOntologyServiceImpl.GOAspect;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Additional tests with updated ontology file, fixing problems getting aspects.
 *
 * @author Paul
 */
@Category(SlowTest.class)
@ContextConfiguration
public class GeneOntologyService2Test extends AbstractJUnit4SpringContextTests implements InitializingBean {

    @Configuration
    @TestComponent
    public static class GeneOntologyService2TestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "load.ontologies=false" );
        }

        @Bean
        public GeneOntologyService geneOntologyService() throws IOException, InterruptedException {
            return new GeneOntologyServiceImpl( "dummy", true );
        }

        @Bean
        public TaskExecutor ontologyTaskExecutor() {
            return mock( TaskExecutor.class );
        }

        @Bean
        public Gene2GOAssociationService gene2GOAssociationService() {
            return mock( Gene2GOAssociationService.class );
        }

        @Bean
        public GeneService geneService() {
            return mock( GeneService.class );
        }

        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager( "GeneOntologyService.goTerms", "GeneOntologyService.term2Aspect" );
        }
    }

    @Autowired
    private GeneOntologyService gos;

    @Override
    public void afterPropertiesSet() throws Exception {
        if ( !gos.isOntologyLoaded() ) {
            gos.setSearchEnabled( false );
            InputStream is = new GZIPInputStream(
                    new ClassPathResource( "/data/loader/ontology/go.bptest.owl.gz" ).getInputStream() );
            gos.initialize( is, false );
        }
    }

    @Test
    public final void testParents() {
        String id = "GO:0034118"; // regulation of erythrocyte aggregation

        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( true, false );
        assertEquals( 1, terms.size() );
        OntologyTerm par = terms.iterator().next();
        assertEquals( "http://purl.obolibrary.org/obo/GO_0034110", par.getUri() ); // regulation of homotypic cell-cell
        // adhesion
    }

    @Test
    public final void testAllParents() {
        String id = "GO:0034118"; // regulation of erythrocyte aggregation

        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, false );

        // excludes "regulates" relations, excludes root.

        /*
         * regulation of homotypic cell-cell adhesion
         *
         * regulation of cell-cell adhesion
         *
         * regulation of cell adhesion
         *
         * regulation of cellular process
         *
         * regulation of biological process
         *
         * biological regulation
         *
         * (biological process)
         */
        assertEquals( 7, terms.size() );
    }


    @Test
    public final void testGetAspect() {
        GOAspect termAspect = gos
                .getTermAspect( "GO:0034118" ); // regulation of erythrocyte aggregationS
        assertNotNull( termAspect );

        String aspect = termAspect.toString().toLowerCase();
        assertEquals( "biological_process", aspect );

    }


    @Test
    public final void testGetAspect2() {
        GOAspect termAspect = gos.getTermAspect( "GO:0007272" ); // ensheathment of neurons
        assertNotNull( termAspect );
        String aspect = termAspect.toString().toLowerCase();
        assertEquals( "biological_process", aspect );
    }

}
