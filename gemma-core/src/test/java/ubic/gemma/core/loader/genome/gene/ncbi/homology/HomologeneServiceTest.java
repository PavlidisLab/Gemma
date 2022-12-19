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

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.annotation.DirtiesContext;
import ubic.gemma.core.util.test.BaseSpringContextTest;

import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests the homologeneService but only access methods that don't require a DB connection (using the gemma db).
 *
 * @author klc
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class HomologeneServiceTest extends BaseSpringContextTest {

    @Autowired
    private HomologeneServiceFactory hgs;

    @Before
    public void setUp() throws Exception {
        hgs.setHomologeneFile( new ClassPathResource( "/data/loader/genome/homologene/homologene.testdata.txt" ) );
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
    public void testGetServiceAsyncThenCancel() {
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

}
