/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.loader.util.fetcher;

import junit.framework.TestCase;
import org.apache.commons.lang3.RandomStringUtils;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.File;
import java.util.Collection;

/**
 * @author pavlidis
 */
public class AbstractFetcherTest extends TestCase {

    private File f;

    public final void testMkdirAlreadyExists() throws Exception {
        TestFetcher tf = new TestFetcher();
        String name = RandomStringUtils.insecure().nextAlphabetic( 4 );
        String usertempdir = System.getProperty( "java.io.tmpdir" );
        assert ( usertempdir != null );
        File g = new File( usertempdir + File.separatorChar + name );
        assert ( g.mkdir() );

        tf.setLocalDataPath( usertempdir );
        f = tf.mkdir( RandomStringUtils.insecure().nextAlphabetic( 4 ) );
        TestCase.assertTrue( f.canRead() );
    }

    public final void testMkdirMakeSubdirs() throws Exception {
        TestFetcher tf = new TestFetcher();
        String usertempdir = System.getProperty( "java.io.tmpdir" );
        TestCase.assertTrue( usertempdir != null );
        tf.setLocalDataPath( usertempdir );
        f = tf.mkdir( RandomStringUtils.insecure().nextAlphabetic( 4 ) );
        TestCase.assertTrue( f.canRead() );
    }

    public final void testMkdirMakeTemp() throws Exception {
        TestFetcher tf = new TestFetcher();
        tf.setLocalDataPath( null );
        f = tf.mkdir( RandomStringUtils.insecure().nextAlphabetic( 4 ) );
        TestCase.assertTrue( f.canRead() );
    }

    @Override
    protected void tearDown() throws Exception {

        super.tearDown();
        if ( f != null && f.canRead() ) {
            EntityUtils.deleteFile( f );
        }
    }

    static class TestFetcher extends AbstractFetcher {

        @Override
        public Collection<LocalFile> fetch( String identifier ) {
            return null;
        }

        @Override
        protected String formLocalFilePath( String identifier, File newDir ) {
            return null;
        }

        @Override
        protected String formRemoteFilePath( String identifier ) {
            return null;
        }

        @Override
        protected void initConfig() {
        }

        void setLocalDataPath( String localDataPath ) {
            this.localBasePath = localDataPath;
        }
    }

}
