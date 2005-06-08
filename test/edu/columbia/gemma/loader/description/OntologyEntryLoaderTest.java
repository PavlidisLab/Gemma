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

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GO" );
        ed.setWebUri( "http://archive.godatabase.org" );
        ed.setType( DatabaseType.ONTOLOGY );

        oeCol = ontologyEntryParser.parse( ed );

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        ontologyEntryLoader.create( oeCol );

        stopWatch.stop();

        LoaderTools.displayTime( stopWatch );

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
