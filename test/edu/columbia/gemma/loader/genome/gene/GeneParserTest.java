package edu.columbia.gemma.loader.genome.gene;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneParserTest extends BaseServiceTestCase {
    private static final String GENE_INFO = "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\gene_info";
    private static final String GENE2ACCESSION = "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\gene2accession";

    protected static final Log log = LogFactory.getLog( GeneParserTest.class );
    private String filename = null;
    private String filename2 = null;

    private GeneLoader geneLoader = null;
    private GeneParser geneParser = null;
    private Map map = null;

    public void testParseFileValidFile() throws Exception {
        geneParser.parseFile( filename );
        map = geneParser.parseFile( filename2 );

        geneLoader.create( map.values() );

        assertEquals( null, null );

    }

    protected void setUp() throws Exception {
        super.setUp();
        geneParser = new GeneParserImpl();
        geneLoader = new GeneLoaderImpl();
        map = new HashMap();
        // TODO don't hardcode
        filename = GENE_INFO;
        filename2 = GENE2ACCESSION;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // TODO can you pass arguments to JUnit tests so I can select this option at runtime?
        geneLoader.removeAll( map.values() );
        geneParser = null;
        geneLoader = null;
        map = null;
        filename = null;
    }

    // public void testParseFileInvalidFile() throws Exception {
    // try {
    // geneParser.parseFile( "badfilename" );
    // } catch ( IOException e ) {
    // e.printStackTrace();
    // assertEquals( null, null );
    // }
    // }
}
