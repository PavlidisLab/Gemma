/*
 * The Gemma project
 *
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.core.ontology.providers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.TestPropertyPlaceholderConfigurer;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * @author Paul
 */
@Category(SlowTest.class)
@ContextConfiguration
public class GeneOntologyServiceTest extends BaseTest implements InitializingBean {
    private static final Log log = LogFactory.getLog( GeneOntologyServiceTest.class.getName() );

    @Configuration
    @TestComponent
    static class GeneOntologyServiceTestContextConfiguration {

        @Bean
        public static TestPropertyPlaceholderConfigurer testPropertyPlaceholderConfigurer() {
            return new TestPropertyPlaceholderConfigurer( "load.ontologies=false", "load.geneOntology=true", "url.geneOntology=dummy" );
        }

        @Bean
        public GeneOntologyService geneOntologyService() throws IOException, InterruptedException {
            return new GeneOntologyServiceImpl();
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
            /*
             * Note that this test file is out of date in some ways. See GeneOntologyServiceTest2
             */
            InputStream is = new GZIPInputStream(
                    new ClassPathResource( "/data/loader/ontology/molecular-function.test.owl.gz" ).getInputStream() );
            // we must force indexing to get consistent test results
            gos.initialize( is, true );
        }
    }

    @Test
    public void testFindTerm() throws OntologySearchException {
        Collection<OntologySearchResult<OntologyTerm>> matches = gos.findTerm( "toxin", 500 );
        assertEquals( 4, matches.size() );
    }

    @Test
    public void testFindTermWithMultipleTerms() throws OntologySearchException {
        Collection<OntologySearchResult<OntologyTerm>> matches = gos.findTerm( "toxin transporter activity", 500 );
        assertEquals( 1, matches.size() );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFindTermWithEmptyQuery() throws OntologySearchException {
        gos.findTerm( " ", 500 );
    }

    @Test
    public final void testAllParents() {
        String id = "GO:0035242";

        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, false );

        assertEquals( 10, terms.size() );
    }

    @Test
    public final void testAllParents2() {
        String id = "GO:0000006";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, false );

        assertFalse( terms.contains( termForId ) );
        assertEquals( 12, terms.size() );
    }

    @Test
    public final void testAsRegularGoId() {
        String id = "GO:0000107";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        String formatedId = asRegularGoId( termForId );
        assertEquals( id, formatedId );
    }

    @Test
    public final void testGetAllChildren() {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( false, false );

        assertEquals( 136, terms.size() );
    }

    @Test
    public final void testGetAspect() {
        String aspect = gos.getTermAspect( "GO:0000107" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = gos.getTermAspect( "GO:0016791" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
        aspect = gos.getTermAspect( "GO:0000107" ).toString().toLowerCase();
        assertEquals( "molecular_function", aspect );
    }

    @Test
    public final void testGetChildren() {
        String id = "GO:0016791";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( true, false );

        assertEquals( 65, terms.size() );
    }

    // latest versions do not have definitions included.
    // public final void testGetDefinition() {
    // String id = "GO:0000007";
    // String definition = gos.getTermDefinition( id );
    // assertNotNull( definition );
    // assertTrue( definition.startsWith( "I am a test definition" ) );
    // }

    @Test
    public final void testGetChildrenPartOf() {
        String id = "GO:0023025";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getChildren( false, true );

        // has a part.
        assertEquals( 1, terms.size() );
    }

    @Test
    public final void testGetParents() {
        String id = "GO:0000014";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( true, false );

        for ( OntologyTerm term : terms ) {
            GeneOntologyServiceTest.log.info( term );
        }
        assertEquals( 1, terms.size() );
    }

    @Test
    public final void testGetParentsPartOf() {
        String id = "GO:0000332";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        Collection<OntologyTerm> terms = termForId.getParents( false, true );

        for ( OntologyTerm term : terms ) {
            GeneOntologyServiceTest.log.info( term );
        }
        // is a subclass and partof.
        assertEquals( 13, terms.size() );
    }

    @Test
    public final void testGetTermForId() {
        String id = "GO:0000310";
        OntologyTerm termForId = gos.getTerm( id );
        assertNotNull( termForId );
        assertEquals( "xanthine phosphoribosyltransferase activity", termForId.getLabel() );
    }

}
