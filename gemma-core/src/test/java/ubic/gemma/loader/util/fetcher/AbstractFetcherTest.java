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
package ubic.gemma.loader.util.fetcher;

import java.io.File;
import java.util.Collection;

import junit.framework.TestCase;

import org.apache.commons.lang3.RandomStringUtils;

import ubic.gemma.model.common.description.LocalFile;

/**
 * @author pavlidis
 * @version $Id$
 */
public class AbstractFetcherTest extends TestCase {

    static class TestFetcher extends AbstractFetcher {

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.loader.util.fetcher.Fetcher#fetch(java.lang.String)
         */
        @Override
        public Collection<LocalFile> fetch( String identifier ) {
            return null;
        }

        public void setLocalDataPath( String localDataPath ) {
            this.localBasePath = localDataPath;
        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formLocalFilePath(java.lang.String, java.io.File)
         */
        @Override
        protected String formLocalFilePath( String identifier, File newDir ) {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#formRemoteFilePath(java.lang.String)
         */
        @Override
        protected String formRemoteFilePath( String identifier ) {
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see ubic.gemma.loader.util.fetcher.AbstractFetcher#initConfig()
         */
        @Override
        protected void initConfig() {
        }
    }

    File f;

    /**
     * Test method for {@link ubic.gemma.loader.util.fetcher.AbstractFetcher#mkdir(java.lang.String)}.
     */
    public final void testMkdirAlreadyExists() throws Exception {
        TestFetcher tf = new TestFetcher();
        String name = RandomStringUtils.randomAlphabetic( 4 );
        String usertempdir = System.getProperty( "java.io.tmpdir" );
        assert ( usertempdir != null );
        File g = new File( usertempdir + File.separatorChar + name );
        assert ( g.mkdir() );

        tf.setLocalDataPath( usertempdir );
        f = tf.mkdir( RandomStringUtils.randomAlphabetic( 4 ) );
        assertTrue( f.canRead() );
    }

    /**
     * Test method for {@link ubic.gemma.loader.util.fetcher.AbstractFetcher#mkdir(java.lang.String)}.
     */
    public final void testMkdirMakeSubdirs() throws Exception {
        TestFetcher tf = new TestFetcher();
        String usertempdir = System.getProperty( "java.io.tmpdir" );
        assertTrue( usertempdir != null );
        tf.setLocalDataPath( usertempdir );
        f = tf.mkdir( RandomStringUtils.randomAlphabetic( 4 ) );
        assertTrue( f.canRead() );
    }

    /**
     * Test method for {@link ubic.gemma.loader.util.fetcher.AbstractFetcher#mkdir(java.lang.String)}.
     */
    public final void testMkdirMakeTemp() throws Exception {
        TestFetcher tf = new TestFetcher();
        tf.setLocalDataPath( null );
        f = tf.mkdir( RandomStringUtils.randomAlphabetic( 4 ) );
        assertTrue( f.canRead() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {

        super.tearDown();
        if ( f != null && f.canRead() ) {
            f.delete();
        }
    }

}
