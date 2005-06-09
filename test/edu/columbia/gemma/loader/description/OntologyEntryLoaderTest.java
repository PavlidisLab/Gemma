package edu.columbia.gemma.loader.description;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
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

    Set goTermsKeysSet = null;
    Map goTermsMap = null;

    Collection<OntologyEntry> oeCol = null;

    OntologyEntryLoaderImpl ontologyEntryLoader = null;

    OntologyEntryParserImpl ontologyEntryParser = null;

    /**
     * @throws SAXException
     * @throws IOException
     */
    public void testBaseCodeGoParser() throws SAXException, IOException {
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
        lf2.setLocalURI( "2nd local file local uri" );
        lf2.setRemoteURI( "2nd local file remote uri" );
        lf2.setSize( 1656000 );

        Object[] dependencies = new Object[3];
        dependencies[0] = ed;
        dependencies[1] = lf;
        dependencies[2] = lf2;

        oeCol = ontologyEntryParser.parseFromHttp( url, dependencies );

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ontologyEntryLoader.create( oeCol );

        stopWatch.stop();

        LoaderTools.displayTime( stopWatch );

        // trying a second parsed xml file
        oeCol = ontologyEntryParser.parseFromHttp( url, dependencies );

        ontologyEntryLoader.create( oeCol );

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

        goTermsMap = null;

    }

}
