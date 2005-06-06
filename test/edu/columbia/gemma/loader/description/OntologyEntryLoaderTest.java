package edu.columbia.gemma.loader.description;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.xml.sax.SAXException;

import baseCode.bio.geneset.GONames;
import edu.columbia.gemma.BaseServiceTestCase;
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

    Map goTermsMap = null;
    OntologyEntryLoader ontologyEntryLoader = null;

    /**
     * @throws SAXException
     * @throws IOException
     */
    public void testBaseCodeGoParser() throws SAXException, IOException {
        log.info( "Testing class: baseCode.GONames throws SAXException, IOException" );

        InputStream is = LoaderTools
                .retrieveByHTTP( "http://archive.godatabase.org/latest/go_200505-termdb.rdf-xml.gz" );
        GZIPInputStream gZipInputStream = new GZIPInputStream( is );

        goNames = new GONames( gZipInputStream );

        goTermsMap = goNames.getMap();

        Collection<String> ontologyEntryCol = goTermsMap.values();

        log.info( "number of ontology entries: " + ontologyEntryCol.size() );

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        for ( Iterator<String> iter = ontologyEntryCol.iterator(); iter.hasNext(); ) {
            OntologyEntry ontologyEntry = OntologyEntry.Factory.newInstance();
            ontologyEntry.setCategory( iter.next() );

            ontologyEntryLoader.getOntologyEntryDao().create( ontologyEntry );
        }

        stopWatch.stop();
        LoaderTools.displayTime( stopWatch );

    }

    /**
     * 
     */
    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();

        ontologyEntryLoader = new OntologyEntryLoader();
        ontologyEntryLoader.setOntologyEntryDao( ( OntologyEntryDao ) ctx.getBean( "ontologyEntryDao" ) );

    }

    /**
     * 
     */
    protected void tearDown() throws Exception {
        super.tearDown();

        Collection<String> ontologyEntryCol = goTermsMap.values();

        ontologyEntryLoader.getOntologyEntryDao().remove( ontologyEntryCol );

        ontologyEntryLoader = null;

        goTermsMap = null;

    }

}
