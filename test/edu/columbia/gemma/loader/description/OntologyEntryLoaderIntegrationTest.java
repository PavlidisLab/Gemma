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
package edu.columbia.gemma.loader.description;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseTransactionalSpringContextTest;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * @author keshav
 * @version $Id$
 */
public class OntologyEntryLoaderIntegrationTest extends BaseTransactionalSpringContextTest {
    protected static final Log log = LogFactory.getLog( OntologyEntryLoaderIntegrationTest.class );
    String url = "http://archive.godatabase.org/latest-termdb/go_daily-termdb.rdf-xml.gz";
    OntologyEntryPersister ontologyEntryPersister = null;
    GeneOntologyEntryParser ontologyEntryParser = null;
    Collection<Object> createdObjects = null;

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
        lf.setLocalURI( "Remote file.  See remote uri for details." );
        lf.setRemoteURI( url );
        lf.setSize( 1656000L );

        // add a second local file
        LocalFile lf2 = LocalFile.Factory.newInstance();
        lf2.setLocalURI( "2nd local file local uri." );
        lf2.setRemoteURI( "2nd local file remote uri." );
        lf2.setSize( 1656000L );

        Object[] dependencies = new Object[3];
        dependencies[0] = ed;
        dependencies[1] = lf;
        dependencies[2] = lf2;

        InputStream is = new GZIPInputStream( ParserAndLoaderTools.retrieveByHTTP( url ) );
        ontologyEntryParser.parse( is );
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

    /**
     * 
     */
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        ontologyEntryParser = new GeneOntologyEntryParser();
        ontologyEntryPersister = new OntologyEntryPersister();
        ontologyEntryPersister.setPersisterHelper( persisterHelper );
    }

}
