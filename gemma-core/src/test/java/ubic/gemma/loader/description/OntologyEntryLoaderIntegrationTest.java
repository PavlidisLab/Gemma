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
package ubic.gemma.loader.description;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.testing.BaseTransactionalSpringContextTest;

/**
 * @author keshav
 * @version $Id$
 */
public class OntologyEntryLoaderIntegrationTest extends BaseTransactionalSpringContextTest {
    protected static final Log log = LogFactory.getLog( OntologyEntryLoaderIntegrationTest.class );
    String url = "http://archive.godatabase.org/latest-termdb/go_daily-termdb.rdf-xml.gz";
    OntologyEntryPersister ontologyEntryPersister = null;
    GeneOntologyEntryParser ontologyEntryParser = null;
    Collection<?> createdObjects = null;

    /**
     * Tests both the parser and the loader.
     */
    public void testParseAndLoad() throws Exception {

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GO" );
        ed.setWebUri( "http://archive.godatabase.org" );
        ed.setType( DatabaseType.ONTOLOGY );

        AuditTrail at = AuditTrail.Factory.newInstance();
        ed.setAuditTrail( at );

        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setLocalURL( null );
        lf.setRemoteURL( new URL( url ) );
        lf.setSize( 1656000L );

        // add a second local file
        LocalFile lf2 = LocalFile.Factory.newInstance();
        lf2.setLocalURL( new URL( "file:///2nd/local/file/local/uri." ) );
        lf2.setRemoteURL( new URL( "http://2nd/local/file/remote/uri." ) );
        lf2.setSize( 1656000L );

        Object[] dependencies = new Object[3];
        dependencies[0] = ed;
        dependencies[1] = lf;
        dependencies[2] = lf2;

        // HttpFetcher hf = new HttpFetcher();
        // Collection<LocalFile> files = hf.fetch( url );
        // final LocalFile localfile = files.iterator().next();
        // final File file = localfile.asFile();

        // this is fancy for a test.
        FutureTask<Boolean> future = new FutureTask<Boolean>( new Callable<Boolean>() {
            public Boolean call() throws FileNotFoundException, IOException {
                InputStream is = new BufferedInputStream( new GZIPInputStream( getClass().getResourceAsStream(
                        "/data/loader/go_daily-termdb.small_test.rdf-xml.gz" ) ) );
                ontologyEntryParser.parse( is );
                return Boolean.TRUE;
            }
        } );
        Executors.newSingleThreadExecutor().execute( future );
        while ( !future.isDone() ) {
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException ie ) {
                ;
            }
            log.info( "parsing..." );
        }

        if ( future.get().booleanValue() ) {
            Collection<Object> ontologyEntries = ontologyEntryParser.getResults();

            // throw out most of the results so this test is faster
            Collection<Object> testSet = new HashSet<Object>();
            int count = 0;
            for ( Object object : ontologyEntries ) {
                testSet.add( object );
                count++;
                if ( count >= 10 ) break;
            }

            createdObjects = ontologyEntryPersister.persist( testSet );
        }

        // localfile.asFile().delete();

    }

    /**
     * 
     */
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ontologyEntryParser = new GeneOntologyEntryParser();
        ontologyEntryPersister = new OntologyEntryPersister();
        ontologyEntryPersister.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
    }

}
