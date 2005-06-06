package edu.columbia.gemma.loader.association;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
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
import edu.columbia.gemma.loader.description.OntologyEntryLoader;
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
public class Gene2GOAssociationTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( Gene2GOAssociationTest.class );

    GoMappings goMappings = null;
    GONames goNames = null;
    Gene2GOAssociationParserImpl goParser = null;

    Map goTermsMap = null;
    Method m = null;
 

    /**
     * @throws NoSuchMethodException
     */
    // public void testFindParseLineMethod() throws NoSuchMethodException {
    // log.info( "Testing class: GeneOntologyParser method: public Method findParseLineMethod( String species ) throws
    // NoSuchMethodException" );
    // m = goParser.findParseLineMethod( "human" );
    // }
    //    
    //
    // public void testParse() throws IOException {
    // log.info( "Testing class: GeneOntologyParser method: public Map parse( InputStream is, Method m ) throws
    // IOException" );
    // InputStream is = goParser.retrieveByHTTP("human");
    // GZIPInputStream gZipInputStream = new GZIPInputStream(is);
    // goParser.parse(gZipInputStream,m);
    // }
    /**
     * 
     */
    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();

        goParser = new Gene2GOAssociationParserImpl();
        goParser.setGoMappings( new GoMappings() );
    }

    /**
     * 
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        goParser = null;
        goMappings = null;
    }

}
