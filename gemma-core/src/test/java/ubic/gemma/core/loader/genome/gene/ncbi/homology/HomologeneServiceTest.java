/*
 * The Gemma project
 *
 * Copyright (c) 2009 Columbia University
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
package ubic.gemma.core.loader.genome.gene.ncbi.homology;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.experimental.categories.Category;
import org.junit.jupiter.api.Tag;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.context.AbstractAsyncFactoryBean;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonReadService;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the homologeneService but only access methods that don't require a DB connection (using the gemma db).
 *
 * <p>Note: this test deliberately does NOT use Spring (no {@code @ContextConfiguration},
 * no {@link org.springframework.test.context.junit.jupiter.SpringExtension}). The factory under test is a
 * {@link AbstractAsyncFactoryBean} (a Spring {@code FactoryBean}); registering it in a Spring context causes
 * {@code MockitoResetTestExecutionListener} (spring-test 6.2) to call {@code getBean()} on it before each test,
 * which eagerly triggers {@code getObject()} and initialises the factory, defeating per-test isolation. We
 * sidestep that by constructing the factory directly in {@link #setUp()} with mocked collaborators.
 *
 * @author klc
 */
@ExtendWith(NetworkAvailableExtension.class)
public class HomologeneServiceTest {

    private HomologeneServiceFactory hgs;

    @BeforeEach
    public void setUp() throws Exception {
        hgs = new HomologeneServiceFactory( mock( GeneService.class ), mock( TaxonReadService.class ) ) {
            @Override
            protected HomologeneService createObject() throws Exception {
                // otherwise some test might fail because the object is created too quickly
                Thread.sleep( 10 );
                return super.createObject();
            }
        };
        hgs.setHomologeneFile( new ClassPathResource( "/data/loader/genome/homologene/homologene.testdata.txt" ) );
        hgs.setLoadHomologene( true ); // ignore setting from Gemma.properties
    }

    @AfterEach
    public void tearDown() throws Exception {
        if ( hgs != null ) {
            hgs.destroy();
        }
    }

    @Test
    public void testGetServiceAsync() {
        assertThat( hgs.isInitialized() ).isFalse();
        assertThat( hgs.getObject() ).succeedsWithin( 10, TimeUnit.SECONDS );
        assertThat( hgs.isInitialized() ).isTrue();
        assertThatThrownBy( () -> hgs.setHomologeneFile( new FileSystemResource( "test" ) ) )
                .isInstanceOf( IllegalStateException.class );
    }

    @Test
    public void testGetServiceAsyncThenCancel() throws Exception {
        assertThat( hgs.isInitialized() ).isFalse();
        Future<HomologeneService> service = hgs.getObject();
        assertThat( service ).isNotDone().isNotCancelled();
        assertThat( hgs.isInitialized() ).isTrue();
        hgs.destroy();
        assertThat( service ).isCancelled();
    }

    @Test
    public final void testGetHomologues() throws Exception {
        long id = 34;
        Collection<Long> homologenes = hgs.getObject().get().getHomologues( id );
        assertNotNull( homologenes );
        assertEquals( 11, homologenes.size() );
    }

    @Test
    public final void testGetHomologues2() throws Exception {
        Collection<Long> homologenes = hgs.getObject().get().getNCBIGeneIdsInGroup( 3 );
        assertNotNull( homologenes );
        assertEquals( 12, homologenes.size() );
    }

    @Test
    @Tag("slow")
    @Category(SlowTest.class)
    @NetworkAvailable(url = "ftp://ftp.ncbi.nlm.nih.gov/pub/HomoloGene/last-archive/homologene.data")
    public final void testHomologeneFromFtpServer() throws Exception {
        hgs.setHomologeneFile( new HomologeneNcbiFtpResource( "homologene.data" ) );
        Future<HomologeneService> homologeneService = hgs.getObject();
        assertThat( homologeneService ).succeedsWithin( 30, TimeUnit.SECONDS );
    }

    @Test
    public final void testHomologeneFromFtpServerThenCancel() throws Exception {
        hgs.setHomologeneFile( new HomologeneNcbiFtpResource( "homologene.data" ) );
        Future<HomologeneService> homologeneService = hgs.getObject();
        assertThat( homologeneService ).isNotCancelled().isNotDone();
        hgs.destroy();
        assertThat( homologeneService ).isCancelled();
    }

    @Test
    public void testDisableLoadHomologene() {
        assertThat( hgs.isInitialized() ).isFalse();
        hgs.setLoadHomologene( false );
        assertThat( hgs.getObject() ).succeedsWithin( 100, TimeUnit.MILLISECONDS );
    }

    /**
     * Factory for {@link HomologeneService}.
     */
    @Slf4j
    public static class HomologeneServiceFactory extends AbstractAsyncFactoryBean<HomologeneService> {

        private static final String HOMOLOGENE_FILE_CONFIG = "ncbi.homologene.fileName";
        private static final String LOAD_HOMOLOGENE_CONFIG = "load.homologene";
        private static final boolean LOAD_HOMOLOGENE = Settings.getBoolean( HomologeneServiceFactory.LOAD_HOMOLOGENE_CONFIG, true );

        private final GeneService geneService;
        private final TaxonReadService taxonService;

        private Resource homologeneFile = new HomologeneNcbiFtpResource( Settings.getString( HOMOLOGENE_FILE_CONFIG ) );
        private boolean loadHomologene = LOAD_HOMOLOGENE;

        public HomologeneServiceFactory( GeneService geneService, TaxonReadService taxonService ) {
            this.geneService = geneService;
            this.taxonService = taxonService;
        }

        /**
         * Set the resource used for loading Homologene.
         */
        public void setHomologeneFile( Resource homologeneFile ) {
            preventModificationIfInitialized( "homologeneFile" );
            this.homologeneFile = homologeneFile;
        }

        /**
         * Set whether to load homologene or not.
         */
        public void setLoadHomologene( boolean loadHomologene ) {
            preventModificationIfInitialized( "loadHomologene" );
            this.loadHomologene = loadHomologene;
        }

        @Override
        protected HomologeneService createObject() throws Exception {
            HomologeneService homologeneService = new HomologeneServiceImpl( geneService, taxonService, homologeneFile );
            if ( loadHomologene ) {
                homologeneService.refresh();
            }
            return homologeneService;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }

        private void preventModificationIfInitialized( String field ) {
            Assert.state( !isInitialized(), String.format( "The Homologene service has already been initialized, changing %s is not allowed.", field ) );
        }
    }
}
