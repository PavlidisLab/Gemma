/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import baseCode.bio.geneset.GONames;
import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * This test is more representative of integration testing than unit testing as it tests multiple both parsing and
 * loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class OntologyEntryLoaderTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( OntologyEntryLoaderTest.class );

    GONames goNames = null;

    Map oeMap = null;

    Map oeMap2 = null;

    Collection<Object> oeCol = null;

    Collection<Object> oeCol2 = null;

    OntologyEntryPersister ontologyEntryLoader = null;

    OntologyEntryParserImpl ontologyEntryParser = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws SAXException
     * @throws IOException
     */
    // @SuppressWarnings("unchecked")
    public void testParseAndLoad() throws IOException {
        log.info( "Testing class: baseCode.GONames throws SAXException, IOException" );

        String url = "http://archive.godatabase.org/latest/go_200505-termdb.rdf-xml.gz";

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GO" );
        ed.setWebUri( "http://archive.godatabase.org" );
        ed.setType( DatabaseType.ONTOLOGY );

        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setLocalURI( "Remote file.  See remote uri for details." );
        lf.setRemoteURI( url );
        lf.setSize( 1656000 );

        // add a second local file
        LocalFile lf2 = LocalFile.Factory.newInstance();
        lf2.setLocalURI( "2nd local file local uri." );
        lf2.setRemoteURI( "2nd local file remote uri." );
        lf2.setSize( 1656000 );

        Object[] dependencies = new Object[3];
        dependencies[0] = ed;
        dependencies[1] = lf;
        dependencies[2] = lf2;

        oeMap = ontologyEntryParser.parseFromHttp( url );

        // throw out most of the results so this test is faster
        Map<Object, Object> testMap = new HashMap<Object, Object>();
        int count = 0;
        for ( Object object : oeMap.keySet() ) {
            testMap.put( object, oeMap.get( object ) );
            count++;
            if ( count >= 10 ) break;
        }

        oeCol = ontologyEntryParser.createOrGetDependencies( dependencies, oeMap );

        ParserAndLoaderTools.loadDatabase( ontologyEntryLoader, oeCol );

        // try loading them again?
        oeCol2 = ontologyEntryParser.createOrGetDependencies( dependencies, oeMap2 );

        ParserAndLoaderTools.loadDatabase( ontologyEntryLoader, oeCol2 );

    }

    /**
     * 
     */
    protected void setUp() throws Exception {
        super.setUp();

        ontologyEntryParser = new OntologyEntryParserImpl();

        ontologyEntryLoader = new OntologyEntryPersister();

        // "tomcatesque" functionality
        ontologyEntryParser.setExternalDatabaseDao( ( ExternalDatabaseDao ) ctx.getBean( "externalDatabaseDao" ) );
        ontologyEntryParser.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );
        ontologyEntryLoader.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );
    }

    /**
     * 
     */
    protected void tearDown() throws Exception {
        super.tearDown();

        // TODO remove all ontology entries as well on tear down

        ontologyEntryLoader = null;

        oeMap = null;

        oeMap2 = null;

        oeCol = null;

        oeCol2 = null;

    }

}
