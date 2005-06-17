package edu.columbia.gemma.loader.description;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import baseCode.bio.geneset.GONames;
import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.description.DatabaseType;
import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;
import edu.columbia.gemma.util.SpringContextUtil;

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

    Collection<OntologyEntry> oeCol = null;

    Collection<OntologyEntry> oeCol2 = null;

    OntologyEntryLoaderImpl ontologyEntryLoader = null;

    OntologyEntryParserImpl ontologyEntryParser = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws SAXException
     * @throws IOException
     */
    public void testParseAndLoad() throws SAXException, IOException {
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

        oeCol = ontologyEntryParser.createOrGetDependencies( dependencies, oeMap );

        LoaderTools.loadDatabase( ontologyEntryLoader, oeCol );

        // parse second file. make sure the duplicates are not persisted again.
        oeMap2 = ontologyEntryParser.parseFromHttp( url );

        oeCol2 = ontologyEntryParser.createOrGetDependencies( dependencies, oeMap2 );

        LoaderTools.loadDatabase( ontologyEntryLoader, oeCol2 );

    }

    /**
     * 
     */
    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();

        ontologyEntryParser = new OntologyEntryParserImpl();

        ontologyEntryLoader = new OntologyEntryLoaderImpl();

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
